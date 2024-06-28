package pt.unl.fct.di.novasys.babel.metrics.exceptions;

public class LabeledMetricException extends RuntimeException {
    public LabeledMetricException(){
        super("Labelled metric used as unlabelled metric!");
    }
}
