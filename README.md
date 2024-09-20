# Babel Self-Discovery, Self-Configuration and Security Core

A variant of the Java framework for developing distributed protocols. Uses [a fork of Network-layer](https://codelab.fct.unl.pt/di/research/tardis/wp6/network-layer-cryptography-support) for network communication that has been enriched to support several different secure channels (providing different guarantees).

This variant was developed in the context of the TaRDIS European project to support the development of distributed protocols to support swarm systems with self-discovery, self-configuration, and security features.

The current core (version 0.5.15) supports Configurable discovery mechanisms for protocols that extend the DiscoverableProtocol class (instead of the GenericProtocol).

Currently Multicast and Broadcast discovery is supported. Copy of the configuration being used for distinct protocols in swarm elements operating within the same local network is supported with optional confirmation requiring multiple inputs from different swarm participants. Obtaining protocol initial configuration from special TXT records associated with a domain through DNS servers is also supported.

Self management of protocols is supported by having protocols extend a specific interface and providing specialized setters and getters for protocol parameters that can be managed autonomically throughout the life cycle of the swarm system.

Cryptographic material management, utilities, and secure channels backed by the fork of Network-layer is provided, being these security features under testing.

## Authors

- Rafael Matos (rd.matos@campus.fct.unl.pt)
- Felipe Rossi (fp.carmo@campus.fct.unl.pt)
- Tomás Galvão (t.galvao@campus.fct.unl.pt)
- João Leitão (jc.leitao@fct.unl.pt)

# Installation

### Dependencies

Copy and paste the following block inside your `pom.xml dependencies` block (this will use the most recent version of this dependency, use <version>0.5.15</version> for this version).

```xml
<dependency>
    <groupId>pt.unl.fct.di.novasys.babel</groupId>
    <artifactId>babel-sc-core</artifactId>
    <version>[0.5.15,)</version>
</dependency>
```

If using Maven Shade plugin, add the following to its configuration, this avoids signature issues due to the inclusion of the Bouncy Castle dependencies. In particular, this ensures that the Bouncy Castle signatures do not get included in the jar, as the resulting jar of your project will be different from the one signed by them, causing the JVM to refuse to run the program.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    ...
    <executions>
        <execution>
            ...
            <configuration>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
                ...
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Repository Setup

If you haven't already done so, you will need to add the following to your ```pom.xml``` file.

```xml
<repositories>
    <repository>
        <id>novasys-mvn</id>
        <url>https://novasys.di.fct.unl.pt/packages/mvn</url>
    </repository>
</repositories>
```
