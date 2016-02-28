package com.bluegosling.possible;

import com.bluegosling.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// TODO: javadoc
final class Possibles {
   private Possibles() {
   }

   static class ListenableFuturePossible<T> extends AbstractDynamicPossible<T> {
      private final ListenableFuture<? extends T> future;
      
      ListenableFuturePossible(ListenableFuture<? extends T> future) {
         this.future = future;
      }
      
      @Override
      public boolean isPresent() {
         return future.isSuccessful();
      }
      
      @Override
      public T get() {
         if (!future.isSuccessful()) {
            throw new IllegalStateException();
         }
         return future.getResult();
      }
   }

   static class FuturePossible<T> extends AbstractDynamicPossible<T> {
      private final Future<? extends T> future;
      private volatile Boolean isPresent;
      private volatile T value;
      
      FuturePossible(Future<? extends T> future) {
         this.future = future;
      }
      
      private synchronized boolean determineIfPresent() {
         if (isPresent != null) {
            return isPresent;
         }
         synchronized (this) {
            // double-checked locking so we only compute these values once
            if (isPresent != null) {
               return isPresent;
            }
            boolean interrupted = false;
            while (true) {
               try {
                  value = future.get();
                  isPresent = true;
               } catch (InterruptedException e) {
                  interrupted = true;
                  continue;
               } catch (ExecutionException e) {
                  isPresent = false;
               } catch (CancellationException e) {
                  isPresent = false;
               }
               break;
            }
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
            return isPresent;
         }
      }
      
      @Override
      public boolean isPresent() {
         return future.isDone() && determineIfPresent();
      }
      
      @Override
      public T get() {
         if (!isPresent()) {
            throw new IllegalStateException();
         }
         return value;
      }
   }
}
