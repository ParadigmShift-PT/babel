# Babel Self-Discovery and Self-Configuration Core

A variant of the Java framework for developing distributed protocols. Uses [network-layer](https://codelab.fct.unl.pt/di/research/tardis/wp6/babel/babel-networklayer) for network communication.

This variant was developed in the context of the TaRDIS European project to support the development of distributed protocols to support swarm systems with self-discovery and self-configuration features.

The current core (version 0.1.0) supports Configurable discovery mechanisms for protocols that extend the DiscoverableProtocol class (instead of the GenericProtocol).

Currently Multicast and Broadcast discovery is supported. Broadcast is still under testing.

##Authors

- Rafael Matos (rd.matos@campus.fct.unl.pt)
- João Leitão (jc.leitao@fct.unl.pt)

# Installation

### Dependencies

Copy and paste the following block inside your ```pom.xml dependencies``` block.

```
<dependency>
	<groupId>pt.unl.fct.di.novasys.babel</groupId>
	<artifactId>babel-sc-core</artifactId>
	<version>[0.1.0,)</version>
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
