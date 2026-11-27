package metrum.world;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.content.Liquids;
import mindustry.entities.Fires;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import metrum.content.MetrumLiquids;

/** Optional oxidizer: one gas at a time, ozone takes priority. */
public class OxidizingFurnace extends GenericCrafter {
    public float gasPerSecond = 6f;
    public float riskInterval = 18f; // simulation ticks, not render frames
    public float explosionDamageFraction = 0.20f;
    /** damage to everything within 1.5x the furnace size */
    public float explosionSplashDamage = 60f;
    /** ozone boost and explosion odds per explosionInterval (with / without a powered radar) */
    public float ozoneSpeed = 1.60f, ozoneYield = 1.50f;
    public float explosionChance = 0.05f, radarExplosionChance = 0.02f, explosionInterval = 18f;
    /** runs only while burning ozone */
    public boolean ozoneOnly = false;
    /** "no major failure" event: after this long on ozone without an explosion the furnace becomes upgradeTo */
    public OxidizingFurnace upgradeTo;
    public float upgradeTicks = 80f * 60f;
    public OxidizingFurnace(String name){
        super(name);
        buildType = OxidizingBuild::new; configurable=true;
    }
    @Override public void setStats(){
        super.setStats();
        stats.add(Stat.booster, t -> {
            t.image(MetrumLiquids.oxygen.uiIcon).size(24f);
            t.add(" +45% / +30% | 10% fire").left().row();
            t.image(Liquids.ozone.uiIcon).size(24f);
            t.add(" +60% / +50% | 20% fire / 5% explosion").left().row();
            t.image(Blocks.radar.uiIcon).size(24f);
            t.add(" 5% → 2%").left().row();
        });
    }
    @Override public void setBars(){
        super.setBars();
        addBar("metrum-oxidizer", (OxidizingBuild b) -> new mindustry.ui.Bar(
            b::oxidizerLabel, b::oxidizerColor, b::oxidizerFill));
        addBar("metrum-radar-bhp", (OxidizingBuild b) -> new mindustry.ui.Bar(
            () -> "Radar BHP: " + (b.radarPowered() ? "ON" : "OFF"),
            () -> b.radarPowered() ? Color.green : Color.scarlet,
            () -> b.radarPowered() ? 1f : 0f));
    }
    public class OxidizingBuild extends GenericCrafterBuild {
        private Liquid burningGas;
        private Liquid displayedGas(){
            return burningGas != null ? burningGas : oxidizer(0f);
        }
        private String oxidizerLabel(){
            Liquid gas=displayedGas();
            if(gas==null)return "Spalanie: standardowe";
            return (burningGas!=null ? "Spalanie: " : "Gotowy: ") + gas.localizedName
                + (gas==Liquids.ozone ? " (+60% / +50%)" : " (+45% / +30%)");
        }
        private Color oxidizerColor(){
            Liquid gas=displayedGas();
            return gas==null ? Color.lightGray : gas.color;
        }
        private float oxidizerFill(){
            Liquid gas=displayedGas();
            return gas==null ? 0f : Mathf.clamp(liquids.get(gas)/liquidCapacity);
        }

        private float riskClock, explosionClock, safeClock;

