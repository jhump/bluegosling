package com.apriori.apt.reflect;

import java.util.EnumSet;

import javax.lang.model.element.Element;

/**
 * An abstract base class for implementations of {@link Member}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
abstract class AbstractMember extends AbstractAnnotatedElement implements Member {

   /**
    * Constructs a new object based on an {@link Element}.
    * 
    * @param element the element
    */
   protected AbstractMember(Element element) {
      super(element);
   }

   @Override
   public Class getDeclaringClass() {
      Element parent = asElement().getEnclosingElement();
      if (parent == null) {
         throw new AssertionError("Enclosing element of member is null");
      }
      Class ret = ReflectionVisitors.CLASS_VISITOR.visit(parent);
      if (ret == null) {
         throw new AssertionError("Enclosing element of member is not a TypeElement");
      }
      return ret;
   }

   @Override
   public EnumSet<Modifier> getModifiers() {
      return Modifier.fromElementModifiers(asElement().getModifiers());
   }

   @Override
   public String getName() {
      return asElement().getSimpleName().toString();
   }
}
