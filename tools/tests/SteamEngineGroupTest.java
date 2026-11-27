import metrum.world.SteamEngine;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Team;

public class SteamEngineGroupTest {
    static SteamEngine.SteamEngineBuild engine(SteamEngine block, Team team) {
        SteamEngine.SteamEngineBuild b = block.new SteamEngineBuild();
        b.block = block;
        b.team = team;
        return b;
    }
    static void check(boolean condition) {
        if (!condition) throw new AssertionError("Incorrect steam engine group");
    }
    static boolean near(float x, float y) { return Math.abs(x - y) < 0.001f; }
    static float power(SteamEngine.SteamEngineBuild b) {
        b.warmup = 0.5f; // below the steam particle threshold
        ((SteamEngine) b.block).warmupSpeed = 0f;
        b.updateTile();
        b.warmup = 1f;
        return b.productionEfficiency * 2f;
    }
    /** steam drawn by one engine in one tick, with 1 unit in its own tank and others empty */
    static float used(SteamEngine.SteamEngineBuild b, mindustry.type.Liquid steam) {
        for (SteamEngine.SteamEngineBuild m : b.connectedEngines()) m.liquids.clear();
        b.liquids.add(steam, 1f);
        new metrum.world.SharedSteamConsumer(steam, 0.2f).update(b);
        return 1f - b.liquids.get(steam);
    }
    /** warmup reached after a long run; efficiency alternates between the two values */
    static float settled(SteamEngine.SteamEngineBuild b, float even, float odd) {
        ((SteamEngine) b.block).warmupSpeed = 0.004f;
        for (int i = 0; i < 2000; i++) {
            b.efficiency = i % 2 == 0 ? even : odd;
            if (b.warmup > 0.49f) b.warmup = 0.49f; // stay below the particle threshold
            b.updateTile();
        }
        return b.warmup;
    }

    static void checkSingleEngineRatedPower() {
        Vars.headless = true;
        Vars.content.createBaseContent();
        new metrum.MetrumMod().loadContent();
        checkAtomizerFactory();
        checkResearchParent(metrum.content.MetrumBlocks.hullGunTurret, metrum.content.MetrumItems.weaponMachineGun);
        checkResearchParent(metrum.content.MetrumBlocks.hullMissileTurret, metrum.content.MetrumItems.weaponMissiles);
        checkResearchParent(metrum.content.MetrumPower.fusionReactor, metrum.content.MetrumItems.quarkMatter);
        checkResearchParent(metrum.content.MetrumPower.reactionControl, metrum.content.MetrumPower.fusionReactor);
        SteamEngine block = metrum.content.MetrumPower.steamEngine;
        checkResearchParent(block, metrum.content.MetrumProduction.steamFurnace);
        block.init();
        SteamEngine.SteamEngineBuild single = engine(block, Team.sharded);
        single.liquids = new mindustry.world.modules.LiquidModule();
        for (int tick = 0; tick < 600; tick++) {
            Vars.state.updateId++;
            single.liquids.set(metrum.content.MetrumLiquids.steam, 1f);
            single.updateConsumption();
            single.updateTile();
        }
        float powerPerSecond = single.getPowerProduction() * 60f;
        check(near(powerPerSecond, 600f));
        System.out.println("Single steam engine power per second: " + powerPerSecond);

        mindustry.world.blocks.production.HeatCrafter receiverBlock =
            new mindustry.world.blocks.production.HeatCrafter("test-engine-cooldown-receiver");
        receiverBlock.size = 3;
        receiverBlock.heatRequirement = 6f;
        mindustry.world.blocks.production.HeatCrafter.HeatCrafterBuild receiver = receiverBlock.new HeatCrafterBuild();
        receiver.block = receiverBlock;
        receiver.team = single.team;
        receiver.tile = single.tile = new mindustry.world.Tile(0, 0);
        single.x = 20f;
        receiver.proximity.add(single);
        single.proximity.add(receiver);
        float[] sides = new float[4];
        float previousPower = powerPerSecond;
        float previousHeat = receiver.calculateHeat(sides);
        check(near(previousHeat, 12f));
        single.liquids.clear();
        for (int tick = 0; tick < 600; tick++) {
            Vars.state.updateId++;
            single.updateConsumption();
            single.updateTile();
            float power = single.getPowerProduction() * 60f;
            float heat = receiver.calculateHeat(sides);
            check(power <= previousPower && heat <= previousHeat);
            previousPower = power;
            previousHeat = heat;
        }
        check(near(previousPower, 0f) && near(previousHeat, 0f));
        for (int tick = 0; tick < 600; tick++) {
            Vars.state.updateId++;
            single.liquids.set(metrum.content.MetrumLiquids.steam, 1f);
            single.updateConsumption();
            single.updateTile();
            float power = single.getPowerProduction() * 60f;
            float heat = receiver.calculateHeat(sides);
            check(power >= previousPower && heat >= previousHeat);
            previousPower = power;
            previousHeat = heat;
        }
        check(near(previousPower, 600f) && near(previousHeat, 12f));
        System.out.println("Recovered steam engine output: " + previousPower + " power/s, " + previousHeat + " heat");
    }

