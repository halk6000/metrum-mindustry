package metrum.world;

import arc.struct.Seq;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.graphics.g2d.Draw;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/** Batch chemistry. Inputs and outputs are ordinary materials, not atomic items. */
public class ChemicalSplitter extends Block {
    public static class Product {
        public final UnlockableContent material;
        public final int amount;
        public Product(UnlockableContent material, int amount) { this.material=material; this.amount=amount; }
    }
    public static class Recipe {
        public final UnlockableContent input;
        public final Product[] outputs;
        public Recipe(UnlockableContent input, Product... outputs) {this.input=input;this.outputs=outputs;}
    }
    public final Seq<Recipe> recipes=new Seq<>();
    public Item boostItem;
    public float boostMultiplier=2.5f, craftTime=60f;
    private TextureRegion body;
    private final ProcessGraphics graphics=new ProcessGraphics();
    public ChemicalSplitter(String name) {
        super(name);
        update=solid=hasItems=hasLiquids=hasPower=outputsLiquid=true;
        itemCapacity=40; liquidCapacity=60f; unloadable=false;
        buildType=SplitterBuild::new; configurable=true;
    }
    public ChemicalSplitter recipe(UnlockableContent input, Product... outputs) {
        if(outputs.length<1 || outputs.length>2) throw new IllegalArgumentException("Chemical splitter requires one or two products");
        recipes.add(new Recipe(input,outputs)); return this;
    }
    @Override public void load(){super.load();body=Core.atlas.find(name+"-body");graphics.load(name);}
    @Override public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime,craftTime/60f,StatUnit.seconds);
        stats.add(Stat.input,t->{for(Recipe r:recipes){
            t.image(r.input.uiIcon).size(24f);t.add(" x2 → ");
            for(Product p:r.outputs){t.image(p.material.uiIcon).size(20f);t.add(" x"+p.amount+"  ");}t.row();
        }});
        if(boostItem!=null) stats.add(Stat.booster,t->{t.image(boostItem.uiIcon).size(24f);t.add(" ×"+boostMultiplier);});
    }
    @Override public void setBars(){
        super.setBars();
        addBar("chemical-reactor", (SplitterBuild b) -> new mindustry.ui.Bar(
            () -> Core.bundle.format("metrum.chemical.reaction", b.chamberName(-1)),
            () -> b.chamberColor(-1),
            () -> b.chamberFill(-1)
        ));
        addBar("chemical-out-a", (SplitterBuild b) -> new mindustry.ui.Bar(
            () -> Core.bundle.format("metrum.chemical.chamber1", b.chamberName(0)),
            () -> b.chamberColor(0),
            () -> b.chamberFill(0)
        ));
        addBar("chemical-out-b", (SplitterBuild b) -> new mindustry.ui.Bar(
            () -> Core.bundle.format("metrum.chemical.chamber2", b.chamberName(1)),
            () -> b.chamberColor(1),
            () -> b.chamberFill(1)
        ));
    }
    @Override public void displayExtra(arc.scene.ui.layout.Table t){
        super.displayExtra(t); TechnicalInfoUI.header(t,this);
        for(Recipe r:recipes){UnlockableContent[] materials=new UnlockableContent[r.outputs.length];int[] amounts=new int[r.outputs.length];for(int i=0;i<r.outputs.length;i++){materials[i]=r.outputs[i].material;amounts[i]=r.outputs[i].amount;}TechnicalInfoUI.recipe(t,r.input,2,materials,amounts,boostItem,boostMultiplier);}
    }
    public class SplitterBuild extends Building {
        private int active=-1, next=0, last=-1;
        private float progress, phase, after;
        // Each recipe and output retains its own fractional carry across saves.
        private float[][] carry;
        private float[][] carry(){if(carry==null){carry=new float[recipes.size][];for(int i=0;i<recipes.size;i++)carry[i]=new float[recipes.get(i).outputs.length];}return carry;}
        private boolean input(UnlockableContent c){for(Recipe r:recipes)if(r.input==c)return true;return false;}
        private boolean boosted(){return boostItem!=null && items.get(boostItem)>0;}
        private float quantity(UnlockableContent c){return c instanceof Item?items.get((Item)c):liquids.get((Liquid)c);}
        private UnlockableContent blockedProduct(int i){
            Recipe r=recipes.get(i);boolean boost=boosted();
            for(int j=0;j<r.outputs.length;j++){
                Product p=r.outputs[j];float raw=BoostedCrafter.productYield(p.amount,boost,boostMultiplier);
                float amount=p.material instanceof Item?(int)Math.floor(raw+carry()[i][j]+0.00001f):raw;
                if(quantity(p.material)+amount>(p.material instanceof Item?itemCapacity:liquidCapacity))return p.material;
            }
            return null;
        }
        private boolean ready(int i){
            if(i<0||i>=recipes.size)return false;
            Recipe r=recipes.get(i);if(quantity(r.input)<2f)return false;
            if(r.input==metrum.content.MetrumLiquids.ammonia && ammoniaOutput())return false;
            if(r.input==mindustry.content.Liquids.arkycite && quantity(metrum.content.MetrumLiquids.ammonia)>0f && !ammoniaOutput())return false;
            return blockedProduct(i)==null;
        }
        private String diagnostic(){
            if(MetrumBuildingUI.noPower(this))return MetrumBuildingUI.tr("no-power");
            if(choose()>=0)return efficiency>0f?MetrumBuildingUI.tr("running"):MetrumBuildingUI.tr("low-supply");
            if(last>=0&&last<recipes.size){
                for(Product p:recipes.get(last).outputs)if(pendingOutput(p.material)&&quantity(p.material)>0.0001f){
                    UnlockableContent blocked=blockedProduct(last);
                    if(blocked!=null)return MetrumBuildingUI.blocked(blocked);
                    for(int i=0;i<recipes.size;i++)if(i!=last&&ready(i))return MetrumBuildingUI.tr("drain-output");
                    break;
                }
            }
            for(int i=0;i<recipes.size;i++){
                Recipe r=recipes.get(i);
                if(quantity(r.input)<2f)continue;
                if(r.input==metrum.content.MetrumLiquids.ammonia&&ammoniaOutput())return MetrumBuildingUI.tr("drain-output");
                if(r.input==mindustry.content.Liquids.arkycite&&quantity(metrum.content.MetrumLiquids.ammonia)>0f&&!ammoniaOutput())return MetrumBuildingUI.tr("mixed-input");
                UnlockableContent blocked=blockedProduct(i);
                if(blocked!=null)return MetrumBuildingUI.blocked(blocked);
            }
            return MetrumBuildingUI.tr("no-input");
        }
        private boolean ammoniaOutput(){
            return last>=0 && last<recipes.size && recipes.get(last).input==mindustry.content.Liquids.arkycite && quantity(metrum.content.MetrumLiquids.ammonia)>0.0001f;
        }
        private boolean pendingOutput(UnlockableContent c){return !input(c) || (c==metrum.content.MetrumLiquids.ammonia && ammoniaOutput());}
        private int choose(){
            if(last>=0 && last<recipes.size){
                boolean pending=false;
                for(Product p:recipes.get(last).outputs)if(pendingOutput(p.material) && quantity(p.material)>0.0001f)pending=true;
                if(pending)return ready(last)?last:-1;
            }
            if(ready(active))return active;for(int n=0;n<recipes.size;n++){int i=(next+n)%recipes.size;if(ready(i))return i;}return -1;}
        @Override public void buildConfiguration(arc.scene.ui.layout.Table t){
            MetrumBuildingUI.openButton(t,this,this::technicalModules);
        }
        @Override public void display(arc.scene.ui.layout.Table t){
            MetrumBuildingUI.display(t,this,this::technicalModules);
        }
        private void technicalModules(arc.scene.ui.layout.Table body){
                MetrumBuildingUI.section(body,"chambers");
                MetrumBuildingUI.chamber(body,"reaction",()->chamberMaterial(-1),()->chamberFill(-1));
                MetrumBuildingUI.chamber(body,"chamber1",()->chamberMaterial(0),()->chamberFill(0));
                MetrumBuildingUI.chamber(body,"chamber2",()->chamberMaterial(1),()->chamberFill(1));
                MetrumBuildingUI.progress(body,()->progress);
                MetrumBuildingUI.booster(body,boostItem,boostMultiplier,()->boostItem==null?0:items.get(boostItem));
                MetrumBuildingUI.section(body,"diagnostics");
                MetrumBuildingUI.status(body,this,this::diagnostic);
        }
        @Override public boolean shouldConsume(){return enabled&&choose()>=0;}
        @Override public boolean acceptItem(Building source,Item item){return (item==boostItem||input(item))&&items.get(item)<itemCapacity;}
        @Override public boolean acceptLiquid(Building source,Liquid liquid){if(liquid==metrum.content.MetrumLiquids.ammonia && (ammoniaOutput() || quantity(mindustry.content.Liquids.arkycite)>0.0001f))return false;
            if(liquid==mindustry.content.Liquids.arkycite && quantity(metrum.content.MetrumLiquids.ammonia)>0.0001f && !ammoniaOutput())return false;
            return input(liquid)&&liquids.get(liquid)<liquidCapacity;}
        // A/B are physical ports on the lower edge, not the redirected destination.
        private int outputSlot(Product p,int index,int outputCount){
            if(p.material==mindustry.content.Liquids.hydrogen)return 0;
            if(p.material==metrum.content.MetrumLiquids.oxygen || p.material==mindustry.content.Liquids.nitrogen)return 1;
            return Math.min(index,1);
        }
        private int port(Building neighbour){
            if(neighbour==null || neighbour.team!=team)return -1;
            // Check the actual tiles touching the bottom edge, including multiblock neighbours.
            int left=tile.x-(size-1)/2, bottom=tile.y-(size-1)/2;
            boolean a=false,b=false;
            for(int dx=0;dx<size;dx++){
                mindustry.world.Tile t=mindustry.Vars.world.tile(left+dx,bottom-1);
                if(t!=null && t.build==neighbour){if(dx<size/2)a=true;else b=true;}
            }
            // A receiver spanning both ports is ambiguous and must not mix gases.
            return a==b?-1:(a?0:1);
        }
        private int slotFor(UnlockableContent material){
            if(material==mindustry.content.Liquids.hydrogen)return 0;
            if(material==metrum.content.MetrumLiquids.oxygen || material==mindustry.content.Liquids.nitrogen)return 1;
            // Solid products can vary by recipe; use the last completed recipe when available.
            if(last>=0 && last<recipes.size){Product[] ps=recipes.get(last).outputs;for(int j=0;j<ps.length;j++)if(ps[j].material==material)return outputSlot(ps[j],j,ps.length);}
            for(Recipe r:recipes)for(int j=0;j<r.outputs.length;j++)if(r.outputs[j].material==material)return outputSlot(r.outputs[j],j,r.outputs.length);
            return -1;
        }
        private int shownRecipe(){
            if(active>=0 && active<recipes.size && warmup()>0f)return active;
            if(last>=0 && last<recipes.size)return last;
            if(choose()>=0)return choose();
            return -1;
        }
        private UnlockableContent chamberMaterial(int slot){
            int shown=shownRecipe();
            if(shown<0 || shown>=recipes.size)return null;
            Recipe r=recipes.get(shown);
            if(slot<0)return r.input;
            for(int j=0;j<r.outputs.length;j++) if(outputSlot(r.outputs[j], j, r.outputs.length)==slot) return r.outputs[j].material;
            return null;
        }
        public String chamberName(int slot){
            UnlockableContent c=chamberMaterial(slot);
            return c==null?"-":c.localizedName;
        }
        public Color chamberColor(int slot){
            UnlockableContent c=chamberMaterial(slot);
            if(c instanceof Liquid) return ((Liquid)c).color;
            if(c instanceof Item) return ((Item)c).color != null ? ((Item)c).color : Color.lightGray;
            return Color.lightGray;
        }
        public float chamberFill(int slot){
            UnlockableContent c=chamberMaterial(slot);
            if(c==null)return 0f;
            float amount = quantity(c);
            float cap = c instanceof Item ? itemCapacity : liquidCapacity;
            return cap <= 0.0001f ? 0f : Math.min(1f, amount / cap);
        }
        @Override public boolean canDumpLiquid(Building to,Liquid liquid){return pendingOutput(liquid)&&port(to)>=0&&port(to)==slotFor(liquid);}
        @Override public boolean canDump(Building to,Item item){return item!=boostItem&&!input(item)&&port(to)>=0&&port(to)==slotFor(item);}
        private void extractLiquid(Liquid liquid){
            int slot=slotFor(liquid);
            if(slot<0)return;
            for(Building neighbour:proximity){
                if(port(neighbour)!=slot)continue;
                Building destination=neighbour.getLiquidDestination(this,liquid);
                if(destination!=neighbour || destination.team!=team || !destination.block.hasLiquids || destination.liquids==null)continue;
                // Reject a redirected endpoint belonging to the other physical port.
                int destinationPort=port(destination);
                if(destinationPort>=0 && destinationPort!=slot)continue;
                // A pipe already containing a different gas must be cleared first.
                if(destination.liquids.current()!=liquid && destination.liquids.currentAmount()>0.001f)continue;
                if(!destination.acceptLiquid(this,liquid))continue;
                float amount=Math.min(liquids.get(liquid),destination.block.liquidCapacity-destination.liquids.get(liquid));
                if(amount>0.0001f)transferLiquid(destination,amount,liquid);
            }
        }
        @Override public void updateTile(){
            for(Recipe r:recipes)for(Product p:r.outputs){
                if(p.material instanceof Item){Item it=(Item)p.material;if(!input(it)&&items.get(it)>0)dump(it);}
                else {Liquid l=(Liquid)p.material;if(pendingOutput(l)&&liquids.get(l)>0.0001f)extractLiquid(l);}
            }
            after=Math.max(0f,after-delta()/45f);
            int i=choose();if(i!=active){active=i;progress=0f;}
            if(!enabled||efficiency<=0f||i<0)return;
            phase=(phase+edelta()*8f/60f)%48f;
            progress+=edelta()/craftTime;
            if(progress<1f||!ready(i))return;
            Recipe r=recipes.get(i);boolean boost=boosted();
            if(r.input instanceof Item)items.remove((Item)r.input,2);else liquids.remove((Liquid)r.input,2f);
            if(boost)items.remove(boostItem,1);
            for(int j=0;j<r.outputs.length;j++){
                Product p=r.outputs[j];float raw=BoostedCrafter.productYield(p.amount,boost,boostMultiplier);
                if(p.material instanceof Item){float sum=raw+carry()[i][j];int count=(int)Math.floor(sum+0.00001f);carry()[i][j]=Math.max(0f,sum-count);items.add((Item)p.material,count);}
                else liquids.add((Liquid)p.material,raw);
            }
            last=i;after=1f;next=(i+1)%recipes.size;active=-1;progress=0f;
        }
        @Override public float warmup(){return enabled&&efficiency>0f&&active>=0?1f:0f;}
        @Override public void draw(){
            // Fixed 32 world units (4 tiles); PNG resolution does not affect scale.
            float w=size*8f;
            Draw.rect(body!=null&&body.found()?body:region,x,y,w,w);
            if(active>=0&&active<recipes.size&&warmup()>0f){Recipe r=recipes.get(active);graphics.draw(0,r.input,phase,x- w*1f/256f,y+w*40.5f/256f,w*112f/256f,w*91f/256f);}
            int shown=active>=0&&warmup()>0f?active:(after>0f?last:-1);
            if(shown>=0&&shown<recipes.size){Recipe r=recipes.get(shown);float f=warmup()>0f?phase:(1f-after)*8f;
                for(int slot=0;slot<2;slot++){
                    int count=0;
                    for(int j=0;j<r.outputs.length;j++)if(outputSlot(r.outputs[j],j,r.outputs.length)==slot)count++;
                    if(count==0)continue;
                    int pick=Math.floorMod((int)(f/4f),count);
                    for(int j=0;j<r.outputs.length;j++)if(outputSlot(r.outputs[j],j,r.outputs.length)==slot && pick--==0){
                        Product p=r.outputs[j];
                        float cx=x+w*(slot==0?-68.5f:71.5f)/256f, cy=y-w*82.5f/256f;
                        graphics.draw(1,p.material,f,cx,cy,w*59f/256f,w*45f/256f);
                        // Distinct product icon even when two gases have similar colors.
                        if(p.material.uiIcon!=null && p.material.uiIcon.found())Draw.rect(p.material.uiIcon,cx,cy,w*18f/256f,w*18f/256f);
                        break;
                    }
                }
            }Draw.reset();
        }
        @Override public byte version(){return 2;}
        @Override public void write(Writes w){super.write(w);w.i(active);w.i(next);w.f(progress);w.i(recipes.size);for(float[] row:carry()){w.i(row.length);for(float v:row)w.f(v);}w.i(last); }
        @Override public void read(Reads r,byte revision){super.read(r,revision);active=r.i();next=r.i();progress=r.f();int n=r.i();for(int i=0;i<n;i++){int m=r.i();for(int j=0;j<m;j++){float v=r.f();if(i<recipes.size&&j<carry()[i].length)carry()[i][j]=v;}}if(revision>=2)last=r.i();if(last<0||last>=recipes.size)last=-1;if(active<0||active>=recipes.size){active=-1;progress=0f;}next=recipes.size==0?0:Math.floorMod(next,recipes.size);}
    }
}
