package pt.unl.fct.di.novasys.channel.secure;

import java.util.Optional;

import pt.unl.fct.di.novasys.channel.ChannelListener;
import pt.unl.fct.di.novasys.network.data.Host;

public interface SecureChannelListener<T> extends ChannelListener<T> {

    void deliverMessage(T msg, Host from, byte[] peerId);

    void messageSent(T msg, Host to, byte[] peerId);

    void messageFailed(T msg, Optional<Host> to, byte[] peerId, Throwable cause);
}
