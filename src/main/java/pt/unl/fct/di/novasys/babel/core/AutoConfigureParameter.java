package pt.unl.fct.di.novasys.babel.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the protocol parameter is autoconfigurable, that is, it should
 * ask the other nodes in the system for a suitable configuration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoConfigureParameter {

}
