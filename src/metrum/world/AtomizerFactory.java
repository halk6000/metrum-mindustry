package metrum.world;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/** Four physical input/output pairs, each with an independent buffer and cycle. */
public class AtomizerFactory extends Block {
    public final ObjectMap<Item, Item> conversions = new ObjectMap<>();
    public final int capacity = 20;
    public final float craftTime = 30f;
    private TextureRegion bottom, top, ring, flash, chamber;
    // West upper/lower and north left/right -> east upper/lower and south left/right.
    private static final int[] ix = {-2,-2,-1,1}, iy = {1,-1,2,2};
    private static final int[] ox = {2,2,-1,1}, oy = {1,-1,-2,-2};
    private static final int[] vx = {-1,-1,1,1}, vy = {1,-1,1,-1};

    public AtomizerFactory(String name) {
        super(name);
        update = solid = hasItems = hasPower = true;
        itemCapacity = 80;
        unloadable = false;
        buildType = FactoryBuild::new;
    }

    @Override public void load() {
        super.load();
        bottom = Core.atlas.find(name + "-bottom");
        top = Core.atlas.find(name + "-top");
        ring = Core.atlas.find(name + "-ring");
        flash = Core.atlas.find(name + "-flash");
        chamber = Core.atlas.find(name + "-chamber");
    }

    @Override public void setStats() {
        super.setStats();
        stats.add(Stat.productionTime, 0.5f, StatUnit.seconds);
        stats.add(Stat.input, t -> {
            for (ObjectMap.Entry<Item, Item> e : conversions) {
                t.image(e.key.uiIcon).size(24f); t.add(" x2 -> ");
                t.image(e.value.uiIcon).size(24f); t.row();
            }
        });
    }

    public class FactoryBuild extends Building {
        final Item[] material = new Item[4];
        final int[] input = new int[4], output = new int[4];
        final float[] progress = new float[4], glow = new float[4];

        int port(Building source) {
            if (source == null || source == this || source.team != team) return -1;
            for (int i=0;i<4;i++) {
                Tile t = Vars.world.tile(tile.x + ix[i], tile.y + iy[i]);
                if (t != null && t.build == source) return i;
            }
            return -1;
        }
        boolean ready(int i) { return material[i] != null && input[i] >= 2 && output[i] < capacity; }
        public int activeSlots() {
            int n=0; if (enabled) for(int i=0;i<4;i++) if(ready(i)) n++;
            return n;
        }
        @Override public boolean shouldConsume() { return activeSlots() > 0; }
        @Override public boolean acceptItem(Building source, Item item) {
            int i=port(source);
            return i>=0 && conversions.containsKey(item) && input[i]<capacity && (material[i]==null || material[i]==item);
        }
        @Override public void handleItem(Building source, Item item) {
            if (!acceptItem(source,item)) return;
            int i=port(source); material[i]=item; input[i]++; items.add(item,1);
        }
        // Bulk deliveries have no physical input port; require conveyors instead.
        @Override public int acceptStack(Item item, int amount, mindustry.gen.Teamc source) { return 0; }
        @Override public void updateTile() {
            for(int i=0;i<4;i++) {
                glow[i]=Math.max(0f,glow[i]-delta()/12f);
                if(ready(i) && efficiency>0f) {
                    progress[i]+=edelta()/craftTime;
                    while(progress[i]>=1f && ready(i)) {
                        progress[i]-=1f; input[i]-=2; output[i]++;
                        items.remove(material[i],2); items.add(conversions.get(material[i]),1);
                        glow[i]=1f; Fx.smeltsmoke.at(x+vx[i]*5.5f,y+vy[i]*5.5f);
                    }
                }
                if(material[i]!=null && output[i]>0) {
                    Tile t=Vars.world.tile(tile.x+ox[i],tile.y+oy[i]);
                    Building dest=t==null?null:t.build;
                    Item product=conversions.get(material[i]);
                    if(dest!=null && dest.team==team && dest.acceptItem(this,product)) {
                        dest.handleItem(this,product); output[i]--; items.remove(product,1);
                    }
                }
                if(input[i]==0 && output[i]==0) { material[i]=null; progress[i]=0f; }
            }
        }
        @Override public void draw() {
            if(!bottom.found() || !top.found()) { super.draw(); return; }
            Draw.rect(bottom,x,y,24f,24f);
            for(int i=0;i<4;i++) {
                float px=x+vx[i]*5.5f, py=y+vy[i]*5.5f;
                if(chamber.found()) Draw.rect(chamber,px,py,10f,10f);
                if(material[i]!=null) {
                    float p=Mathf.clamp(progress[i]);
                    if(ring.found()) Draw.rect(ring,px,py,8f,8f,p*360f);
                    Item shown=glow[i]>0.35f?conversions.get(material[i]):material[i];
                    float sz=glow[i]>0.35f?4f:5f*(1f-0.7f*p);
                    Draw.rect(shown.uiIcon,px,py,sz,sz);
                }
                if(glow[i]>0f && flash.found()) { Draw.alpha(glow[i]); Draw.rect(flash,px,py,9f,9f); Draw.reset(); }
            }
            Draw.rect(top,x,y,24f,24f); Draw.reset();
        }
        @Override public void write(Writes w) {
            super.write(w);
            for(int i=0;i<4;i++) { w.s(material[i]==null?-1:material[i].id); w.i(input[i]); w.i(output[i]); w.f(progress[i]); }
        }
        @Override public void read(Reads r, byte revision) {
            super.read(r,revision);
            for(int i=0;i<4;i++) { int id=r.s(); material[i]=id<0?null:Vars.content.item(id); input[i]=r.i(); output[i]=r.i(); progress[i]=r.f(); }
        }
    }
}
