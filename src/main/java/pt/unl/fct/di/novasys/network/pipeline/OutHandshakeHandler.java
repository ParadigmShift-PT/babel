package pt.unl.fct.di.novasys.network.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.messaging.control.ControlMessage;
import pt.unl.fct.di.novasys.network.messaging.control.FirstHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.InvalidAttributesMessage;
import pt.unl.fct.di.novasys.network.messaging.control.NthHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.SecondHandshakeMessage;
import pt.unl.fct.di.novasys.network.userevents.HandshakeCompleted;

public class OutHandshakeHandler extends ChannelDuplexHandler {

    private static final Logger logger = LogManager.getLogger(OutHandshakeHandler.class);

    public static final String NAME = "OutHandshakeHandler";

    protected final long connectionId;
    protected final AttributeValidator validator;
    protected final List<Attributes> peerAttrs;
    protected final List<Attributes> myAttrs;
    protected final int handshakeSteps;

    public OutHandshakeHandler(long connectionId, AttributeValidator validator, Attributes myAttrs, int handshakeSteps) {
        this.connectionId = connectionId;
        this.validator = validator;
        this.myAttrs = new ArrayList<>((handshakeSteps + 1) / 2);
        this.myAttrs.add(myAttrs);
        this.peerAttrs = new ArrayList<>(handshakeSteps / 2);
        this.handshakeSteps = handshakeSteps;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(new NetworkMessage(NetworkMessage.CTRL_MSG, new FirstHandshakeMessage(myAttrs.getFirst())));
        ctx.fireChannelActive();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
        NetworkMessage msg = (NetworkMessage) obj;
        if (msg.code != NetworkMessage.CTRL_MSG)
            throw new Exception("Received application message in outHandshake: " + msg);

        ControlMessage cMsg;
        try {
            cMsg = (ControlMessage) msg.payload;
            switch (cMsg.type) {
                case ControlMessage.Type.HEARTBEAT -> {
                    return;
                }
                case ControlMessage.Type.SECOND_HS -> handleSecondHandshakeMessage(ctx, (SecondHandshakeMessage) cMsg);
                case ControlMessage.Type.NTH_HS -> handleNthHandshakeMessage(ctx, (NthHandshakeMessage) cMsg);
                case ControlMessage.Type.INVALID_ATTR -> throw new Exception("Attributes refused");
                default -> throw new Exception("Received unexpected control message in outHandshake: " + msg);
            }
        } catch (InvalidHandshakeException e) {
            ctx.channel().writeAndFlush(new NetworkMessage(NetworkMessage.CTRL_MSG, new InvalidAttributesMessage()));
            logger.debug("Invalid attributes received from connection with " + ctx.channel().remoteAddress() + ": " + e
                    + (e.getCause() != null ? " " + e.getCause() : ""));
            throw e;
        }
    }

    protected void handleSecondHandshakeMessage(ChannelHandlerContext ctx, SecondHandshakeMessage shm)
            throws InvalidHandshakeException {
        peerAttrs.add(shm.attributes);
        byte nextStep = 3;
        Attributes replyAttrs = validator.getNthHandshakeAttributes(connectionId, nextStep, peerAttrs, myAttrs);

        if (handshakeSteps > 2) {
            myAttrs.add(replyAttrs);
            ctx.channel().writeAndFlush(
                    new NetworkMessage(NetworkMessage.CTRL_MSG, new NthHandshakeMessage(replyAttrs, nextStep)));
        }

        if (handshakeSteps == 2 || handshakeSteps == 3) {
            ctx.fireUserEventTriggered(new HandshakeCompleted(shm.attributes));
            ctx.pipeline().remove(this);
        }
    }

    protected void handleNthHandshakeMessage(ChannelHandlerContext ctx, NthHandshakeMessage nhm)
            throws InvalidHandshakeException {
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
    }

}
