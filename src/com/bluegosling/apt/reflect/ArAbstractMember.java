package com.bluegosling.apt.reflect;

import java.util.EnumSet;

import javax.lang.model.element.Element;

/**
 * An abstract base class for implementations of {@link ArMember}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class ArAbstractMember<E extends Element> extends ArAbstractAnnotatedElement<E>
      implements ArMember {

   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected ArAbstractMember(E element) {
      super(element);
   }

   @Override
   public ArClass getDeclaringClass() {
      Element parent = asElement().getEnclosingElement();
      if (parent == null) {
         throw new AssertionError("Enclosing element of member is null");
      }
      ArClass ret = ReflectionVisitors.CLASS_VISITOR.visit(parent);
      if (ret == null) {
         throw new AssertionError("Enclosing element of member is not a TypeElement");
      }
      return ret;
   }

   @Override
   public EnumSet<ArModifier> getModifiers() {
      return ArModifier.fromElementModifiersWithVisibility(asElement().getModifiers());
   }

   @Override
   public String getName() {
      return asElement().getSimpleName().toString();
   }
}
