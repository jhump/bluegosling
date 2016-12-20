/**
 * A package scanner that uses reflection to enumerate the contents of a package. This is only able
 * to enumerate the contents that are loadable from well-known boot class paths or loadable from
 * class loaders that extend {@link java.net.URLClassLoader}.
 */
package com.bluegosling.reflect.scanner;