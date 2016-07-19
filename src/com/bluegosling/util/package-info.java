/**
 * Miscellaneous utility classes. 
 * 
 * <p>These include a {@link com.bluegosling.util.Clock} and a {@link com.bluegosling.util.Stopwatch}. The
 * {@code Clock} varies considerably from the JRE's {@link java.time.Clock}. The one here is not
 * meant for general date and time use or inter-op with the {@code java.time} package. Instead, it
 * is a light-weight way to abstract {@link System#currentTimeMillis()}, {@link System#nanoTime()},
 * and {@link Thread#sleep(long)}, mainly for testability. (See {@link com.bluegosling.util.FakeClock}.)
 * 
 * <p>There are also utility classes related to {@linkplain com.bluegosling.util.IoStreams IO},
 * {@linkplain com.bluegosling.util.Throwables exceptions}, {@linkplain com.bluegosling.util.Cloner cloning},
 * {@linkplain com.bluegosling.util.Result result values} (which are something between a
 * {@link java.util.concurrent.Future} and a {@link com.bluegosling.vars.Variable}), and a handful of
 * other random things.
 */
package com.bluegosling.util;
