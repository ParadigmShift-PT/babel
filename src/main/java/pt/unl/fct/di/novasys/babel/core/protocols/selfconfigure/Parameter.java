package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;

/**
 * Immutable descriptor for a single auto-configurable parameter on a {@link SelfConfigurableProtocol}.
 * Bundles the reflective getter/setter methods together with the owning protocol instance and the
 * parameter's logical name so that self-configuration protocols can read and write the value without
 * knowing the concrete protocol type at compile time.
 *
 * @param getter the reflective getter method that reads the current parameter value
 * @param setter the reflective setter method that applies a new parameter value
 * @param proto  the protocol instance that owns this parameter
 * @param name   the logical name of the parameter (used for matching over the network)
 */
public record Parameter(Method getter, Method setter, SelfConfigurableProtocol proto, String name) {
}