        /** survived long enough on ozone: rebuild in place as the safe furnace, keeping its contents */
        void upgrade(){
            Tile t=tile;mindustry.game.Team tm=team;int rot=rotation;
            mindustry.world.modules.ItemModule oldItems=items.copy();
            arc.struct.Seq<mindustry.type.LiquidStack> oldLiquids=new arc.struct.Seq<>();
            liquids.each((l,a)->oldLiquids.add(new mindustry.type.LiquidStack(l,a)));
            Fx.upgradeCore.at(x,y,size);
            t.setNet(upgradeTo,tm,rot);
            if(t.build!=null){
                Building nb=t.build;
                oldItems.each((it,a)->nb.items.add(it,Math.min(a,nb.getMaximumAccepted(it))));
                for(mindustry.type.LiquidStack ls:oldLiquids)nb.liquids.add(ls.liquid,Math.min(ls.amount,nb.block.liquidCapacity));
            }
        }
        private Liquid oxidizer(float need){
            if(liquids.get(Liquids.ozone)>=need && liquids.get(Liquids.ozone)>0f)return Liquids.ozone;
            if(liquids.get(MetrumLiquids.oxygen)>=need && liquids.get(MetrumLiquids.oxygen)>0f)return MetrumLiquids.oxygen;
            return null;
        }
        // Include diagonal neighbours and a single free tile between footprints.
        private boolean radarPowered(){
            int left=tile.x-(size-1)/2, bottom=tile.y-(size-1)/2;
            for(int dx=-2;dx<=size+1;dx++)for(int dy=-2;dy<=size+1;dy++){
                if(dx>=0&&dx<size&&dy>=0&&dy<size)continue;
                mindustry.world.Tile near=Vars.world.tile(left+dx,bottom+dy);
                Building b=near==null?null:near.build;
                if(b!=null && b.team==team && b.block==Blocks.radar && b.enabled && b.power!=null && b.power.status>0.001f)return true;
            }
            return false;
        }
        private String diagnostic(){
            if(MetrumBuildingUI.noPower(this))return MetrumBuildingUI.tr("no-power");
            if(items.get(mindustry.content.Items.coal)<1)return MetrumBuildingUI.tr("no-coal");
            if(liquids.get(Liquids.water)<0.001f)return MetrumBuildingUI.tr("no-water");
            if(outputLiquids!=null)for(mindustry.type.LiquidStack out:outputLiquids){
                if(liquids.get(out.liquid)>=liquidCapacity-0.001f)return MetrumBuildingUI.blocked(out.liquid);
            }
            return efficiency>0f&&shouldConsume()?MetrumBuildingUI.tr("running"):MetrumBuildingUI.tr("low-supply");
        }
        @Override public void buildConfiguration(arc.scene.ui.layout.Table t){
            MetrumBuildingUI.openButton(t,this,this::technicalModules);
        }
        @Override public void display(arc.scene.ui.layout.Table t){
            MetrumBuildingUI.display(t,this,this::technicalModules);
        }
        private void technicalModules(arc.scene.ui.layout.Table body){
                MetrumBuildingUI.section(body,"combustion");
                MetrumBuildingUI.chamber(body,"oxidizer",this::displayedGas,this::oxidizerFill);
                MetrumBuildingUI.value(body,"speed",()->displayedGas()==Liquids.ozone?Math.round(ozoneSpeed*100)+"%":displayedGas()==MetrumLiquids.oxygen?"145%":"100%");
                MetrumBuildingUI.value(body,"yield",()->displayedGas()==Liquids.ozone?Math.round(ozoneYield*100)+"%":displayedGas()==MetrumLiquids.oxygen?"130%":"100%");
                MetrumBuildingUI.progress(body,()->progress);
                MetrumBuildingUI.section(body,"safety");
                MetrumBuildingUI.bar(body,()->"Radar BHP: "+(radarPowered()?"ON":"OFF"),()->radarPowered()?Color.green:Color.scarlet,()->radarPowered()?1f:0f);
                MetrumBuildingUI.value(body,"fire",()->displayedGas()==Liquids.ozone?"20%":displayedGas()==MetrumLiquids.oxygen?"10%":"0%");
                MetrumBuildingUI.value(body,"explosion",()->displayedGas()==Liquids.ozone?arc.util.Strings.autoFixed((radarPowered()?radarExplosionChance:explosionChance)*100f,1)+"%":"0%");
                MetrumBuildingUI.value(body,"interval",()->arc.util.Strings.autoFixed(explosionInterval,0));
                MetrumBuildingUI.section(body,"diagnostics");
                MetrumBuildingUI.status(body,this,this::diagnostic);
        }
        @Override public boolean acceptLiquid(Building source,Liquid liquid){
            if(liquid==Liquids.ozone || liquid==MetrumLiquids.oxygen)return liquids.get(liquid)<liquidCapacity;
            return super.acceptLiquid(source,liquid);
        }
        @Override public void updateTile(){
            burningGas=null;
            if(enabled && efficiency>0f && shouldConsume()){
                float base=edelta();
                float need=gasPerSecond/60f*base;
                Liquid gas=oxidizer(need);
                if(ozoneOnly && gas!=Liquids.ozone)gas=null;
                float speed=gas==Liquids.ozone?ozoneSpeed:gas==MetrumLiquids.oxygen?1.45f:1f;
                float yield=gas==Liquids.ozone?ozoneYield:gas==MetrumLiquids.oxygen?1.30f:1f;
                if(ozoneOnly && gas==null){warmup=Mathf.approachDelta(warmup,0f,warmupSpeed);totalProgress+=warmup*Time.delta;dumpOutputs();return;}
                // Limit the complete reaction to the remaining steam buffer.
                float inc=Math.min(base*speed,Math.max(0f,1f-progress)*craftTime);
                if(outputLiquids!=null)for(mindustry.type.LiquidStack out:outputLiquids){
                    if(out.amount>0f)inc=Math.min(inc,Math.max(0f,liquidCapacity-liquids.get(out.liquid))/(out.amount*yield));
                }
                if(inc>0f){
                    progress+=inc/craftTime;
                    warmup=Mathf.approachDelta(warmup, warmupTarget(), warmupSpeed);
                    if(outputLiquids!=null)for(mindustry.type.LiquidStack out:outputLiquids)handleLiquid(this,out.liquid,Math.min(out.amount*inc*yield,Math.max(0f,liquidCapacity-liquids.get(out.liquid))));
                    if(gas!=null){
                        burningGas=gas;
                        float elapsed=inc/speed;
                        liquids.remove(gas,gasPerSecond/60f*elapsed);
                        if(!Vars.net.client()){
                            riskClock+=elapsed;
                            float interval=Math.max(1f,riskInterval);
                            while(riskClock>=interval && health>0f){
                                riskClock-=interval;
                                boolean ozone=gas==Liquids.ozone;
                                if(Mathf.chance(ozone?0.20:0.10))metrum.entities.MetrumFires.create(tile);
                            }
                            if(gas==Liquids.ozone){
                                safeClock+=elapsed;
                                explosionClock+=elapsed;
                            }
                            while(explosionClock>=Math.max(1f,explosionInterval) && health>0f){
                                explosionClock-=Math.max(1f,explosionInterval);
                                if(Mathf.chance(radarPowered()?radarExplosionChance:explosionChance)){
                                    safeClock=0f;
                                    // a real, noticeable blast: big flash, sound, shake, fire and splash on neighbours
                                    Fx.massiveExplosion.at(x,y);
                                    mindustry.gen.Sounds.explosion.at(x,y,arc.math.Mathf.random(0.9f,1.1f));
                                    Effect.shake(3f,20f,x,y);
                                    // the splash is for the neighbours; the furnace itself loses only its own share below
                                    float before=health;
                                    mindustry.entities.Damage.damage(null,x,y,size*Vars.tilesize*1.5f,explosionSplashDamage);
                                    health=Math.max(health,before);
                                    metrum.entities.MetrumFires.create(tile);
                                    damage(maxHealth*explosionDamageFraction);
                                }
                            }
                            if(upgradeTo!=null && safeClock>=upgradeTicks && health>0f){upgrade();return;}
                        }
                    }else riskClock=0f;
                    if(wasVisible && Mathf.chanceDelta(updateEffectChance))updateEffect.at(x+Mathf.range(size*updateEffectSpread),y+Mathf.range(size*updateEffectSpread));
                }else warmup=Mathf.approachDelta(warmup,0f,warmupSpeed);
            }else warmup=Mathf.approachDelta(warmup,0f,warmupSpeed);
            totalProgress+=warmup*Time.delta;
            if(progress>=1f && health>0f)craft();
            if(health>0f)dumpOutputs();
        }
        @Override public byte version(){return 2;}
        @Override public void write(Writes w){super.write(w);w.f(riskClock);w.f(explosionClock);w.f(safeClock);}
        @Override public void read(Reads r,byte revision){
            super.read(r,revision);
            riskClock=revision>=1?r.f():0f;
            if(revision>=2){explosionClock=r.f();safeClock=r.f();}
        }
    }
}
