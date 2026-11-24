package metrum.ai.planner;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
public class WaterConnectionTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    static Building place(int x,int y,Block block,int rotation) {
        Building b=Building.create(); b.block=block; b.team=Team.crux; b.rotation=rotation;
        b.tile=Vars.world.tile(x,y); b.tile.build=b; return b;
    }
    public static void main(String[] args) {
        Vars.content=new ContentLoader();
        Blocks.air=new mindustry.world.blocks.environment.Floor("air"); Blocks.reinforcedConduit=new Block("pipe");
        Vars.world=new World(); Vars.world.tiles=new Tiles(8,8);
        for(int x=0;x<8;x++) for(int y=0;y<8;y++) Vars.world.tiles.set(x,y,new Tile(x,y));
        Block consumer=new Block("consumer"); consumer.hasLiquids=true;
        Building a=place(1,1,Blocks.reinforcedConduit,0), b=place(2,1,Blocks.reinforcedConduit,0);
        Building c=place(3,1,Blocks.reinforcedConduit,0), target=place(4,1,consumer,0);
        check(BasePlanner.waterLineConnected(a,target));
        mindustry.content.Liquids.water = new mindustry.type.Liquid("water");
        mindustry.type.Liquid oil = new mindustry.type.Liquid("oil");
        c.liquids = new mindustry.world.modules.LiquidModule();
        c.liquids.add(oil, 1f);
        check(!BasePlanner.waterLineConnected(a,target));
        c.liquids.clear(); c.liquids.add(mindustry.content.Liquids.water, 1f);
        check(BasePlanner.waterLineConnected(a,target));
        c.liquids.add(oil, 1f); check(!BasePlanner.waterLineConnected(a,target)); c.liquids.clear();
        b.rotation=2; check(!BasePlanner.waterLineConnected(a,target)); b.rotation=0;
        c.tile.build=null; check(!BasePlanner.waterLineConnected(a,target)); c.tile.build=c;
        target.team=Team.sharded; check(!BasePlanner.waterLineConnected(a,target)); target.team=Team.crux;
        b.rotation=1;
        Building turn=place(2,2,Blocks.reinforcedConduit,0), cornerTarget=place(3,2,consumer,0);
        check(BasePlanner.waterLineConnected(a,cornerTarget));
        turn.rotation=3; check(!BasePlanner.waterLineConnected(a,target));
        System.out.println("OK: straight, opposing, broken, foreign target, corner, cycle, water, foreign liquid, mixed liquids");
    }
}