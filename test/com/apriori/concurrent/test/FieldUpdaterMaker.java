package com.apriori.concurrent.test;

import com.apriori.concurrent.AtomicBooleanFieldUpdater;

/** An interface for creating AtomicBooleanFieldUpdaters. */
public interface FieldUpdaterMaker {
   <T> AtomicBooleanFieldUpdater<T> make(Class<T> clazz, String fieldName);
}
