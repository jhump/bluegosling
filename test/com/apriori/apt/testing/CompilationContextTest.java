package com.apriori.apt.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.apriori.vars.VariableInt;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

/**
 * Test cases for {@link CompilationContext}.
 */
// TODO: more tests!
public class CompilationContextTest {

   /** Sample source file. It has just a single annotation. */
   private final static String TEST_JAVA_CONTENTS =
         "public class Test implements Comparable<Test> {\n"
               + "  @Override public int compareTo(Test t) {\n"
               + "    return 0;\n" + "  }\n"
               + "}\n";

   @SupportedAnnotationTypes("*")
   class TestProcessor extends AbstractProcessor {

      private final boolean ret;

      TestProcessor(boolean ret) {
         this.ret = ret;
      }

      @Override
      public SourceVersion getSupportedSourceVersion() {
         return SourceVersion.latestSupported();
      }

      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         if (!roundEnv.processingOver()) {
            assertEquals(1, annotations.size());
            TypeElement annotation = annotations.iterator().next();
            assertEquals(Override.class.getCanonicalName(), annotation.getQualifiedName()
                  .toString());
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            assertEquals(1, elements.size());
            Element element = elements.iterator().next();
            assertEquals(ElementKind.METHOD, element.getKind());
            assertEquals("compareTo", element.getSimpleName().toString());
         }
         return ret;
      }
   }

   @SupportedAnnotationTypes("*")
   class FailingProcessor extends AbstractProcessor {

      private final RuntimeException e;

      FailingProcessor(RuntimeException e) {
         this.e = e;
      }

      @Override
      public SourceVersion getSupportedSourceVersion() {
         return SourceVersion.latestSupported();
      }

      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         throw e;
      }
   }

   private CompilationContext context;
   private JavaFileObject sourceFile;

   @Before
   public void setUp() {
      context = new CompilationContext();
      // create single source file
      sourceFile =
            context.getFileManager().createJavaFileObject(StandardLocation.SOURCE_PATH, "Test",
                  Kind.SOURCE, TEST_JAVA_CONTENTS);
   }

   @Test
   public void runProcessor_true() throws Throwable {
      assertTrue(context.newTask().processingFiles(sourceFile)
            .withProcessor(new TestProcessor(true)).run());
   }

   @Test
   public void runProcessor_false() throws Throwable {
      assertFalse(context.newTask().processingFiles(sourceFile)
            .withProcessor(new TestProcessor(false)).run());
   }

   @Test
   public void runProcessor_fails() throws Throwable {
      IllegalArgumentException expected = new IllegalArgumentException();
      try {
         context.newTask().processingFiles(sourceFile)
               .withProcessor(new FailingProcessor(expected)).run();
         fail("Expecting IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         assertSame(expected, e);
      }

      // check that ProcessorInvocationException is unwrapped
      IOException expectWrapped = new IOException();
      try {
         context.newTask()
               .processingFiles(sourceFile)
               .withProcessor(new FailingProcessor(new ProcessorInvocationException(expectWrapped)))
               .run();
         fail("Expecting IOException");
      } catch (IOException e) {
         assertSame(expectWrapped, e);
      }
   }

   @Test
   public void runTask() {
      final VariableInt timesRun = new VariableInt();
      final Object ret = new Object();
      Object actual =
            context.newTask().processingFiles(sourceFile).run(new ProcessingTask<Object>() {
               @Override
               public Object run(TestEnvironment env) {
                  assertEquals(1, timesRun.incrementAndGet());
                  return ret;
               }
            });

      assertSame(ret, actual);
      assertEquals(1, timesRun.get());
   }

   @Test
   public void runTask_fails() {
      final Throwable thrown = new Throwable();
      try {
         context.newTask().processingFiles(sourceFile)
               .run(new CheckedProcessingTask<Object, Throwable>() {
                  @Override
                  public Object run(TestEnvironment env) throws Throwable {
                     throw thrown;
                  }
               });
         fail("Expected Throwable");
      } catch (Throwable t) {
         assertSame(thrown, t);
      }
   }

   @Test
   public void runTask_reentrant() {
      final VariableInt timesRun = new VariableInt();
      final Object ret = new Object();
      Object actual =
            context.newTask().processingFiles(sourceFile)
                  .runReentrant(new ProcessingTask<Object>() {
                     @Override
                     public Object run(TestEnvironment env) {
                        if (env.roundEnvironment().processingOver()) {
                           // 2nd invocation is to broadcast that processing is over
                           assertEquals(2, timesRun.incrementAndGet());
                        } else {
                           // first round
                           assertEquals(1, timesRun.incrementAndGet());
                        }
                        return ret;
                     }
                  });
      assertSame(ret, actual);
      assertEquals(2, timesRun.get());
   }
}