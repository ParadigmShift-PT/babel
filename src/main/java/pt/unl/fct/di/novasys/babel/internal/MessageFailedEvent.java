package pt.unl.fct.di.novasys.babel.internal;

import java.util.Optional;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class MessageFailedEvent extends InternalEvent {

    private final BabelMessage msg;
    private final Host to;
    private final Optional<byte[]> toId;
    private final Throwable cause;
    private final int channelId;

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageFailedEvent(BabelMessage msg, Host to, Throwable cause, int channelId) {
        this(msg, to, null, cause, channelId);
    }

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageFailedEvent(BabelMessage msg, Host to, byte[] toId, Throwable cause, int channelId) {
        super(EventType.MESSAGE_FAILED_EVENT);
        this.msg = msg;
        this.to = to;
        this.toId = Optional.ofNullable(toId);
        this.cause = cause;
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "MessageFailedEvent{" +
                "msg=" + msg +
                ", to=" + to +
                toId.map(id -> ", toId=" + PeerIdEncoder.encodeToString(id)).orElse("") +
                ", cause=" + cause +
                ", channelId=" + channelId +
                '}';
    }

    public final Host getTo() {
        return to;
    }

    public final Optional<byte[]> getToId() {
        return this.toId;
    }

    public int getChannelId() {
        return channelId;
    }

    public Throwable getCause() {
        return cause;
    }

    public BabelMessage getMsg() {
        return msg;
    }

}
