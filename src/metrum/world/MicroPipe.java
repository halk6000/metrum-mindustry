package metrum.world;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.world.Block;

/**
 * Directed overhead pipe. Only its endpoints occupy tiles.
 * Higher tiers can link to several pipes and also work as a relay: what another pipe sends in
 * is passed on to all of its own links, sharing the tier's flow.
 */
public class MicroPipe extends Block {
    public float range=96f, flow=12f/60f;
    /** outgoing links; above 1 the pipe can also relay what another pipe sends in */
    public int maxLinks=1;
    public MicroPipe(String name){
        super(name);
        size=1; solid=true; update=true; configurable=true; hasLiquids=true;
        outputsLiquid=true; liquidCapacity=20f; sync=true; clipSize=range*2f+32f;
        buildType=MicroBuild::new;
        config(Integer.class,(MicroBuild b,Integer pos)->{
            if(pos==-1){b.targets.clear();return;}
            if(b.targets.contains(pos)){b.targets.removeValue(pos);return;}
            Building other=Vars.world.build(pos);
            if(b.canLink(other))b.targets.add(pos);
        });
    }
    public class MicroBuild extends Building {
        public final IntSeq targets=new IntSeq();
        private int nextOut;

        boolean valid(Building b){return b instanceof MicroBuild && b!=this && b.team==team && b.isValid() && within(b,range);}
        boolean relay(){return maxLinks>1;}
        MicroBuild receiver(int i){Building b=Vars.world.build(targets.get(i));return valid(b)?(MicroBuild)b:null;}
        boolean sendsTo(MicroBuild other){return targets.contains(other.pos());}

        public boolean canLink(Building other){
            if(!valid(other) || targets.size>=maxLinks)return false;
            MicroBuild o=(MicroBuild)other;
            if(o.sendsTo(this))return false; // no two-way loops
            if(!relay() || !o.relay()){
                // classic single pipes: only an empty, free endpoint, as before
                return o.targets.isEmpty() && !o.hasIncoming() && liquids.currentAmount()<=0.001f && o.liquids.currentAmount()<=0.001f
                    && (relay() || !hasIncoming());
            }
            return true;
        }
        // Scan only when configuring, not on every simulation tick.
        boolean hasIncoming(){
            int r=(int)(range/Vars.tilesize)+1;
            for(int dx=-r;dx<=r;dx++)for(int dy=-r;dy<=r;dy++){
                Building b=Vars.world.build(tile.x+dx,tile.y+dy);
                if(valid(b)&&((MicroBuild)b).sendsTo(this))return true;
            }
            return false;
        }
        @Override public boolean onConfigureBuildTapped(Building other){
            if(other==this){configure(-1);return false;}
            if(valid(other)){configure(other.pos());return false;}
            return true;
        }
        @Override public void buildConfiguration(Table t){
            t.button(Core.bundle.get("metrum.micro.unlink"),()->configure(-1)).size(160f,44f);
        }
        @Override public Object config(){return targets.toArray();}
        @Override public void configured(mindustry.gen.Unit builder,Object value){
            if(value instanceof int[]){targets.clear();for(int p:(int[])value)if(targets.size<maxLinks)targets.add(p);}
            else super.configured(builder,value);
        }
        boolean compatible(Liquid l){return liquids.currentAmount()<0.001f || liquids.current()==l;}
        @Override public boolean acceptLiquid(Building from,Liquid l){
            if(from==null||from.team!=team||!compatible(l)||liquids.currentAmount()>=liquidCapacity-0.001f)return false;
            // a relay passes on what another pipe sends in; a classic pipe only accepts at its free end
            if(from instanceof MicroBuild)return ((MicroBuild)from).sendsTo(this) && (targets.isEmpty() || relay());
            return !targets.isEmpty() && proximity.contains(from,true);
        }
        @Override public boolean canDumpLiquid(Building to,Liquid l){return targets.isEmpty() && !(to instanceof MicroBuild) && to.team==team;}
        @Override public void updateTile(){
            if(!enabled)return;
            Liquid l=liquids.current();
            if(l==null||liquids.get(l)<=0.001f)return;
            if(targets.isEmpty()){dumpLiquid(l);return;}
            // the tier's flow is shared by its links, taking turns so none starves
            float budget=Math.min(flow*delta(),liquids.get(l));
            int n=targets.size;
            for(int k=0;k<n && budget>0.0001f;k++){
                int i=(nextOut+k)%n;
                MicroBuild out=receiver(i);
                if(out==null||!out.enabled||!out.acceptLiquid(this,l))continue;
                float share=Math.min(budget/(n-k),out.block.liquidCapacity-out.liquids.currentAmount());
                if(share<=0f)continue;
                out.handleLiquid(this,l,share);liquids.remove(l,share);budget-=share;
            }
            nextOut=(nextOut+1)%Math.max(1,n);
        }
        void drawLink(float opacity){
            if(opacity<=0f)return;
            float z=Draw.z();Draw.z(Layer.power);
            for(int i=0;i<targets.size;i++){
                MicroBuild other=receiver(i);if(other==null)continue;
                Draw.color(Color.darkGray);Draw.alpha(opacity);Lines.stroke(3f);Lines.line(x,y,other.x,other.y);
                Draw.color(liquids.currentAmount()>0.001f?liquids.current().color:Color.valueOf("69c88b"));Draw.alpha(opacity);Lines.stroke(1.3f);Lines.line(x,y,other.x,other.y);
            }
            Draw.reset();Draw.z(z);
        }
        @Override public void draw(){super.draw();drawLink(MetrumVisibility.pipeOpacity());}
        @Override public void drawConfigure(){super.drawConfigure();Draw.color(Color.valueOf("69c88b"));Lines.stroke(1f);Lines.circle(x,y,range);Draw.reset();drawLink(1f);}
        @Override public byte version(){return 1;}
        @Override public void write(Writes w){
            super.write(w);
            w.i(targets.size);
            for(int i=0;i<targets.size;i++)w.i(targets.get(i));
        }
        @Override public void read(Reads r,byte revision){
            super.read(r,revision);
            targets.clear();
            if(revision<1){int t=r.i();if(t!=-1)targets.add(t);return;} // single-link saves
            int n=r.i();
            for(int i=0;i<n;i++)targets.add(r.i());
        }
    }
}
