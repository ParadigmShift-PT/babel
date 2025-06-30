package pt.unl.fct.di.novasys.babel.metrics.exceptions;

public class IncorrectLabelNumberException extends RuntimeException{
    public IncorrectLabelNumberException(){
        super("Number of label values does not match number of labels");
    }
}
