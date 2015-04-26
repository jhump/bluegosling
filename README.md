# bluegosling

Experiments in Java.

The experiments are broken up into the following categories:

* New Collection implementations (tries & trie-based structures, a fibonacci heap, filtering & transforming collections,
persistent and immutable data structures, sharded concurrent sets, and numerous new implementations of standard collection
interfaces).
* Tuples, similar to small, heterogeneous, type-safe collections.
* Reflection utilities, ranging from API for operating with generic types (`Types` and `TypeRef`) to a proxy tool for dynamically
casting an object to an interface it does not implement and even dynamically dispatching method calls (`Caster`).
* Unit testing tools (`InterfaceVerifier`)
* Annotation processor tools. These include reflection APIs that make querying source elements feel more natural by mimicing the
API in `java.lang.reflect`. They also include unit testing tools in the form of a JUnit4 test runner and accompanying APIs and
annotations. (Implementation is mostly complete and has been used ad hoc and tested in a limited way, but it still needs tests.)
* Static dependency injection tools, to generate code to satisfy dependencies. (Mostly a thought experiment at this point --
very, very far from complete.)

Please note that all of this code is in a Java package named `com.apriori`, however our domain is not `apriori.com` but rather
`apriori.bluegosling.com` (which also probably helps to explain how we landed on this project name). Apologies to the actual
owners of `apriori.com` (aPriori, a completely unrelated software company in Massachusetts). One of these days we'll do a
refactor to rename/move everything into `com.bluegosling.apriori`...

(Exported from http://code.google.com/p/bluegosling on 3/21/2015)
