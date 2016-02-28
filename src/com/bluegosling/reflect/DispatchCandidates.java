package com.bluegosling.reflect;


import com.bluegosling.reflect.DispatchSettings.Visibility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A set of eligible dispatch candidates. This is a set of methods that could be used for method
 * dispatch. A single candidate can be selected from the set based on dispatch argument types.
 * 
 * <p>Internally, methods are stored as a two-dimensional collection of methods (a list of lists as
 * it were). Each "top-level" list is a group of "related" methods. Two methods are "related" when
 * one is more or less specific than the other. Methods in one group are orthogonal to those in a
 * different group such that a method in one group is neither more nor less specific than one in a
 * different group. Methods in a group are sorted so that iteration over a group yields the most
 * specific method first and the least specific method last.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Caster
 */
class DispatchCandidates {
   private final Collection<List<Method>> methods;
   
   private DispatchCandidates(Collection<List<Method>> methods) {
      this.methods = methods;
   }
   
   static class Builder {
      private final Collection<List<Method>> methods = new ArrayList<List<Method>>();
      
      /**
       * Adds a method to the set of candidates.
       * 
       * @param m a method
       */
      public void add(Method m) {
         for (List<Method> category : methods) {
            Method other = category.get(0);
            if (Members.isMoreSpecific(m, other) || Members.isMoreSpecific(other,  m)) {
               category.add(m);
            }
         }
         List<Method> category = new ArrayList<Method>();
         methods.add(category);
         category.add(m);
      }
      
      /**
       * Builds a candidate set with the methods added so far.
       * 
       * @return a new set of dispatch candidates
       */
      @SuppressWarnings("synthetic-access")
      public DispatchCandidates build() {
         return new DispatchCandidates(organize());
      }
      
      /**
       * Returns an organized copy of the methods. In the returned copy, each group of related
       * methods will be sorted with the most specific method first.
       */
      private Collection<List<Method>> organize() {
         // deep copy and sort each group as we go
         Collection<List<Method>> copy = new ArrayList<List<Method>>(methods.size());
         for (List<Method> group : methods) {
            List<Method> groupCopy = new ArrayList<Method>(group);
            Collections.sort(groupCopy, new Comparator<Method>() {
               @Override
               public int compare(Method o1, Method o2) {
                  if (Members.isMoreSpecific(o1, o2)) {
                     return -1;
                  } else if (Members.isMoreSpecific(o2, o1)) {
                     return 1;
                  } else {
                     return 0;
                  }
               }
            });
            copy.add(groupCopy);
         }
         return copy;
      }
   }
   
   /**
    * Determines whether the matrix has any methods or not.
    * 
    * @return true if there are no methods
    */
   public boolean isEmpty() {
      return methods.isEmpty();
   }
   
   /**
    * Requires that the specified collection has one suitable candidate and returns that one
    * candidate. If the collection has more than one and is ignoring ambiguities then one of the
    * candidates is chosen arbitrarily and returned.
    * 
    * @param m the interface method
    * @param candidates the set of dispatch candidates
    * @param ignoreAmbiguities if false only one candidate is allowed and an exception will be
    *       thrown if the specified collection has more than one
    * @throws NoSuchMethodException if the specified collection is empty
    * @throws AmbiguousMethodException if the specified collection has more than one candidate and
    *       {@code ignoreAmbiguities} is false
    */
   static DispatchCandidate requireOneCandidate(Method m, Collection<DispatchCandidate> candidates,
         boolean ignoreAmbiguities)
         throws NoSuchMethodException, AmbiguousMethodException {
      if (candidates.isEmpty()) {
         throw new NoSuchMethodException(m.toString());
      } else if (candidates.size() > 1 && !ignoreAmbiguities) {
         Collection<Method> methods = new ArrayList<Method>(candidates.size());
         for (DispatchCandidate candidate : candidates) {
            methods.add(candidate.getMethod());
         }
         throw new AmbiguousMethodException(m, methods);
      }
      return candidates.iterator().next();
   }

