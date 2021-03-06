package com.bluegosling.util.test;

import java.util.function.Function;
import java.util.function.Supplier;

import com.bluegosling.reflect.CallingClass;

public class CallingClassTestHelper
      implements Supplier<Class<?>>, Function<Iterable<? extends ClassLoader>, Class<?>> {
   
   @Override
   public Class<?> apply(Iterable<? extends ClassLoader> cl) {
      return CallingClass.getCaller(cl);
   }

   @Override
   public Class<?> get() {
      return CallingClass.getCaller();
   }
}
