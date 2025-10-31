package pt.unl.fct.di.novasys.network;

import java.util.List;

import pt.unl.fct.di.novasys.network.data.Attributes;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeAttributesException;
import pt.unl.fct.di.novasys.network.exceptions.InvalidHandshakeException;

public interface AttributeValidator {

    String CHANNEL_MAGIC_ATTRIBUTE = "magic_number";

    AttributeValidator ALWAYS_VALID = attr -> true;

    boolean validateAttributes(Attributes peerAttrs);

    /**
     * Returns a copy of {@code myAttr} adapted to act as a reply to the received
     * {@code peerAttr}.
     * <p>
     * The default implementation simply calls
     * {@link AttributeValidator#validateAttributes(Attributes)} and
     * returns the given {@code myAttr}.
     *
     *
     * @param connectionId the id of the connection to be created when the handshake
     *                     succeeds.
     * @param peerAttr     the attributes received from the first handshake message.
     * @param myBaseAttrs   the original attributes of the current channel.
     * @return The attributes to be sent with the second handshake message.
     * @throws InvalidHandshakeAttributesException if the last received peer
     *                                             attributes were invalid for this
     *                                             handshake step.
     */
    default Attributes getSecondHandshakeAttributes(long connectionId, Attributes peerAttrs, Attributes myBaseAttrs)
            throws InvalidHandshakeException {
        if (validateAttributes(peerAttrs))
            return myBaseAttrs;
        else
            throw new InvalidHandshakeAttributesException(peerAttrs, "Invalid first handshake attributes.");
    }

    /**
     * <b>This should only be used to handle handshake messages when
     * {@code handshakeN >= 3}</b>. Before that, use
     * {@link #getSecondHandshakeAttributes}
     * <p>
     * Validates the last attribute in {@code peerAttrs} and constructs a new
     * {@link Attributes} to act as a reply for the the '{@code handshakeN - 1}'th
     * handshake step.
     * <p>
     * If this is the last handshake step, and no reply attributes are required,
     * {@code null} may be returned.
     * <p>
     * The default implementation simply calls
     * {@link AttributeValidator#validateAttributes(Attributes)} and returns
     * {@code myAttrs.getLast()}.
     *
     * @param connectionId the id of the connection to be created when the handshake
     *                     succeeds.
     * @param handshakeN   the handshake step to be generated.
     * @param peerAttrs    the attributes received from the peer's handshake
     *                     messages, where {@code peerAttrs.getLast()} is the
     *                     attributes that was just received.
     * @param mySentAttrs  the previous attributes sent as a part of this
     *                     handshake.
     * @return The attributes to be sent with the {@code handshakeN}th handshake
     *         message.
     * @throws InvalidHandshakeAttributesException if the last received peer
     *                                             attributes were invalid for this
     *                                             handshake step.
     */
    default Attributes getNthHandshakeAttributes(long connectionId, int handshakeN,
            List<Attributes> peerAttrs, List<Attributes> mySentAttrs)
            throws InvalidHandshakeException {
        if (validateAttributes(peerAttrs.getLast()))
            return mySentAttrs.getLast();
        else
            throw new InvalidHandshakeAttributesException(peerAttrs.getLast(), handshakeN - 1);
    }

}
