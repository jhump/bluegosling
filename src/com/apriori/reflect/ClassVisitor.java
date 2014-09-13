package com.apriori.reflect;

/**
 * An interface for implementing visitor pattern over class tokens.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <R> result type of invoking the visitor
 * @param <P> parameter type for invoking the visitor (or {@code Void} if there is no parameter)
 */
public interface ClassVisitor<R, P> {
   /**
    * Visit a class token.
    * 
    * @param clazz a class token
    * @param param a parameter which may be {@code null}
    * @return a result
    */
   R visit(Class<?> clazz, P param);
}
