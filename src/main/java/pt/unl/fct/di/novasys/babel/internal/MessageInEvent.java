package pt.unl.fct.di.novasys.babel.internal;

import java.util.Optional;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.internal.security.PeerIdEncoder;
import pt.unl.fct.di.novasys.network.data.Host;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class MessageInEvent extends InternalEvent {

    private final BabelMessage msg;
    private final Host from;
    private final Optional<byte[]> fromId;
    private final int channelId;

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageInEvent(BabelMessage msg, Host from, int channelId) {
        this(msg, from, null, channelId);
    }

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageInEvent(BabelMessage msg, Host from, byte[] fromId, int channelId) {
        super(EventType.MESSAGE_IN_EVENT);
        this.from = from;
        this.msg = msg;
        this.fromId = Optional.ofNullable(fromId);
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "MessageInEvent{" +
                "msg=" + msg +
                ", from=" + from +
                fromId.map(id -> ", fromId=" + PeerIdEncoder.encodeToString(id)).orElse("") +
                ", channelId=" + channelId +
                '}';
    }

    public final Host getFrom() {
        return this.from;
    }

    public final Optional<byte[]> getFromId() {
        return this.fromId;
    }

    public int getChannelId() {
        return channelId;
    }

    public BabelMessage getMsg() {
        return msg;
    }

}
