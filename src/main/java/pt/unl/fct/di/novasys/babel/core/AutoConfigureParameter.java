package pt.unl.fct.di.novasys.babel.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the protocol parameter is autoconfigurable, that is, it should
 * ask the other nodes in the system for a suitable configuration.
 * 
 * Every parameter anotated with this annotation should have a getter and a
 * setter called "getFirst" + ParameterNameCapitalized and "setFirst" +
 * ParameterNameCapitalized respectively
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoConfigureParameter {
    String propsName() default "none";
}
