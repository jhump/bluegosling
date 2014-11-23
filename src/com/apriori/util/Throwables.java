package com.apriori.util;

// TODO: javadoc
// TODO: tests
public final class Throwables {
   public static <T extends Throwable> T withCause(T throwable, Throwable cause) {
      throwable.initCause(cause);
      return throwable;
   }
}