    static void checkResearchParent(mindustry.ctype.UnlockableContent content,
                                    mindustry.ctype.UnlockableContent parent) {
        check(content.techNode != null && parent.techNode != null && content.techNode.parent == parent.techNode);
        check(parent.techNode.children.contains(content.techNode, true));
    }

    static void checkAtomizerFactory() {
        metrum.world.AtomizerFactory block = metrum.content.MetrumPhysics.atomizerFactory;
        checkResearchParent(block, metrum.content.MetrumPhysics.atomizer);
        check(block.techNode.objectives.contains(objective -> objective instanceof mindustry.game.Objectives.Research
            && ((mindustry.game.Objectives.Research)objective).content == metrum.content.MetrumItems.carborundum));
        block.init();
        Vars.world = new mindustry.core.World();
        Vars.world.tiles = new mindustry.world.Tiles(9, 9);
        Vars.world.tiles.fill();
        mindustry.gen.Groups.init();
        metrum.world.AtomizerFactory.FactoryBuild factory = factoryFixture(block);
        int[][] sides = {{2, 5}, {2, 3}, {3, 6}, {5, 6}}; // the four input ports
        mindustry.type.Item[] minerals = {mindustry.content.Items.copper, mindustry.content.Items.lead,
            mindustry.content.Items.graphite, mindustry.content.Items.silicon};
        mindustry.world.Block duct = mindustry.content.Blocks.duct;
        for (int side = 0; side < sides.length; side++) {
            mindustry.world.Tile tile = Vars.world.tile(sides[side][0], sides[side][1]);
            mindustry.gen.Building feeder = mindustry.gen.Building.create();
            feeder.tile = tile;
            tile.build = feeder;
            feeder.block = duct;
            factory.proximity.add(feeder);
            feeder.team = Team.crux;
            check(!factory.acceptItem(feeder, minerals[side]));
            feeder.team = factory.team;
            check(factory.acceptItem(feeder, minerals[side]));
            factory.handleItem(feeder, minerals[side]);
            // The same connection can feed more than one raw material.
            mindustry.type.Item next = minerals[(side + 1) % minerals.length];
            check(factory.acceptItem(feeder, next));
            factory.handleItem(feeder, next);
        }
        factory.proximity.clear();
        factory.updateConsumption();
        check(factory.activeSlots() == 4 && near(block.consPower.requestedPower(factory) * 60f, 510f));
        for (int tick = 0; tick <= block.craftTime; tick++) {
            factory.updateConsumption();
            factory.updateTile();
        }
        check(factory.items.total() == 4 && factory.activeSlots() == 0);
        for (int side = 0; side < sides.length; side++) {
            mindustry.gen.Building receiver = duct.buildType.get();
            receiver.tile = Vars.world.tile(sides[side][0], sides[side][1]);
            receiver.block = duct;
            receiver.rotation = side;
            receiver.team = factory.team;
            receiver.items = new mindustry.world.modules.ItemModule();
            factory.proximity.add(receiver);
            factory.updateTile();
            check(receiver.items.total() == 1);
            factory.proximity.clear();
        }
        check(factory.items.total() == 0);
        mindustry.type.Item bufferedOutput = block.conversions.get(minerals[0]);
        factory.items.add(bufferedOutput, block.capacity - 1);
        factory.items.add(minerals[0], 8);
        factory.items.add(minerals[1], 2);
        factory.updateConsumption();
        check(factory.activeSlots() == 2);
        for (int tick = 0; tick <= block.craftTime; tick++) {
            factory.updateConsumption();
            factory.updateTile();
        }
        check(factory.items.get(bufferedOutput) == block.capacity && factory.items.get(minerals[0]) == 6);
        check(factory.items.get(block.conversions.get(minerals[1])) == 1 && factory.activeSlots() == 0);
        factory.items.remove(bufferedOutput, 3);
        factory.updateConsumption();
        check(factory.activeSlots() == 3);
        checkAtomizerSharesOneMaterial(block, minerals[0]);
        checkAtomizerSaveMigration(block, minerals[0]);
        System.out.println("OK: flexible Atomizer Factory routing, independent production, legacy save and 510 power/s");
    }

