package metrum.ai.planner;

import arc.Events;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.io.TypeIO;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/** Plans awaiting a replacement constructor, stored separately from the destroyed unit. */
final class AbandonedBuilds implements mindustry.io.SaveFileReader.CustomChunk {
    static class Saved {
        int position, team;
        Seq<BuildPlan> plans = new Seq<>();
    }
    static final Seq<Saved> pending = new Seq<>();
    static void init() {
        mindustry.io.SaveVersion.addCustomChunk("metrum-abandoned-builds-v1", new AbandonedBuilds());
        Events.on(mindustry.game.EventType.ResetEvent.class, e -> pending.clear());
    }
    static void restore() {
        for (Saved saved : pending) {
            Building b = Vars.world.build(saved.position);
            if (b instanceof CoreBuild && b.team.id == saved.team && b.team == Vars.state.rules.waveTeam)
                BasePlanner.abandonedPlans.put((CoreBuild)b, saved.plans);
        }
        pending.clear();
    }
    @Override
    public void write(java.io.DataOutput out) throws java.io.IOException {
        restore();
        Seq<CoreBuild> cores = new Seq<>();
        for (CoreBuild core : BasePlanner.abandonedPlans.keySet()) if (core.isValid()) cores.add(core);
        out.writeInt(cores.size);
        Writes writer = new Writes(out);
        for (CoreBuild core : cores) {
            out.writeInt(core.tile.pos()); out.writeInt(core.team.id);
            Seq<BuildPlan> plans = BasePlanner.abandonedPlans.get(core);
            out.writeInt(plans.size);
            for (BuildPlan plan : plans) TypeIO.writePlan(writer, plan);
        }
    }
    @Override
    public void read(java.io.DataInput in) throws java.io.IOException {
        pending.clear();
        Seq<Saved> decoded = new Seq<>();
        int count = in.readInt();
        if (count < 0) throw new java.io.IOException("Negative abandoned queue count");
        Reads reader = new Reads(in);
        for (int i=0; i<count; i++) {
            Saved saved = new Saved(); saved.position=in.readInt(); saved.team=in.readInt();
            int length=in.readInt();
            if (length < 0) throw new java.io.IOException("Negative abandoned plan count");
            for (int j=0; j<length; j++) {
                BuildPlan plan;
                try { plan=TypeIO.readPlan(reader); }
                catch (RuntimeException error) {
                    if (error.getCause() instanceof java.io.IOException) throw (java.io.IOException)error.getCause();
                    throw error;
                }
                if (plan != null && (plan.breaking || plan.block != null)) saved.plans.add(plan);
            }
            decoded.add(saved);
        }
        pending.addAll(decoded);
    }
}