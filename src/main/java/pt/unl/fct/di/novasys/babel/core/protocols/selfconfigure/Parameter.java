package pt.unl.fct.di.novasys.babel.core.protocols.selfconfigure;

import java.lang.reflect.Method;

import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;

public record Parameter(String name, Method getter, Method setter, SelfConfiguredProtocol proto) {
    
}
