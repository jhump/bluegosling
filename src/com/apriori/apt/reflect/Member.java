package com.apriori.apt.reflect;

import java.util.EnumSet;

//TODO: javadoc!!
public interface Member {
   Class getDeclaringClass();
   EnumSet<Modifier> getModifiers();
   String getName();
}
