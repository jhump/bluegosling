/**
 * Objects that may or may not have a value. These can be used as indirect references. Concrete
 * classes can often distinguish between a value being absent vs. being present but {@code null}
 * (all but {@link com.apriori.possible.Optional Optional}). Some are mutable (
 * {@link com.apriori.possible.Holder Holder} and, to a limited extent,
 * {@link com.apriori.possible.Fulfillable Fulfillable}). And one
 * ({@link com.apriori.possible.Optional Optional}) can be used to provide safer API compared to
 * using nullable references.
 */
package com.apriori.possible;