package com.apriori.di;

import java.lang.annotation.Annotation;

import javax.inject.Named;

// TODO: javadoc!!
public class Names {
   private Names() {}
   
   public static Named named(final String name) {
      // TODO use Annotations util class (which will also generate appropriate equals() and hashCode())
      return new Named() {
         @Override
         public Class<? extends Annotation> annotationType() {
            return Named.class;
         }

         @Override
         public String value() {
            return name;
         }
      };
   }
}