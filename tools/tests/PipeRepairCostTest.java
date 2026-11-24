package metrum.ai.planner;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.modules.ItemModule;
public class PipeRepairCostTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        Vars.content=new ContentLoader(); Vars.state=new GameState();
        Item a=new Item("a"), b=new Item("b");
        Building core=Building.create(); core.items=new ItemModule();
        ItemStack[] cost={new ItemStack(a,2),new ItemStack(b,1)};
        core.items.add(a,6); core.items.add(b,2);
        check(!BasePlanner.payBatch(core,cost,3));
        check(core.items.get(a)==6 && core.items.get(b)==2);
        core.items.add(b,1); check(BasePlanner.payBatch(core,cost,3));
        check(core.items.get(a)==0 && core.items.get(b)==0);
        Vars.state.rules.buildCostMultiplier=1.5f;
        core.items.add(a,9); core.items.add(b,6);
        check(BasePlanner.payBatch(core,cost,3));
        check(core.items.get(a)==0 && core.items.get(b)==0);
        check(BasePlanner.payBatch(core,cost,0));
        Vars.state.rules.buildCostMultiplier=1f;
        core.items.add(a,3);
        check(!BasePlanner.payBatch(core,new ItemStack[]{new ItemStack(a,2),new ItemStack(a,2)},1));
        check(core.items.get(a)==3);
        System.out.println("OK: atomic failure, exact budget, multiplier rounding, zero quantity, repeated item");
    }
}