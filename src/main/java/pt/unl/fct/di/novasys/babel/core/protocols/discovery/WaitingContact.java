package pt.unl.fct.di.novasys.babel.core.protocols.discovery;

import pt.unl.fct.di.novasys.babel.core.SelfConfigurableProtocol;

/**
 * Associates a raw announcement datagram with the self-configurable protocol that is waiting
 * to receive a contact address from the discovery mechanism.
 *
 * @param anouncement the serialised discovery announcement payload
 * @param proto       the protocol that is waiting to be contacted
 */
public record WaitingContact(byte[] anouncement, SelfConfigurableProtocol proto) {
}
