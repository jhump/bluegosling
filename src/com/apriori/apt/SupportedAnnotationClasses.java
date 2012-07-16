package com.apriori.apt;

import java.lang.annotation.Annotation;

//TODO: javadoc!
public @interface SupportedAnnotationClasses {
   Class<? extends Annotation>[] value();
}
