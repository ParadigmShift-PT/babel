package pt.unl.fct.di.novasys.network.pipeline;

import io.netty.channel.*;
import pt.unl.fct.di.novasys.network.AttributeValidator;
import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;
import pt.unl.fct.di.novasys.network.messaging.NetworkMessage;
import pt.unl.fct.di.novasys.network.messaging.control.ControlMessage;
import pt.unl.fct.di.novasys.network.messaging.control.FirstHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.InvalidAttributesMessage;
import pt.unl.fct.di.novasys.network.messaging.control.NthHandshakeMessage;
import pt.unl.fct.di.novasys.network.messaging.control.SecondHandshakeMessage;
import pt.unl.fct.di.novasys.network.userevents.HandshakeCompleted;

import java.util.ArrayList;
import java.util.List;

public class InHandshakeHandler extends ChannelDuplexHandler {

    public static final String NAME = "InHandshakeHandler";

    protected final AttributeValidator validator;
    protected final long connectionId;
    protected final List<Attributes> peerAttrs;
    protected final List<Attributes> myAttrs;
    protected final Attributes baseAttrs;
    protected final int handshakeSteps;

    public InHandshakeHandler(long connectionId, AttributeValidator validator,
            Attributes baseAttrs, int handshakeSteps) {
        this.connectionId = connectionId;
        this.validator = validator;
        this.myAttrs = new ArrayList<>(handshakeSteps / 2);
        this.peerAttrs = new ArrayList<>((handshakeSteps + 1) / 2);
        this.baseAttrs = baseAttrs;
        this.handshakeSteps = handshakeSteps;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
        NetworkMessage msg = (NetworkMessage) obj;
        if (msg.code != NetworkMessage.CTRL_MSG)
            throw new Exception("Received application message in inHandshake: " + msg);

        try {
            ControlMessage cMsg = (ControlMessage) msg.payload;
            switch (cMsg.type) {
                case ControlMessage.Type.HEARTBEAT -> {
                    return;
                }
                case ControlMessage.Type.FIRST_HS -> handleFirstHandshakeMessage(ctx, (FirstHandshakeMessage) cMsg);
                case ControlMessage.Type.NTH_HS -> handleNthHandshakeMessage(ctx, (NthHandshakeMessage) cMsg);
                case ControlMessage.Type.INVALID_ATTR ->
                    throw new Exception("Attributes refused from in connection with " + ctx.channel().remoteAddress());
                default -> throw new Exception("Received unexpected control message in inHandshake: " + msg);
            }
        } catch (InvalidHandshakeException e) {
            ctx.channel().writeAndFlush(new NetworkMessage(NetworkMessage.CTRL_MSG, new InvalidAttributesMessage()));
            throw e;
        }
    }

    protected void handleFirstHandshakeMessage(ChannelHandlerContext ctx, FirstHandshakeMessage fhm)
            throws InvalidHandshakeException {
        Attributes replyAttrs = validator.getSecondHandshakeAttributes(connectionId, fhm.attributes, baseAttrs);

        peerAttrs.add(fhm.attributes);
        myAttrs.add(replyAttrs);
        ctx.channel().writeAndFlush(
                new NetworkMessage(NetworkMessage.CTRL_MSG, new SecondHandshakeMessage(replyAttrs)));

        if (handshakeSteps <= 2) {
            ctx.fireUserEventTriggered(new HandshakeCompleted(fhm.attributes));
            ctx.pipeline().remove(this);
        }
    }

    protected void handleNthHandshakeMessage(ChannelHandlerContext ctx, NthHandshakeMessage nhm)
            throws InvalidHandshakeException {
        if (nhm.handshakeStep / 2 != peerAttrs.size())
            throw new InvalidHandshakeException(
                    "Invalid NthHandshakeMessage step for InHandshakeHandler: " + nhm.handshakeStep);
        peerAttrs.add(nhm.attributes);

        int nextStep = nhm.handshakeStep + 1;

        Attributes replyAttrs = validator.getNthHandshakeAttributes(connectionId, nextStep, peerAttrs, myAttrs);

        if (nextStep <= handshakeSteps) {
            myAttrs.add(replyAttrs);
            ctx.channel().writeAndFlush(
                    new NetworkMessage(NetworkMessage.CTRL_MSG,
                            new NthHandshakeMessage(replyAttrs, nextStep)));
        } else {
            var finalPeerAttributes = new Attributes();
            for (var attr : peerAttrs)
                finalPeerAttributes.putAll(attr);

            ctx.fireUserEventTriggered(new HandshakeCompleted(finalPeerAttributes));
            ctx.pipeline().remove(this);
        }

    }

}
