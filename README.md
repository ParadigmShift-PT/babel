# Babel Self-Discovery, Self-Configuration and Security Core

A variant of the Java framework for developing distributed protocols. Uses [a fork of Network-layer](https://codelab.fct.unl.pt/di/research/tardis/wp6/network-layer-cryptography-support) for network communication and to support secure channels.

This variant was developed in the context of the TaRDIS European project to support the development of distributed protocols to support swarm systems with self-discovery, self-configuration and security features.

The current core (version 0.4.0) supports Configurable discovery mechanisms for protocols that extend the DiscoverableProtocol class (instead of the GenericProtocol).

Currently Multicast and Broadcast discovery is supported. Copy of the configuration inside the same network is supported with confirmation with multiple nodes. Search for the configuration in DNS servers is also supported.

Cryptographic material management, utilities and secure channels backed by the fork of Network-layer. It is under testing.

## Authors

- Rafael Matos (rd.matos@campus.fct.unl.pt)
- Felipe Carmo (fp.carmo@campus.fct.unl.pt)
- João Leitão (jc.leitao@fct.unl.pt)

# Installation

### Dependencies

Copy and paste the following block inside your ```pom.xml dependencies``` block.

```
<dependency>
	<groupId>pt.unl.fct.di.novasys.babel</groupId>
	<artifactId>babel-sc-core</artifactId>
	<version>[0.4.0,)</version>
</dependency>
```


### Repository Setup

If you haven't already done so, you will need to add the following to your ```pom.xml``` file.

```
<repositories>
    <repository>
        <id>novasys-mvn</id>
        <url>https://novasys.di.fct.unl.pt/packages/mvn</url>
    </repository>
</repositories>
```
