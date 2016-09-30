package com.bluegosling.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * Iterates through prime numbers. The sequence enumerates all primes, starting with two. The
 * sequence is complete when the maximum possible range is exhausted. For example, an iterator of
 * prime {@code byte} values stops after the largest prime that is less than or equal to 127 (which
 * is the maximum value that can be represented by a byte). Since {@link BigInteger} has no maximum
 * value, the {@link #hasNext()} method of an iterator of prime {@link BigInteger}s will never
 * return false.
 * 
 * <p>This is implemented using a <a href="http://en.wikipedia.org/wiki/Sieve_of_Eratosthenes">Sieve
 * of Eratosthenes</a>. This implementation is an incremental sieve, so it has no fixed upper
 * bound other than being bound by heap space. Generating the {@code n}<sup>th</sup> prime requires
 * <em>O(n)</em> memory, so a lot of memory can be required for very large {@code n}.
 * 
 * <p>The phrase "current numeric type" in the rest of this documentation refers to the concrete
 * type for type argument {@code T}. For example, with a {@code PrimeIterator<Long>}, the "current
 * numeric type" would be {@code long}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of numeric value generated
 */
public abstract class PrimeIterator<T extends Number & Comparable<T>> implements Iterator<T> {

   /**
    * Constructs an iterator of {@code byte}s that are prime.
    * 
    * @return an iterator of prime {@code byte}s
    */
   public static PrimeIterator<Byte> primeByteIterator() {
      return new PrimeIterator<Byte>() {
         @Override
         protected Byte two() {
            return 2;
         }

         @Override
         protected Byte inc(Byte t) {
            return t == Byte.MAX_VALUE ? null : (byte) (t + 1);
         }
         
         @Override
         protected Byte add(Byte t1, Byte t2) {
            if ((byte) (Byte.MAX_VALUE - t2) < t1) {
               return null;
            }
            return (byte) (t1 + t2);
         }
      };
   }
   
   /**
    * Constructs an iterator of {@code short}s that are prime.
    * 
    * @return an iterator of prime {@code short}s
    */
   public static PrimeIterator<Short> primeShortIterator() {
      return new PrimeIterator<Short>() {
         @Override
         protected Short two() {
            return 2;
         }

         @Override
         protected Short inc(Short t) {
            return t == Short.MAX_VALUE ? null : (short) (t + 1);
         }
         
         @Override
         protected Short add(Short t1, Short t2) {
            if ((short) (Short.MAX_VALUE - t2) < t1) {
               return null;
            }
            return (short) (t1 + t2);
         }
      };
   }

   /**
    * Constructs an iterator of {@code int}s that are prime. There are enough primes in this range
    * that the heap could be exhausted before enumerating all of them.
    * 
    * @return an iterator of prime {@code int}s
    */
   public static PrimeIterator<Integer> primeIntegerIterator() {
      return new PrimeIterator<Integer>() {
         @Override
         protected Integer two() {
            return 2;
         }

         @Override
         protected Integer inc(Integer t) {
            return t == Integer.MAX_VALUE ? null : t + 1;
         }
         
         @Override
         protected Integer add(Integer t1, Integer t2) {
            if (Integer.MAX_VALUE - t2 < t1) {
               return null;
            }
            return t1 + t2;
         }
      };
   }
   
   /**
    * Constructs an iterator of {@code long}s that are prime. There are enough primes in this range
    * that the heap will likely be exhausted before enumerating all of them.
    * 
    * @return an iterator of prime {@code long}s
    */
   public static PrimeIterator<Long> primeLongIterator() {
      return new PrimeIterator<Long>() {
         @Override
         protected Long two() {
            return 2L;
         }

         @Override
         protected Long inc(Long t) {
            return t == Long.MAX_VALUE ? null : t + 1;
         }
         
         @Override
         protected Long add(Long t1, Long t2) {
            if (Long.MAX_VALUE - t2 < t1) {
               return null;
            }
            return t1 + t2;
         }
      };
   }

   /**
    * The maximum integral value that can be perfectly expressed as a 32-bit float. Values greater
    * than this get truncated (and shifted via exponent) so can no longer be incremented by 1.0.
    * 
    * <p>32-bit floats have 24 mantissa bits. Not coincidentally, the largest integral value is
    * 2<sup>24</sup>.
    */
   static float MAX_FLOAT = 16_777_216f;

   /**
    * Constructs an iterator of {@code float}s that are prime. This will not yield any primes that
    * are larger than 2<sup>24</sup>. That is the size of the mantissa in a 32-bit float and thus
    * also the largest integral value that can be perfectly represented (beyond that, large
    * integers may lose precision). There are enough primes in this range that the heap could be
    * exhausted before enumerating all of them.
    * 
    * @return an iterator of prime {@code float}s
    */
   public static PrimeIterator<Float> primeFloatIterator() {
      return new PrimeIterator<Float>() {
         @Override
         protected Float two() {
            return 2f;
         }

         @Override
         protected Float inc(Float t) {
            return t == MAX_FLOAT ? null : t + 1f;
         }
         
         @Override
         protected Float add(Float t1, Float t2) {
            if (MAX_FLOAT - t2 < t1) {
               return null;
            }
            return t1 + t2;
         }
      };
   }
   
   /**
    * The maximum integral value that can be perfectly expressed as a 64-bit double. Values greater
    * than this get truncated (and shifted via exponent) so can no longer be incremented by 1.0.
    * 
    * <p>64-bit floats have 53 mantissa bits. Not coincidentally, the largest integral value is
    * 2<sup>53</sup>.
    */
   static double MAX_DOUBLE = 9_007_199_254_740_992.0;

   /**
    * Constructs an iterator of {@code double}s that are prime. This will not yield any primes that
    * are larger than 2<sup>53</sup>. That is the size of the mantissa in a 64-bit float and thus
    * also the largest integral value that can be perfectly represented (beyond that, large
    * integers may lose precision). There are enough primes in this range that the heap will likely
    * be exhausted before enumerating all of them.
    * 
    * @return an iterator of prime {@code double}s
    */
   public static PrimeIterator<Double> primeDoubleIterator() {
      return new PrimeIterator<Double>() {
         @Override
         protected Double two() {
            return 2.0;
         }

         @Override
         protected Double inc(Double t) {
            return t == MAX_DOUBLE ? null : t + 1.0;
         }
         
         @Override
         protected Double add(Double t1, Double t2) {
            if (MAX_DOUBLE - t2 < t1) {
               return null;
            }
            return t1 + t2;
         }
      };
   }

   /**
    * The {@link BigInteger} representation of the number 2.
    */
   static final BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
   
   /**
    * Constructs an iterator of {@link BigInteger}s that are prime. Since this class allows for
    * arbitrary precision and word length, this will generate primes until the heap is exhausted.
    * In other words, {@link #hasNext()} will never return {@code false}.
    * 
    * @return an iterator of prime {@link BigInteger}s
    */
   public static PrimeIterator<BigInteger> primeBigIntegerGenerator() {
      return new PrimeIterator<BigInteger>() {
         
         @Override
         protected BigInteger two() {
            return TWO;
         }

         @Override
         protected BigInteger inc(BigInteger t) {
            return t.add(BigInteger.ONE);
         }
         
         @Override
         protected BigInteger add(BigInteger t1, BigInteger t2) {
            return t1.add(t2);
         }
      };
   }

   /**
    * The {@link BigDecimal} representation of the number 2.
    */
   static final BigDecimal TWO_DECIMAL = new BigDecimal(TWO);
   
   /**
    * Constructs an iterator of {@link BigDecimal}s that are prime. Since this class allows for
    * arbitrary precision and word length, this will generate primes until the heap is exhausted.
    * In other words, {@link #hasNext()} will never return {@code false}.
    * 
    * @return an iterator of prime {@link BigDecimal}s
    */
   public static PrimeIterator<BigDecimal> primeBigDecimalGenerator() {
      return new PrimeIterator<BigDecimal>() {
         
         @Override
         protected BigDecimal two() {
            return TWO_DECIMAL;
         }

         @Override
         protected BigDecimal inc(BigDecimal t) {
            return t.add(BigDecimal.ONE);
         }
         
         @Override
         protected BigDecimal add(BigDecimal t1, BigDecimal t2) {
            return t1.add(t2);
         }
      };
   }

   /**
    * Represents a single element in the sieve. This is a previously-generated prime number and its
    * next multiple, which is the smallest multiple that is greater than all already-generated
    * primes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of numeric value 
    */
   private static class PrimeFilter<T extends Comparable<T>> implements Comparable<PrimeFilter<T>> {
      final T prime;
      T nextMultiple;
      
      PrimeFilter(T prime, T nextMultiple) {
         this.prime = prime;
         this.nextMultiple = nextMultiple;
      }

      @Override
      public int compareTo(PrimeFilter<T> o) {
         return nextMultiple.compareTo(o.nextMultiple);
      }
   }
   
   /**
    * The prime filters. A given candidate value is not prime if it is equal to the next multiple
    * of any of these filters. A priority queue is used since we generate monotonically increasing
    * candidates. At any given time, the filters in the queue represent the next multiples (of known
    * prime numbers) that are greater than the previously generated prime.
    */
   private final PriorityQueue<PrimeFilter<T>> queue;
   
   /**
    * The next prime number in the sequence. This value is eagerly generated. It is set to zero when
    * no more primes can be generated for the current numeric type.
    */
   private T next;
   
   PrimeIterator() {
      queue = new PriorityQueue<PrimeFilter<T>>();
      next = two();
   }

   /**
    * Returns the current numeric type's representation of the number two.
    * 
    * @return the number two
    */
   protected abstract T two();
   
   /**
    * Returns the specified value, incremented by one. If the specified value is already the
    * greatest representable value (e.g. incrementing would overflow) then {@code null} is returned.
    * 
    * @param t the value
    * @return the specified value plus one or {@code null} on overflow
    */
   protected abstract T inc(T t);
   
   /**
    * Adds two values and returns their sum. If their sum would overflow the current numeric type
    * (for example, a sum greater than <code>2<sup>32</sup> - 1</code> with 32-bit signed integers)
    * then {@code null} is returned.
    * 
    * @param t1 the first addend
    * @param t2 the second addend
    * @return the sum of the two addends or {@code null} on overflow
    */
   protected abstract T add(T t1, T t2);

   /**
    * Returns {@code true} when another prime can be generated for the current numeric type.
    * 
    * @return true when another prime can be generated
    */
   @Override public boolean hasNext() {
      return next != null;
   }

   /**
    * Returns the next generated prime.
    * 
    * @return the next generated prime
    * @throws NoSuchElementException if no more primes can be generated for the current numeric type
    */
   @Override public T next() {
      if (next == null) {
         throw new NoSuchElementException();
      }
      T result = next;
      T nextMultiple = add(result, result);
      if (nextMultiple != null) {
         queue.add(new PrimeFilter<T>(result, nextMultiple));
      }
      // Recompute next. Assume next prime is previous prime + 1. As that assumption is shown false
      // by comparing to prime filters, increment and try again.
      next = inc(next);
      while (next != null) {
         PrimeFilter<T> filter = queue.peek();
         int c = filter.nextMultiple.compareTo(next);
         if (c > 0) {
            break;
         } else {
            // this filter is below the threshold, so remove it and replace with its next multiple
            queue.remove();
            filter.nextMultiple = add(filter.prime, filter.nextMultiple);
            if (filter.nextMultiple != null) {
               queue.add(filter);
            }
            if (c == 0) {
               // This means our candidate == filter, which indicates that it is non-prime.
               // So increment and continue testing.
               next = inc(next);
            }
         }
      }
      return result;
   }

   /**
    * Optional operation not supported.
    * 
    * @throws UnsupportedOperationException always
    */
   @Override public void remove() {
      throw new UnsupportedOperationException();
   }
}
