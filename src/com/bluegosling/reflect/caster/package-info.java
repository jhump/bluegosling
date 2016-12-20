/**
 * An API that leverages reflection to dynamically cast an object to an arbitrary interface. The
 * object does not need to have been declared to implement the interface as long as it is
 * structurally compatible (or at least partially compatible). Dispatch is handled via reflection.
 * This allows casting private API of an object as a public interface and even enables general
 * dynamic dispatch (though, admittedly, inefficiently).
 */
package com.bluegosling.reflect.caster;