   /**
    * Filters the matrix of methods to only the one(s) that best suit the specified method and
    * argument types. If there are multiple "maximally specific" candidates methods that are suited
    * then the returned collection will include all such candidates.
    * 
    * @param m the interface method that will be dispatched
    * @param argTypes the argument types for the interface method, which could be actual runtime
    *       types (during dynamic dispatch) instead of the method's declared argument types
    * @param settings flags that control various dispatch options
    * @param varArgsExpanded if true then a var-arg array has already been expanded and appended
    *       to the specified array of argument types
    * @return the best candidate(s) from the specified set or an empty list if there are no
    *       suitable candidates
    */
   private Collection<DispatchCandidate> getBestCandidates(Method m, Class<?> argTypes[],
         DispatchSettings settings, boolean varArgsExpanded) {
      List<DispatchCandidate> bestMatches = new ArrayList<DispatchCandidate>();
      for (Iterable<Method> methodGroup : this.methods) {
         DispatchCandidate currentCandidate = null;
         for (Method method : methodGroup) {
            DispatchCandidate maybe = DispatchCandidate.create(method, m, argTypes, settings,
                  varArgsExpanded);
            if (maybe != null) {
               if (currentCandidate == null
                     || currentCandidate.getNumArgs() < maybe.getNumArgs()
                     || (currentCandidate.getNumArgs() == maybe.getNumArgs()
                           && currentCandidate.getNumCastArgs() > maybe.getNumCastArgs())
                     || (currentCandidate.getNumArgs() == maybe.getNumArgs()
                           && currentCandidate.getNumCastArgs() == maybe.getNumCastArgs()
                           && currentCandidate.getPhase() > maybe.getPhase())) {
                  currentCandidate = maybe;
                  if (currentCandidate.getPhase() == 0 && currentCandidate.getNumCastArgs() == 0) {
                     // best we'll find in this category, so don't bother looking at the others
                     break;
                  }
               }
            }
         }
         if (currentCandidate != null) {
            if (bestMatches.isEmpty()) {
               bestMatches.add(currentCandidate);
            } else {
               DispatchCandidate bestSoFar = bestMatches.get(0);
               if (bestSoFar.getNumCastArgs() > currentCandidate.getNumCastArgs()) {
                  bestMatches.clear(); // replace current best with even better
                  bestMatches.add(currentCandidate);
               } else if (bestSoFar.getNumCastArgs() == currentCandidate.getNumCastArgs()) {
                  if (currentCandidate.getPhase() < bestSoFar.getPhase()) {
                     bestMatches.clear(); // replace current best with even better
                     bestMatches.add(currentCandidate);
                  } else if (currentCandidate.getPhase() == bestSoFar.getPhase()) {
                     // add another match
                     bestMatches.add(currentCandidate);
                  }
               }
            }
         }
      }
      return bestMatches;
   }
   
   // TODO: doc
   public DispatchCandidate getBestCandidate(Method m, Class<?> argTypes[],
         DispatchSettings settings) throws NoSuchMethodException, AmbiguousMethodException {
      return requireOneCandidate(m, 
            getBestCandidates(m, argTypes, settings, false),
            settings.isIgnoringAmbiguities());
   }

