package metrum.world;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.world.Block;

/** Directed overhead pipe. Only its endpoints occupy tiles. */
public class MicroPipe extends Block {
    public float range=96f, flow=12f/60f;
    public MicroPipe(String name){
        super(name);
        size=1; solid=true; update=true; configurable=true; hasLiquids=true;
        outputsLiquid=true; liquidCapacity=20f; sync=true; clipSize=range*2f+32f;
        buildType=MicroBuild::new;
        config(Integer.class,(MicroBuild b,Integer pos)->{
            if(pos==-1 || pos==b.target){b.target=-1;return;}
            Building other=Vars.world.build(pos);
            if(b.valid(other) && ((MicroBuild)other).target==-1 && !b.hasIncoming() && !((MicroBuild)other).hasIncoming() && b.liquids.currentAmount()<=0.001f && other.liquids.currentAmount()<=0.001f) b.target=pos;
        });
    }
    public class MicroBuild extends Building {
        public int target=-1;
        boolean valid(Building b){return b instanceof MicroBuild && b!=this && b.team==team && b.isValid() && within(b,range);}
        MicroBuild receiver(){Building b=target==-1?null:Vars.world.build(target);return valid(b)?(MicroBuild)b:null;}
        // Scan only when configuring, not on every simulation tick.
        boolean hasIncoming(){
            int r=(int)(range/Vars.tilesize)+1;
            for(int dx=-r;dx<=r;dx++)for(int dy=-r;dy<=r;dy++){
                Building b=Vars.world.build(tile.x+dx,tile.y+dy);
                if(valid(b)&&((MicroBuild)b).target==pos())return true;
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
        @Override public Integer config(){return target;}
        boolean compatible(Liquid l){return liquids.currentAmount()<0.001f || liquids.current()==l;}
        @Override public boolean acceptLiquid(Building from,Liquid l){
            if(from==null||from.team!=team||!compatible(l)||liquids.currentAmount()>=liquidCapacity-0.001f)return false;
            if(from instanceof MicroBuild)return target==-1 && ((MicroBuild)from).receiver()==this;
            return receiver()!=null && proximity.contains(from,true);
        }
        @Override public boolean canDumpLiquid(Building to,Liquid l){return target==-1 && !(to instanceof MicroBuild) && to.team==team;}
        @Override public void updateTile(){
            if(!enabled)return;
            MicroBuild out=receiver();
            Liquid l=liquids.current();
            if(l==null||liquids.get(l)<=0.001f)return;
            if(target!=-1){
                if(out==null||!out.enabled||!out.acceptLiquid(this,l))return;
                float amount=Math.min(flow*delta(),Math.min(liquids.get(l),liquidCapacity-out.liquids.currentAmount()));
                if(amount>0f){out.handleLiquid(this,l,amount);liquids.remove(l,amount);}
            }else dumpLiquid(l);
        }
        void drawLink(float opacity){
            MicroBuild other=receiver();if(other==null||opacity<=0f)return;
            float z=Draw.z();Draw.z(Layer.power);
            Draw.color(Color.darkGray);Draw.alpha(opacity);Lines.stroke(3f);Lines.line(x,y,other.x,other.y);
            Draw.color(liquids.currentAmount()>0.001f?liquids.current().color:Color.valueOf("69c88b"));Draw.alpha(opacity);Lines.stroke(1.3f);Lines.line(x,y,other.x,other.y);
            Draw.reset();Draw.z(z);
        }
        @Override public void draw(){super.draw();drawLink(MetrumVisibility.pipeOpacity());}
        @Override public void drawConfigure(){super.drawConfigure();Draw.color(Color.valueOf("69c88b"));Lines.stroke(1f);Lines.circle(x,y,range);Draw.reset();drawLink(1f);}
        @Override public void write(Writes w){super.write(w);w.i(target);}
        @Override public void read(Reads r,byte revision){super.read(r,revision);target=r.i();}
    }
}
