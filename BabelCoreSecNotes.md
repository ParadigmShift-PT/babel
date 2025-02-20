# babel.handlers

The following handlers have a new method that also take the related peer id, but
with a default implementation that ignores it.

- `MessageFailedHandler`
- `MessageInHandler`
- `MessageSentHandler`

Example:

```java
public interface MessageFailedHandler<T extends ProtoMessage> {
    void onMessageFailed(T msg, Host to, short destProto, Throwable cause, int channelId);

    default void onMessageFailed(T msg, Host to, byte[] toId, short destProto, Throwable cause, int channelId) {
        onMessageFailed(msg, to, destProto, cause, channelId);
    }
}
```

The following new handlers are functional interfaces that extend their "non-secure"
counterparts, and override the methods so the version *with* peer id has *no default
implementation*, and the version *without* peer id has a default implementation that
passes peer id as null, so it can be also used by non-secure channels.

- `SecureMessageFailedHandler`
- `SecureMessageInHandler`
- `SecureMessageSentHandler`

Example:

```java
public interface SecureMessageFailedHandler<T extends ProtoMessage> extends MessageFailedHandler<T> {
    @Override
    void onMessageFailed(T msg, Host to, byte[] toId, short destProto, Throwable cause, int channelId);

    @Override
    default void onMessageFailed(T msg, Host to, short destProto, Throwable cause, int channelId) {
        onMessageFailed(msg, to, null, destProto, cause, channelId);
    }
}
```


# babel.core

## Additions to `GenericProtocol`

### New fields

- `IdentityCrypt defaultIdentityCrypt`
- `IdentityPair defaultIdentity`
- `SecretCrypt defaultSecret`

**Note:** I decided to not set a default *secure* channel separate from a default
non-secure channel.

### New methods

- Communication methods:
    - Several `registerMessageHandler`s with pretty much every combination of the
      secure and non-secure handlers described above;  
      **Note:** If a secure handler is triggered by a non-secure channel, the
      peer id passed to it will be `null`.
    - `createSecureChannel(channelName, props)` -- Creates a new secure channel that
      uses all available identities;
    - `createSecureChannel(channelName, props, identity)` -- Creates a new secure
      channel that uses only the specified identity;
    - `createSecureChannelWithIdentities(channelName, props, identities...)` -- Creates
      a new secure channel that uses only the specified identities;
    - `createSecureChannelWithAliases(channelName, props, identityAliases...)` -- same;
    - `createSecureChannelWithProtoIdentities(channelName, props)` -- Creates a new
      secure channel that uses only the identities whose aliases start with "protoName.";
    - several `sendMessage` methods that take a peer id instead of a Host;
    - `openConnection(Host peer, byte[] peerId, int channelId?)` -- Opens a new
      connection to the specified peer identity with the specified address. Should
      only be used for secure channels.
    - some `closeConnection` methods that take a peer id instead of a Host.
- Identity management methods:
  (mostly proxies for `babelSecurity` methods, with access to channel defaults)
    - `generateIdentity(boolean persistOnDisk = true)` -- Calls
      `babelSecurity.generateIdentityWithAliasPrefix(persistOnDisk, protoName)` and
      sets the new identity as the default for the protocol, if none is already set;
    - `generateIdentity(boolean persistOnDisk = true, String alias)` -- same, but
      doesn't auto-generate the alias;
    - `generateIdentity(boolean persistOnDisk = true, String alias?, KeyPair keyPair)` --
      same, but don't auto-generate the identity's key pair;
    - `setDefaultProtoIdentity(String alias or byte[] identity)` -- self explanatory;
    - `getOrGenerateDefaultProtoIdentity()` -- self explanatory;
    - `getDefaultProtoIdentityCrypt()` -- self explanatory (throws `NoSuchElementException`);
    - `getDefaultProtoIdentity()` -- self explanatory (throws `NoSuchElementException`).
- Secret management methods:
  (mostly proxies for `babelSecurity` methods, with access to channel defaults)
    - very similar to the ones above.
    - `generateSecretFromPassword(...)`
    - `generateSecret(...)`
    - `addSecret(...)`
    - `setDefaultProtoSecret(String alias)`
    - `getDefaultProtoSecret()`

**Note:** The `handleMessageIn/Failed/Sent` methods were adapted to also pass
the peer id to the handler (which is `null` if the channel is non-secure, and
ignored if the handler is non-secure).

## `SecureChannelToProtoForwarder`

Extension of `ChannelToProtoForwarder` that registers consumers for secure channel
events, that are the same as the regular ones (MessageIn, MessageSent, MessageFailed),
but also include the destination (or source) peer identity.

## Additions to `Babel`

### New fields

- `Map<String, SecureChannelInitializer<?>> secureChannelInitializers`
- `Map<Integer, Triple<SecureIChannel<BabelMessage>, SecureChannelToProtoForwarder, BabelMessageSerializer>> secureChannelMap`  
    Parallel to `channelMap`.  
    Generally, when Babel needs to get a channel, first it checks if it's on `channelMap`,
    else, it tries to get from `secureChannelMap`.  
    If, instead of the target address, the target peer identity is specified when
    getting a channel, Babel looks only on `secureChannelMap`, as regular channels
    don't use identities.

### New methods

- `createSecureChannel` -- Creates a new secure channel using the specified ids or aliases if non-null. Otherwise, uses every identity available;
- `sendMessage` that uses target id instead of target address, for secure channels only;
- `closeConnection` that uses target id instead of target address, for secure channels only;
- `openConnection` that takes target id in addition to target address, for secure channels only;
- `loadConfig` now also calls `BabelSecurity.getInstance().loadConfig()`

