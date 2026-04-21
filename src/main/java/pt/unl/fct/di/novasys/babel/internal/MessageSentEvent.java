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
public class MessageSentEvent extends InternalEvent {

    private final BabelMessage msg;
    private final Host to;
    private final Optional<byte[]> toId;
    private final int channelId;

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageSentEvent(BabelMessage msg, Host to, int channelId) {
        this(msg, to, null, channelId);
    }

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageSentEvent(BabelMessage msg, Host to, byte[] toId, int channelId) {
        super(EventType.MESSAGE_SENT_EVENT);
        this.msg = msg;
        this.to = to;
        this.toId = Optional.ofNullable(toId);
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "MessageSentEvent{" +
                "msg=" + msg +
                ", to=" + to +
                toId.map(id -> ", toId=" + PeerIdEncoder.encodeToString(id)).orElse("") +
                ", channelId=" + channelId +
                '}';
    }

    /**
     * Returns the remote host to which the message was successfully sent.
     *
     * @return destination host
     */
    public final Host getTo() {
        return to;
    }

    /**
     * Returns the cryptographic peer identity of the recipient, if known.
     *
     * @return an {@code Optional} containing the recipient's raw ID bytes, or empty if not set
     */
    public final Optional<byte[]> getToId() {
        return this.toId;
    }

    /**
     * Returns the ID of the channel on which the message was sent.
     *
     * @return channel ID
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Returns the message that was successfully delivered.
     *
     * @return the sent {@link BabelMessage}
     */
    public BabelMessage getMsg() {
        return msg;
    }

}
