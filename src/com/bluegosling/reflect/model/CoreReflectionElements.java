package com.bluegosling.reflect.model;

import com.bluegosling.reflect.AnnotatedTypes;
import com.bluegosling.reflect.ClassHierarchyScanner;
import com.bluegosling.reflect.MethodSignature;
import com.bluegosling.util.IndentingPrintWriter;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;

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
      StringBuilder sb = new StringBuilder(name);
      int p = sb.length();
      // Dots in the name could be separators between enclosing and nested class names. The actual
      // binary name for such a class would have a dollar sign, not a dot. So we first try the given
      // name, as is. Then we start replacing dots with dollars, starting from the end.
      while (p >= 0) {
         if (p < sb.length()) {
            sb.setCharAt(p, '$');
         }
         try {
            return new CoreReflectionTypeElement(Class.forName(name.toString()));
         } catch (ClassNotFoundException e) {
            p = sb.lastIndexOf(".", p);
         }
      }
      return null;
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
      if (!e.getKind().isClass()) {
         // only classes can inherit annotations
         return e.getAnnotationMirrors();
      }
      CoreReflectionTypeElement type = (CoreReflectionTypeElement) e;
      // Class#getAnnotations retrieves all annotations (including inherited ones)
      Annotation annotations[] = type.base().getAnnotations();
      List<AnnotationMirror> mirrors = new ArrayList<>(annotations.length);
      for (Annotation a : annotations) {
         mirrors.add(AnnotationMirrors.CORE_REFLECTION_INSTANCE.getAnnotationAsMirror(a));
      }
      return Collections.unmodifiableList(mirrors);
   }
   
   @Override
   public boolean hides(Element hider, Element hidden) {
      if (!hider.getSimpleName().equals(hidden.getSimpleName())) {
         // an element cannot hide another if they don't have the same name
         return false;
      }
      if (hider.equals(hidden)) {
         // an element does not hide itself
         return false;
      }
      return hider.accept(HIDES_VISITOR, hidden);
   }

   private static final ElementVisitor<Boolean, Element> HIDES_VISITOR =
         new ElementKindVisitor8<Boolean, Element>(false) {
            @Override
            public Boolean visitVariableAsEnumConstant(VariableElement e, Element hidden) {
               return visitVariableAsField(e, hidden);
            }

            @Override
            public Boolean visitVariableAsField(VariableElement e, Element hidden) {
               return checkField(e, hidden, false);
            }
            
            @Override
            public Boolean visitVariableAsParameter(VariableElement e, Element hidden) {
               Element member = e.getEnclosingElement();
               assert member instanceof CoreReflectionExecutableElement;
               return checkField(e.getEnclosingElement(), hidden, true);
            }
            
            // NB: We don't need to handle other VariableElements -- e.g. local variables, caught
            // exception variables, resource variables -- because core reflection has no way to
            // represent them. 
            
            private boolean checkField(Element memberElement, Element hidden,
                  boolean includeGivenType) {
               // variables can hide fields on supertypes, and optionally on the given type too
               if (!hidden.getKind().isField()) {
                  return false;
               }
               Field hid = ((CoreReflectionFieldElement) hidden).base();
               Class<?> hidType = hid.getDeclaringClass();
               Class<?> hidingType =
                     ((CoreReflectionTypeElement) memberElement.getEnclosingElement()).base();
               if (!hidType.isAssignableFrom(hidingType)) {
                  return false;
               }
               if (!includeGivenType && hidType == hidingType) {
                  return false;
               }
               // Only remaining check is for visibility -- if hid is not visible from hiding, then
               // it cannot be hidden.
               if (java.lang.reflect.Modifier.isPublic(hid.getModifiers())
                     || java.lang.reflect.Modifier.isProtected(hid.getModifiers())) {
                  return true;
               }
               if (java.lang.reflect.Modifier.isPrivate(hid.getModifiers())) {
                  return false;
               }
               // Hidden field is package private. So declarer of hiding must be in same package.
               return hidType.getPackage().getName().equals(hidingType.getPackage().getName());
            }
            
            @Override
            public Boolean visitExecutableAsMethod(ExecutableElement e, Element hidden) {
               if (hidden.getKind() != ElementKind.METHOD) {
                  return false;
               }
               Method hiding = (Method) ((CoreReflectionExecutableElement) e).base();
               Method hid = (Method) ((CoreReflectionExecutableElement) hidden).base();
               if (!java.lang.reflect.Modifier.isStatic(hiding.getModifiers())
                     || !java.lang.reflect.Modifier.isStatic(hid.getModifiers())) {
                  // only static methods can hide other methods (instance methods "override")
                  return false;
               }
               Class<?> hidDeclarer = hid.getDeclaringClass();
               Class<?> hidingDeclarer = hiding.getDeclaringClass();
               if (!hidDeclarer.isAssignableFrom(hidingDeclarer)) {
                  // a method can only hide methods on supertypes
                  return false;
               }
               assert !hiding.equals(hid);
               if (!ExecutableSignature.of(hiding).isSubsignatureOf(ExecutableSignature.of(hid))) {
                  return false;
               }
               // Only remaining check is for visibility -- if hid is not visible from hiding, then
               // it cannot be hidden.
               if (java.lang.reflect.Modifier.isPublic(hid.getModifiers())
                     || java.lang.reflect.Modifier.isProtected(hid.getModifiers())) {
                  return true;
               }
               if (java.lang.reflect.Modifier.isPrivate(hid.getModifiers())) {
                  return false;
               }
               // Hidden method is package private. So the declarer of hiding must be in the same
               // package, or it must have a supertype (that is a subtype of the declarer of hid)
               // that overrides it and broadens its visibility.
               String packageName = hidDeclarer.getPackage().getName();
               if (hidingDeclarer.getPackage().getName().equals(packageName)) {
                  return true;
               }
               assert !hidDeclarer.isInterface() && !hidingDeclarer.isInterface();
               String name = hid.getName();
               Class<?>[] paramTypes = hid.getParameterTypes();
               while (true) {
                  hidingDeclarer = hidingDeclarer.getSuperclass();
                  if (hidingDeclarer == hidDeclarer) {
                     // hid is not visible
                     return false;
                  }
                  try {
                     Method m = hidingDeclarer.getDeclaredMethod(name, paramTypes);
                     if (java.lang.reflect.Modifier.isPublic(m.getModifiers())
                           || java.lang.reflect.Modifier.isProtected(m.getModifiers())) {
                        // hid is visible!
                        return true;
                     }
                  } catch (NoSuchMethodException ex) {
                     // fall-through to next iteration of loop
                  }
               }
            }

            @Override
            public Boolean visitTypeParameter(TypeParameterElement e, Element hidden) {
               TypeVariable<?> typeVar = ((CoreReflectionTypeParameterElement) e).base();
               GenericDeclaration decl = typeVar.getGenericDeclaration();
               if (decl instanceof Class) {
                  Class<?> declaringClass = (Class<?>) decl;
                  if (!declaringClass.isMemberClass() && !declaringClass.isLocalClass()) {
                     // type variables for top-level classes can't hide anything
                     return false;
                  }
                  assert !declaringClass.isAnonymousClass();
               }
               return checkType(decl, ((CoreReflectionBaseElement<?>) hidden).base());
            }
            
            @Override
            public Boolean visitType(TypeElement e, Element hidden) {
               Class<?> type = ((CoreReflectionTypeElement) e).base();
               if (!type.isMemberClass() && !type.isLocalClass()) {
                  return false;
               }
               return checkType(type, ((CoreReflectionBaseElement<?>) hidden).base());
            }
            
            private boolean checkType(AnnotatedElement member, AnnotatedElement hidden) {
               // Member types (and type parameters on members) can hide other member types on
               // the enclosing types (or the enclosing types' supertypes). They can also hide
               // type variables on the enclosing types if they are non-static members (or type
               // variables on non-static members).
               
               AnnotatedElement hiddenEnclosing = getEnclosing(hidden);
               boolean hiddenIsTypeVar = hidden instanceof TypeVariable;
               if (!hiddenIsTypeVar && !(hidden instanceof Class)) {
                  // can only hide types and type variables
                  return false;
               }
               AnnotatedElement e = member;
               while (true) {
                  if (!isStatic(e) && hiddenIsTypeVar) {
                     return false;
                  }
                  AnnotatedElement enclosing = getEnclosing(e);
                  if (enclosing == null) {
                     return false;
                  }
                  if (matches(enclosing, hiddenEnclosing)) {
                     return true;
                  }
                  e = enclosing;
               }
            }
            
            private AnnotatedElement getEnclosing(AnnotatedElement e) {
               if (e instanceof Class) {
                  Class<?> clazz = (Class<?>) e;
                  if (clazz.isLocalClass()) {
                     Class<?> enclosingClass = clazz.getEnclosingClass();
                     if (enclosingClass.isLocalClass()) {
                        // nested local class, so enclosing class is immediately enclosing element
                        assert clazz.isLocalClass();
                        return enclosingClass;
                     }
                     // otherwise, enclosing method or ctor is immediately enclosing element
                     Executable ex = clazz.getEnclosingMethod();
                     if (ex == null) {
                        ex = clazz.getEnclosingConstructor();
                        assert ex != null;
                     }
                     return ex;
                  }
                  return clazz.getEnclosingClass();
               } else {
                  Executable ex = (Executable) e;
                  return ex.getDeclaringClass();
               }
            }
            
            private boolean matches(AnnotatedElement enclosesHider, AnnotatedElement enclosesHidden) {
               if (enclosesHider.equals(enclosesHidden)) {
                  return true;
               }
               if ((enclosesHider instanceof Class) && (enclosesHidden instanceof Class)) {
                  return ((Class<?>) enclosesHidden).isAssignableFrom((Class<?>) enclosesHider);
               }
               return false;
            }
            
            private boolean isStatic(AnnotatedElement e) {
               int mods = e instanceof Class
                     ? ((Class<?>) e).getModifiers()
                     : ((Executable) e).getModifiers();
               return java.lang.reflect.Modifier.isStatic(mods);
            }
         };
         
   @Override
   public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
         TypeElement type) {
      if (overrider.getKind() != ElementKind.METHOD || overridden.getKind() != ElementKind.METHOD) {
         return false;
      }
      Method overriderMethod = (Method) ((CoreReflectionExecutableElement) overrider).base();
      Method overriddenMethod = (Method) ((CoreReflectionExecutableElement) overridden).base();
      if (java.lang.reflect.Modifier.isStatic(overriderMethod.getModifiers())
            || java.lang.reflect.Modifier.isStatic(overriddenMethod.getModifiers())) {
         // static methods cannot override (they hide)
         return false;
      }
      Class<?> overriderType = overriderMethod.getDeclaringClass();
      Class<?> overriddenType = overriddenMethod.getDeclaringClass();
      Class<?> clazz = ((CoreReflectionTypeElement) type).base();
      if (!overriderType.isAssignableFrom(clazz)) {
         return false;
      }
      if (!overriddenType.isAssignableFrom(clazz)) {
         return false;
      }
      
      // Overridden type should usually be a supertype of the overriding type. If that's not
      // the case, the only way it can override is if the overrider is a class and overridden
      // is an interface (in which case the given type, being a subtype of both, inherits the
      // overrider and thus overrides, aka implements, the overridden)
      if (!overriddenType.isAssignableFrom(overriderType)
            && (overriderType.isInterface() || !overriddenType.isInterface())) {
         return false;
      }
      if (!ExecutableSignature.of(overriderMethod)
            .isSubsignatureOf(ExecutableSignature.of(overriddenMethod))) {
         return false;
      }
      // Only remaining check is for visibility -- if overridden is not visible from overrider, then
      // it cannot be overriding.
      if (java.lang.reflect.Modifier.isPublic(overriddenMethod.getModifiers())
            || java.lang.reflect.Modifier.isProtected(overriddenMethod.getModifiers())) {
         return true;
      }
      if (java.lang.reflect.Modifier.isPrivate(overriddenMethod.getModifiers())) {
         return false;
      }
      // Overridden method is package private. So the declarer of overrider must be in the same
      // package, or it must have a supertype (that is a subtype of the declarer of overridden)
      // that overrides it and broadens its visibility.
      String packageName = overriddenType.getPackage().getName();
      if (overriderType.getPackage().getName().equals(packageName)) {
         return true;
      }
      assert !overriddenType.isInterface() && !overriderType.isInterface();
      String name = overriddenMethod.getName();
      Class<?>[] paramTypes = overriddenMethod.getParameterTypes();
      while (true) {
         overriderType = overriderType.getSuperclass();
         if (overriderType == overriddenType) {
            // hid is not visible
            return false;
         }
         try {
            Method m = overriderType.getDeclaredMethod(name, paramTypes);
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())
                  || java.lang.reflect.Modifier.isProtected(m.getModifiers())) {
               // hid is visible!
               return true;
            }
         } catch (NoSuchMethodException ex) {
            // fall-through to next iteration of loop
         }
      }
   }
   
   @Override
   public String getConstantExpression(Object value) {
      if (value instanceof String) {
         String s = (String) value;
         StringBuilder sb = new StringBuilder();
         sb.append('"');
         for (int i = 0; i < s.length(); i++) {
            encodeChar(s.charAt(i), true, sb);
         }
         sb.append('"');
         return sb.toString();
      } else if (value instanceof Boolean) {
         return ((Boolean) value) ? "true" : "false";
      } else if (value instanceof Byte) {
         return "(byte)0x" + Integer.toHexString(((Byte) value).intValue() & 0xff);
      } else if (value instanceof Character) {
         StringBuilder sb = new StringBuilder();
         sb.append('\'');
         encodeChar((Character) value, false, sb);
         sb.append('\'');
         return sb.toString();
      } else if (value instanceof Short) {
         return "(short)" + ((Byte) value).toString();
      } else if (value instanceof Integer) {
         return ((Integer) value).toString();
      } else if (value instanceof Long) {
         return ((Long) value).toString() + "L";
      } else if (value instanceof Float) {
         Float f = (Float) value;
         if (f.isNaN()) {
            return "0.0F/0.0F";
         }
         if (f.isInfinite()) {
            return f > 0 ? "1.0F/0.0F" : "-1.0F/0.0F";
         }
         return f.toString() + "F";
      } else if (value instanceof Double) {
         Double d = (Double) value;
         if (d.isNaN()) {
            return "0.0/0.0";
         }
         if (d.isInfinite()) {
            return d > 0 ? "1.0/0.0" : "-1.0/0.0";
         }
         return d.toString();
      } else {
         throw new IllegalArgumentException("value must be a string or a primitive, instead was a "
               + value.getClass().getTypeName());
      }
   }
   
   private void encodeChar(char ch, boolean inDoubleQuotes, StringBuilder sb) {
      // Unicode characters are allowed in Java source. But, to be conservative, we'll only directly
      // emit ASCII printable characters and use escapes for the rest. 
      if ((ch > 31 && ch < 127 && ch != '\\' && ch != '\'' && ch != '"')
            || (ch == '\'' && inDoubleQuotes) || (ch == '"' && !inDoubleQuotes)) {
         sb.append(ch);
      } else {
         // escape sequences, JLS 3.10.6
         sb.append('\\');
         switch (ch) {
            case '\b':
               sb.append('b');
               break;
            case '\t':
               sb.append('t');
               break;
            case '\n':
               sb.append('n');
               break;
            case '\f':
               sb.append('f');
               break;
            case '\r':
               sb.append('r');
               break;
            case '"': case '\'': case '\\':
               sb.append(ch);
               break;
            default:
               if (ch < 256) {
                  // octal escape
                  String octal = Integer.toOctalString(ch);
                  assert octal.length() < 3
                        || (octal.length() == 3
                              && octal.charAt(0) >= '0' && octal.charAt(0) <= '3');
                  sb.append(octal);
               } else {
                  // unicode escape, JLS 3.3
                  sb.append('u');
                  String hex = Integer.toHexString(ch);
                  assert hex.length() <= 4;
                  for (int i = 0; i < 4 - hex.length(); i++) {
                     sb.append('0');
                  }
                  sb.append(hex);
               }
         }
      }
   }

   @Override
   public void printElements(Writer w, Element... elements) {
      IndentingPrintWriter p = new IndentingPrintWriter(w, 2);
      for (Element e : elements) {
         e.accept(PRINTING_VISITOR, p);
         p.println();
      }
   }

   private static final ElementVisitor<Void, IndentingPrintWriter> PRINTING_VISITOR =
         new ElementKindVisitor8<Void, IndentingPrintWriter>() {
            @Override
            public Void visitPackage(PackageElement e, IndentingPrintWriter p) {
               printPreamble(e, p);
               p.printf("package %s;%n", e.getQualifiedName());
               return null;
            }

            @Override
            public Void visitTypeAsAnnotationType(TypeElement e, IndentingPrintWriter p) {
               visitType(e, "@interface", p);
               return null;
            }

            @Override
            public Void visitTypeAsClass(TypeElement e, IndentingPrintWriter p) {
               visitType(e, "class", p);
               return null;
            }

            @Override
            public Void visitTypeAsEnum(TypeElement e, IndentingPrintWriter p) {
               visitType(e, "enum", p);
               return null;
            }

            @Override
            public Void visitTypeAsInterface(TypeElement e, IndentingPrintWriter p) {
               visitType(e, "interface", p);
               return null;
            }
            
            private void visitType(TypeElement e, String typeKeyword, IndentingPrintWriter p) {
               if (e.getNestingKind() == NestingKind.TOP_LEVEL) {
                  visitPackage((PackageElement) e.getEnclosingElement(), p);
               }
               printPreamble(e, p);
               p.print(typeKeyword);
               p.printf(" %s", e.getSimpleName());
               printTypeArgs(e.getTypeParameters(), p);
               if (!e.getKind().isInterface()) {
                  TypeMirror superclass = e.getSuperclass();
                  if (!isSimpleObject(superclass)) {
                     p.printf(" extends %s", superclass);
                  }
               }
               List<? extends TypeMirror> interfaces = e.getInterfaces();
               if (!interfaces.isEmpty()) {
                  p.print(" implements ");
                  boolean first = true;
                  for (TypeMirror i : interfaces) {
                     if (first) {
                        first = false;
                     } else {
                        p.print(", ");
                     }
                     p.print(i.toString());
                  }
               }
               p.println(" {");
               p.indent();
               try {
                  for (Element enclosed : e.getEnclosedElements()) {
                     enclosed.accept(this, p);
                  }
               } finally {
                  p.outdent();
               }
               p.println('}');
            }

            @Override
            public Void visitVariableAsEnumConstant(VariableElement e,
                  IndentingPrintWriter p) {
               return visitVariableAsField(e, p);
            }

            @Override
            public Void visitVariableAsField(VariableElement e, IndentingPrintWriter p) {
               printPreamble(e, p);
               p.printf("%s %s;%n", e.asType(), e.getSimpleName());
               return null;
            }

            @Override
            public Void visitExecutableAsConstructor(ExecutableElement e,
                  IndentingPrintWriter p) {
               visitExecutable(e, false, p);
               return null;
            }

            @Override
            public Void visitExecutableAsMethod(ExecutableElement e, IndentingPrintWriter p) {
               visitExecutable(e, false, p);
               return null;
            }
            
            private void visitExecutable(ExecutableElement e, boolean printReturnType,
                  IndentingPrintWriter p) {
               printPreamble(e, p);
               if (e.isDefault()) {
                  p.print(" default");
               }
               printTypeArgs(e.getTypeParameters(), p);
               if (printReturnType) {
                  p.printf("%s ", e.getReturnType());
               }
               p.printf("%s(", e.getSimpleName());
               boolean first = true;
               if (hasAnnotations(e.getReceiverType())) {
                  p.printf("%s this", e.getReceiverType());
                  first = false;
               }
               List<? extends VariableElement> params = e.getParameters();
               for (int i = 0, len = params.size(); i < len; i++) {
                  if (first) {
                     first = false;
                  } else {
                     p.print(',');
                  }
                  printParam(params.get(i), e.isVarArgs() && i == len - 1, p);
               }
               p.print(')');
               List<? extends TypeMirror> thrown = e.getThrownTypes();
               if (!thrown.isEmpty()) {
                  p.print(" throws ");
                  boolean firstThrown = true;
                  for (TypeMirror m : thrown) {
                     if (firstThrown) {
                        firstThrown = false;
                     } else {
                        p.print(',');
                     }
                     p.print(m);
                  }
               }
               p.println(';');
            }
            
            private boolean hasAnnotations(TypeMirror t) {
               return t.accept(HAS_ANNOTATIONS, null);
            }
            
            private void printPreamble(Element e, IndentingPrintWriter p) {
               printAnnotations(e.getAnnotationMirrors(), false, p);
               printModifiers(e.getModifiers(), p);
            }

            private void printFlatPreamble(Element e, IndentingPrintWriter p) {
               printAnnotations(e.getAnnotationMirrors(), true, p);
               printModifiers(e.getModifiers(), p);
            }

            private void printAnnotations(List<? extends AnnotationMirror> annotations,
                  boolean flat, IndentingPrintWriter p) {
               for (AnnotationMirror a : annotations) {
                  printAnnotation(a, p);
                  if (flat) {
                     p.print(' ');
                  } else {
                     p.println();
                  }
               }
            }
            
            private void printModifiers(Set<Modifier> modifiers, IndentingPrintWriter p) {
               Object[] array = modifiers.toArray();
               Arrays.sort(array);
               for (Object o : array) {
                  p.print(o);
               }
            }
            
            private void printTypeArgs(List<? extends TypeParameterElement> vars,
                  IndentingPrintWriter p) {
               if (vars.isEmpty()) {
                  return;
               }
               p.print('<');
               boolean first = true;
               for (TypeParameterElement var : vars) {
                  if (first) {
                     first = false;
                  } else {
                     p.print(',');
                  }
                  visitTypeParameter(var, p);
               }
               p.print("> ");
            }

            private void printParam(VariableElement e, boolean varArg,
                  IndentingPrintWriter p) {
               printFlatPreamble(e, p);
               p.printf("%s%s %s", e.asType(), varArg ? "..." : "", e.getSimpleName());
            }
            
            @Override
            public Void visitTypeParameter(TypeParameterElement e, IndentingPrintWriter p) {
               p.print(e.getSimpleName().toString());
               List<? extends TypeMirror> bounds = e.getBounds();
               if (isJustSimpleObject(bounds)) {
                  return null;
               }
               p.print(" extends ");
               boolean firstBound = true;
               for (TypeMirror b : bounds) {
                  if (firstBound) {
                     firstBound = false;
                  } else {
                     p.print('&');
                  }
                  p.print(b.toString());
               }
               return null;
            }

            private boolean isJustSimpleObject(List<? extends TypeMirror> mirrors) {
               if (mirrors.isEmpty()) {
                  return true;
               }
               if (mirrors.size() > 1) {
                  return false;
               }
               return isSimpleObject(mirrors.get(0));
            }

            private boolean isSimpleObject(TypeMirror mirror) {
               // check if the type is Object with no annotations
               return mirror.getAnnotationMirrors().isEmpty()
                     && mirror.getKind() == TypeKind.DECLARED
                     && ((TypeElement) ((DeclaredType) mirror).asElement()).getQualifiedName()
                           .equals("java.lang.Object");
            }
         };

   static final TypeVisitor<Boolean, Void> HAS_ANNOTATIONS =
         new SimpleTypeVisitor8<Boolean, Void>() {
            @Override
            protected Boolean defaultAction(TypeMirror e, Void p) {
               return !e.getAnnotationMirrors().isEmpty();
            }
      
            @Override
            public Boolean visitIntersection(IntersectionType t, Void p) {
               if (defaultAction(t, p)) {
                  return true;
               }
               for (TypeMirror b : t.getBounds()) {
                  if (b.accept(this, p)) {
                     return true;
                  }
               }
               return false;
            }

            @Override
            public Boolean visitUnion(UnionType t, Void p) {
               if (defaultAction(t, p)) {
                  return true;
               }
               for (TypeMirror b : t.getAlternatives()) {
                  if (b.accept(this, p)) {
                     return true;
                  }
               }
               return false;
            }

            @Override
            public Boolean visitArray(ArrayType t, Void p) {
               if (defaultAction(t, p)) {
                  return true;
               }
               return t.getComponentType().accept(this, p);
            }

            @Override
            public Boolean visitDeclared(DeclaredType t, Void p) {
               if (defaultAction(t, p)) {
                  return true;
               }
               if (t.getEnclosingType().accept(this, p)) {
                  return true;
               }
               for (TypeMirror b : t.getTypeArguments()) {
                  if (b.accept(this, p)) {
                     return true;
                  }
               }
               return false;
            }

            @Override
            public Boolean visitWildcard(WildcardType t, Void p) {
               if (defaultAction(t, p)) {
                  return true;
               }
               if (t.getExtendsBound().accept(this, p)) {
                  return true;
               }
               return t.getSuperBound().accept(this, p);
            }
         };
         
   private static final AnnotationValueVisitor<Void, PrintWriter> ANNOTATION_PRINTER =
         new SimpleAnnotationValueVisitor8<Void, PrintWriter>() {
            @Override
            public Void defaultAction(Object v, PrintWriter p) {
               p.print(INSTANCE.getConstantExpression(v));
               return null;
            }
            
            @Override
            public Void visitType(TypeMirror t, PrintWriter p) {
               p.printf("%s.class",
                     ((TypeElement) ((DeclaredType) t).asElement()).getQualifiedName());
               return null;
            }

            @Override
            public Void visitEnumConstant(VariableElement c, PrintWriter p) {
               TypeElement enumType = (TypeElement) c.getEnclosingElement();
               p.printf("%s.%s", enumType.getQualifiedName(), c.getSimpleName());
               return null;
            }

            @Override
            public Void visitAnnotation(AnnotationMirror a, PrintWriter p) {
               printAnnotation(a, p);
               return null;
            }

            @Override
            public Void visitArray(List<? extends AnnotationValue> vals, PrintWriter p) {
               p.print("{");
               boolean first = true;
               for (AnnotationValue v : vals) {
                  if (first) {
                     first = false;
                  } else {
                     p.print(",");
                  }
                  v.accept(this, p);
               }
               p.print("}");
               return null;
            }
         };
         
   static void printAnnotation(AnnotationMirror a, PrintWriter p) {
      p.printf("@%s",
            ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName());
      Map<? extends ExecutableElement, ? extends AnnotationValue> values =
            a.getElementValues();
      if (!values.isEmpty()) {
         p.print("(");
         if (isJustValue(values.keySet())) {
            printAnnotationValue(values.values().iterator().next(), p);
         } else {
            for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                  : values.entrySet()) {
               p.printf("%s=", entry.getKey().getSimpleName());
               printAnnotationValue(entry.getValue(), p);
            }
         }
         p.print(")");
      }
   }
   
   static void printAnnotationValue(AnnotationValue v, PrintWriter p) {
      v.accept(ANNOTATION_PRINTER, p);
   }

   static boolean isJustValue(Set<? extends ExecutableElement> methods) {
      return methods.size() == 1
            && methods.iterator().next().getSimpleName().equals("value");
   }

   @Override
   public Name getName(CharSequence cs) {
      return CoreReflectionName.of(cs.toString());
   }
   
   @Override
   public boolean isFunctionalInterface(TypeElement type) {
      Class<?> clazz = ((CoreReflectionTypeElement) type).base();
      return com.bluegosling.reflect.Types.isFunctionalInterface(clazz);
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