   /**
    * Performs a dynamic dispatch for a given object and interface method. This will use the runtime
    * types of the method arguments (instead of the statically declared argument types) to find the
    * most appropriate method in this matrix to dispatch.
    * 
    * @param caster the caster that created the proxy that is performing dynamic dispatch
    * @param o the object on which dispatch will occur
    * @param m the interface method that is being invoked
    * @param args the arguments supplied to the interface method
    * @return the result of performing dynamic dispatch, which is the value returned from the
    *       dispatch candidate that best fits the runtime types of the supplied arguments
    * @throws NoSuchMethodException if there are no suitable dispatch candidates
    * @throws AmbiguousMethodException if there are multiple equally specific dispatch candidates
    *       and {@code caster.ignoreAmbiguities} is false
    */
   public Object invokeDynamic(Caster<?> caster, Object o, Method m, Object args[])
         throws Throwable {
      Class<?> argTypes[] = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
         argTypes[i] = args[i].getClass();
      }
      DispatchSettings settings = caster.getDispatchSettings();
      Collection<DispatchCandidate> bestCandidates = getBestCandidates(m, argTypes, settings, false);
      if (bestCandidates.isEmpty() && m.isVarArgs() && settings.isExpandingVarArgs()
            && args[args.length-1] != null) {
         // expand var args array and re-try finding a dispatch method
         int nonVarArgLen = args.length - 1;
         Object varArgs[] = (Object[]) args[nonVarArgLen];
         Object newArgs[] = new Object[nonVarArgLen + varArgs.length];
         System.arraycopy(args, 0, newArgs, 0, nonVarArgLen);
         System.arraycopy(varArgs, 0, newArgs, nonVarArgLen, varArgs.length);
         Class<?> newArgTypes[] = new Class<?>[newArgs.length];
         System.arraycopy(argTypes, 0, newArgTypes, 0, nonVarArgLen);
         for (int i = 0; i < varArgs.length; i++) {
            newArgTypes[nonVarArgLen + i] = varArgs[i].getClass();
         }
         // swap
         args = newArgs;
         argTypes = newArgTypes;
         bestCandidates = getBestCandidates(m, argTypes, settings, true);
      }
      DispatchCandidate dispatch;
      try {
         dispatch = requireOneCandidate(m, bestCandidates, settings.isIgnoringAmbiguities());
      }
      catch (NoSuchMethodException e) {
         throw new UnsupportedOperationException(m.getName(), e);
      }
      catch (AmbiguousMethodException e) {
         throw new UnsupportedOperationException(m.getName(), e);
      }
      return dispatch.invoke(caster, o, args);
   }

   /**
    * Selects a broad range of candidates for dispatching the specified method. This set of
    * candidates is the starting point for dynamic dispatches. The resulting set can be further
    * filtered during proxy creation to generate suitable method dispatches that are not dynamic.
    * 
    * <p>This set of candidates is based solely on method name and number of arguments.
    * 
    * @param method the method that is being dispatched
    * @param dispatchClass the object to which the method is being delegated (the resulting
    *       candidates come from this class's set of public methods)
    * @param expandVarArgs if true then additional candidates will be returned that might be
    *       compatible for dynamic dispatch in the event that var-arg arrays are expanded
    * @param visibility the minimum allowed visibility for candidates
    * @return the set of potentially suitable candidates
    */
   public static DispatchCandidates getEligibleCandidates(Method method, Class<?> dispatchClass,
         boolean expandVarArgs, Visibility visibility) {
      DispatchCandidates.Builder methods = new DispatchCandidates.Builder();
      int argsLen = method.getParameterTypes().length;
      String name = method.getName();
      for (Method m : Members.findMethods(dispatchClass, name)) {
         if (m.getName().equals(name) && visibility.isVisible(m.getModifiers())) {
            int otherLen = m.getParameterTypes().length;
            if (otherLen == argsLen || (m.isVarArgs() && otherLen <= argsLen + 1)
                  || (expandVarArgs && method.isVarArgs() && argsLen <= otherLen + 1)) {
               methods.add(m);
            }
         }
      }
      return methods.build();
   }
   
   // TODO: doc!
   static DispatchCandidate getBestCandidate(Method method, Class<?> dispatchClass,
         DispatchSettings settings)
         throws NoSuchMethodException, AmbiguousMethodException {
      return getEligibleCandidates(method, dispatchClass, false, settings.visibility())
            .getBestCandidate(method, method.getParameterTypes(), settings);
   }
}
