package com.apriori.di;

import java.lang.annotation.Annotation;
import java.util.Map;

//TODO: javadoc!
//TODO: this should be a class not an interface!
public interface AnnotationSpec<T extends Annotation> {
   Class<T> annotationType();
   boolean hasAttributes();
   Map<String, Object> attributes();
   T asAnnotation();
   AnnotationSpec<T> withoutAttributes();
   @Override boolean equals(Object other);
}
