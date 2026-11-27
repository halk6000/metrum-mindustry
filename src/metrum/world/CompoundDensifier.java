package metrum.world;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class CompoundDensifier extends Block {
    public static class Recipe {
        final Liquid input; final int amount; final UnlockableContent output; final int yield;
        Recipe(Liquid i,int a,UnlockableContent o,int y){input=i;amount=a;output=o;yield=y;}
    }
    public final Seq<Recipe> recipes=new Seq<>();
    public Item boostItem;
    public float boostMultiplier=2.5f, craftTime=60f;
    private final TextureRegion[] compressor=new TextureRegion[8];
    private TextureRegion body;
    private final ProcessGraphics graphics=new ProcessGraphics();
    public CompoundDensifier(String name){super(name);update=solid=hasItems=hasLiquids=hasPower=outputsLiquid=true;itemCapacity=20;liquidCapacity=40;unloadable=false;buildType=DensifierBuild::new;configurable=true;}
    public CompoundDensifier recipe(Liquid i,int a,UnlockableContent o,int y){if(i==o||a<=0||y<=0||!(o instanceof Liquid||o instanceof Item))throw new IllegalArgumentException();recipes.add(new Recipe(i,a,o,y));return this;}
    @Override public void load(){super.load();body=Core.atlas.find(name+"-body");graphics.load(name);for(int i=0;i<8;i++)compressor[i]=Core.atlas.find(name+"-compressor-"+i);}
    @Override public void setStats(){super.setStats();stats.add(Stat.productionTime,craftTime/60f,StatUnit.seconds);stats.add(Stat.input,t->{for(Recipe r:recipes){t.image(r.input.uiIcon).size(24);t.add(" ×"+r.amount+" → ");t.image(r.output.uiIcon).size(24);t.add(" ×"+r.yield).row();}});if(boostItem!=null)stats.add(Stat.booster,t->{t.image(boostItem.uiIcon).size(24);t.add(" ×"+boostMultiplier);});}
    @Override public void displayExtra(Table t){super.displayExtra(t);TechnicalInfoUI.header(t,this);for(Recipe r:recipes)TechnicalInfoUI.recipe(t,r.input,r.amount,new UnlockableContent[]{r.output},new int[]{r.yield},boostItem,boostMultiplier);}
    @Override public void setBars(){super.setBars();for(int k=0;k<2;k++){final int slot=k;addBar("densifier-"+k,(DensifierBuild b)->new Bar(()->Core.bundle.format(slot==0?"metrum.densifier.in":"metrum.densifier.out",b.material(slot)==null?"-":b.material(slot).localizedName),()->b.color(slot),()->b.fill(slot)));}}
    /** the HD sprite is 256 px; draw the placement ghost at the real 2x2 footprint, not at sprite size */
    @Override public void drawPlanRegion(mindustry.entities.units.BuildPlan plan, arc.util.Eachable<mindustry.entities.units.BuildPlan> list){
        float w=size*mindustry.Vars.tilesize;
        Draw.rect(region,plan.drawx(),plan.drawy(),w,w);
    }
    public class DensifierBuild extends Building {
        private int active=-1,last=-1; private float progress,phase; private float[] carry;
        private float[] carry(){if(carry==null)carry=new float[recipes.size];return carry;}
        private boolean boost(){return boostItem!=null&&items.get(boostItem)>0;}
        private float qty(UnlockableContent c){return c instanceof Item?items.get((Item)c):liquids.get((Liquid)c);}
        private boolean input(Liquid l){for(Recipe r:recipes)if(r.input==l)return true;return false;}
        private boolean pending(){return last>=0&&last<recipes.size&&qty(recipes.get(last).output)>0.0001f;}
        private float batchYield(int i){Recipe r=recipes.get(i);float v=BoostedCrafter.productYield(r.yield,boost(),boostMultiplier);return r.output instanceof Item?(float)Math.floor(v+carry()[i]+0.00001f):v;}
        private boolean ready(int i){if(i<0||i>=recipes.size)return false;Recipe r=recipes.get(i);return qty(r.input)>=r.amount&&qty(r.output)+batchYield(i)<=(r.output instanceof Item?itemCapacity:liquidCapacity);}
        private int choose(){if(pending())return ready(last)?last:-1;if(ready(active))return active;for(int i=0;i<recipes.size;i++)if(ready(i))return i;return -1;}
        private String diagnostic(){
            if(MetrumBuildingUI.noPower(this))return MetrumBuildingUI.tr("no-power");
            if(choose()>=0)return efficiency>0f?MetrumBuildingUI.tr("running"):MetrumBuildingUI.tr("low-supply");
            if(pending()){
                Recipe r=recipes.get(last);
                if(qty(r.output)+batchYield(last)>(r.output instanceof Item?itemCapacity:liquidCapacity))return MetrumBuildingUI.blocked(r.output);
                for(int i=0;i<recipes.size;i++)if(i!=last&&ready(i))return MetrumBuildingUI.tr("drain-output");
            }
            for(int i=0;i<recipes.size;i++){
                Recipe r=recipes.get(i);
                if(qty(r.input)>=r.amount&&qty(r.output)+batchYield(i)>(r.output instanceof Item?itemCapacity:liquidCapacity))return MetrumBuildingUI.blocked(r.output);
            }
            return MetrumBuildingUI.tr("no-input");
        }
        @Override public void buildConfiguration(Table t){
            MetrumBuildingUI.openButton(t,this,this::technicalModules);
        }
        @Override public void display(arc.scene.ui.layout.Table t){
            MetrumBuildingUI.display(t,this,this::technicalModules);
        }
        private void technicalModules(arc.scene.ui.layout.Table body){
                MetrumBuildingUI.section(body,"chambers");
                MetrumBuildingUI.chamber(body,"input",()->material(0),()->fill(0));
                MetrumBuildingUI.chamber(body,"output",()->material(1),()->fill(1));
                MetrumBuildingUI.progress(body,()->progress);
                MetrumBuildingUI.booster(body,boostItem,boostMultiplier,()->boostItem==null?0:items.get(boostItem));
                MetrumBuildingUI.section(body,"diagnostics");
                MetrumBuildingUI.status(body,this,this::diagnostic);
        }
        @Override public boolean shouldConsume(){return enabled&&choose()>=0;}
        @Override public boolean acceptItem(Building source,Item i){return i==boostItem&&items.get(i)<itemCapacity;}
        @Override public boolean acceptLiquid(Building source,Liquid l){return input(l)&&(!pending()||recipes.get(last).output!=l)&&liquids.get(l)<liquidCapacity;}
        @Override public boolean canDump(Building to,Item i){return last>=0&&recipes.get(last).output==i;}
        @Override public boolean canDumpLiquid(Building to,Liquid l){return last>=0&&recipes.get(last).output==l&&to.liquids!=null&&(to.liquids.current()==l||to.liquids.currentAmount()<0.001f);}
        @Override public void updateTile(){
            if(pending()){UnlockableContent o=recipes.get(last).output;if(o instanceof Item)dump((Item)o);else dumpLiquid((Liquid)o);}
            int i=choose();if(i!=active){active=i;progress=0;}
            if(!enabled||efficiency<=0||i<0)return;
            phase=(phase+edelta()*8f/60f)%8f;progress+=edelta()/craftTime;if(progress<1||!ready(i))return;
            Recipe r=recipes.get(i);boolean b=boost();float raw=BoostedCrafter.productYield(r.yield,b,boostMultiplier), amount=batchYield(i);
            liquids.remove(r.input,r.amount);if(b)items.remove(boostItem,1);
            if(r.output instanceof Item){carry()[i]=Math.max(0,raw+carry()[i]-amount);items.add((Item)r.output,(int)amount);}else liquids.add((Liquid)r.output,amount);
            last=i;progress=0;active=-1;
        }
        private int shown(){if(active>=0)return active;if(last>=0)return last;for(int i=0;i<recipes.size;i++)if(qty(recipes.get(i).input)>0)return i;return -1;}
        UnlockableContent material(int slot){int i=shown();return i<0?null:slot==0?recipes.get(i).input:recipes.get(i).output;}
        Color color(int slot){UnlockableContent c=material(slot);return c instanceof Item?((Item)c).color:c instanceof Liquid?((Liquid)c).color:Color.gray;}
        float fill(int slot){UnlockableContent c=material(slot);return c==null?0:Math.min(1,qty(c)/(c instanceof Item?itemCapacity:liquidCapacity));}
        @Override public void draw(){float w=size*8f;Draw.rect(body.found()?body:region,x,y,w,w);if(material(0)!=null&&(active>=0||pending())){graphics.draw(0,material(0),phase,x,y+w*.235f,w*.37f,w*.31f);graphics.draw(1,material(1),phase,x,y-w*.26f,w*.37f,w*.31f);if(active>=0&&efficiency>0){TextureRegion r=compressor[(int)phase%8];if(r.found())Draw.rect(r,x,y-w*.02f,w*.49f,w*.30f);}}Draw.reset();}
        @Override public byte version(){return 1;}
        @Override public void write(Writes w){super.write(w);w.i(active);w.i(last);w.f(progress);w.i(recipes.size);for(float v:carry())w.f(v);}
        @Override public void read(Reads r,byte revision){super.read(r,revision);active=r.i();last=r.i();progress=r.f();int n=r.i();for(int i=0;i<n;i++){float v=r.f();if(i<recipes.size)carry()[i]=v;}if(active<0||active>=recipes.size){active=-1;progress=0;}if(last<0||last>=recipes.size)last=-1;}
    }
}
