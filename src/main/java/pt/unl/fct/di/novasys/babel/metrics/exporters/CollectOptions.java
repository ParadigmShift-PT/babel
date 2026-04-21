package pt.unl.fct.di.novasys.babel.metrics.exporters;

/**
 * Class that holds the collect options for a metric.
 * Collect options are as follows:
 * <ul>
 *     <li>reset_on_collect: if true, the metric will be reset after being collected</li>
 * </ul>
 */
public class CollectOptions{
    private boolean reset_on_collect;

    /**
     * Default CollectOptions with reset_on_collect set to false
     */
    public static final CollectOptions DEFAULT_COLLECT_OPTIONS = new CollectOptions();

    /**
     * Constructor, sets reset_on_collect to the given value
     * @param reset_on_collect if true, the metric will be reset after being collected
     */
    public CollectOptions(boolean reset_on_collect) {
        this.reset_on_collect = reset_on_collect;
    }

    /**
     * Default constructor, sets reset_on_collect to false
     */
    public CollectOptions(){
        this.reset_on_collect = false;
    }

    /**
     * Sets whether the metric should be reset to its zero value after each collection.
     *
     * @param reset_on_collect {@code true} to reset the metric after collection, {@code false} to retain its value
     */
    public void setResetOnCollect(boolean reset_on_collect) {
        this.reset_on_collect = reset_on_collect;
    }

    /**
     * Returns whether the metric is reset to its zero value after each collection.
     *
     * @return {@code true} if the metric resets on collect, {@code false} otherwise
     */
    public boolean getResetOnCollect() {
        return reset_on_collect;
    }

    /**
     * Returns a human-readable description of these collect options.
     *
     * @return string representation showing the reset-on-collect flag value
     */
    public String toString(){
        return "Reset on collect: " + reset_on_collect;
    }

}
