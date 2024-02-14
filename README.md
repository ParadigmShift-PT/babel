# Babel-Core

A Java framework for developing distributed protocols. Uses [network-layer](https://codelab.fct.unl.pt/di/research/tardis/wp6/babel/babel-networklayer) for network communications.

More details about this project can be found in the [SRDS'22 paper](https://ieeexplore.ieee.org/abstract/document/9996836).

Examples of usage can be found [here](https://github.com/pedroAkos/EdgeOverlayNetworks), [here](https://github.com/pfouto/chain) and [here](https://github.com/pedroAkos/babel-case-studies).

A guided tutorial on how you can use Babel to build distributed protocols can be found [here](https://codelab.fct.unl.pt/di/research/tardis/wp6/babel/babel-examples).

The installation steps for this version can be found in [here](https://codelab.fct.unl.pt/di/research/tardis/wp6/babel/babel-core/-/packages/9)

##Authors

- Pedro Fouto (p.fouto@campus.fct.unl.pt)
- Pedro Ákos Costa (pah.costa@campus.fct.unl.pt)
- Nuno Preguiça (nuno.preguica@fct.unl.pt)
- João Leitão (jc.leitao@fct.unl.pt)

# Installation

### Dependencies

Copy and paste the following block inside your ```pom.xml dependencies``` block.

```
<dependency>
	<groupId>pt.unl.fct.di.novasys.babel</groupId>
	<artifactId>babel-core</artifactId>
	<version>[1.0.0,)</version>
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
