package com.apriori.testing;

import com.apriori.function.Suppliers;

import org.apache.commons.collections.BulkTest;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BulkTestRunner extends Runner implements Filterable {
   private final Class<?> testClass;
   private final Supplier<Description> description = Suppliers.memoize(this::computeDescription);
   private Filter filter;
   
   public BulkTestRunner(Class<? extends BulkTest> testClass) {
      this.testClass = testClass;
   }
   
   private Map<String, TestCase> getCases() {
      Map<String, TestCase> tests = new TreeMap<>();
      collectCases(BulkTest.makeSuite(testClass), "", tests);
      return tests;
   }
   
   private void collectCases(TestSuite suite, String prefix, Map<String, TestCase> tests) {
      for (Enumeration<Test> en = suite.tests(); en.hasMoreElements(); ) {
         Test t = en.nextElement();
         if (t instanceof TestSuite) {
            TestSuite ts = (TestSuite) t;
            collectCases(ts, prefix + ts.getName() + ".", tests);
         } else {
            TestCase tc = (TestCase) t;
            String name = prefix + ((TestCase) t).getName();
            if (filter == null
                  || filter.shouldRun(Description.createTestDescription(testClass, name))) {
               tests.put(name, tc);
            }
         }
      }
   }

   @Override
   public Description getDescription() {
      return description.get();
   }
   
   private Description computeDescription() {
      Description ret = Description.createSuiteDescription(testClass);
      for (String name : getCases().keySet()) {
         ret.addChild(Description.createTestDescription(testClass, name));
      }
      return ret;
   }

   @Override
   public void run(RunNotifier notifier) {
      for (Entry<String, TestCase> entry : getCases().entrySet()) {
         Description d = Description.createTestDescription(testClass, entry.getKey());
         try {
            notifier.fireTestStarted(d);
         } catch (StoppedByUserException e) {
            return;
         }
         try {
            entry.getValue().runBare();
         } catch (Throwable th) {
            notifier.fireTestFailure(new Failure(d, th));
         }
         notifier.fireTestFinished(d);
      }
   }

   @Override
   public void filter(Filter f) throws NoTestsRemainException {
      this.filter = this.filter == null ? f : this.filter.intersect(f);
   }
}
