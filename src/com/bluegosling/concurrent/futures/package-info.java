/**
 * Utilities and new classes related to using {@link java.util.concurrent.CompletableFuture} and
 * {@link java.util.concurrent.CompletionStage}. These include read-only versions of
 * {@link java.util.concurrent.CompletableFuture}, which prevents code from arbitrarily completing
 * or obtruding the futures. It also includes runnable versions, which can be given to an
 * {@link java.util.concurrent.Executor} to run some associated task (like
 * {@link java.util.concurrent.RunnableFuture}). 
 */
package com.bluegosling.concurrent.futures;