package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;


abstract class CoreReflectionBaseElement extends CoreReflectionBase implements Element {
   private final Name name;
   
   CoreReflectionBaseElement(AnnotatedElement base, String name) {
      super(base);
      this.name = new CoreReflectionName(name);
   }
   
   @Override
   public Name getSimpleName() {
      return name;
   }
}
