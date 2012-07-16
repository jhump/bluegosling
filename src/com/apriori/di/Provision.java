package com.apriori.di;

/**
 * A marker interface for interfaces and abstract classes that used to provide
 * injected values. Each public non-void concrete method in sub-classes will be treated like
 * {@code @Provides} methods in a Guice module. Abstract methods can be used to define
 * "hooks" that will be implemented by subclasses. If an {@linkplain InjectedEntryPoint
 * entry point} has a provision that is an interface or is abstract, a concrete class will
 * be generated and implementations of abstract methods will be also be generated using
 * the other bindings and provisions present in the transitive closure of all bindings and
 * provisions for the injected entry point.
 * 
 * <p>A {@link Provision} can also be injected. If one was generated for an entry point,
 * then an instance of the generated class is what will actually be injected.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Provision {}