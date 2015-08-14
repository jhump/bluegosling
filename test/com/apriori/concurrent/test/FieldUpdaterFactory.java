package com.apriori.concurrent.test;

import com.apriori.concurrent.unsafe.UnsafeFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeIntegerFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeLongFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeReferenceFieldUpdater;

/** An interface for creating {@link UnsafeFieldUpdater}s. */
public interface FieldUpdaterFactory {
   <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName);
   <T> UnsafeLongFieldUpdater<T> makeLongUpdater(Class<T> clazz, String fieldName);
   <T, V> UnsafeReferenceFieldUpdater<T, V> makeReferenceUpdater(Class<T> tclazz, Class<V> vclazz,
         String fieldName);
}