    static void checkAtomizerSharesOneMaterial(metrum.world.AtomizerFactory block, mindustry.type.Item input) {
        metrum.world.AtomizerFactory.FactoryBuild factory = factoryFixture(block);
        mindustry.world.Block duct = mindustry.content.Blocks.duct;
        mindustry.gen.Building feeder = duct.buildType.get();
        feeder.tile = Vars.world.tile(2, 5);
        feeder.tile.build = feeder;
        feeder.block = duct;
        feeder.rotation = 2;
        feeder.items = new mindustry.world.modules.ItemModule();
        feeder.team = factory.team;
        factory.proximity.add(feeder);
        mindustry.type.Item output = block.conversions.get(input);
        for (int item = 0; item < 8; item++) factory.handleItem(feeder, input);
        tickFactory(factory);
        check(factory.activeSlots() == 4 && near(block.consPower.requestedPower(factory) * 60f, 510f));
        for (int tick = 0; tick < block.craftTime; tick++) tickFactory(factory);
        check(factory.items.get(input) == 0 && factory.items.get(output) == 4);
        for (int turn = 0; turn < 4; turn++) {
            factory.handleItem(feeder, input);
            factory.handleItem(feeder, input);
            tickFactory(factory);
            check(factory.activeSlots() == 1);
            java.io.ByteArrayOutputStream snapshot = new java.io.ByteArrayOutputStream();
            factory.write(new arc.util.io.Writes(new java.io.DataOutputStream(snapshot)));
            arc.util.io.Reads saved = new arc.util.io.Reads(new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(snapshot.toByteArray())));
            for (int slot = 0; slot < 4; slot++) {
                check(saved.s() == (slot == turn ? input.id : -1));
                saved.f();
            }
            check(saved.ub() == (turn + 1) % 4);
            for (int tick = 0; tick < block.craftTime; tick++) tickFactory(factory);
            check(factory.items.get(output) == 5 + turn);
        }
    }

    static void tickFactory(metrum.world.AtomizerFactory.FactoryBuild factory) {
        factory.updateConsumption();
        factory.updateTile();
    }

    static metrum.world.AtomizerFactory.FactoryBuild factoryFixture(metrum.world.AtomizerFactory block) {
        mindustry.world.Tile tile = Vars.world.tile(4, 4);
        Vars.world.setGenerating(true);
        try {
            tile.setBlock(mindustry.content.Blocks.air);
            tile.setBlock(block, Team.sharded);
        } finally {
            Vars.world.setGenerating(false);
        }
        metrum.world.AtomizerFactory.FactoryBuild factory = (metrum.world.AtomizerFactory.FactoryBuild)tile.build;
        factory.power.status = 1f;
        return factory;
    }

    static void checkAtomizerSaveMigration(metrum.world.AtomizerFactory block, mindustry.type.Item input) {
        metrum.world.AtomizerFactory.FactoryBuild factory = factoryFixture(block);
        mindustry.type.Item output = block.conversions.get(input);
        factory.items.add(input, 3);
        factory.items.add(output, 1);
        java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
        arc.util.io.Writes write = new arc.util.io.Writes(new java.io.DataOutputStream(saved));
        factory.writeBase(write);
        for (int slot = 0; slot < 4; slot++) {
            write.s(slot == 0 ? input.id : -1);
            write.i(slot == 0 ? 3 : 0);
            write.i(slot == 0 ? 1 : 0);
            write.f(slot == 0 ? 0.5f : 0f);
        }
        factory = factoryFixture(block);
        factory.readAll(new arc.util.io.Reads(new java.io.DataInputStream(
            new java.io.ByteArrayInputStream(saved.toByteArray()))), (byte)0);
        check(factory.items.get(input) == 3 && factory.items.get(output) == 1 && factory.activeSlots() == 1);
        saved.reset();
        factory.writeAll(write);
        factory = factoryFixture(block);
        factory.readAll(new arc.util.io.Reads(new java.io.DataInputStream(
            new java.io.ByteArrayInputStream(saved.toByteArray()))), factory.version());
        for (int tick = 0; tick <= block.craftTime / 2f; tick++) {
            factory.updateConsumption();
            factory.updateTile();
        }
        check(factory.items.get(input) == 1 && factory.items.get(output) == 2 && factory.activeSlots() == 0);
    }

    public static void main(String[] args) {
        Vars.content = new ContentLoader();
        Vars.state = new mindustry.core.GameState();
        SteamEngine block = new SteamEngine("test-engine");
        SteamEngine.SteamEngineBuild a = engine(block, Team.sharded),
            b = engine(block, Team.sharded), c = engine(block, Team.sharded),
            enemy = engine(block, Team.crux);
        check(a.connectedEngines().size == 1);
        a.proximity.add(b); b.proximity.add(a);
        b.proximity.add(c); c.proximity.add(b);
        c.proximity.add(enemy); enemy.proximity.add(c);
        check(a.connectedEngines().size == 3 && c.connectedEngines().size == 3);
        // A cycle must not count engines twice.
        a.proximity.add(c); c.proximity.add(a);
        check(b.connectedEngines().size == 3);
        a.proximity.clear(); b.proximity.remove(a, true); c.proximity.remove(a, true);
        check(a.connectedEngines().size == 1 && b.connectedEngines().size == 2);
        mindustry.type.Liquid steam = new mindustry.type.Liquid("test-steam");
        for (SteamEngine.SteamEngineBuild member : new SteamEngine.SteamEngineBuild[]{a, b, c, enemy}) {
            member.liquids = new mindustry.world.modules.LiquidModule();
            member.efficiency = 1f;
        }
        arc.util.Time.delta = 1f;
        metrum.world.SharedSteamConsumer consumer = new metrum.world.SharedSteamConsumer(steam, 0.2f);
        c.liquids.add(steam, 1f);
        check(consumer.efficiency(b) == 1f);
        check(consumer.efficiency(a) == 0f);
        consumer.update(b);
        check(near(b.liquids.get(steam), 0.31f) && near(c.liquids.get(steam), 0.5f));
        // After a split nobody can reach the other half any more.
        b.proximity.remove(c, true); c.proximity.remove(b, true);
        b.liquids.clear();
        Vars.state.updateId++;
        check(consumer.efficiency(b) == 0f && near(c.liquids.get(steam), 0.5f));
        // Scarce steam piped into one end is shared equally and conserved.
        a.proximity.clear(); b.proximity.clear(); c.proximity.clear();
        a.proximity.add(b); b.proximity.add(a); b.proximity.add(c); c.proximity.add(b);
        SteamEngine.SteamEngineBuild[] row = {a, b, c};
        float[] burned = new float[3];
        for (SteamEngine.SteamEngineBuild m : row) m.liquids.clear();
        for (int tick = 0; tick < 100; tick++) {
            Vars.state.updateId++;
            c.liquids.add(steam, 0.2f);
            for (int k = 0; k < 3; k++) {
                consumer.efficiency(row[k]);
                float before = row[k].liquids.get(steam);
                consumer.update(row[k]);
                burned[k] += before - row[k].liquids.get(steam);
            }
        }
        check(near(burned[0], burned[1]) && near(burned[1], burned[2]));
        check(Math.abs(burned[0] + burned[1] + burned[2] - 20f) < 0.01f);
        // Bonus: lost after a split, flat for two or three engines, never shared with enemies.
        block.heatOutput = 12f;
        for (SteamEngine.SteamEngineBuild m : new SteamEngine.SteamEngineBuild[]{a, b, c, enemy}) { m.warmup = 1f; m.proximity.clear(); }
        c.proximity.add(enemy);
        check(near(c.heat(), 12f) && near(power(c), 1f) && near(used(c, steam), 0.2f));
        a.proximity.add(b); b.proximity.add(a);
        check(near(a.heat() + b.heat(), 26.4f) && near(power(a), 1.1f));
        check(near((used(a, steam) + used(b, steam)) * 60f, 22.8f));
        b.proximity.add(c); c.proximity.add(b);
        check(near(b.heat(), 13.2f) && near(power(b), 1.1f) && near(used(b, steam), 0.19f));
        // The whole group heats through whichever engines touch a real heat crafter.
        Vars.state.updateId++;
        mindustry.world.blocks.production.HeatCrafter crucible = new mindustry.world.blocks.production.HeatCrafter("test-crucible");
        crucible.size = 3;
        crucible.heatRequirement = 10f;
        block.size = 2;
        mindustry.world.blocks.production.HeatCrafter.HeatCrafterBuild h = crucible.new HeatCrafterBuild();
        h.block = crucible;
        h.team = Team.sharded;
        h.tile = new mindustry.world.Tile(0, 0);
        a.tile = b.tile = c.tile = new mindustry.world.Tile(0, 0);
        a.x = 20f; b.x = 36f; c.x = -20f; // a and c flush with the crucible, b behind a
        a.proximity.add(h); h.proximity.add(a);
        for (SteamEngine.SteamEngineBuild m : row) m.warmup = 1f;
        check(near(h.calculateHeat(new float[4]), 39.6f) && near(b.groupHeat(), 39.6f));
        Vars.state.updateId++;
        c.proximity.add(h); h.proximity.add(c);
        check(near(h.calculateHeat(new float[4]), 39.6f)); // second contact does not duplicate heat
        Vars.state.updateId++;
        a.proximity.clear(); b.proximity.clear(); c.proximity.clear(); h.proximity.clear();
        a.proximity.add(h); h.proximity.add(a);
        check(near(h.calculateHeat(new float[4]), 12f));
        mindustry.world.blocks.production.HeatCrafter.HeatCrafterBuild opposite = crucible.new HeatCrafterBuild();
        opposite.block = crucible;
        opposite.team = Team.sharded;
        opposite.tile = new mindustry.world.Tile(0, 0);
        opposite.x = 40f;
        a.proximity.add(opposite);
        opposite.proximity.add(a);
        float[] warmupThresholds = {0f, 0.05f, 0.25f, 0.5f, 0.75f};
        for (int stage = 0; stage < warmupThresholds.length; stage++) {
            a.warmup = warmupThresholds[stage];
            float expectedHeat = stage * 3f;
            check(near(h.calculateHeat(new float[4]), expectedHeat));
            check(near(opposite.calculateHeat(new float[4]), expectedHeat));
        }
        // Output follows the steam actually supplied, also when it arrives in bursts.
        check(near(settled(a, 0.2f, 0.2f), 0.2f) && Math.abs(settled(a, 0.6f, 0f) - 0.3f) < 0.03f);
        check(settled(a, 1f, 1f) > 0.48f && near(settled(a, 0f, 0f), 0f));
        System.out.println("OK: isolated, chained, cyclic, split and enemy engine groups");
        checkSingleEngineRatedPower();
    }
}
