package pt.unl.fct.di.novasys.network.tls.pipeline;

import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.data.Bytes;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.messaging.control.FirstHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.NthHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.SecondHandshakeMessage;
import pt.unl.fct.di.novasys.network.pipeline.OutHandshakeHandler;
import pt.unl.fct.di.novasys.network.security.X509IKeyManager;
import pt.unl.fct.di.novasys.network.tls.userevents.PreTLSHandshakeCompleted;

public class OutPreTLSHandshakeHandler extends OutHandshakeHandler {

    // TODO just copied from OutHandshakeHandler for now

    private static final Logger logger = LogManager.getLogger(OutPreTLSHandshakeHandler.class);

    private final X509IKeyManager identityChooser;

    private byte[] chosenId;
    private Optional<byte[]> expectedId;

    public OutPreTLSHandshakeHandler(long connectionId, AttributeValidator validator, Attributes myAttrs,
            int handshakeSteps, X509IKeyManager identityChooser) {
        super(connectionId, validator, myAttrs, handshakeSteps);
        this.identityChooser = identityChooser;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.trace("Starting out handshake...");

        var firstHsAttrs = myAttrs.getFirst().shallowClone();

        expectedId = Optional.ofNullable(firstHsAttrs.getBytes(EXPECTED_ID_ATTR));

        synchronized (identityChooser) {
            // TODO specify keytype?
            var myAlias = identityChooser.chooseClientAlias(new String[0], null, null);
            chosenId = identityChooser.getAliasId(myAlias);
        }

        firstHsAttrs.putBytes(ID_ATTR, chosenId);

        ctx.channel().writeAndFlush(
                new NetworkMessage(NetworkMessage.CTRL_MSG, new FirstHandshakeMessage(firstHsAttrs)));
        ctx.fireChannelActive();
    }

    @Override
    protected void handleSecondHandshakeMessage(ChannelHandlerContext ctx, SecondHandshakeMessage shm)
            throws InvalidHandshakeException {
        peerAttrs.add(shm.attributes);
        byte nextStep = 3;
        Attributes replyAttrs = validator.getNthHandshakeAttributes(connectionId, nextStep, peerAttrs, myAttrs);

        if (handshakeSteps > 2) {
            // TODO make a way for this to be compatible with pre TLS handshake
            throw new UnsupportedOperationException("TODO");
            /*
            myAttrs.add(replyAttrs);
            ctx.channel().writeAndFlush(
                    new NetworkMessage(NetworkMessage.CTRL_MSG, new NthHandshakeMessage(replyAttrs, nextStep)));
            */
        }

        if (handshakeSteps == 2 || handshakeSteps == 3) {
            //ctx.fireUserEventTriggered(new HandshakeCompleted(shm.attributes));
            ctx.fireUserEventTriggered(handshakeCompletedEvent(shm.attributes));
            ctx.pipeline().remove(this);
        }
    }

    // TODO should these be the responsability of the channel?
    public static final String ID_ATTR = "identity";
    public static final String EXPECTED_ID_ATTR = "expected_identity";

    private PreTLSHandshakeCompleted handshakeCompletedEvent(Attributes peerAttrs)
            throws InvalidHandshakeAttributesException {
        byte[] peerId = peerAttrs.getBytes(ID_ATTR);
        if (peerId == null || expectedId.map(expected -> !Arrays.equals(peerId, expected)).orElse(false))
            throw new InvalidHandshakeAttributesException(peerAttrs, "Given identity %s did not match the expected %s"
                    .formatted(Bytes.of(peerId), Bytes.of(expectedId.orElse(null))));

        return new PreTLSHandshakeCompleted(peerAttrs, chosenId, peerId);
    }

    @Override
    protected void handleNthHandshakeMessage(ChannelHandlerContext ctx, NthHandshakeMessage nhm)
            throws InvalidHandshakeException {
        // TODO make a way for this to be compatible with pre TLS handshake
        throw new UnsupportedOperationException("TODO"); // TODO

        /*
        if (nhm.handshakeStep / 2 != myAttrs.size())
            throw new InvalidHandshakeException(
                    "Invalid NthHandshakeMessage step for OutHandshakeHandler: " + nhm.handshakeStep);
        peerAttrs.add(nhm.attributes);

        int nextStep = nhm.handshakeStep + 1;

        Attributes replyAttrs = validator.getNthHandshakeAttributes(connectionId, nextStep, peerAttrs, myAttrs);

        if (nextStep <= handshakeSteps) {
            myAttrs.add(replyAttrs);
            ctx.channel().writeAndFlush(
                    new NetworkMessage(NetworkMessage.CTRL_MSG,
                            new NthHandshakeMessage(replyAttrs, nextStep)));
        }

        if (handshakeSteps == nhm.handshakeStep || handshakeSteps == nextStep) {
            var finalPeerAttributes = new Attributes();
            for (var attr : peerAttrs)
                finalPeerAttributes.putAll(attr);

            ctx.fireUserEventTriggered(new HandshakeCompleted(finalPeerAttributes));
            ctx.pipeline().remove(this);
        }
        */
    }

}
