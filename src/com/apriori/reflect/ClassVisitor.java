package com.apriori.reflect;

// TODO: javadoc!
public interface ClassVisitor<R, P> {
   R visit(Class<?> clazz, P param);
}
