package pt.unl.fct.di.novasys.babel.protocols.discovery;

import java.lang.reflect.Method;

import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;

public record WaitingContact(byte[] anouncement, Method setter, SelfConfiguredProtocol proto) {
}
