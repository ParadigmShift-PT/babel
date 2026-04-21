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

    /**
     * Returns the remote host to which delivery was attempted.
     *
     * @return destination host
     */
    public final Host getTo() {
        return to;
    }

    /**
     * Returns the cryptographic peer identity of the intended recipient, if known.
     *
     * @return an {@code Optional} containing the recipient's raw ID bytes, or empty if not set
     */
    public final Optional<byte[]> getToId() {
        return this.toId;
    }

    /**
     * Returns the ID of the channel on which the send failed.
     *
     * @return channel ID
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Returns the exception or error that caused the message delivery to fail.
     *
     * @return the failure cause
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the message that failed to be delivered.
     *
     * @return the failed {@link BabelMessage}
     */
    public BabelMessage getMsg() {
        return msg;
    }

}
