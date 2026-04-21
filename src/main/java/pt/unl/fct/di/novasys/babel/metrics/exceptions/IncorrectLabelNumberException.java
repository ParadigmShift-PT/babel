package pt.unl.fct.di.novasys.babel.metrics.exceptions;

/**
 * Thrown when the number of label values supplied to a labelled metric does not match
 * the number of label names declared for that metric.
 */
public class IncorrectLabelNumberException extends RuntimeException{
    /**
     * Creates a new exception with a fixed message describing the arity mismatch.
     */
    public IncorrectLabelNumberException(){
        super("Number of label values does not match number of labels");
    }
}
