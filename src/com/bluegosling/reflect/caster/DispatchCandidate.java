package com.bluegosling.reflect.caster;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * A single dispatch candidate. A dispatch candidate represents a single method on the cast
 * object and includes sufficient information to actually invoke the method, given the set of
 * arguments passed to the target interface method.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Caster
 */
// TODO: javadoc members
class DispatchCandidate {
   private final Method method;
   private final int numArgs;
   private final int phase;
   private final int numCastArgs;
   private final Converter<?, ?> argConverters[];
   private final Function<Object[], Object[]> varArgsConverter;
   private final Converter<?, ?> returnConverter;
   
   DispatchCandidate(Method method, boolean autobox, boolean varArgs, int numCastArgs,
         Converter<?, ?> argConverters[], Function<Object[], Object[]> varArgsConverter,
         Converter<?, ?> returnConverter) {
      this.method = method;
      this.numArgs = method.getParameterTypes().length;
      this.phase = (autobox ? 1 : 0) + (varArgs ? 2 : 0);
      this.numCastArgs = numCastArgs;
      this.argConverters = argConverters;
      this.varArgsConverter = varArgsConverter;
      this.returnConverter = returnConverter;
   }
   
   public Method getMethod() {
      return method;
   }
   
   public int getNumArgs() {
      return numArgs;
   }
   
   public int getPhase() {
      return phase;
   }
   
   public int getNumCastArgs() {
      return numCastArgs;
   }
   
   // unchecked casts for converting args and return value should be safe since types should have
   // all been checked during creation of converters prior to creating this dispatch candidate
   @SuppressWarnings("unchecked") 
   public Object invoke(Caster<?> caster, Object obj, Object args[]) throws Throwable {
      try {
         if (argConverters != null) {
            for (int i = 0, len = args.length; i < len; i++) {
               @SuppressWarnings("rawtypes") // so we can pass Object as input to conversion
               Converter converter = argConverters[i];
               if (converter != null) {
                  args[i] = converter.convert(args[i], caster);
               }
            }
         }
         if (varArgsConverter != null) {
            args = varArgsConverter.apply(args);
         }
         Object ret = method.invoke(obj, args);
         if (returnConverter != null) {
            @SuppressWarnings("rawtypes") // so we can pass Object as input to conversion
            Converter converter = returnConverter;
            ret = converter.convert(ret, caster);
         }
         return ret;
      } catch (InvocationTargetException e) {
         throw e.getCause();
      }
   }

