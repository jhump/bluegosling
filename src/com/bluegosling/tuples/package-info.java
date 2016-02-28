/**
 * APIs for type-safe tuples. {@link com.bluegosling.tuples.Pair} is a commonly needed tuple, for
 * combining two objects or for returning two values from a function. A typical way to handle this
 * need in Java is to create a new class that contains the two (or more values). But tuples are a
 * more generic and functional way of combining objects.
 * 
 * <p>This package includes typesafe APIs for tuples with up to 5 elements. It does support tuples
 * with {@linkplain com.bluegosling.tuples.NTuple greater than 5 elements}, but items after the fifth
 * receive no special API and thus are not typesafe (e.g. require downcasting from
 * {@link java.lang.Object}).
 */
package com.bluegosling.tuples;
