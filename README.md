# bluegosling

[![Build Status](https://travis-ci.org/jhump/bluegosling.svg?branch=master)](https://travis-ci.org/jhump/bluegosling/branches)

## What is this project?

This project started off as a place for me to experiment with ideas in the Java programming language. Over the years, this dumping ground has grown and grown. And now there are numerous things herein, many of which may actually be useful for others.

Some of the contents of this library seem to overlap with Google's [Guava](https://github.com/google/guava). I am a huge fan of Guava, and originally wanted to reproduce parts of it. Some of my interest was exploring alternate APIs, for better usability than what is already present in Guava -- largely thanks to new Java 8 language features. But most of my motivation was self-education. I've since removed most of the duplication. The bits that remain are documented as to why they exist separate from what Guava already provides (some related to Java 8, some related to small features unavailable Guava's version of similar functionality).

While going through the motions of implementing numerous [collections](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/overview.html), [synchronizers](http://gee.cs.oswego.edu/dl/papers/aqs.pdf), and reflection and annotation tools from scratch, I've intimately learned a large swatch of the JRE and Guava. Not just the APIs -- by reading a lot of the code that powers these libraries, I've also become quite familiar with their implementations, too. I even had to file a lot of bugs in the JDK along the way (the support for type annotations in core reflection is quite broken, especially so in early builds of Java 8).

## What's the status?

This has been a random dumping ground for lots of ideas, many half-baked (or less). I created the project when I was working at Google, so felt that [Google Code](https://code.google.com) was the place to do it. On 3/21/2015, I moved it to [Github](https://github.com/jhump/bluegosling) (from [http://code.google.com/p/bluegosling](http://code.google.com/p/bluegosling)).

The project has now been broken up into numerous modules, each of which could be used in other Java projects. I spent several months, sometimes just a few hours a week, cleaning up cross-package dependencies, refactoring into packages and modules that made sense, and documenting things. There are still numerous rough edges and cruft -- classes/APIs/data structures that I never finished implementing. Some of them are very close to done and just need tests (and thus fixes). Some are mere skeletons, serving only as placeholders for what they might eventually become. But the amount of cruft is shrinking and the cruft that is there is slowly getting better documented so its status is obvious to any would-be users.

This repo uses the [Pants](https://pantsbuild.github.io/) build tool. If you clone this repo and `cd` into it, just run `./pants test ::` to compile everything and run all of the tests. Each Java package is its own stand-alone module. Take a look at the `BUILD` file in each package to see its dependencies.

This repo uses [Travis CI](https://travis-ci.org/) for CI, to improve stability. (Early on, I had plenty of commits that accidentally broke large portions of the project! No more.).

At some point, I'd like to publish some of the modules that I suspect are of value to something like Maven central. For now, users can run `./pants jar <path-to-module>` and use the resulting JAR files in their projects (found in `.pants.d/jar/create`). Publishing it into a Maven repository will make it much easier for people to find and use in the future.

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

You can peruse the complete java doc here: [https://jhump.github.io/bluegosling/javadoc/index.html](https://jhump.github.io/bluegosling/javadoc/index.html)

## Who are you?

Ostensibly, we are [Apriori Enterprises](http://apriori.bluegosling.com). Really, it's just me. My name is [Joshua](https://github.com/jhump) [Humphries](https://www.linkedin.com/in/jhumphries131). I am currently a software engineer at [FullStory](https://fullstory.com/); I am a former [Square](https://squareup.com/) and [Xoogler](http://google.about.com/od/wx/g/xooglers.htm); and I have over 17 years of software engineering experience.

