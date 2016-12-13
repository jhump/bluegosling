/**
 * Implementations of sorting algorithms. These are largely for educational purposes. The multiple
 * parallel sort implementations are not practical, but perhaps useful as a thought experiment as to
 * how to employ parallelism in large sorting operations. The other sort algorithm here,
 * {@link com.bluegosling.collections.sorting.SmoothSort}, is an interesting algorithm that has
 * ideal properties (logarithmic upper-bound, closer to linear runtime with already-sorted or
 * semi-sorted inputs), but does not actually outperform the JRE's built-in sorting (which uses
 * merge-sort for primitive types and <a href="https://en.wikipedia.org/wiki/Timsort">timsort</a>
 * for reference types).
 */
package com.bluegosling.collections.sorting;