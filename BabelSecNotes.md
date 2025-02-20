
# babel.core.BabelSecurity

**NOTE ON IDENTITIES:** "Identity" here can refer to one of:
- What the user sees as a peer identifier: A byte array hash of the public key.
- Credentials: A certificate (in Babel's format) containing the peer's public
    key and the "identifier" described above.
- Private credentials: "Credentials" + the corresponding private key.


## Attributes

- `keyStore` -- The persistent `KeyStore` (loaded and saved) used for asymmetric key pairs and certificates.  
    Only stores asymmetric key pairs and certificates.
- `ephKeyStore` -- A temporary `KeyStore` whose contents are lost after the program ends.
- `keyManager` -- The `KeyManager` passed to channels for automatic selection of private keys,
    given the identity (or certificate of an identity) intended to be used.
- `trustStore` -- The persistent `KeyStore` (loaded and saved) used for trusted public keys and certificates.  
    Only stores certificates. (I don't remember if can store pub keys without certs)
- `ephTrustStore` -- A temporary `KeyStore` whose contents are lost after the program ends.
- `trustManager` -- The "trust" store passed to channels for customized selection of which
    received certificates should be trusted.
- `secretStore` -- The persistent `KeyStore` (loaded and saved) used for symmetric keys.
    Only stores symmetric keys.
- `ephSecretStore` -- A temporary `KeyStore` whose contents are lost after the program ends.
- `idAliasMapper` -- overly complicated two-way hash map to translate between
    Babel identities (byte arrays) and their aliases (strings, possibly manually
    created) on the asymmetric key stores.
- `nonceRng` -- Secure random for generating Nonces, IVs, etc.
- `keyRng` -- Secure*r* random for generating keys.

## Methods

- `get[...]Store|Manager` -- self explanatory.
- General utilities:
    - `generateIv`, `generateIvParam`, `getSecureRandom`, `generateKeyPair`
- Self identity operations:
    - `deleteIdentity` -- Purges one of my identities from the key stores and id-alias mapper.  
    - `generateIdentity` -- Generates a new, random key-pair and identity, possibly with a user-defined alias.  
        If a KeyPair is passed, a certificate and identifier is generated for them and it's added to the key store.  
    - `addIdentity` -- Adds a private key and certificate (that should correspond to a Babel identity) to the key store.
    - `get(Default)IdentityCrypt` -- Gets an `IdentityCrypt` for performing cryptographic operations
        using the identity's keys.
    - `getAllIdentities` -- A set of all of my identity byte[]s and their key store aliases.
    - `getAllIdentitiesWithPrefix` -- ^ same but only identities whose aliases start with the given string.
    - `getIdentityAlias` -- The alias that identity is associated with in the key store.
    - `getAliasIdentity` -- ^ symmetric.
    - `getDefaultIdentity` -- The identity pair that is set as the default in the id-alias mapper.
- Trusted peers operations:
    - `addTrustedCertificate` -- self explanatory.
    - `addTrustedPeerIdentity` -- Any certificate that contains the added identity
    (and it's public key hash results in that identity) will be trusted.
    - `removeTrustedPeerIdentity` -- self explanatory.
    - `getTrustedCertificate` -- Gets a known certificate containing that identity.
    - `setTrustManagerPolicy` -- self explanatory.
- Symmetric key operations:  
    - `generateSecretFromPassword` -- Uses a PBKDFwith the given password and
    salt (or the configured defaults) to generate a secret key and store put it
    on the secret store.
    - `generateSecret` -- Generates a random secret key and puts it on the
    secret store.
    - `addSecret` -- Puts a secret key on the secret store.
    - `getSecretCrypt` -- An object to perform cryptographic operations on the
        secret key with the given alias, using the default configured algorithms.
- General cryptographic operations:  
    These methods act as a proxy for using the material stored in the KeyStores, using
    the configured defaults crypto algorithms and parameters unless explicitly passed by the user.  
    Only signature verification is done here, as other operations will be done
    through returned `Identity|SecretCrypt` objects.
    - `verifySignature` -- Tries to verify a signature by getting, from the
        trustStore, the known public key of the peerId passed, so signatures of
        known, trusted peers can be verified easily.
    - `initVerifySignature` -- Same as above, but returns the `Signature`
        object mid operation.
- Configuration methods:  
    - `loadConfig` -- Loads the configuration from a `Properties` object, passed
        and called by Babel's `loadConfig`. Any unspecified parameters use the
        default.


## Auxiliary classes

### CipherData (record)

Simply a pair (record) containing ciphertext bytes and the algorithm parameters
used, so the user knows how to decrypt it. In hindsight, there's already probably
a santardized way to do this I haven't seen.

### SecretCrypt

Class returned by `BabelSecurity`'s utilities to do cryptographic operations on
an symmetric key. Can return its associated data (key alias in key store, the
secret key) and performing MAC, decryption, and encryption operations.

### IdentityCrypt

Class returned by `BabelSecurity`'s utilities to do cryptographic operations on
an asymmetric key pair, which should be associated with a Babel identity (hence
the name). Currently it's only capable of returning its associated data
(pub/priv keys, id alias in key store, id bytes, key algorithm, associated
certificate chain) and signing. I didn't have time to get around the encryption
operations due to it requiring extra considerations (performance, key wrapping,
not every asym key algo can do encryption).

Yet again, in hindsight, it kinda sucks that it doesn't provide any utilities for
key wrapping, which would've been a prime use case for this. But it can be added
easily if we choose to keep this class.


### IdentityPair (record)

Simply a pair containing an identity's byte representation and its key store alias.

### IdentityGenerator (interface)

Interface used by `BabelSecurity` to know how to generate credentials (i.e.,
a KeyStore private key entry, containing the private key and a certificate (chain)
that has the public key and describes the associated identity). The default
implementation is `BabelCredentialHandler`, but it can be overridden in the
security configuration, as we chose to use public key hashes as identities by
default, but some applications may call for different schemes.


### SimpleIdentityGenerator (interface, extends IdentityGenerator)

Simply overrides the `IdentityGenerator.generateRandomCredentials()` method to
use a random key pair generated by `BabelSecurity` so implementations only have
to worry about implementing the `generateCredentials(KeyPair)` method.

I guess this could've simply been a `default` in `IdentityGenerator`.


### IdFromCertExtractor (interface)

Interface used by `BabelSecurity` to know how to get the identity from a
certificate. Default implementation is `BabelCredentialHandler`, and can be
overridden in the security configuration.

**Should be consistent with `IdentityGenerator`'s implementation!**


# babel.internal.security

## BabelCredentialHandler

The default implementation of the `SimpleIdentityGenerator` and
`IdFromCertExtractor` interfaces, used by `BabelSecurity`.

Extract identity form certificate:

- Must be X509 certificate;
- Get the string from the certificate's UNIQUE_IDENTIFIER RDN;
- Get a Base64 string of the hash of the public key in the certificate;
- Check if the UNIQUE_IDENTIFIER RDN and the Base64 hash are the same;
- Return the public key hash (the peer id).

Generate credentials:

- Creates a self signed certificate with the passed key pair and the Base64
  string of the public key hash as the UNIQUE_IDENTIFIER.


## IdAliasMapper

Essentially a two-way hash map to translate between Babel identities (byte
arrays) and their aliases (strings, possibly manually created) on the
asymmetric key stores.

Only supports one alias (therefore one active key pair) per identity. I'm going
to hate asking this, but is supporting more than one desirable?


## PeerIdEncoder

Class of static methods that simply converts public keys into the default
implementation of identities, and (de)base64s them. Used by the default
(overridable) implementations to make (or extract) the certificates, but also
used by the rest of the security classes to log errors and select default key
store aliases if none are given.

## X509BabelKeyManager

Class passed to channels for automatic selection of private keys,
given the identity (or certificate of an identity) intended to be used.

Extends `X509IKeyManager` which extends `javax.net.ssl.X509ExtendedKeyManager`,
so it can also be passed to TLS sockets.


## X509BabelTrustManager

Class passed to channels for customized selection of which received
certificates should be trusted.

Currently has three levels of trust policy, which can be set globally:

- `UNKNOWN`: Will accept any certificate that can be verified to have been signed
      by the correct public key and identity.  
      **Not safe** if the identity can't be derived from the public key and the 
      default certificate signature verification handler is used.
- `KNOWN_ID`: Will accept any certificate that can be verified to have been signed
      by the correct public key and the contained identity is in Babel's trust store.
      **Not safe** if the identity can't be derived from the public key and the 
      default certificate signature verification handler is used.
- `KNOWN_CERT`: Will only accept the exact certificates that are in Babel's trust store.

The following fields can be overridden by BabelSecurity's configuration:

- `tustPolicy` -- explained above.
- `targetTrustStore` -- Which trust store (if any) to store newly trusted
    certificates when using `UNKNOWN` and `KNOWN_ID` policies.  
    BabelSecurity can define it as the persistent trust store, the ephemeral
    one, or null.
- `trustUnknownPeerCallback` -- Called when `KNOWN_ID` or `KNOWN_CERT` policies
    are active and the certificate doesn't correspond to any known identity or
    certificate. If this returns true, the certificate is trusted anyways. By
    default, it's just a function that always returns false. The intended
    use-cases are for possibly:
    1. integration with one of the implemented Babel protocols to ask peers
       whether that new peer should be trusted;
    2. or prompting the user whether the peer should be trusted anyways.
- `verifyCertificateSignature` -- Called to verify certificates' signatures. By
    default, it just verifies whether the certificate was self-signed and its
    signature is valid. Could be overridden to, e.g., verify whether it was signed
    by an already trusted public key.
- `idExtractor` -- explained above, in [`IdFromCertExtractor`](#idfromcertextractor-interface).

Extends `X509ITrustManager` which extends `javax.net.ssl.X509ExtendedTrustManager`,
so it can also be passed to TLS sockets.

*Currently doesn't support operations on certificate chains.*

## X509CertificateChainPredicate (interface)

Functional interface used by `X509BabelTrustManager` for its callbacks


# Configuration

All configuration is done with the property prefix `babel.security`.

Configurable options ?DEFAULT:

- Handlers:
    - Identity extractor ?`BabelCredentialHandler`
    - Identity generator ?`BabelCredentialHandler`
- Key Store configurations:
    - Store type ?PKCS2
    - Store path ?./babelKeyStore.jks
    - Writeable (writable keystore path) ?null
    - Protection (one or the other):
        - Password ?""
        - Handler (a callback function) ?NONE
    - Default identity ?NONE (it'll be the first that is set)
- Trust Store configurations:
    - Store type ?keyStoreType
    - Store path ?./babelTrustStore.jks
    - Writeable (writable truststore path) ?null
    - Protection (one or the other):
        - Password ?""
        - Handler (a callback function) ?NONE
- Trust manager configurations:
    - Policy ?`UNKNOWN`
    - Save encountered ?true
    - Persist discovered certificates (ignored if the above is false) ?false
    - trustUnknownPeerCallback ?`(certChain, id) -> false`
    - verifySignatureCallback ?`(certChain, id) -> certChain[0].verify(certChain[0].getPublicKey())`
- Secret Store configurations:
    - Store type ?keyStoreType
    - Store path ?./babelSecretStore.jks
    - Writeable (writable truststore path) ?null
    - Protection (one or the other):
        - Password ?""
        - Handler (a callback function) ?NONE
- Default asymmetric key parameters:
    - Key algorithm ?RSA
    - Key length (ignored if key parameters is supplied) ?2048
    - Key parameters ?(2048 bits, F4 public exponent)
- Default symmetric key parameters:
    - Key algorithm ?AES
    - Key length (ignored if key parameters is supplied) ?128 (oh damn that's not great)
    - Key parameters <- NOT IMPLEMENTED
    - Default Password Based Key Derivation Function parameters:
        - Algorithm ?"PBKDF2WithHmacSHA256"
        - Salt ?*a stupid hardcoded string*
        - Iterations ?131072
        - Resulting key length ?256
        - Startup password ?null  
            (If non-null, the password is used to generate a secret key and add
             it to the key store on startup.)
        - Startup password derived key alias ?null  
            (The alias to store the initial pwd derived key with.)
- Default cipher parameters (defaults loosely follow TLS):  
    **Note:** The cipher algorithm isn't specified here because the key
    algorithm is what is used.
    - Transformation ?null
    - or:
        - Mode of operation ?"GCM"
        - Padding ?"NoPadding"
    - Cipher parameter supplier ?`() -> new GCMParameterSpec(128, generateIv(12))`
    - IV size ?null  
        (ignored if the above is set.)
- Other default hash parameters:
    - Hash algorithm ?SHA256
    - MAC algorithm ?null  
        (when null, an H-MAC with the configured hash algorithm is used.)

