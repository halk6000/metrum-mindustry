package metrum.world;
import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.ctype.UnlockableContent;
import mindustry.ui.Styles;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.world.Block;

/** Shared, native content-info technical cards. */
public final class TechnicalInfoUI {
    private TechnicalInfoUI(){}
    public static void header(Table t, Block block){
        t.row(); t.add(Core.bundle.get("metrum.tech.title")).color(mindustry.graphics.Pal.accent).left().padTop(12).row();
        t.image(block.uiIcon).size(128).pad(8).row();
    }
    public static void recipe(Table t, UnlockableContent input, int amount, UnlockableContent[] outputs, int[] counts, Item booster, float multiplier){
        t.table(card -> {
            final boolean[] boosted={false};
            card.table(flow -> {
                material(flow,input,amount,1f);
                flow.add(" → ").pad(5);
                for(int i=0;i<outputs.length;i++){
                    final int j=i;
                    if(i>0)flow.add(" + ");
                    flow.table(col -> {
                        col.button(new arc.scene.style.TextureRegionDrawable(outputs[j].uiIcon), Styles.cleari, () -> Vars.ui.content.show(outputs[j])).size(42).row();
                        col.add(outputs[j].localizedName).wrap().width(90).row();
                        col.label(() -> "×"+arc.util.Strings.autoFixed(counts[j]*(boosted[0]?multiplier:1f),1));
                    });
                }
            }).pad(6).row();
            if(booster!=null) card.button(Core.bundle.get("metrum.tech.boost")+" ×"+multiplier, Styles.togglet, () -> boosted[0]=!boosted[0]).pad(5).row();
        }).left().pad(4).row();
    }
    private static void material(Table t, UnlockableContent c,int amount,float scale){
        t.table(col -> {
            col.button(new arc.scene.style.TextureRegionDrawable(c.uiIcon), Styles.cleari, () -> Vars.ui.content.show(c)).size(42).row();
            col.add(c.localizedName).wrap().width(90).row(); col.add("×"+amount);
        });
    }
}
