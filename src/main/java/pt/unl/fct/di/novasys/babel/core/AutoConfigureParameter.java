package pt.unl.fct.di.novasys.babel.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the protocol parameter is autoconfigurable, that is, it should
 * ask the other nodes in the system for a suitable configuration
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfigureParameter {

}
