package com.bluegosling.util.test;

import com.bluegosling.util.CallingClass;

import java.util.function.Function;
import java.util.function.Supplier;

public class CallingClassTestHelper
      implements Supplier<Class<?>>,Function<Iterable<? extends ClassLoader>, Class<?>> {
   
   @Override
   public Class<?> apply(Iterable<? extends ClassLoader> cl) {
      return CallingClass.getCaller(cl);
   }

   @Override
   public Class<?> get() {
      return CallingClass.getCaller();
   }
}
