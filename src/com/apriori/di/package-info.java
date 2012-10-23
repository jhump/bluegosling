/**
 * An API for dependency injection that relies on static bindings and code generation
 * instead of dynamic bindings and runtime reflection. A minimal level of dynamic
 * binding is supported through the notion of "selectors".
 * 
 * <p>Code that uses the DI marks "entry points" types using annotations. The code
 * generation step creates concrete sub-classes of those entry points with abstract
 * methods implemented using the defined bindings. The entry point methods can be
 * used to create factory methods that use DI bindings to generate objects, to create
 * wrappers around other methods with fewer parameters (where remaining parameters
 * are automatically bound using DI bindings), and to create a {@code main} method
 * that can accept arguments other than the typical {@code String[] args} (those
 * arguments are bound automatically using DI bindings and then passed to the
 * method). These are the various ways that this static DI can be used to create
 * object graphs on behalf of the application.
 * 
 * <p>Two notions are used to create the bindings for this API:
 * <ol>
 * <li><strong>Binding</strong>: A binding is an explicit link from one type (an interface or
 * abstract class) to another. This makes it easy to define the implementation
 * types to use when injecting instances of an interface or abstract class.</li>
 * <li><strong>Provision</strong>: A provision is a method that allows more control over how
 * injected objects are instantiated. The return value of a method is the injected
 * instance and the parameters to the provision method are its dependencies.</li>
 * </ol>
 * 
 * <p>The API looks unsurprisingly similar to that of Google's
 * <a href="http://code.google.com/p/google-guice/">Guice</a>. However, due to the goal
 * of static DI with code generation, it lacks some of the dynamic features (particularly
 * AOP/method interceptors) that Guice provides. This API uses and depends on the
 * {@code javax.inject} package (mostly annotations) defined by JSR-330.
 * 
 * <p>Since the API uses bindings known at compile-time to generate code, it should be
 * able to generate code that results in far faster start-up times than dynamic systems
 * (like Guice) which require executing the code that configures the modules and then
 * performing runtime reflection to verify dependencies and bindings, all at runtime.
 * Furthermore, bound instance creation uses constructor reflection, so that is another
 * runtime performance penalty for dynamic systems.
 * 
 * <p><strong>NOTE:</strong> This package is still under construction. Nothing is really
 * implemented yet. This is just a stab at public API and some positing on how it might
 * work and how it could be used.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.di;
