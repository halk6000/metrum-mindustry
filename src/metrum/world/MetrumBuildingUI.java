package metrum.world;

import arc.Core;
import arc.func.Prov;
import arc.func.Floatp;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import arc.util.Strings;

/** Composable live panels. No global HUD replacement or simulation changes. */
public final class MetrumBuildingUI {
    private MetrumBuildingUI(){}
    public interface Modules { void build(Table t); }
    public static String tr(String key){return Core.bundle.get("metrum.panel."+key);}
    public static void openButton(Table t, Building b, Modules modules){
        t.button(tr("open"), () -> {
            BaseDialog dialog=new BaseDialog(b.block.localizedName);
            dialog.addCloseButton();
            dialog.cont.pane(body -> {
                body.defaults().growX().pad(4f);
                body.table(header -> {
                    header.image(b.block.uiIcon).size(48f).padRight(10f);
                    header.add(tr("title")).color(Pal.accent).left();
                }).row();
                bar(body, () -> tr("health"), () -> Pal.health, b::healthf);
                modules.build(body);
            }).grow().maxWidth(440f).maxHeight(500f);
            dialog.cont.update(() -> {if(!b.isValid() || mindustry.Vars.state.isMenu() || mindustry.Vars.player==null || b.team!=mindustry.Vars.player.team())dialog.hide();});
            dialog.show();
        }).width(220f).height(48f);
    }
    /** Replaces the vanilla hover panel for opted-in buildings only. */
    public static void display(Table t, Building b, Modules modules){
        t.table(header -> {
            header.image(b.block.uiIcon).size(32f).padRight(6f);
            header.add(b.block.localizedName).wrap().width(205f).left();
        }).growX().left().row();
        // Match vanilla visibility: do not expose enemy building internals.
        if(mindustry.Vars.player==null || b.team!=mindustry.Vars.player.team())return;
        t.table(body -> {
            body.defaults().growX().pad(2f);
            bar(body, () -> tr("health"), () -> Pal.health, b::healthf);
            // a generator's power status is always 0; show what it actually produces instead
            if(b instanceof mindustry.world.blocks.power.PowerGenerator.GeneratorBuild){
                mindustry.world.blocks.power.PowerGenerator.GeneratorBuild g=(mindustry.world.blocks.power.PowerGenerator.GeneratorBuild)b;
                bar(body, () -> Core.bundle.format("bar.poweroutput", arc.util.Strings.fixed(g.getPowerProduction()*60f*g.timeScale(), 1)),
                    () -> Pal.powerBar, () -> arc.math.Mathf.clamp(g.productionEfficiency));
            }else if(b.power!=null)bar(body, () -> Core.bundle.get("bar.power"), () -> Pal.powerBar, () -> b.power.status);
            modules.build(body);
        }).growX().left().row();
    }
    public static void section(Table t,String key){t.add(tr(key)).color(Pal.accent).left().padTop(4f).row();}
    public static void value(Table t,String key,Prov<String> value){
        t.table(row -> {row.add(tr(key)).wrap().width(146f).left();row.label(() -> value.get()).width(78f).right().padLeft(4f);}).growX().row();
    }
    public static void bar(Table t,Prov<String> name,Prov<Color> color,Floatp fill){
        t.add(new Bar(() -> name.get(),color,()->Mathf.clamp(fill.get()))).growX().minWidth(240f).height(18f).row();
    }
    public static void chamber(Table t,String key,Prov<UnlockableContent> material,Floatp fill){
        bar(t,()->tr(key)+": "+(material.get()==null?"—":material.get().localizedName),
            ()->{UnlockableContent c=material.get();return c instanceof Item?((Item)c).color:c instanceof mindustry.type.Liquid?((mindustry.type.Liquid)c).color:Color.gray;},fill);
    }
    public static void booster(Table t,Item item,float multiplier,Floatp stock){
        if(item==null)return;
        section(t,"boost");
        t.table(row->{row.image(item.uiIcon).size(28f).padRight(8f);row.label(()->item.localizedName+" ×"+Strings.autoFixed(multiplier,1)+" | "+Strings.autoFixed(stock.get(),0)).wrap().width(195f).left();}).row();
    }
    public static void progress(Table t,Floatp progress){bar(t,()->tr("progress")+": "+Math.round(Mathf.clamp(progress.get())*100f)+"%",()->Pal.accent,progress);}
    public static void status(Table t,Building b,Prov<String> reason){
        t.label(() -> !b.enabled ? tr("disabled") : reason.get()).wrap().width(236f).left().row();
    }
    public static boolean noPower(Building b){return b.power!=null && b.block.consPower!=null && b.power.status<=0.001f;}
    public static String blocked(UnlockableContent c){return Core.bundle.format("metrum.panel.blocked",c.localizedName);}

}
