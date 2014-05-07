package com.apriori.collections;

import org.apache.commons.collections.collection.AbstractTestCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestSuite;


public class TreiberStackTest extends AbstractTestCollection {

   public static TestSuite suite() {
      return makeSuite(TreiberStackTest.class);
   }
   
   public TreiberStackTest(String testName) {
      super(testName);
   }

   @Override
   public Collection<?> makeConfirmedCollection() {
      return new ArrayList<Object>();
   }

   @Override
   public Collection<?> makeConfirmedFullCollection() {
      return new ArrayList<Object>(Arrays.asList(getFullElements()));
   }

   @Override
   public Collection<?> makeCollection() {
      return new TreiberStack<>();
   }

   @Override
   public Collection<?> makeFullCollection() {
      return new TreiberStack<>(Arrays.asList(getFullElements()));
   }
   
   // TODO: concurrency test
   // TODO: tests for Stack methods
}
