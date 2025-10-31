package pt.unl.fct.di.novasys.network.tls.userevents;

import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.userevents.HandshakeCompleted;

public class PreTLSHandshakeCompleted extends HandshakeCompleted {

    private final byte[] selectedId;
    private final byte[] peerId;

    public PreTLSHandshakeCompleted(Attributes peerAttrs, byte[] selectedId, byte[] peerId) {
        super(peerAttrs);
        this.selectedId = selectedId;
        this.peerId = peerId;
    }

    public byte[] getSelectedId() {
        return selectedId;
    }

    public byte[] getPeerId() {
        return peerId;
    }

}
