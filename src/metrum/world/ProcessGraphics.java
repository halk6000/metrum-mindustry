package metrum.world;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;

/** Texture resolution independent of the building's world dimensions. */
public class ProcessGraphics {
    private final TextureRegion[][][] frames = new TextureRegion[2][3][8];
    public void load(String name) {
        String[] parts = {"main", "out"}, states = {"solid", "liquid", "gas"};
        for (int p=0;p<2;p++) for(int s=0;s<3;s++) for(int f=0;f<8;f++)
            frames[p][s][f] = Core.atlas.find(name+"-"+parts[p]+"-"+states[s]+"-"+f);
    }
    public void draw(int part, UnlockableContent content, float phase, float x, float y, float w, float h) {
        if(content == null) return;
        int state=content instanceof Liquid ? (((Liquid)content).gas ? 2 : 1) : 0;
        TextureRegion tex=frames[part][state][Math.floorMod((int)phase,8)];
        if(tex == null || !tex.found()) return;
        Draw.color(content instanceof Item ? ((Item)content).color : ((Liquid)content).color);
        Draw.rect(tex,x,y,w,h);
        Draw.reset();
    }
}