   /**
    * Creates a {@link DispatchCandidate} that represents the specified candidate method as a
    * possible dispatch method for the specified target interface method.
    * 
    * @param candidateMethod the candidate method
    * @param targetMethod the target interface method
    * @param argTypes the argument types for the target interface method (could represent actual
    *       runtime argument types instead of method's declared types)
    * @param settings flags that control various dispatch options
    * @param varArgsExpanded if true then the argument types specified represent runtime types with
    *       the contents of a var-args array expanded into multiple arguments
    * @return the candidate or {@code null} if the specified candidate method is not suitable
    */
   // TODO: this method is a ugly behemoth! need to simplify or at least break it up
   static DispatchCandidate create(Method candidateMethod, Method targetMethod, Class<?> argTypes[],
         DispatchSettings settings, boolean varArgsExpanded) {
      Class<?> candidateTypes[] = candidateMethod.getParameterTypes();
      if (candidateTypes.length != argTypes.length &&
            (!candidateMethod.isVarArgs() || candidateTypes.length > argTypes.length + 1)) {
         // bad number of args? no candidate
         return null;
      } else {
         boolean varArgs = candidateTypes.length != argTypes.length;
         boolean autobox = false;
         int numCastArgs = 0;
         Converter<?, ?> argConverters[] = new Converter<?, ?>[argTypes.length];
         boolean anyArgNeedsConversion = false;
         int len = varArgs ? candidateTypes.length - 1 : candidateTypes.length;
         int i;
         for (i = 0; i < len; i++) {
            ConversionStrategy<?, ?> strategy =
                  ConversionStrategy.getConversionStrategy(argTypes[i], candidateTypes[i],
                        settings.isCastingArguments());
            if (strategy != null) {
               if (strategy.doesRequireAutoBoxOrUnbox()) {
                  autobox = true;
               }
               if (strategy.doesRequireCast()) {
                  numCastArgs++;
               }
               Converter<?, ?> argConverter = strategy.getConverter(); 
               if (argConverter != null) {
                  anyArgNeedsConversion = true;
                  argConverters[i] = argConverter; 
               }
            } else if (i == candidateTypes.length - 1 && candidateMethod.isVarArgs()) {
               // we'll check if argument is compatible w/ var arg element type below
               varArgs = true;
               break;
            } else {
               // incompatible!
               return null;
            }
         }
         Function<Object[], Object[]> varArgsConverter = null;
         boolean appendIncomingVarArgs = false;
         if (varArgs) {
            // compare to component type of var args array
            Class<?> candidateType = candidateTypes[i].getComponentType();
            for (; i < argTypes.length; i++) {
               ConversionStrategy<?, ?> strategy = ConversionStrategy.getConversionStrategy(argTypes[i], candidateType,
                     settings.isCastingArguments());
               if ((strategy == null || strategy.doesRequireCast()) && i == argTypes.length - 1
                     && targetMethod.isVarArgs() && !varArgsExpanded) {
                  // prefer repackaging var-args (if possible) over casting
                  Class<?> argType = argTypes[i].getComponentType();
                  ConversionStrategy<?, ?> varArgStrategy = ConversionStrategy.getConversionStrategy(argType, candidateType,
                        settings.isCastingArguments());
                  if (varArgStrategy != null) {
                     varArgStrategy = varArgStrategy.forArray();
                  }
                  if (varArgStrategy != null) {
                     appendIncomingVarArgs = true;
                     strategy = varArgStrategy;
                  }
               }
               if (strategy == null) {
                  // incompatible!
                  return null;
               }
               if (strategy.doesRequireAutoBoxOrUnbox()) {
                  autobox = true;
               }
               if (strategy.doesRequireCast()) {
                  numCastArgs++;
               }
               @SuppressWarnings("rawtypes") // so we can pass Object as input
               Converter argConverter = strategy.getConverter(); 
               if (argConverter != null) {
                  anyArgNeedsConversion = true;
                  argConverters[i] = argConverter; 
               }
            }
            final int candidateArgsLen = candidateTypes.length;
            final Class<?> varArgType = candidateTypes[candidateArgsLen - 1].getComponentType();
            final boolean doAppendVarArgs = appendIncomingVarArgs;
            varArgsConverter = new Function<Object[], Object[]>() {
               // we are sufficiently guarding access by checking doAppendVarArgs, but compiler
               // can't tell that this prevents null pointer de-reference
               @SuppressWarnings("null")
               @Override
               public Object[] apply(Object[] input) {
                  if (input == null) {
                     input = new Object[0];
                  }
                  Object ret[] = new Object[candidateArgsLen];
                  if (candidateArgsLen > 1) {
                     System.arraycopy(input, 0, ret, 0, candidateArgsLen - 1);
                  }
                  int varArgLen = input.length - candidateArgsLen + 1;
                  Object incomingVarArgs[] = doAppendVarArgs ? (Object[]) input[input.length - 1]
                        : null;
                  Object varArgValues[];
                  if (doAppendVarArgs) {
                     varArgLen--; // skip the last arg, which is the incoming var arg array
                     varArgValues = (Object[]) Array.newInstance(varArgType, varArgLen +
                           incomingVarArgs.length);
                  } else {
                     varArgValues = (Object[]) Array.newInstance(varArgType, varArgLen);
                  }
                  System.arraycopy(input, candidateArgsLen - 1, varArgValues, 0, varArgLen);
                  if (doAppendVarArgs) {
                     System.arraycopy(incomingVarArgs, 0, varArgValues, varArgLen,
                           incomingVarArgs.length);
                  }
                  ret[ret.length - 1] = varArgValues;
                  return ret;
               }
            };
         }
         // args are good, so now check return type
         ConversionStrategy<?, ?> strategy = ConversionStrategy.getConversionStrategy(candidateMethod.getReturnType(),
               targetMethod.getReturnType(), settings.isCastingReturnTypes());
         if (strategy == null) {
            // incompatible return type!
            return null;
         }
         if (strategy.doesRequireAutoBoxOrUnbox()) {
            autobox = true;
         }
         if (strategy.doesRequireCast()) {
            numCastArgs++;
         }
         @SuppressWarnings("rawtypes") // so we can pass Object as input
         Converter returnConverter = strategy.getConverter();
         // got a good candidate! make sure we can invoke it
         candidateMethod.setAccessible(true);
         return new DispatchCandidate(candidateMethod, autobox, varArgs, numCastArgs,
               anyArgNeedsConversion ? argConverters : null, varArgsConverter, returnConverter);
      }
   }
}
