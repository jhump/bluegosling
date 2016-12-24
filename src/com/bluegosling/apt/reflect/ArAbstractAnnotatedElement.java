package com.bluegosling.apt.reflect;

import static com.bluegosling.apt.ProcessingEnvironments.elements;

import java.util.List;

import javax.lang.model.element.Element;

/**
 * An abstract base class for implementations of {@link ArAnnotatedElement}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the sub-type of {@link Element} represented
 */
abstract class ArAbstractAnnotatedElement<E extends Element> extends ArAbstractAnnotatedConstruct<E>
      implements ArAnnotatedElement {

   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected ArAbstractAnnotatedElement(E element) {
      super(element);
   }

   @Override
   public List<ArAnnotation> getAnnotations() {
      return ArAnnotation.fromMirrors(elements().getAllAnnotationMirrors(asElement()));
   }

   @Override
   public E asElement() {
      return delegate();
   }
}
