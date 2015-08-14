package com.apriori.concurrent.test;

import com.apriori.concurrent.unsafe.UnsafeFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeIntegerFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeLongFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeReferenceFieldUpdater;


/**
 * Test classes for testing the visibility checks when instantiating
 * {@link UnsafeFieldUpdater} instances.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SuppressWarnings("unused")
public class TestPublicClass implements FieldUpdaterFactory {
   private volatile boolean privateBoolean;
   protected volatile boolean protectedBoolean;
   volatile boolean nonPublicBoolean;
   public volatile boolean publicBoolean;

   private volatile int privateInt;
   protected volatile int protectedInt;
   volatile int nonPublicInt;
   public volatile int publicInt;
   
   private volatile long privateLong;
   protected volatile long protectedLong;
   volatile long nonPublicLong;
   public volatile long publicLong;

   public static FieldUpdaterFactory createPublic() {
      return new TestPublicClass();
   }
   
   @SuppressWarnings("synthetic-access")
   public static FieldUpdaterFactory createPrivate() {
      return new TestPrivateClass();
   }
   
   public static FieldUpdaterFactory createNonPublic() {
      return new TestNonPublicClass();
   }

   @Override
   public <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName) {
      return new UnsafeIntegerFieldUpdater<>(clazz, fieldName);
   }

   @Override
   public <T> UnsafeLongFieldUpdater<T> makeLongUpdater(Class<T> clazz, String fieldName) {
      return new UnsafeLongFieldUpdater<>(clazz, fieldName);
   }
   
   @Override
   public <T, V> UnsafeReferenceFieldUpdater<T, V> makeReferenceUpdater(Class<T> tclazz,
         Class<V> vclazz, String fieldName) {
      return new UnsafeReferenceFieldUpdater<>(tclazz, vclazz, fieldName);
   }

   private static class TestPrivateClass implements FieldUpdaterFactory {
      private volatile boolean privateBoolean;
      protected volatile boolean protectedBoolean;
      volatile boolean nonPublicBoolean;
      public volatile boolean publicBoolean;

      private volatile int privateInt;
      protected volatile int protectedInt;
      volatile int nonPublicInt;
      public volatile int publicInt;
      
      private volatile long privateLong;
      protected volatile long protectedLong;
      volatile long nonPublicLong;
      public volatile long publicLong;
      
      @Override
      public <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName) {
         return new UnsafeIntegerFieldUpdater<>(clazz, fieldName);
      }

      @Override
      public <T> UnsafeLongFieldUpdater<T> makeLongUpdater(Class<T> clazz, String fieldName) {
         return new UnsafeLongFieldUpdater<>(clazz, fieldName);
      }
      
      @Override
      public <T, V> UnsafeReferenceFieldUpdater<T, V> makeReferenceUpdater(Class<T> tclazz,
            Class<V> vclazz, String fieldName) {
         return new UnsafeReferenceFieldUpdater<>(tclazz, vclazz, fieldName);
      }
   }
}

@SuppressWarnings("unused")
class TestNonPublicClass implements FieldUpdaterFactory {
   private volatile boolean privateBoolean;
   protected volatile boolean protectedBoolean;
   volatile boolean nonPublicBoolean;
   public volatile boolean publicBoolean;

   private volatile int privateInt;
   protected volatile int protectedInt;
   volatile int nonPublicInt;
   public volatile int publicInt;
   
   private volatile long privateLong;
   protected volatile long protectedLong;
   volatile long nonPublicLong;
   public volatile long publicLong;
   
   @Override
   public <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName) {
      return new UnsafeIntegerFieldUpdater<>(clazz, fieldName);
   }

   @Override
   public <T> UnsafeLongFieldUpdater<T> makeLongUpdater(Class<T> clazz, String fieldName) {
      return new UnsafeLongFieldUpdater<>(clazz, fieldName);
   }
   
   @Override
   public <T, V> UnsafeReferenceFieldUpdater<T, V> makeReferenceUpdater(Class<T> tclazz,
         Class<V> vclazz, String fieldName) {
      return new UnsafeReferenceFieldUpdater<>(tclazz, vclazz, fieldName);
   }
}
