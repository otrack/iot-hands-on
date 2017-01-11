# A distributed sum computation atop Infinispan.

In this lab session, we consider a distributed set of nodes, each receiving a stream of integers.
Our end goal is to approximate the global sum of these integers using Infinispan.

## Context

Systems such as social networks, search engines or trading platforms operate geographically distant sites that continuously generate streams of events at high-rate.
Such events can be access logs to a set of web servers, feeds of messages from participants to a social network, or financial data, among others.
The ability to timely detect trends and/or popularity variations is of key interest in such systems.

## Problematic

In this hands-on, we consider a group of nodes that each monitor a stream of integers.
Our objective is to *approximate* the total sum of these integers over time.

In formal terms, let us denote *Time* the interval of time we are interested in, and *Nodes* the set of nodes.
Then, we define *stream(t,N)* the value at time *t* of the stream at node *N*.
We aim at approximating the integral of *stream(t,N)* over both *Time* and *Nodes*

## Overview of the solution

To construct the approximation, we use a master-slave distributed architecture on top of Infinispan.
More precisely, we consider a particular node among the group of nodes.
This nodes acts as the *master* node and it coordinates the process of computing the global sum.
The other nodes are *slaves*, and each receives a stream of integers.

Each slave listens to its stream of integers, and maintains a *local sum*.
A slave node also maintains a [Constraint](src/main/java/eu/tsp/distsum/Constraint.java) object.
This constrains consists of an *upper* and a *lower* bound.

The master maintains the approximation of the *global sum*.
When at some slave node, an update makes the local sum violates the constraint, i.e., the local sum is outside of the bounds, the slave informs the master.
The master then asks all the slaves to send their local value of the sum.
The coordinator recomputes the global sum and updates the constraints at the slave nodes.

## Architecture of the solution

The [Master](src/main/java/eu/tsp/distsum/Master.java) and [Slave](src/main/java/eu/tsp/distsum/Slave.java) classes model respectively the master and the slave nodes.
Both classes inherit from the [Node](src/main/java/eu/tsp/distsum/Node.java) class.
At core this class is a listener which receives messages from other nodes as cache notifications.

In detail, nodes communicate using the [Channel] (src/main/java/eu/tsp/distsum/Channel.java) class.
This class contains a `Cache` object. 
When a node *N* registers to an instance of a `Channel` object, it sets a listener together with a `NodeFilter`object up.
By default, a listener triggers upon all the updates in the `Cache`.
The filter ensures that solely updates regarding node *N* trigger at the listener.
The [NodeFilterFactory](src/main/java/eu/tsp/distsum/NodeFilterFactory.java) implements a factory of filters.
When provided with some node identifier, it construct a `NodeFilter` for that node.

This java project is built with [Apache Maven](https://maven.apache.org).
Maven is a build automation tool, used primarily in Java projects.
This tool addresses several aspects of the lifetime of a software.
In particular, it describes how the software is built, what are its dependencies and how to package it.
More advanced functions, include testing and deployment.

The structure of this project is defined in the [pom.xml](pom.xml) at the root.
Before delving into the code, it is highly advised to check the [following](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) Maven beginner tutorial.
This tutorial covers the basics of Maven you need to successfully do this lab.

## Your tasks

To successfully complete this lab, you should complete the existing code available under `src/main/java`.
To this end, you should search for the `.java` files that include a **TODO** tags, and fulfill the code appropriately.

## Testing your code

The [DistributedSum](src/test/java/eu/tsp/distsum/DistributedSum.java) class tests the correctness of your code.
In detail, this program runs several rounds of computation.
At each round,  it injects a new update in each of the slave node, then it checks that the master holds a correct approximation of the global sum.
If this approximation is incorrect, an exception is raised.

The input parameters of `DistributedSum` are the number of slaves (`NUMBER_SLAVES`) and the number of rounds to execute the computation (`NUMBDER_ROUNDS`).
Once you have completed all the necessary pieces of code, this test should run smoothly whatever these entry parameters are.
Nonetheless, we advise you to set `NUMBER_SLAVES` not to far from the total number of cores available on your machine, in order to avoid saturating it.

Notice that the `DistributedSum` program uses the [TestNG](http://testng.org/doc/index.html) framework to implements its tests.
This framework is close to the more common JUnit framework, with some advanced capabilities.
