/**
 * Miscellaneous utility classes. 
 * 
 * <p>This includes several new functional interfaces, like {@link com.apriori.util.PartialFunction}
 * (and its kin) and numerous primitive specializations of standard interfaces (like
 * {@link com.apriori.util.BooleanBinaryOperator}, {@link com.apriori.util.FloatPredicate}, and
 * {@link com.apriori.util.ShortConsumer} to name just a few).
 * 
 * <p>Also included is a {@link com.apriori.util.Clock} and a {@link com.apriori.util.Stopwatch}.
 * The {@code Clock} varies considerably from the JRE's {@link java.time.Clock}. The one here is not
 * meant for general date and time use or inter-op with the {@code java.time} package. Instead, it
 * is a light-weight way to abstract {@link System#currentTimeMillis()}, {@link System#nanoTime()},
 * and {@link Thread#sleep(long)}, mainly for testability. (See {@link com.apriori.util.FakeClock}.)
 */
package com.apriori.util;