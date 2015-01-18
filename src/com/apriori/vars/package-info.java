/**
 * This package contains simple, mutable "holders" for reference types and primitives. These are
 * lighter-weight and more intuitive than using single-element arrays for such uses. The APIs are
 * very similar to atomic variables in {@code java.util.concurrent}, but none of these classes are
 * thread-safe. They are meant more for use as "out parameters", where a function can modify a
 * variable defined in the scope of its caller (all on one thread). 
 */
package com.apriori.vars;
