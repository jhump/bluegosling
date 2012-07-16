package com.apriori.apt;

import com.apriori.apt.reflect.AnnotatedElement;
import com.apriori.apt.reflect.Class;
import com.apriori.apt.reflect.Constructor;
import com.apriori.apt.reflect.Field;
import com.apriori.apt.reflect.Method;
import com.apriori.apt.reflect.Package;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

//TODO: javadoc!
public class RoundEnvironment {
   
   private final javax.annotation.processing.RoundEnvironment env;
   
   RoundEnvironment(javax.annotation.processing.RoundEnvironment env) {
      this.env = env;
   }

   public boolean hasErrorFromPreviousRound() {
      return env.errorRaised();
   }
   
   public boolean isProcessingOver() {
      return env.processingOver();
   }
   
   public Set<? extends AnnotatedElement> allElements() {
      // TODO
      env.getRootElements();
      return null;
   }

   public Set<Package> allPackages() {
      // TODO
      return null;
   }
   
   public Set<Class> allClasses() {
      // TODO
      return null;
   }
   
   public Set<Class> allClasses(EnumSet<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : allClasses()) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<AnnotatedElement> annotatedElements(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<AnnotatedElement> annotatedElements(Class annotation) {
      // TODO
      return null;
   }

   public Set<Class> annotatedClasses(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<Class> annotatedClasses(Class annotation) {
      // TODO
      return null;
   }

   public Set<Class> annotatedClasses(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation,
         EnumSet<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<Class> annotatedClasses(Class annotation, EnumSet<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<Package> annotatedPackages(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<Package> annotatedPacakges(Class annotation) {
      // TODO
      return null;
   }

   public Set<Field> annotatedFields(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<Field> annotatedFields(Class annotation) {
      // TODO
      return null;
   }

   public Set<Method> annotatedMethods(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<Method> annotatedMethods(Class annotation) {
      // TODO
      return null;
   }

   public Set<Constructor> annotatedConstructors(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      // TODO
      return null;
   }

   public Set<Constructor> annotatedConstructors(Class annotation) {
      // TODO
      return null;
   }
}