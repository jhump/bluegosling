package com.bluegosling.di;

import com.bluegosling.reflect.Annotations;

import java.util.HashMap;

import javax.inject.Named;

// TODO: javadoc!!
public class Names {
   private Names() {}
   
   public static Named named(final String name) {
      HashMap<String, Object> attributes = new HashMap<String, Object>();
      attributes.put("value", name);
      return Annotations.create(Named.class, attributes);
   }
}
