package com.apriori.concurrent.test;

import com.apriori.concurrent.AtomicBooleanFieldUpdater;


/**
 * Test classes for testing the visibility checks when instantiating
 * {@link AtomicBooleanFieldUpdater} instances.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SuppressWarnings("unused")
public class TestPublicClass implements FieldUpdaterMaker {
   private volatile boolean privateFlag;
   protected volatile boolean protectedFlag;
   volatile boolean nonPublicFlag;
   public volatile boolean publicFlag;

   private static class TestPrivateClass implements FieldUpdaterMaker {
      private volatile boolean privateFlag;
      protected volatile boolean protectedFlag;
      volatile boolean nonPublicFlag;
      public volatile boolean publicFlag;
      
      @Override
      public <T> AtomicBooleanFieldUpdater<T> make(Class<T> clazz, String fieldName) {
         return AtomicBooleanFieldUpdater.newUpdater(clazz, fieldName);
      }
   }
   
   public static FieldUpdaterMaker createPublic() {
      return new TestPublicClass();
   }
   
   @SuppressWarnings("synthetic-access")
   public static FieldUpdaterMaker createPrivate() {
      return new TestPrivateClass();
   }
   
   public static FieldUpdaterMaker createNonPublic() {
      return new TestNonPublicClass();
   }

   @Override
   public <T> AtomicBooleanFieldUpdater<T> make(Class<T> clazz, String fieldName) {
      return AtomicBooleanFieldUpdater.newUpdater(clazz, fieldName);
   }
}

@SuppressWarnings("unused")
class TestNonPublicClass implements FieldUpdaterMaker {
   private volatile boolean privateFlag;
   protected volatile boolean protectedFlag;
   volatile boolean nonPublicFlag;
   public volatile boolean publicFlag;
   
   @Override
   public <T> AtomicBooleanFieldUpdater<T> make(Class<T> clazz, String fieldName) {
      return AtomicBooleanFieldUpdater.newUpdater(clazz, fieldName);
   }
}
