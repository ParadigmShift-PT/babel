package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import pt.unl.fct.di.novasys.babel.core.SelfConfiguredProtocol;

public record WaitingContact(byte[] anouncement, SelfConfiguredProtocol proto) {
}
