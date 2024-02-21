# DistLedger

Distributed Systems Project 2022/2023

## Authors
  
**Group A52**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members


| Number | Name              | User                               | Email                                            |
|--------|-------------------|------------------------------------|--------------------------------------------------|
| 99189  | Carolina Coelho   | <https://github.com/carolcoelho11> | <mailto:carolina.coelho@tecnico.ulisboa.pt>      |
| 99293  | Nuno Ribeiro      | <https://github.com/nunoribeiro02> | <mailto:nuno.m.p.ribeiro@tecnico.ulisboa.pt>     |
| 99297  | Pedro Cruz        | <https://github.com/pedrocruzac02> | <mailto:pedro.agostinho.cruz@tecnico.ulisboa.pt> |

## Important notes about the solution (phase 3)

As informações sobre esta entrega encontram-se no ficheiro DistLedgerReport.pdf

## How to run the project

O servidor de nomes deve ser lançado sem argumentos e ficará à escuta no porto 5001, podendo ser lançado a partir da pasta NamingServer da seguinte forma:

(NamingServer)
```s
$ mvn exec:java 
```

Ou com função de debug:

(NamingServer)
```s
$ mvn exec:java -Ddebug
```

Os servidor devem ser lançados a partir da pasta Server, recebendo como argumentos o porto e o seu qualificador ('A', 'B', etc.)

Por exemplo, um servidor, pode ser lançado da seguinte forma a partir da pasta Server ($ representa a shell do sistema operativo):

```s
$ mvn exec:java -Dexec.args="2001 A"
```

Ou com função de debug:

```s
$ mvn exec:java -Dexec.args="2001 A" -Ddebug
```


Ambos os tipos de processo cliente (utilizador e administrador) recebem comandos a partir da consola. Todos os processos cliente deverão mostrar o símbolo > sempre que se encontrarem à espera que um comando seja introduzido. Estes processos são executados com os comandos:

(User/Admin)
```s
$ mvn exec:java
```

Ou com função de debug:

(User/Admin)
```s
$ mvn exec:java -Ddebug
```

Os processos devem ser inicializados da seguinte forma:

1) NamingServer
2) DistLedgerServers
3) User / Admin

## Getting Started

The overall system is made up of several modules. The main server is the _DistLedgerServer_. The clients are the _User_ 
and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
