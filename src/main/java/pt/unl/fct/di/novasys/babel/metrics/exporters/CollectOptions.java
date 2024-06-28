package pt.unl.fct.di.novasys.babel.metrics.exporters;

public class CollectOptions{
    public enum ReduceType {NONE, SUM, AVG, MAX, MIN}
    private final boolean reset_on_collect;
    private final ReduceType reduce_type;

    /**
     *
     * @param reset_on_collect
     * @param reduce_type
     */
    public CollectOptions(boolean reset_on_collect, ReduceType reduce_type) {
        this.reset_on_collect = reset_on_collect;
        this.reduce_type = reduce_type;
    }

    public CollectOptions(){
        this.reset_on_collect = false;
        this.reduce_type = ReduceType.NONE;
    }

    public CollectOptions(CollectOptions collectOptions, boolean reset_on_collect){
        this.reset_on_collect = reset_on_collect;
        this.reduce_type = collectOptions.reduce_type;
    }



    public boolean getResetOnCollect() {
        return reset_on_collect;
    }

    public ReduceType getReduceType() {
        return reduce_type;
    }
}
