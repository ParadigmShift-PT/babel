package pt.unl.fct.di.novasys.network.tls.pipeline;

import static pt.unl.fct.di.novasys.network.tls.pipeline.OutPreTLSHandshakeHandler.EXPECTED_ID_ATTR;
import static pt.unl.fct.di.novasys.network.tls.pipeline.OutPreTLSHandshakeHandler.ID_ATTR;

import io.netty.channel.*;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.messaging.control.FirstHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.NthHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.SecondHandshakeMessage;
import pt.unl.fct.di.novasys.network.pipeline.InHandshakeHandler;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.tls.userevents.PreTLSHandshakeCompleted;

public class InPreTLSHandshakeHandler extends InHandshakeHandler {

    private final X509IKeyManager identityChooser;

    public InPreTLSHandshakeHandler(long connectionId, AttributeValidator validator,
            Attributes baseAttrs, int handshakeSteps, X509IKeyManager identityChooser) {
        super(connectionId, validator, baseAttrs, handshakeSteps);
        this.identityChooser = identityChooser;
    }

    @Override
    protected void handleFirstHandshakeMessage(ChannelHandlerContext ctx, FirstHandshakeMessage fhm)
            throws InvalidHandshakeException {
        Attributes replyAttrs = validator.getSecondHandshakeAttributes(connectionId, fhm.attributes, baseAttrs);

        // TODO should this be the responsability of the channel?

        // Get peer identity
        byte[] peerId = fhm.attributes.getBytes(ID_ATTR);
        if (peerId == null)
            throw new InvalidHandshakeAttributesException(fhm.attributes, "Peer didn't specify its identity");

        // Choose identity
        byte[] chosenId = replyAttrs.getBytes(ID_ATTR);
        if (replyAttrs.getBytes(ID_ATTR) == null) {
            byte[] expectedId = fhm.attributes.getBytes(EXPECTED_ID_ATTR);

            if (expectedId != null) {
                if (identityChooser.getIdAlias(expectedId) != null)
                    chosenId = expectedId;
            }

            if (chosenId == null) {
                // TODO specify keytype?
                String idAlias = identityChooser.chooseServerAlias(null, null, null);
                chosenId = identityChooser.getAliasId(idAlias);
                if (chosenId == null)
                    throw new InvalidHandshakeException("Couldn't choose identity for in connection.");
            }

            replyAttrs.putBytes(ID_ATTR, chosenId);
        }

        if (handshakeSteps <= 2) {
            // This needs to happen before sending the reply so the client doesn't
            // start TLS before we're ready
            ctx.fireUserEventTriggered(handshakeCompletedEvent(fhm.attributes, chosenId, peerId));
            ctx.pipeline().remove(this);
        }

        peerAttrs.add(fhm.attributes);
        myAttrs.add(replyAttrs);
        ctx.channel().writeAndFlush(
                new NetworkMessage(NetworkMessage.CTRL_MSG, new SecondHandshakeMessage(replyAttrs)));
    }

    private PreTLSHandshakeCompleted handshakeCompletedEvent(Attributes peerAttrs, byte[] chosenId, byte[] peerId) {
        return new PreTLSHandshakeCompleted(peerAttrs, chosenId, peerId);
    }

    @Override
    protected void handleNthHandshakeMessage(ChannelHandlerContext ctx, NthHandshakeMessage nhm)
            throws InvalidHandshakeException {
        // TODO make a way for this to be compatible with pre TLS handshake
        throw new UnsupportedOperationException("TODO"); // TODO

        /*
         * if (nhm.handshakeStep / 2 != peerAttrs.size())
         * throw new InvalidHandshakeException(
         * "Invalid NthHandshakeMessage step for InHandshakeHandler: " +
         * nhm.handshakeStep);
         * peerAttrs.add(nhm.attributes);
         * 
         * int nextStep = nhm.handshakeStep + 1;
         * 
         * Attributes replyAttrs = validator.getNthHandshakeAttributes(connectionId,
         * nextStep, peerAttrs, myAttrs);
         * 
         * if (nextStep <= handshakeSteps) {
         * myAttrs.add(replyAttrs);
         * ctx.channel().writeAndFlush(
         * new NetworkMessage(NetworkMessage.CTRL_MSG,
         * new NthHandshakeMessage(replyAttrs, nextStep)));
         * } else {
         * var finalPeerAttributes = new Attributes();
         * for (var attr : peerAttrs)
         * finalPeerAttributes.putAll(attr);
         * 
         * ctx.fireUserEventTriggered(new HandshakeCompleted(finalPeerAttributes));
         * ctx.pipeline().remove(this);
         * }
         */
    }

}
