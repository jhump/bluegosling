/**
 * This package contains the über-Future. It is an extension of the {@code Future} interface that
 * allows completion callbacks to be registered. This means code has the flexibility of either doing
 * things asynchronously with callbacks or synchronously by blocking for the future to complete. It
 * also provides numerous other new methods to greatly improve the ease of use of futures.
 * 
 * <p>This API is similar in key ways to Guava's
 * {@link com.google.common.util.concurrent.ListenableFuture}. Also, it is similar to the new
 * {@link java.util.concurrent.CompletionStage} interface added in Java 8. This version attempts to
 * combine the best of both, and thus draws heavy inspiration from both. The key differences follow:
 * <ol>
 * <li>Like {@link java.util.concurrent.CompletionStage}, this future includes a very wide, fluent
 * API for chaining and monad-like use. Thanks to Java 8's default methods, the surface area that
 * concrete implementations need to provide is much smaller and is closer to that of
 * {@link com.google.common.util.concurrent.ListenableFuture}.</li>
 * <li>Its API uses method names that are more consistent with {@link java.util.stream.Stream} than
 * the methods of {@link java.util.concurrent.CompletionStage} or the utility methods in Guava's
 * {@link com.google.common.util.concurrent.Futures} API.</li>
 * <li>Unlike {@link java.util.concurrent.CompletionStage}, it extends
 * {@link java.util.concurrent.Future}, thus providing methods for blocking until completion. It
 * also provides additional APIs around awaiting completion.</li>
 * <li>It provides numerous new accessors to make inspecting the disposition of a completed future
 * much simpler. It also provides a visitor pattern, as a new idiom for interacting with a future
 * (that is similar in many respects to Guava's
 * {@link com.google.common.util.concurrent.Futures#addCallback(
 * com.google.common.util.concurrent.ListenableFuture,
 * com.google.common.util.concurrent.FutureCallback) Futures.addCallback}.</li>  
 * </ol>
 */
package com.bluegosling.concurrent.fluent;