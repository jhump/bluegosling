package com.apriori.di;

import java.lang.annotation.Annotation;

// TODO: javadoc!
public interface ScoperFactory {
   Scoper getScoper(Annotation annotation);
}
