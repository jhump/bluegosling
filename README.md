# bluegosling

[![Build Status](https://travis-ci.org/jhump/bluegosling.svg?branch=master)](https://travis-ci.org/jhump/bluegosling/branches)

## What is this project?

This project started off as a place for me to experiment with ideas in the Java programming language. Over the years, this dumping ground has grown and grown. And now there are numerous things herein, many of which may actually be useful for others.

A lot of the contents of this library heavily overlap with Google's [Guava](https://github.com/google/guava). I am a huge fan of Guava, and wanted to reproduce parts of it. Some of my interest was exploring alternate APIs, for better usability than what is already present in Guava -- largely thanks to new Java 8 language features. But most of my motivation was self-education. While going through the motions of implementing numerous [collections](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/overview.html), [synchronizers](http://gee.cs.oswego.edu/dl/papers/aqs.pdf), and reflection and annotation tools from scratch, I've intimately learned a large swatch of the JRE and Guava. Not just the APIs -- by reading a a lot of the code that powers these libraries, I've also become quite familiar with their implementations, too.

## What's the status?

This has been a random dumping ground for lots of ideas, many half-baked (or less). I created the project when I was working at Google, so felt that [Google Code](https://code.google.com) was the place to do it. On 3/21/2015, I moved it to [Github](https://github.com/jhump/bluegosling) (from http://code.google.com/p/bluegosling).

I'm in the process of cleaning it up, breaking it up into smaller and more usable modules and moving the cruft out of the way. The cruft takes the form of incomplete ideas -- classes/APIs/data structures that I never finished implementing. Some of them are very close to done and just need tests (and thus fixes). Some are mere skeletons, serving only as placeholders for what they might eventually become.

The first step in this clean-up was picking a build tool. For the longest time, I just created an ad-hoc Eclipse project, and added the small handful of JARs that comprise the 3rd party dependencies, and just ran code and tests from the IDE. Now, this repo uses [Pants](https://pantsbuild.github.io/). If you clone this repo and `cd` into it, just run `./pants test ::` to compile everything and run all of the tests.

The second step was to integrate the repo with [Travis CI](https://travis-ci.org/) so it can become stable (no more commits that accidentally break large portions of the project).

Subsequent steps will involve breaking the project up into smaller, independent libraries.

## What's in here?

The experiments are numerous. At a high-level, here are some of the things you'll find here:

* New Collection implementations (tries & trie-based structures, a fibonacci heap, filtering & transforming collections,
persistent and immutable data structures, sharded concurrent sets, and numerous new implementations of standard collection
interfaces).
* Tuples, similar to small, heterogeneous, type-safe collections.
* Vast reflection utilities, including utilities for working with generic types, annotations, and annotated types.
* Numerous APIs for working with annotations and annotation processors.
* Computation graphs, for expressing a computation as a graph of related phases, and then executing the computation with maximum parallelism afforded by the graph.
* Lots of concurrency utilities, including new executors and synchronizers and a reimagined `Future` API.

## How can I learn more?

You can peruse the complete java doc here: http://jhump.github.io/bluegosling/javadoc/index.html

## Who are you?

Ostensibly, we are [Apriori Enterprises](http://apriori.bluegosling.com). Really, it's just me. My name is [Joshua](https://github.com/jhump) [Humphries](https://www.linkedin.com/in/jhumphries131). I am currently a software engineering manager at [Square](https://squareup.com/), a [Xoogler](http://google.about.com/od/wx/g/xooglers.htm), and (admittedly irrelevant) a [home brewer](http://www.humpsbrewing.bluegosling.com/).

