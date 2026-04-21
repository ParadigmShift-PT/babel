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
 * 
 * The propsName field should be completed when the the name of the field in a
 * Properties object is better suited for a DNS TXT record rather than its name
 * in code
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoConfigureParameter {
    /**
     * Returns the property name used in a DNS TXT record for this parameter.
     * Defaults to {@code "none"} when the field name in code is already suitable.
     *
     * @return the DNS TXT record property name, or {@code "none"} if not overridden
     */
    String propsName() default "none";
}
