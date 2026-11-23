package metrum.ai.planner;
import java.io.*;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.type.Item;
import mindustry.type.ItemStack;

public class EconomyGoalsTest {
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    static DataInputStream input(byte[] bytes) { return new DataInputStream(new ByteArrayInputStream(bytes)); }
    public static void main(String[] args) throws Exception {
        Vars.content = new ContentLoader();
        Item iron = new Item("test-iron"), silicon = new Item("test-silicon");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(1); out.writeInt(42); out.writeInt(1);
        EconomyGoals.writeGoal(out, new ItemStack(iron, 7));
        out.writeInt(3);
        EconomyGoals.writeGoal(out, new ItemStack(silicon, 13));
        out.writeUTF("removed-item"); out.writeInt(5);
        EconomyGoals.writeGoal(out, new ItemStack(iron, 21));
        new EconomyGoals().read(input(bytes.toByteArray()));
        EconomyGoals.Saved saved = EconomyGoals.pending.first();
        check(saved.position == 42 && saved.team == 1 && saved.current.item == iron && saved.current.amount == 7);
        check(saved.parents.size() == 2);
        check(saved.parents.pop().item == silicon && saved.parents.pop().amount == 21);
        byte[] truncated = java.util.Arrays.copyOf(bytes.toByteArray(), bytes.size() - 1);
        try { new EconomyGoals().read(input(truncated)); throw new AssertionError(); }
        catch (EOFException expected) { check(EconomyGoals.pending.isEmpty()); }
        bytes.reset(); out.writeInt(-1);
        try { new EconomyGoals().read(input(bytes.toByteArray())); throw new AssertionError(); }
        catch (IOException expected) { check(EconomyGoals.pending.isEmpty()); }
        bytes.reset(); EconomyGoals.writeGoal(out, null);
        check(EconomyGoals.readGoal(input(bytes.toByteArray())) == null);
        System.out.println("OK: goals, parent order, unknown item, truncated input, negative count, empty goal");
    }
}