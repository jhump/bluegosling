package com.bluegosling.collections;

import com.bluegosling.collections.BitSequence.BitOrder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

/**
 * Factory methods for useful {@link BitConverter} implementations. All of the implementations
 * returned produce a sequence of bits in most-significant-bit-first order. Those that produce
 * multi-byte values (e.g. the underlying object is greater than 8 bits) use big-endian ordering
 * of bytes. This way, ordering values by their corresponding bit streams (assuming "false" is less
 * than "true") tends to follow the values' natural ordering.
 */
// TODO: javadoc
// TODO: tests
public final class BitConverters {
   private BitConverters() {      
   }
   
   private static final ThreadLocal<ByteArrayOutputStream> OUT =
         new ThreadLocal<ByteArrayOutputStream>() {
           @Override protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream();
           }
         };
         
   private static final BitConverter<Serializable> SERIALIZER = new BitConverter<Serializable>() {
      @Override
      public BitSequence getComponents(Serializable object) {
         @SuppressWarnings("synthetic-access")
         ByteArrayOutputStream out = OUT.get();
         out.reset();
         try {
            ObjectOutputStream objectOut = new ObjectOutputStream(out);
            objectOut.writeObject(object);
            objectOut.flush();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
         return BitSequences.fromBytes(out.toByteArray(), BitOrder.MSB);
      }
   };
   
   /**
    * Returns a converter that will serialize a given object. The stream of bits corresponds to the
    * bits in the resulting serialized bytes.
    */
   @SuppressWarnings("unchecked")
   public static <T extends Serializable> BitConverter<T> forSerializable() {
      return (BitConverter<T>) SERIALIZER;
   }
   
   /**
    * Returns a converter that converts bytes into streams of eight bits.
    */
   public static BitConverter<Byte> forByte() {
      return new BitConverter<Byte>() {
         @Override
         public BitSequence getComponents(Byte t) {
            return BitSequences.fromBitTuple(t.longValue(), Byte.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts shorts into streams of sixteen bits.
    */
   public static BitConverter<Short> forShort() {
      return new BitConverter<Short>() {
         @Override
         public BitSequence getComponents(Short t) {
            return BitSequences.fromBitTuple(t.longValue(), Short.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts characters into streams of bits. The default character
    * encoding is UTF-16BE, so most characters are represented as sixteen bits and will be ordered
    * by their unicode code point.
    */
   public static BitConverter<Character> forCharacter() {
      return forCharacter(Charset.forName("UTF-16BE"));
   }
   
   /**
    * Returns a converter that converts characters into stream of bits using the specified character
    * encoding.
    */
   public static BitConverter<Character> forCharacter(Charset charset) {
      return new BitConverter<Character>() {
         @Override
         public BitSequence getComponents(Character t) {
            return BitSequences.fromString(t.toString(), charset, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts integers into streams of 32 bits.
    */
   public static BitConverter<Integer> forInteger() {
      return new BitConverter<Integer>() {
         @Override
         public BitSequence getComponents(Integer t) {
            return BitSequences.fromBitTuple(t.longValue(), Integer.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts longs into streams of 64 bits.
    */
   public static BitConverter<Long> forLong() {
      return new BitConverter<Long>() {
         @Override
         public BitSequence getComponents(Long t) {
            return BitSequences.fromBitTuple(t.longValue(), Long.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts floats into streams of 32 bits. The bits will be the given
    * value's representation in IEEE 754 single-precision layout. 
    */
   public static BitConverter<Float> forFloat() {
      return new BitConverter<Float>() {
         @Override
         public BitSequence getComponents(Float t) {
            return BitSequences.fromBitTuple(Float.floatToIntBits(t), Float.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts doubles into streams of 64 bits. The bits will be the given
    * value's representation in IEEE 754 double-precision layout. 
    */
   public static BitConverter<Double> forDouble() {
      return new BitConverter<Double>() {
         @Override
         public BitSequence getComponents(Double t) {
            return BitSequences.fromBitTuple(Double.doubleToLongBits(t), Double.SIZE, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts strings into streams of bits. The default character
    * encoding is UTF-16BE, so most characters are represented as sixteen bits and will be ordered
    * by their unicode code point. The first character in the string is considered "most
    * significant".
    */
   public static BitConverter<CharSequence> forString() {
      return forString(Charset.forName("UTF-16BE"));
   }

   /**
    * Returns a converter that converts strings into streams of bits using the specifed character
    * encoding. The first character in the string is considered "most significant".
    */
   public static BitConverter<CharSequence> forString(final Charset charset) {
      return new BitConverter<CharSequence>() {
         @Override
         public BitSequence getComponents(CharSequence t) {
            return BitSequences.fromString(t, charset, BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts big integers into streams of bits. This returns variable
    * length sequences, depending on the precision of a given big integer. So this converter does
    * not maintain natural ordering of values (unless all values have the same precision and thus
    * same bit length).
    */
   public static BitConverter<BigInteger> forBigInteger() {
      return new BitConverter<BigInteger>() {
         @Override
         public BitSequence getComponents(BigInteger t) {
            byte[] bytes = t.toByteArray();
            // reverse the array so least significant bits first
            for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
               byte tmp = bytes[j];
               bytes[j] = bytes[i];
               bytes[i] = tmp;
            }
            return BitSequences.fromBytes(t.toByteArray(), BitOrder.MSB);
         }
      };
   }

   /**
    * Returns a converter that converts big decimals into streams of bits. This returns variable
    * length sequences, depending on the precision and scale of a given big decimal. So this
    * converter does not maintain natural ordering of values.
    */
   public static BitConverter<BigDecimal> forBigDecimal() {
      return new BitConverter<BigDecimal>() {
         @Override
         public BitSequence getComponents(BigDecimal t) {
            return BitSequences.fromString(t.toPlainString(), Charset.forName("US-ASCII"),
                  BitOrder.MSB);
         }
      };
   }
}
