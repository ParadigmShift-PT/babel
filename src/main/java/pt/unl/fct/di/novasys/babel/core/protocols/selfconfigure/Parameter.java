package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;

public record Parameter(Method getter, Method setter, SelfConfigurableProtocol proto, String name) {
    
}
