package com.bluegosling.concurrent.test;

import com.bluegosling.concurrent.unsafe.UnsafeFieldUpdater;
import com.bluegosling.concurrent.unsafe.UnsafeIntegerFieldUpdater;
import com.bluegosling.concurrent.unsafe.UnsafeLongFieldUpdater;
import com.bluegosling.concurrent.unsafe.UnsafeReferenceFieldUpdater;

/** An interface for creating {@link UnsafeFieldUpdater}s. */
public interface FieldUpdaterFactory {
   <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName);
   <T> UnsafeLongFieldUpdater<T> makeLongUpdater(Class<T> clazz, String fieldName);
   <T, V> UnsafeReferenceFieldUpdater<T, V> makeReferenceUpdater(Class<T> tclazz, Class<V> vclazz,
         String fieldName);
}
