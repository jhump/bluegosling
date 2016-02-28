/**
 * Objects that may or may not have a value. These can be used as indirect references, and carry the
 * same API as {@link java.util.Optional}. Some implementations distinguish between a value being
 * absent vs. being present but {@code null} (unlike {@link java.util.Optional}). Some are mutable
 * ({@link com.bluegosling.possible.Holder Holder} and, to a limited extent,
 * {@link com.bluegosling.possible.Fulfillable Fulfillable}).
 */
package com.bluegosling.possible;
