package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when a labelled metric is accessed through an API intended for unlabelled metrics.
 */
public class LabeledMetricException extends RuntimeException {
    /**
     * Creates a new exception with a fixed message indicating the misuse.
     */
    public LabeledMetricException(){
        super("Labelled metric used as unlabelled metric!");
    }
}
