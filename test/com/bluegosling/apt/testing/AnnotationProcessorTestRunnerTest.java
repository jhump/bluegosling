package com.bluegosling.apt.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.bluegosling.collections.TransformingList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import junit.framework.AssertionFailedError;

/**
 * Test cases for {@link AnnotationProcessorTestRunner}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public class AnnotationProcessorTestRunnerTest {
   
   /**
    * Gets the list of initialization errors when trying to initialize an
    * {@link AnnotationProcessorTestRunner} for the specified test class.
    * 
    * @param testClass the test class
    * @return a list of errors (empty if none)
    */
   private static Collection<Throwable> getInitializationErrors(Class<?> testClass) {
      try {
        new AnnotationProcessorTestRunner(testClass);
        return Collections.emptyList();
      } catch (InitializationError e) {
         return e.getCauses();
      }
   }
   
   /**
    * Creates a new exception that combines the error messages of all of the specified errors. This
    * is mainly to make JUnit's reporting of errors more useful since it will show the error message
    * but won't show other exception data (like {@link InitializationError#getCauses() for example).
    * 
    * <p>This function is defined to return {@link AssertionFailedError} but it actually always
    * throws the exception. The return value is for syntactic sugar since the compiler doesn't
    * otherwise know that this method always throws. So the following two lines are equivalent:
    * <pre>
    * throw consolidate(someListOfErrors);
    * consolidate(someListOfErrors); // also throws
    * </pre>
    * 
    * @param errors the list of errors
    * @return a single error that combines error message information from all in the list
    * @throws AssertionFailedError always
    */
   private static AssertionFailedError consolidate(List<Throwable> errors) {
      for (Throwable t : errors) {
         t.printStackTrace();
      }
      
      if (errors.size() == 1) {
         // single exception
         Throwable t = errors.get(0);
         throw new AssertionFailedError(t.toString());
      }
      // if multiple, put them into a list of messages
      StringBuilder sb = new StringBuilder();
      int i = 1;
      for (Throwable t : errors) {
         if (i != 1) {
            sb.append("\n");
         }
         sb.append("#");
         sb.append(i++);
         sb.append(": ");
         sb.append(t.toString());
      }
      throw new AssertionFailedError(sb.toString());
   }
   
   /**
    * Runs the specified test using an {@link AnnotationProcessorTestRunner} and returns the
    * result.
    * 
    * @param testClass the test class
    * @return the test results
    */
   private static Result runTest(Class<?> testClass) {
      AnnotationProcessorTestRunner runner;
      try {
         runner = new AnnotationProcessorTestRunner(testClass);
      } catch (InitializationError e) {
         throw consolidate(e.getCauses());
      }
      RunNotifier notifier = new RunNotifier();
      Result result = new Result();
      notifier.addListener(result.createListener());
      runner.run(notifier);
      return result;
   }

   /**
    * Asserts that the specified test results contain no failures.
    * 
    * @param result the test results to inspect.
    * @param testCount the number of expected test cases in the results
    */
   private void assertNoFailures(Result result, int testCount) {
      assertEquals(testCount, result.getRunCount());
      assertEquals(0, result.getIgnoreCount());
      List<Failure> failures = result.getFailures();
      if (!failures.isEmpty()) {
         throw consolidate(new TransformingList<Failure, Throwable>(failures,
               Failure::getException));
      }
   }
   
   @SupportedAnnotationTypes("*")
   @SupportedSourceVersion(SourceVersion.RELEASE_8)
   static class TestProcessor extends AbstractProcessor {
      int initCount;
      
      @Override
      public void init(ProcessingEnvironment processingEnv) {
         initCount++;
         super.init(processingEnv);
      }

      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         processingEnv.getElementUtils().printElements(new OutputStreamWriter(System.out), annotations.toArray(new Element[0]));
         
         TypeElement list = processingEnv.getElementUtils().getTypeElement("java.util.List");
         for (Element e : list.getEnclosedElements()) {
            if (e.getKind() != ElementKind.METHOD) {
               continue;
            }
            ExecutableElement ee = (ExecutableElement) e;
            if (!ee.getSimpleName().toString().equals("addAll") || ee.getParameters().size() != 1) {
               continue;
            }
            VariableElement parm = ((ExecutableElement) e).getParameters().get(0);
            for (int i = 0; i < 2; i++) {
               TypeMirror captured = processingEnv.getTypeUtils().capture(parm.asType());
               assertSame(TypeKind.DECLARED, captured.getKind());
               TypeMirror arg = ((DeclaredType) captured).getTypeArguments().get(0);
               assertSame(TypeKind.TYPEVAR, arg.getKind());
               TypeVariable var = (TypeVariable) arg;
               TypeParameterElement varElement = (TypeParameterElement) var.asElement();
               dumpElement(varElement.getSimpleName().toString(), varElement);
               dumpElement("generic declaration", varElement.getGenericElement());
               dumpElement("enclosing", varElement.getEnclosingElement());
               PackageElement pkg = processingEnv.getElementUtils().getPackageElement("");
               dumpElement("unnamed package", pkg);
            }
         }
         
         return false;
      }

      private void dumpElement(String title, Element element) {
         System.out.println(title);
         System.out.println("--------");
         dumpElementRecursive(element, "");
         System.out.println();
      }

      private void dumpElementRecursive(Element element, String indent) {
         dumpElementShallow(element, indent);
         System.out.print(indent + "Enclosing: ");
         Element enclosing = element.getEnclosingElement();
         if (enclosing == null) {
            System.out.println("null");
         } else {
            System.out.println();
            dumpElementRecursive(enclosing, indent + "    ");
         }
         System.out.print(indent + "Enclosed: ");
         Throwable thrown = null;
         List<? extends Element> enclosed;
         try {
            enclosed = element.getEnclosedElements();
         } catch (Throwable t) {
            thrown = t;
            enclosed = null;
         }
         if (thrown != null) {
            System.out.println(thrown);
         }
         else if (enclosed == null) {
            System.out.println("null!!");
         } else if (enclosed.isEmpty()) {
            System.out.println("none");
         } else {
            System.out.println();
            for (Element e : enclosed) {
               dumpElementShallow(e, indent + "    ");
            }
         }
      }
      
      private void dumpElementShallow(Element element, String indent) {
         System.out.println(indent + "Object: " + element.getClass().getName()
               + " @ " + System.identityHashCode(element));
         System.out.println(indent + "Name: " + processingEnv.getElementUtils()
               .getConstantExpression(element.getSimpleName().toString()));
         System.out.println(indent + "Kind: " + element.getKind());
         if (element.getKind() == ElementKind.PACKAGE) {
            System.out.println(indent + "Full Name: " + ((PackageElement) element).getQualifiedName());
            System.out.println(indent + "Unnamed? " + ((PackageElement) element).isUnnamed());
         }
      }
   }
   
   @Test public void testProcessorUnderTest() {
      Result result = runTest(TestProcessorUnderTest.class);
      assertNoFailures(result, 1);
   }
   
   @RunWith(AnnotationProcessorTestRunner.class)
   @ProcessorUnderTest(TestProcessor.class)
   public static class TestProcessorUnderTest {
      public TestProcessorUnderTest() {}
      @Test public void test(TestEnvironment testEnv) {
         assertEquals(1, ((TestProcessor) testEnv.processorUnderTest()).initCount);
         testEnv.invokeProcessor();
      }
   }
   
   @Test public void testInitializeProcessorField() {
      Result result = runTest(TestInitializeProcessorField.class);
      assertNoFailures(result, 1);
   }

   @RunWith(AnnotationProcessorTestRunner.class)
   @InitializeProcessorField("tp")
   public static class TestInitializeProcessorField {
      private TestProcessor tp;
      public TestInitializeProcessorField() {}
      @Before public void setUp() {
         tp = new TestProcessor();
      }
      @Test public void test(TestEnvironment testEnv) {
         assertSame(tp, testEnv.processorUnderTest());
         assertEquals(1, ((TestProcessor) testEnv.processorUnderTest()).initCount);
      }
   }
}
