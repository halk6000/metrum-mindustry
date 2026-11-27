package metrum.world;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import arc.scene.ui.layout.Table;
import arc.scene.ui.TextButton;
import arc.util.Log;
import mindustry.gen.Building;
import mindustry.ui.fragments.BlockInventoryFragment;
import java.lang.reflect.Field;
import mindustry.gen.Unit;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.storage.CoreBlock;

/** Client preferences only. Never sent to the server or written into a world save. */
public final class MetrumVisibility {
    private static final String drones="metrum-mask-drones", pipes="metrum-pipe-view";
    private MetrumVisibility(){}
    public static boolean masked(Unit unit){return !Vars.headless && Vars.player!=null && unit.isAdded() && unit.team==Vars.player.team() && Core.settings.getBool(drones,false);}
    public static float pipeOpacity(){int mode=Core.settings.getInt(pipes,0);return mode==2?0f:mode==1?0.2f:1f;}
    private static boolean initialized;
    private static Field inventoryTable, inventoryBuilding;
    private static TextButton visibilityButton;
    private static boolean inventoryHookFailed;

    public static void init(){
        if(Vars.headless || initialized)return;
        initialized=true;
        try{
            inventoryTable=BlockInventoryFragment.class.getDeclaredField("table");
            inventoryBuilding=BlockInventoryFragment.class.getDeclaredField("build");
            inventoryTable.setAccessible(true);
            inventoryBuilding.setAccessible(true);
            Events.run(Trigger.update,MetrumVisibility::attachInventoryButton);
        }catch(Exception e){
            Log.warn("Metrum: visibility inventory hook unavailable; use Settings > Metrum.");
        }
    }

    /** Append to the native inventory without replacing item withdrawal listeners. */
    private static void attachInventoryButton(){
        if(inventoryHookFailed || Vars.control==null || Vars.control.input==null)return;
        try{
            BlockInventoryFragment inv=Vars.control.input.inv;
            if(inv==null)return;
            Table table=(Table)inventoryTable.get(inv);
            Building build=(Building)inventoryBuilding.get(inv);
            boolean ownCore=Vars.state.isGame() && Vars.player!=null && build instanceof CoreBlock.CoreBuild
                && build.isValid() && build.team==Vars.player.team() && table.visible && build.items!=null && build.items.total()>0;
            if(!ownCore){
                // The native fragment owns its rows and removes them on hide/rebuild.
                visibilityButton=null;
                return;
            }
            if(visibilityButton!=null && visibilityButton.parent==table)return;
            // Native inventory rebuilds its children when item types change.
            // Find our existing button before adding another one.
            for(arc.scene.Element child:table.getChildren()){
                if(child instanceof TextButton && "metrum-visibility-button".equals(child.name)){
                    visibilityButton=(TextButton)child;
                    return;
                }
            }
            table.row();
            visibilityButton=table.button(Core.bundle.get("metrum.visibility.button", Core.bundle.getLocale().getLanguage().equals("pl") ? "Widoczność" : "Visibility"),MetrumVisibility::show)
                .colspan(3).width(144f).height(40f).pad(4f).get();
            visibilityButton.getLabel().setWrap(false);
            visibilityButton.name="metrum-visibility-button";
            table.row();
            table.invalidateHierarchy();
        }catch(Exception e){
            inventoryHookFailed=true;
            Log.warn("Metrum: visibility inventory hook disabled; use Settings > Metrum.");
        }
    }
    public static void show(){
        BaseDialog dialog=new BaseDialog(Core.bundle.get("metrum.visibility.title"));
        dialog.cont.button(Core.bundle.get("metrum.visibility.drones"),()->Core.settings.put(drones,!Core.settings.getBool(drones,false))).growX().height(48f).row();
        dialog.cont.label(()->Core.bundle.get(Core.settings.getBool(drones,false)?"metrum.visibility.dim":"metrum.visibility.full")).pad(8f).row();
        dialog.cont.button("MicroPipe",()->Core.settings.put(pipes,(Core.settings.getInt(pipes,0)+1)%3)).growX().height(48f).row();
        dialog.cont.label(()->Core.bundle.get(pipeOpacity()==0f?"metrum.visibility.hidden":pipeOpacity()<1f?"metrum.visibility.dim":"metrum.visibility.full")).pad(8f).row();
        // power lasers use the game's own "lasersopacity" setting (0-100): full -> 20% -> hidden
        dialog.cont.button(Core.bundle.get("metrum.visibility.lasers"),()->{
            int now=Core.settings.getInt("lasersopacity",100);
            Core.settings.put("lasersopacity",now>20?20:now>0?0:100);
        }).growX().height(48f).row();
        dialog.cont.label(()->{int v=Core.settings.getInt("lasersopacity",100);return Core.bundle.get(v==0?"metrum.visibility.hidden":v<100?"metrum.visibility.dim":"metrum.visibility.full");}).pad(8f).row();
        dialog.addCloseButton();dialog.show();
    }
}
