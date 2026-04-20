# Babel Core

A Java framework for developing distributed protocols. Protocols are first-class composition
units — each extends `GenericProtocol` and communicates with peers and with other protocols
via typed messages, notifications, timers, and requests, all dispatched on a per-protocol
event loop managed by the Babel runtime.

**Group ID:** `pt.paradigmshift.babel`  
**Artifact ID:** `babel-core`  
**Current version:** `1.0.0`

---

## Origin

This library is a fork of the Babel framework originally developed at
[NOVA School of Science and Technology (FCT-NOVA)](https://www.fct.unl.pt)
as part of the [TaRDIS](https://tardis-project.eu) European research project
on swarm systems (work package 6):

> **Original repository:**
> https://codelab.fct.unl.pt/di/research/tardis/wp6/babel-swarm/babel-core-swarm
>
> **Original authors:** Rafael Matos, Felipe Rossi, Tomás Galvão, João Leitão

The fork was created to serve as the production runtime for the StoneFlux platform
and is maintained by [ParadigmShift](https://www.paradigmshift.pt).
All original authorship is acknowledged and preserved. Additions and modifications
made after the fork are copyright ParadigmShift.

---

## Key abstractions

| Abstraction | Base class | Purpose |
|---|---|---|
| Protocol | `GenericProtocol` | Core unit of composition; owns an event loop |
| Discoverable protocol | `DiscoverableProtocol` | Protocol with multicast/broadcast peer discovery |
| Self-configurable protocol | `SelfConfigurableProtocol` | Protocol with runtime parameter management |
| Message | `ProtoMessage` | Typed network message sent to a remote peer |
| Notification | `ProtoNotification` | Broadcast to all subscribed protocols in the same node |
| Request / Reply | `ProtoRequest` / `ProtoReply` | Typed call between protocols on the same node |
| Timer | `ProtoTimer` | Scheduled callback on the owning protocol's thread |

### Channels (via `babel-network-layer`)

All channel types from [`babel-network-layer`](https://maven.paradigmshift.pt) are usable
directly from Babel through the corresponding `*ChannelInitializer` classes.

---

## Usage

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>paradigmshift-repository</id>
        <name>ParadigmShift Repository</name>
        <url>https://maven.paradigmshift.pt/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>pt.paradigmshift.babel</groupId>
        <artifactId>babel-core</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

If you use `maven-shade-plugin`, exclude Bouncy Castle signatures to prevent the JVM
from refusing to load the resulting JAR:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
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
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Minimal protocol

```java
public class MyProtocol extends GenericProtocol {

    public static final short PROTO_ID = 100;

    public MyProtocol() throws HandlerRegistrationException {
        super("MyProtocol", PROTO_ID);
        registerMessageHandler(MyMessage.serializer, this::uponMyMessage,
                MyMessage.MSG_ID);
        registerTimerHandler(MyTimer.TIMER_ID, this::uponMyTimer);
    }

    @Override
    public void init(Properties props) {
        setupPeriodicTimer(new MyTimer(), 0, 5000);
    }

    private void uponMyMessage(MyMessage msg, Host from, short srcProto, int channelId) {
        // handle inbound message
    }

    private void uponMyTimer(MyTimer timer, long uId) {
        sendMessage(new MyMessage(...), peer, PROTO_ID, channelId);
    }
}
```

---

## Building

Requires Java 17 and Maven 3.6+.

```bash
mvn verify    # compile + test
mvn package   # produces JAR, sources JAR, and Javadoc JAR
mvn deploy    # publish to maven.paradigmshift.pt (requires REPOSILITE_TOKEN)
```

## Releasing

Push a version tag — the GitHub Actions CI workflow builds, tests, and deploys automatically:

```bash
git tag v1.0.1
git push origin v1.0.1
```

---

## License

Copyright (c) 2026 ParadigmShift, Lda. See [LICENSE](LICENSE) for full terms.

Commercial use outside of ParadigmShift requires a written licence.  
Contact: [info@paradigmshift.pt](mailto:info@paradigmshift.pt)
