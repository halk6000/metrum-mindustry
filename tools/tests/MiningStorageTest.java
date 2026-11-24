package metrum.ai;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;
public class MiningStorageTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        Vars.content=new ContentLoader();
        Item ore=new Item("test-ore");
        Building core=new Building(){ @Override public int getMaximumAccepted(Item item){return 10;} };
        core.items=new ItemModule();
        check(SmartMining.canStoreMined(core,ore));
        core.items.add(ore,9); check(SmartMining.canStoreMined(core,ore));
        core.items.add(ore,1); check(!SmartMining.canStoreMined(core,ore));
        core.items.add(ore,5); check(!SmartMining.canStoreMined(core,ore));
        check(!SmartMining.canStoreMined(null,ore)); check(!SmartMining.canStoreMined(core,null));
        System.out.println("OK: empty, room remaining, full, overfull, missing core/item");
    }
}