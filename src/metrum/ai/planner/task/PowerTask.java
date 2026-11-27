package metrum.ai.planner.task;
import arc.Events;
import arc.util.Time;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import java.util.*;
import metrum.ai.planner.goal.StrategicPlanner;
public final class PowerTask {
    private static final Map<CoreBuild,float[]> data=new HashMap<>();
    public static void init(){Events.on(WorldLoadEvent.class,e->data.clear());}
    public static void refresh(CoreBuild c){
        Set<PowerGraph> graphs=Collections.newSetFromMap(new IdentityHashMap<PowerGraph,Boolean>());
        Vars.indexer.eachBlock(c.team,c.x,c.y,40f*Vars.tilesize,b->b.power!=null&&b.closestCore()==c,b->graphs.add(b.power.graph));
        float p=0,n=0; for(PowerGraph g:graphs){p+=g.getLastPowerProduced();n+=g.getLastPowerNeeded();}
        float scale=60f/Math.max(0.001f,Time.delta); data.put(c,new float[]{p*scale,n*scale});
        data.keySet().removeIf(k->!k.isValid());
    }
    public static boolean generating(CoreBuild c){float[] v=data.get(c);return v!=null&&v[0]>0.01f;}
    public static String status(CoreBuild c){float[] v=data.get(c); if(v==null)return "\n"+StrategicPlanner.text("Prąd: pomiar", "Power: measuring");
        return "\n"+StrategicPlanner.text("Prąd: ","Power: ")+StrategicPlanner.text("produkcja ","generation ")+Strings.fixed(v[0],1)+"/s | "+StrategicPlanner.text("pobór ","demand ")+Strings.fixed(v[1],1)+"/s | "+StrategicPlanner.text("bilans ","balance ")+Strings.fixed(v[0]-v[1],1);}
}
