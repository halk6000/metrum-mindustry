package metrum.ai.planner;

import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/** Persists economic objectives by core position and content name, independently of runtime IDs. */
final class EconomyGoals implements mindustry.io.SaveFileReader.CustomChunk {
    static class Saved {
        int position, team;
        ItemStack current;
        java.util.Deque<ItemStack> parents = new java.util.ArrayDeque<>();
    }
    static final Seq<Saved> pending = new Seq<>();

    static void init() {
        mindustry.io.SaveVersion.addCustomChunk("metrum-economy-goals-v1", new EconomyGoals());
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> pending.clear());
    }

    static void restore() {
        for (Saved saved : pending) {
            Building building = Vars.world.build(saved.position);
            if (!(building instanceof CoreBuild) || building.team.id != saved.team) continue;
            CoreBuild core = (CoreBuild) building;
            if (saved.current != null) BasePlanner.miningDemand.put(core, saved.current);
            if (!saved.parents.isEmpty()) BasePlanner.deferredDemand.put(core, saved.parents);
        }
        pending.clear();
    }

    static void writeGoal(java.io.DataOutput out, ItemStack goal) throws java.io.IOException {
        out.writeUTF(goal == null ? "" : goal.item.name);
        out.writeInt(goal == null ? 0 : goal.amount);
    }

    static ItemStack readGoal(java.io.DataInput in) throws java.io.IOException {
        Item item = Vars.content.item(in.readUTF());
        int amount = in.readInt();
        return item == null || amount <= 0 ? null : new ItemStack(item, amount);
    }

    @Override
    public void write(java.io.DataOutput out) throws java.io.IOException {
        restore();
        java.util.Set<CoreBuild> cores = new java.util.HashSet<>(BasePlanner.miningDemand.keySet());
        cores.addAll(BasePlanner.deferredDemand.keySet());
        cores.removeIf(core -> !core.isValid());
        out.writeInt(cores.size());
        for (CoreBuild core : cores) {
            out.writeInt(core.tile.pos());
            out.writeInt(core.team.id);
            writeGoal(out, BasePlanner.miningDemand.get(core));
            java.util.Deque<ItemStack> parents = BasePlanner.deferredDemand.get(core);
            out.writeInt(parents == null ? 0 : parents.size());
            if (parents != null) for (ItemStack goal : parents) writeGoal(out, goal);
        }
    }

    @Override
    public void read(java.io.DataInput in) throws java.io.IOException {
        pending.clear();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            Saved saved = new Saved();
            saved.position = in.readInt();
            saved.team = in.readInt();
            saved.current = readGoal(in);
            int parents = in.readInt();
            for (int j = 0; j < parents; j++) {
                ItemStack goal = readGoal(in);
                if (goal != null) saved.parents.addLast(goal);
            }
            pending.add(saved);
        }
    }
}