package com.bluegosling.apt.reflect;

import java.lang.reflect.Member;
import java.util.EnumSet;

/**
 * A member of a Java class or interface. Members include fields (including enum constants), methods
 * (including annotation methods), and constructors. This is analogous to {@link Member}, except
 * that it represents members in Java source (during annotation processing) vs. representing members
 * of runtime types or elements (methods and constructors) of runtime types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Member
 */
public interface ArMember {
   /**
    * Returns the class which declared this member.
    * 
    * @return the class in which this member was declared
    * 
    * @see java.lang.reflect.Member#getDeclaringClass()
    */
   ArClass getDeclaringClass();
   
   /**
    * Returns the modifiers that apply to this member. Intentionally unlike 
    * {@link java.lang.reflect.Member#getModifiers() java.lang.reflect.Member.getModifiers()}, this
    * returns a set of modifiers (defined in an {@linkplain ArModifier enum}) instead of a bitmasked
    * integer.
    * 
    * @return the modifiers for this member
    * 
    * @see java.lang.reflect.Member#getModifiers()
    */
   EnumSet<ArModifier> getModifiers();
   
   /**
    * Returns the member's name.
    * 
    * @return the member's name
    */
   String getName();
}
