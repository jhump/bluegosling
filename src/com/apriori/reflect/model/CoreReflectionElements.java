package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;
import com.apriori.reflect.ClassHierarchyScanner;
import com.apriori.reflect.MethodSignature;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleElementVisitor8;

/**
 * The implementation of {@link Elements} that is backed by core reflection.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
enum CoreReflectionElements implements Elements {
   INSTANCE;

   @Override
   public PackageElement getPackageElement(CharSequence name) {
      String packageName = name.toString();
      if (!CoreReflectionPackages.doesPackageExist(packageName)) {
         return null;
      }
      Package pkg = CoreReflectionPackages.getPackage(packageName);
      return pkg == null
            ? new CoreReflectionSyntheticPackageElement(name.toString())
            : new CoreReflectionPackageElement(pkg);
   }

   @Override
   public TypeElement getTypeElement(CharSequence name) {
      try {
         return new CoreReflectionTypeElement(Class.forName(name.toString()));
      } catch (ClassNotFoundException e) {
         return null;
      }
   }

   @Override
   public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
         AnnotationMirror a) {
      // NB: Annotation mirrors returned by core reflection elements and type mirrors will always
      // be fully formed. So this method is only useful for user-constructed incomplete mirrors that
      // are backed by a core reflection annotation type.
      CoreReflectionDeclaredType type = (CoreReflectionDeclaredType) a.getAnnotationType();
      CoreReflectionTypeElement element = (CoreReflectionTypeElement) type.asElement();
      assert element.base().isAnnotation();
      Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>(a.getElementValues());
      for (Method m : element.base().getDeclaredMethods()) {
         ExecutableElement ee = getExecutableElement(m);
         if (!values.containsKey(ee)) {
            Object o = m.getDefaultValue();
            if (o == null) {
               throw new IllegalArgumentException(
                     "given annotation mirror does not include required field " + m.getName()
                     + ": " + a);
            }
            values.put(ee, AnnotationMirrors.CORE_REFLECTION_INSTANCE.getAnnotationValue(o));
         }
      }
      return Collections.unmodifiableMap(values);
   }

   @Override
   public String getDocComment(Element e) {
      // NB: no access to doc comments via core reflection
      return null;
   }

   @Override
   public boolean isDeprecated(Element e) {
      // NB: no access to doc comments via core reflection, so we can only look for this annotation
      // and cannot check for "@deprecated" javadoc tag.
      return e.getAnnotation(Deprecated.class) != null;
   }

   @Override
   public Name getBinaryName(TypeElement type) {
      return CoreReflectionName.of(((CoreReflectionTypeElement) type).base().getName());
   }

   @Override
   public PackageElement getPackageOf(Element type) {
      while (type.getKind() != ElementKind.PACKAGE) {
         type = type.getEnclosingElement();
      }
      return (PackageElement) type;
   }

   @Override
   public List<? extends Element> getAllMembers(TypeElement type) {
      Class<?> clazz = ((CoreReflectionTypeElement) type).base();

      List<Constructor<?>> ctors = crawl(clazz, Class::getDeclaredConstructors,
            c -> new MethodSignature("init", c.getParameterTypes()));
      List<Method> methods = crawl(clazz, Class:: getDeclaredMethods, m -> new MethodSignature(m));
      List<Field> fields = crawl(clazz, Class::getDeclaredFields, Function.identity());
      List<Class<?>> nestedTypes = crawl(clazz, Class::getDeclaredClasses, Function.identity());
      
      List<Element> allElements = new ArrayList<>(ctors.size() + methods.size() + fields.size()
            + nestedTypes.size());
      for (Field f : fields) {
         allElements.add(getFieldElement(f));
      }
      for (Constructor<?> c : ctors) {
         allElements.add(getExecutableElement(c));
      }
      for (Method m : methods) {
         allElements.add(getExecutableElement(m));
      }
      for (Class<?> cl : nestedTypes) {
         allElements.add(getTypeElement(cl));
      }
      return Collections.unmodifiableList(allElements);
   }

   @Override
   public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
      // TODO: implement me
      return null;
   }

   private static final ElementVisitor<Boolean, Element> HIDES_VISITOR =
         new SimpleElementVisitor8<Boolean, Element>() {
            @Override
            public Boolean visitVariable(VariableElement e, Element p) {
               // TODO: implement me
               return super.visitVariable(e, p);
            }

            @Override
            public Boolean visitType(TypeElement e, Element p) {
               // TODO: implement me
               return super.visitType(e, p);
            }

            @Override
            public Boolean visitExecutable(ExecutableElement e, Element p) {
               // TODO: implement me
               return super.visitExecutable(e, p);
            }

            @Override
            public Boolean visitTypeParameter(TypeParameterElement e, Element p) {
               // TODO: implement me
               return super.visitTypeParameter(e, p);
            }
         };
         
   @Override
   public boolean hides(Element hider, Element hidden) {
      return hider.accept(HIDES_VISITOR, hidden);
   }

   @Override
   public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
         TypeElement type) {
      // TODO: implement me
      return false;
   }

   @Override
   public String getConstantExpression(Object value) {
      if (value instanceof String) {
         // TODO
         return null;
      } else if (value instanceof Boolean) {
         return ((Boolean) value) ? "true" : "false";
      } else if (value instanceof Byte) {
         return "(byte)" + ((Byte) value).toString();
      } else if (value instanceof Character) {
         // TODO
         return null;
      } else if (value instanceof Short) {
         return "(short)" + ((Byte) value).toString();
      } else if (value instanceof Integer) {
         return ((Integer) value).toString();
      } else if (value instanceof Long) {
         return ((Long) value).toString() + "L";
      } else if (value instanceof Float) {
         Float f = (Float) value;
         if (f.isNaN()) {
            return "java.lang.Float.NaN";
         }
         if (f.isInfinite()) {
            return f > 0
                  ? "java.lang.Float.POSITIVE_INFINITY"
                  : "java.lang.Float.NEGATIVE_INFINITY";
         }
         return f.toString() + "F";
      } else if (value instanceof Double) {
         Double d = (Double) value;
         if (d.isNaN()) {
            return "java.lang.Double.NaN";
         }
         if (d.isInfinite()) {
            return d > 0
                  ? "java.lang.Double.POSITIVE_INFINITY"
                  : "java.lang.Double.NEGATIVE_INFINITY";
         }
         return d.toString();
      } else {
         throw new IllegalArgumentException("value must be a string or a primitive, instead was a "
               + value.getClass().getTypeName());
      }
   }

   @Override
   public void printElements(Writer w, Element... elements) {
      // TODO: implement me
      
   }

   @Override
   public Name getName(CharSequence cs) {
      return CoreReflectionName.of(cs.toString());
   }
   
   @Override
   public boolean isFunctionalInterface(TypeElement type) {
      // TODO: implement me
      return false;
   }

   @Override
   public TypeElement getTypeElement(Class<?> clazz) {
      return new CoreReflectionTypeElement(clazz);
   }

   @Override
   public PackageElement getPackageElement(Package pkg) {
      return new CoreReflectionPackageElement(pkg);
   }

   @Override
   public VariableElement getParameterElement(Parameter parameter) {
      return new CoreReflectionParameterElement(parameter);
   }

   @Override
   public VariableElement getFieldElement(Field field) {
      return new CoreReflectionFieldElement(field);
   }

   @Override
   public ExecutableElement getExecutableElement(Executable executable) {
      return new CoreReflectionExecutableElement(executable);
   }

   @Override
   public TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar) {
      if (typeVar instanceof AnnotatedCapturedType.CapturedTypeVariable) {
         CoreReflectionCapturedType mirror =
               new CoreReflectionCapturedType(AnnotatedTypes.newAnnotatedTypeVariable(typeVar));
         return mirror.asElement();
      } else {
         return new CoreReflectionTypeParameterElement(typeVar);
      }
   }
   
   /**
    * Crawls the given type's hierarchy (including super-classes and interfaces) and returns a
    * list of extracted objects. A given function is used to extract objects from each type in
    * the hierarchy, and another given function is used to compute keys for each extracted object
    * (to de-dup objects that may be defined on more than one type, e.g. overridden methods).
    *
    * @param clazz the type to crawl
    * @param objExtract a function that extracts an array of objects from a given class token
    * @param keyExtract a function that computes a key for a given extracted object
    * @return the (de-dup'ed) list of objects extracted from the given type's hierarchy
    */
   private static <T, K> List<T> crawl(Class<?> clazz, Function<Class<?>, T[]> objExtract,
         Function<T, K> keyExtract) {
      Map<K, T> map = new LinkedHashMap<>();
      ClassHierarchyScanner.scanWith(clazz, map, (aClass, aMap) -> {
            for (T t : objExtract.apply(aClass)) {
               K k = keyExtract.apply(t);
               // don't overwrite an entry with one from a more distant type
               if (!aMap.containsKey(k)) {
                  aMap.put(k, t);
               }
            }
            return null;
      });
      return new ArrayList<>(map.values());
   }
}
