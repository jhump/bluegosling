package com.apriori.collections;

import java.math.BigInteger;
import java.util.NavigableMap;

/**
 * Like a {@link RadixTrieMap}, but with {@link BigInteger} keys.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of values in the map
 */
//TODO: javadoc
//TODO: implement me and remove abstract modifier (don't forget serialization and cloning)
public abstract class BigIntegerRadixTrieMap<V>
      implements NavigableMap<BigInteger, V> {

}
