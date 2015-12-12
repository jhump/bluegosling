package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;

/**
 * An abstract base type for all {@link Element}s backed by core reflection. One notable exception:
 * {@link CoreReflectionSyntheticPackageElement} does not extend this base type as it has no
 * annotated element.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class CoreReflectionBaseElement<E extends AnnotatedElement> extends CoreReflectionBase<E>
implements Element {
   private final Name name;
   
   CoreReflectionBaseElement(E base, String name) {
      super(base);
      this.name = new CoreReflectionName(name);
   }
   
   @Override
   public Name getSimpleName() {
      return name;
   }
}
