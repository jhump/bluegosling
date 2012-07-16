package com.apriori.util;

//TODO: javadoc
public interface Predicate<T> extends Function<T, Boolean> {
   interface Bivariate<T1, T2> extends Function.Bivariate<T1, T2, Boolean> {
   }
   interface trivariate<T1, T2, T3> extends Function.Trivariate<T1, T2, T3, Boolean> {
   }
}
