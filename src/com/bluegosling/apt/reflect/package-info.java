/**
 * This package provides an alternate set of APIs for interacting with annotations and types from an
 * annotation processor. The layout of classes and interfaces here closely resembles those in
 * {@code java.lang.reflect}, which many programmers will feel are more natural and easier to use
 * than the APIs provided in the {@code javax.lang.model.element} and {@code java.lang.model.type}
 * packages (aka language model APIs).
 * 
 * <p>All classes in this package have names that match similar classes in the standard reflection
 * APIs, but with an {@code Ar} prefix -- short for "<strong>A</strong>nnotation processing
 * <strong>R</strong>eflection". The prefix is meant to prevent import collisions with the
 * actual reflection types and to prevent confusion when looking at variable declarations.
 * 
 * <p>To make the most of the APIs in this package, your annotation processor should extend
 * {@link com.bluegosling.apt.reflect.ArProcessor}. This alternate base class provides a reference to
 * a {@link com.bluegosling.apt.reflect.ArRoundEnvironment}, which is very similar to the standard
 * {@link javax.annotation.processing.RoundEnvironment} except that its API is defined in terms of
 * this set of reflection APIs (instead of in terms of elements and type mirrors).
 * 
 * <p>This API strives to maintain the abstraction fidelity provided by the {@code javax.lang.model}
 * APIs, but also provide the ease-of-use provided by core reflection APIs. So, like the
 * {@code javax.lang.model} types they wrap, the types in this API come in three flavors --
 * elements, types, and annotations -- which are described in more detail below.
 * 
 * <h3>Elements</h3>
 * Elements represent components of a Java program: classes, methods, fields, etc. They map to
 * declarations in source code. They are analogous to the most commonly used types in the core
 * reflection API: {@link java.lang.Class}, {@link java.lang.reflect.Constructor},
 * {@link java.lang.reflect.Method}, and {@link java.lang.reflect.Field}. In this package, they are
 * represented by the {@link com.bluegosling.apt.reflect.ArAnnotatedElement} type hierarchy and
 * generally wrap {@code javax.lang.model.element} objects.
 * 
 * <p>An exception to this is {@link com.bluegosling.apt.reflect.ArClass}. Most classes <em>do</em>
 * wrap a {@link javax.lang.model.element.TypeElement}, but this API allows usage of class tokens
 * for primitive and array types, just like core reflection.
 * {@link javax.lang.model.element.TypeElement}s, on the other hand, only model declared types
 * and thus cannot represent primitive and array types.
 *
 * <h3>Types</h3>
 * In core reflection, there are two different way to represent types: the
 * {@link java.lang.reflect.Type} hierarchy, introduced for modeling generic types in Java 5, and
 * the {@link java.lang.reflect.AnnotatedType} hierarchy, introduced for modeling types with type
 * annotations in Java 8. The {@link java.lang.reflect.Type} hierarchy "blurred the lines" between
 * types and elements by including a couple of classes that also represent elements:
 * {@link java.lang.Class} and {@link java.lang.reflect.TypeVariable}. The
 * {@code java.lang.model.type} APIs do not blur the lines. And because they were a cleaner
 * separation from how elements were modeled, they did not need to introduce a parallel type
 * hierarchy to model type annotations when Java 8 came along.
 * 
 * <p>This package models types with the {@link com.bluegosling.apt.reflect.ArType} hierarchy. Each
 * type object wraps a {@link javax.lang.model.type.TypeMirror} object. The API resembles a mix of
 * {@link javax.lang.model.type.TypeMirror} (from language model APIs) and
 * {@link java.lang.reflect.AnnotatedType} (from core reflection APIs), attempting to provide
 * greater ease-of-use than type mirror while still maintaining the abstraction fidelity.
 * 
 * <h3>Annotations</h3>
 * In core reflection, annotations are instances of the annotation interface itself. In the
 * language model APIs, there are mirrors. This package provides a blend. The
 * {@link com.bluegosling.apt.reflect.ArAnnotation} class is the analog of an
 * {@link javax.lang.model.element.AnnotationMirror}. But annotated constructs in this package
 * also provide the ability to create an {@link com.bluegosling.apt.reflect.ArAnnotationBridge},
 * which is an implementation of an actual annotation interface with some additional static API
 * for interacting with annotations that contains references to types that are not available during
 * annotation processing (e.g. classes that only available in source form, and cannot be loaded as
 * runtime types). 
 * 
 * <h3>Differences from Core Reflection</h3>
 * As alluded to above, this package models types more like type mirrors. Core reflection has
 * parallel APIs for generic types and annotated types, the former lacking the ability to model type
 * annotations and the latter addressing that. This package, however, has only a single way to model
 * types.
 * 
 * <p>Since types are modeled more like {@link java.lang.reflect.AnnotatedType} than
 * {@link java.lang.reflect.Type}, this package does not re-use class tokens and type variables for
 * modeling both language elements and types. And, like type mirrors, it does not distinguish in the
 * type system between declared types with type parameters and those without.
 * 
 * <p>The table below shows the type in this package that is used to model the same thing that a
 * given core reflection type models:
 * <table border=1 summary="java.lang.reflect vs. com.bluegosling.apt.reflect">
 * <tr><th>Context</th><th>Core Reflection</th><th>This Package</th></tr>
 * <tr><td rowspan=7>Elements</td>
 *     <td>{@link java.lang.Class}</td><td>{@link com.bluegosling.apt.reflect.ArClass}</td></tr>
 * <tr><td>{@link java.lang.reflect.TypeVariable}</td><td>{@link com.bluegosling.apt.reflect.ArTypeParameter}</td></tr>
 * <tr><td>{@link java.lang.reflect.Field}</td><td>{@link com.bluegosling.apt.reflect.ArField}</td></tr>
 * <tr><td>{@link java.lang.reflect.Constructor}</td><td>{@link com.bluegosling.apt.reflect.ArConstructor}</td></tr>
 * <tr><td>{@link java.lang.reflect.Method}</td><td>{@link com.bluegosling.apt.reflect.ArMethod}</td></tr>
 * <tr><td>{@link java.lang.reflect.Parameter}</td><td>{@link com.bluegosling.apt.reflect.ArParameter}</td></tr>
 * <tr><td>{@link java.lang.Package}</td><td>{@link com.bluegosling.apt.reflect.ArPackage}</td></tr>
 * <tr><td rowspan=16>Types</td>
 *     <td colspan=2 align="center"><em>Generic Types</em></td></tr>
 * <tr><td rowspan=3>{@link java.lang.Class}</td><td>{@link com.bluegosling.apt.reflect.ArDeclaredType}&nbsp;*</td></tr>
 * <tr>                                          <td>{@link com.bluegosling.apt.reflect.ArPrimitiveType}&nbsp;†</td></tr>
 * <tr>                                          <td>{@link com.bluegosling.apt.reflect.ArArrayType}&nbsp;‡</td></tr>
 * <tr><td>{@link java.lang.reflect.TypeVariable}</td><td>{@link com.bluegosling.apt.reflect.ArTypeVariable}</td></tr>
 * <tr><td>{@link java.lang.reflect.ParameterizedType}</td><td>{@link com.bluegosling.apt.reflect.ArDeclaredType}&nbsp;*</td></tr>
 * <tr><td>{@link java.lang.reflect.GenericArrayType}</td><td>{@link com.bluegosling.apt.reflect.ArArrayType}&nbsp;‡</td></tr>
 * <tr><td>{@link java.lang.reflect.WildcardType}</td><td>{@link com.bluegosling.apt.reflect.ArTypeVariable}</td></tr>
 * <tr><td colspan=2 align="center"><em>Annotated Types</em></td></tr>
 * <tr><td rowspan=2>{@link java.lang.reflect.AnnotatedType}&nbsp;§</td><td>{@link com.bluegosling.apt.reflect.ArDeclaredType}&nbsp;*</td></tr>
 * <tr>                                                                 <td>{@link com.bluegosling.apt.reflect.ArPrimitiveType}&nbsp;†</td></tr>
 * <tr><td>{@link java.lang.reflect.AnnotatedTypeVariable}</td><td>{@link com.bluegosling.apt.reflect.ArTypeVariable}</td></tr>
 * <tr><td>{@link java.lang.reflect.AnnotatedParameterizedType}</td><td>{@link com.bluegosling.apt.reflect.ArDeclaredType}&nbsp;*</td></tr>
 * <tr><td>{@link java.lang.reflect.AnnotatedArrayType}</td><td>{@link com.bluegosling.apt.reflect.ArArrayType}</td></tr>
 * <tr><td>{@link java.lang.reflect.AnnotatedWildcardType}</td><td>{@link com.bluegosling.apt.reflect.ArWildcardType}</td></tr>
 * </table>
 * <ul style="list-style: none;margin-left: 0;padding-left: 1em;text-indent: -1em">
 * <li><strong>*</strong> This package models all declared types, whether they have parameters or
 * not, using the same {@link com.bluegosling.apt.reflect.ArDeclaredType}. It is a parameterized
 * type if it has {@linkplain com.bluegosling.apt.reflect.ArDeclaredType#getActualTypeArguments()
 * type arguments}.</li>
 * <li><strong>†</strong> This package models declared reference types differently from primitive
 * types, whereas core reflection does not.</li>
 * <li><strong>‡</strong> This package models all array types, whether their component type is
 * generic or not, using the same {@link com.bluegosling.apt.reflect.ArArrayType}. This is similar
 * to how annotated array types are modeled in core reflection.</li>
 * <li><strong>§</strong> In core reflection {@link java.lang.reflect.AnnotatedType} is the root of
 * the annotated type hierarchy. But it also used to represent an annotated type that is
 * neither a type variable nor a parameterized type, nor an array, nor a wildcard. The table above
 * indicates how that latter usage corresponds to this package.</li>
 * </ul>
 * 
 * <h3>Differences from Language Models</h3>
 * In Java's language model APIs, elements that represent types can only represent types defined
 * in source code, thus excluding them for use in representing primitive and array types. But this
 * package's {@link com.bluegosling.apt.reflect.ArClass} can be used for declared type elements and
 * for primitive and array types, much like the core reflection {@link java.lang.Class}.
 * 
 * <p>Unlike type mirrors, this package's {@link com.bluegosling.apt.reflect.ArType} does not have
 * a way to represent intersection or union types nor does it have a way to represent the "none"
 * type (e.g. the super-type of {@code Object}), the {@code null} type (aka "bottom type")
 * packages, or type signatures of methods and constructors. So its representation is more like core
 * reflection in those regards. Furthermore, the {@code void} type is modeled as an
 * {@link com.bluegosling.apt.reflect.ArPrimitiveType}, like
 * {@linkplain java.lang.Class#isPrimitive() core reflection} but unlike type mirrors. 
 * 
 * <p>The table below shows the type in this package that is used to model the same thing that a
 * given language model type models:
 * <table border=1 summary="javax.lang.model vs. com.bluegosling.apt.reflect">
 * <tr><th>Context</th><th>Language Models</th><th>This Package</th></tr>
 * <tr><td rowspan=7>Elements</td>
 *     <td>{@link javax.lang.model.element.TypeElement}</td><td>{@link com.bluegosling.apt.reflect.ArClass}&nbsp;*</td></tr>
 * <tr><td>{@link javax.lang.model.element.TypeParameterElement}</td><td>{@link com.bluegosling.apt.reflect.ArTypeParameter}</td></tr>
 * <tr><td rowspan=2>{@link javax.lang.model.element.VariableElement}&nbsp;†</td><td>{@link com.bluegosling.apt.reflect.ArField}</td></tr>
 * <tr>                                                                   <td>{@link com.bluegosling.apt.reflect.ArParameter}</td></tr>
 * <tr><td rowspan=2>{@link javax.lang.model.element.ExecutableElement}&nbsp;‡</td><td>{@link com.bluegosling.apt.reflect.ArConstructor}</td></tr>
 * <tr>                                                                     <td>{@link com.bluegosling.apt.reflect.ArMethod}</td></tr>
 * <tr><td>{@link javax.lang.model.element.PackageElement}</td><td>{@link com.bluegosling.apt.reflect.ArPackage}</td></tr>
 * <tr><td rowspan=11>Types</td>
 *     <td>{@link javax.lang.model.type.DeclaredType}</td><td>{@link com.bluegosling.apt.reflect.ArDeclaredType}</td></tr>
 * <tr><td>{@link javax.lang.model.type.PrimitiveType}</td><td rowspan=2>{@link com.bluegosling.apt.reflect.ArPrimitiveType}</td></tr>
 * <tr><td>{@link javax.lang.model.type.NoType}&nbsp;§</td>
 * <tr><td>{@link javax.lang.model.type.ArrayType}</td><td>{@link com.bluegosling.apt.reflect.ArArrayType}</td></tr>
 * <tr><td>{@link javax.lang.model.type.WildcardType}</td><td>{@link com.bluegosling.apt.reflect.ArWildcardType}</td></tr>
 * <tr><td>{@link javax.lang.model.type.TypeVariable}</td><td>{@link com.bluegosling.apt.reflect.ArTypeVariable}</td></tr>
 * <tr><td>{@link javax.lang.model.type.ErrorType}&nbsp;‖</td><td><em>None</em></td></tr>
 * <tr><td>{@link javax.lang.model.type.ExecutableType}&nbsp;‖</td><td><em>None</em></td></tr>
 * <tr><td>{@link javax.lang.model.type.IntersectionType}&nbsp;‖</td><td><em>None</em></td></tr>
 * <tr><td>{@link javax.lang.model.type.NullType}&nbsp;‖</td><td><em>None</em></td></tr>
 * <tr><td>{@link javax.lang.model.type.UnionType}&nbsp;‖</td><td><em>None</em></td></tr>
 * </table>
 * <ul style="list-style: none;margin-left: 0;padding-left: 1em;text-indent: -1em">
 * <li><strong>*</strong> As already stated, {@link com.bluegosling.apt.reflect.ArClass} can
 * represent primitive and array types, too. Such instances will have no corresponding
 * {@link javax.lang.model.element.TypeElement}.</li>
 * <li><strong>†</strong> Variable elements that are 
 * {@linkplain javax.lang.model.element.ElementKind#EXCEPTION_PARAMETER exception parameters},
 * {@linkplain javax.lang.model.element.ElementKind#LOCAL_VARIABLE local variables}, or
 * {@linkplain javax.lang.model.element.ElementKind#RESOURCE_VARIABLE try-with-resources variables}
 * are not modeled with types in this package. Variable elements that are
 * {@linkplain javax.lang.model.element.ElementKind#ENUM_CONSTANT enum constants} or
 * {@linkplain javax.lang.model.element.ElementKind#FIELD fields} are both modeled with
 * {@link com.bluegosling.apt.reflect.ArField}.</li>
 * <li><strong>‡</strong> Executable elements that are
 * {@linkplain javax.lang.model.element.ElementKind#INSTANCE_INIT instance initialization blocks} or
 * {@linkplain javax.lang.model.element.ElementKind#STATIC_INIT static initializers} are not modeled
 * with types in this package.</li>
 * <li><strong>§</strong> Type mirrors that represent
 * {@linkplain javax.lang.model.type.TypeKind#NONE the "none" type} or
 * {@linkplain javax.lang.model.type.TypeKind#PACKAGE packages} are not modeled with types in this
 * package. So the only {@link javax.lang.model.type.NoType} supported is
 * {@linkplain javax.lang.model.type.TypeKind#VOID void}, which is considered a primitive type by
 * this package. The "none" type is generally represented by a {@code null} instance in this
 * package. Packages are treated like other unsupported kinds of type mirrors (see more below).</li>
 * <li><strong>‖</strong> Like core reflection, various specialized kinds of type mirrors are not
 * representable by this package. Attempts to wrap such a type mirror as an instance of
 * {@link com.bluegosling.apt.reflect.ArType} will result in a
 * {@link javax.lang.model.type.MirroredTypeException} being thrown.
 * </ul>
 * 
 * <p><strong>NOTE:</strong> APIs in this package requires the executing thread to be {@linkplain
 * com.bluegosling.apt.ProcessingEnvironments#setup(javax.annotation.processing.ProcessingEnvironment)
 * setup for the current processing environment}. If you are running from the same thread on which
 * the processor was invoked and the processor extends {@link com.bluegosling.apt.AbstractProcessor}
 * then this will have already been done for you.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.bluegosling.apt.reflect;
