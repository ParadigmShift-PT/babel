package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;

public record WaitingContact(byte[] anouncement, SelfConfigurableProtocol proto) {
}
