package com.apriori.apt.testing;

import com.apriori.util.Streams;

import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Inherited;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.ToolProvider;

/**
 * A test runner for executing tests inside the context of the annotation processing phase of a Java compiler.
 * 
 * <p>The environment of the test is setup using annotations, either on the class or on the test method.
 * 
 * <p><strong>On the test class:</strong>
 * <ul>
 * <li>{@link ProcessorUnderTest @ProcessorUnderTest}: Indicates the class of the processor under test.</li>
 * <li>{@link InitializeProcessorField @InitializeProcessorField}: Indicates the name of a field on the test
 * class that has the processor under test. The processor must be instantiated and the field set in a set-up
 * step (i.e. {@code @Before} method). If neither this annotation nor {@link ProcessorUnderTest} is present,
 * no processor is setup. If a test methods needs to invoke methods on an instance of a processor, it will have
 * to instantiate the processor itself.</li>
 * <li>{@link OptionsForProcessing @OptionsForProcessing}: Indicates the sequence of options (like command-line
 * options) that are provided to the Java compiler. These can include options interpreted by the processor
 * under test.</li>
 * <li>{@link FilesToProcess @FilesToProcess}: Indicates the set of input files (like source and resource files)
 * for the Java compiler and the annotaiton processor.</li>
 * <li>{@link ClassesToProcess @ClassesToProcess}: Indicates the set of classes to process. These classes will
 * be included in the root elements on each round of processing, even if there are no input files to process.</li>
 * <li>{@link ValidateGeneratedFiles @ValidateGeneratedFiles}: Indicates a set of "golden" output files. When
 * the processor completes, the test runner will verify that the processor produced the correct output by
 * comparing it to these files.</li>
 * <li>{@link SupportedSourceVersion @SupportedSourceVersion}: Usually the supported source version comes from
 * the processor under test. But if neither {@link ProcessorUnderTest} nor {@link InitializeProcessorField} is
 * used, then you can use this annotation on the test class to provide this information.<br/>
 * <strong>Note:</strong> Despite the fact that this annotation is <em>not</em> defined  {@linkplain Inherited
 * inherited}, this test runner will interpret the value as inherited. So if you sub-class a test that has the
 * annotation, it is not necessary re-annotate the sub-class.</li>
 * <li>{@link SupportedAnnotationTypes @SupportedAnnotationTypes}: Same remarks as for {@link SupportedSourceVersion}.
 * If a test does not otherwise define a processor for testing, this annotation can be used to provide this
 * information to the environment. Also same notes about inheritance of this annotation.</li>
 * <li>{@link SupportedOptions @SupportedOptions}: Same remarks again as for {@link SupportedSourceVersion} and
 * {@link SupportedAnnotationTypes}.</li>
 * </ul>
 * 
 * <p><strong>On the test method:</strong>
 * <ul>
 * <li>{@link NoProcess @NoProcess}: Indicates that a given method is a normal test method. The processing
 * environment will not be setup for this method.</li>
 * <li>{@link Reentrant @Reentrant}: Indicates that a given method can be called more than once if the processing
 * phase results in multiple rounds. So the method may be called more than once during the course of a single
 * test case, once for each round.</li>
 * <li>{@link ProcessorUnderTest @ProcessorUnderTest}: This is the same as used on the class, except that it
 * overrides the processor to test for a single method.</li>
 * <li>{@link OptionsForProcessing @OptionsForProcessing}: This is the same as used on the class, except that it
 * overrides the options for a single method. It can also be used to <em>add to</em> the options that are defined
 * on the class.</li>
 * <li>{@link FilesToProcess @FilesToProcess}: This is the same as used on the class, except that it overrides
 * the files used for a single method. It can also be used to <em>add to</em> the set of files that are defined
 * on the class.</li>
 * <li>{@link ClassesToProcess @ClassesToProcess}: This is the same as used on the class, except that it overrides
 * the classes processed for a single method. It can also be used to <em>add to</em> the set of classes that are
 * defined on the test class.</li>
 * <li>{@link ValidateGeneratedFiles @ValidateGeneratedFiles}: This is the same as used on the class, except that
 * it overrides the output files to validate for a single method. It can also be used to <em>add to</em> the set
 * of validated output files that are defined on the class.</li>
 * </ul>
 * 
 * <p>Unlike normal test methods, tests run by {@link AnnotationProcessorTestRunner} can accept arguments. That
 * is how the test method gets access to the processing environment. Test methods' can have arguments of the
 * following types:
 * <ul>
 * <li>{@link TestEnvironment}: a reference to the test environment, including accessor methods for everything
 * else and a few extra utility methods, too. This is really the only thing any test method <em>needs</em>, but
 * the other types are supported for convenience.</li>
 * <li>{@link TestJavaFileManager} or {@link JavaFileManager}: a reference to the Java compiler's file manager.</li>
 * <li>{@link CategorizingDiagnosticCollector}: a reference to the collector of diagnostics emitted by the Java
 * compiler. (See {@link DiagnosticListener}.)</li>
 * <li>{@link ProcessingEnvironment}: a reference to the processing environment.</li>
 * <li>{@link Elements}: a reference to the utility class for working with elements. (See
 * {@link ProcessingEnvironment#getElementUtils()}).</li>
 * <li>{@link Types}: a reference to the utility class for working with type mirrors. (See
 * {@link ProcessingEnvironment#getTypeUtils()}).</li>
 * <li>{@link Filer}: a reference to the interface used by an annotation processor to read and write files.
 * (See {@link ProcessingEnvironment#getFiler()}).</li>
 * <li>{@link Messager}: a reference to the interfaced used by an annotation processor to emit messages and/or
 * diagnostics. (See {@link ProcessingEnvironment#getMessager()}).</li>
 * <li>{@link SourceVersion}: a reference to the current version of the source code being processed. See
 * {@link ProcessingEnvironment#getSourceVersion()}).</li>
 * <li>{@code Map<String, String>}: a reference to the current options. See
 * {@link ProcessingEnvironment#getOptions()}).</li>
 * <li>{@link RoundEnvironment}: a reference to the current round environment.</li>
 * <li><code>Set&lt;{@link TypeElement}&gt;</code>: a reference to the set of annotation types for the current
 * round of processing.</li>
 * <li>{@code ? extends} {@link Processor}: a reference to the processor under test. If the method accepts a
 * sub-class or sub-interface of {@link Processor} that is not compatible with the actual processor instance
 * under test, the test method will fail with a {@link ClassCastException}.</li>
 * </ul>
 * Test methods are also allowed to return {@code boolean}, which represents the return value of executing
 * {@link Processor#process(Set, RoundEnvironment)} and indicates whether the annotations for that round were
 * consumed by the processor.
 * 
 * <p>Note that a method annotated with {@link NoProcess @NoProcess} must accept no arguments and return void
 * since it is run as a normal test method.
 * 
 * <p>Here is a short example of a test.
 * 
 * <pre>
 * {@literal @}RunWith(AnnotationProcessorTestRunner.class)
 * {@literal @}FilesToProcess({@literal @}InputFiles({"test/my/package/MyClass1.java", "test/my/package/MyClass2.java"}))
 * {@literal @}ClassesToProcess({SomeClassWithAnnotations.class, AnotherClassWithAnnotations.class})
 * {@literal @}SupportedSourceVersion(SourceVersion.RELEASE_6)
 * public class MyAnnotationProcessorTest {
 *   // You can specify {@literal @}FilesToProcess and/or {@literal @}ClassesToProcess for a given method, and it
 *   // will override the values specified at the class level.
 *   {@literal @}Test public void someTest(TestEnvironment testEnv) {
 *      // Do something in the environment -- like unit tests on processor methods that require tool context.
 *      // If we had defined a processor using {@literal @}ProcessorUnderTest or
 *      // {@literal @}InitializeProcessorField annotations, we could simply do this:
 *      //    testEnv.invokeProcessor();
 *      // To construct and run a processor (like to run a processor that hasn't been setup using
 *      // {@literal @}ProcessorUnderTest or {@literal @}InitializeProcessorField):
 *      SomeOtherProcessor processor = new SomeOtherProcessor();
 *      processor.init(testEnv.processingEnvironment());
 *      processor.process(testEnv.filterAnnotationsFor(processor), testEnv.roundEnvironment());
 *      // TestEnvironment provides other methods for doing verification/assertions on processor
 *      // output.
 *      FileObject generatedFile = testEnv.fileManager().getFileForInput(
 *            StandardLocation.CLASS_OUTPUT, "", "path/to/generated/file");
 *      testEnv.validateGeneratedFile(generatedFile, "path/to/resource/against/which/to/compare");
 *      // Test method can return boolean to indicate whether annotations are "claimed". If test method
 *      // returns void, assume annotations claimed. Note: if the test method is marked as {@literal @}Reentrant
 *      // and the compiler performs another round of processing, the test method will be called again
 *      // during the same {@code javac} invocation, once for each subsequent round.
 *   }
 * }
 * </pre>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: add validation to check config and verify invariants prior to running a test case, including verifying
// that a test case has at least one file or class to process (maybe even check for validity/presence of all
// referenced resources?), and verifying annotation compatibility (for example, @ProcessorUnderTest and @NoProcess
// not allowed on same method)
public class AnnotationProcessorTestRunner extends BlockJUnit4ClassRunner {

   @SuppressWarnings("unchecked")
   private static final Set<Class<?>> ALLOWED_PROCESS_RETURN_TYPES =
         new HashSet<Class<?>>(Arrays.asList(void.class, boolean.class, Boolean.class));
   
   @SuppressWarnings("unchecked")
   private static final Set<Class<?>> ALLOWED_NO_PROCESS_RETURN_TYPES = new HashSet<Class<?>>(Arrays.asList(void.class));
   
   public AnnotationProcessorTestRunner(Class<?> klass) throws InitializationError {
      super(klass);
   }
   
   @Override
   public Statement methodInvoker(final FrameworkMethod method, final Object test) {
      if (method.getMethod().isAnnotationPresent(NoProcess.class)) {
         // run as a plain-jane test
         return new Statement() {
            @Override
            public void evaluate() throws Throwable {
               method.invokeExplosively(test);
            }
         };
      } else {
         // otherwise, we run a Java compiler to create the environment and have it
         // point to a special "test" processor that ends up calling our test method
         return new Statement() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void evaluate() throws Throwable {
               JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
               CategorizingDiagnosticCollector diagnosticCollector = new CategorizingDiagnosticCollector();
               TestJavaFileManager fileManager = new TestJavaFileManager(
                     compiler.getStandardFileManager(diagnosticCollector.getListener(), null, null));
               Iterable<String> options = options(method.getMethod(), test.getClass());
               Iterable<String> classNames = classNamesToProcess(method.getMethod(), test.getClass());
               Iterable<JavaFileObject> files = filesToProcess(method.getMethod(), test.getClass(), fileManager);
               JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                     diagnosticCollector.getListener(), options, classNames, files);
               // The compiler eats everything thrown by an annotation processor. So we'll use this reference to
               // communicate the source exception up here so we can re-throw it (that way, junit indicates the
               // original assertion or test exception instead of just a generic CompilationFailedException)
               AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
               TestMethodProcessor processor = new TestMethodProcessor(method, test, fileManager, diagnosticCollector, errorRef);
               task.setProcessors(Arrays.asList(processor));
               try {
                  boolean success = task.call();
                  Throwable t = errorRef.get();
                  if (t != null) {
                     throw t;
                  } else if (!success) {
                     throw new CompilationFailedException("Compilation task failed",
                           diagnosticCollector.getDiagnostics(Diagnostic.Kind.ERROR));
                  } else if (processor.getInvocationCount() == 0) {
                     throw new CompilationFailedException("Compilation never invoked processor",
                           diagnosticCollector.getDiagnostics(Diagnostic.Kind.ERROR));
                  }
               } catch (TestMethodInvocationException e) {
                  throw e.getCause();
               }
            }
         };
      }
   }
   
   @Override
   protected void validateTestMethods(List<Throwable> errors) {
      // Do not call super since it performs overly strict validation on test methods.
      for (Method m : getTestClass().getJavaClass().getMethods()) {
         if (m.isAnnotationPresent(Test.class)) {
            if (m.isAnnotationPresent(NoProcess.class)) {
               if (m.getParameterTypes().length > 0) {
                  errors.add(new Exception("Method " + m.getName() + " in class " + m.getDeclaringClass().getName()
                        + " must accept no parameters"));
               }
               if (!ALLOWED_NO_PROCESS_RETURN_TYPES.contains(m.getReturnType())) {
                  errors.add(new Exception("Method " + m.getName() + " in class " + m.getDeclaringClass().getName()
                        + " must return one of " + ALLOWED_NO_PROCESS_RETURN_TYPES));
               }
            } else {
               TestMethodParameterInjector.verifyParameterTypes(m, errors);
               if (!ALLOWED_PROCESS_RETURN_TYPES.contains(m.getReturnType())) {
                  errors.add(new Exception("Method " + m.getName() + " in class " + m.getDeclaringClass().getName()
                        + " must return one of " + ALLOWED_PROCESS_RETURN_TYPES));
               }
            }
         }
      }
   }
   
   /**
    * Determines the options to pass to the Java compiler for a test, based on the presence of
    * {@link OptionsForProcessing} annotations on the test method and/or test class.
    * 
    * @param method the test method
    * @param clazz the test class
    * @return the options to pass to the compiler
    * 
    * @see JavaCompiler#getTask(java.io.Writer, JavaFileManager, DiagnosticListener, Iterable, Iterable, Iterable)
    */
   private Iterable<String> options(Method method, Class<?> clazz) {
      ArrayList<String> options = new ArrayList<String>();
      OptionsForProcessing forMethod = method.getAnnotation(OptionsForProcessing.class);
      if (forMethod == null || forMethod.incremental()) {
         OptionsForProcessing forClass = clazz.getAnnotation(OptionsForProcessing.class);
         if (forClass != null) {
            options.addAll(Arrays.asList(forClass.value()));
         }
      }
      if (forMethod != null) {
         options.addAll(Arrays.asList(forMethod.value()));
      }
      return Collections.unmodifiableList(options);
   }
   
   /**
    * Determines the set of class names to pass to the Java compiler for processing, based on
    * the presence of {@link ClassesToProcess} annotations on the test method and/ot test class.
    * 
    * @param method the test method
    * @param clazz the test class
    * @return the class names to process
    * 
    * @see JavaCompiler#getTask(java.io.Writer, JavaFileManager, DiagnosticListener, Iterable, Iterable, Iterable)
    */
   private Iterable<String> classNamesToProcess(Method method, Class<?> clazz) {
      HashSet<String> classNames = new HashSet<String>();
      ClassesToProcess forMethod = method.getAnnotation(ClassesToProcess.class);
      boolean includeClass = true;
      if (forMethod != null) {
         if (!forMethod.incremental()) {
            includeClass = false;
         }
         for (Class<?> classToProcess : forMethod.value()) {
            classNames.add(classToProcess.getCanonicalName());
         }
      }
      if (includeClass) {
         ClassesToProcess forClass = clazz.getAnnotation(ClassesToProcess.class);
         if (forClass != null) {
            for (Class<?> classToProcess : forClass.value()) {
               classNames.add(classToProcess.getName());
            }
         }
      }
      return Collections.unmodifiableSet(classNames);
   }
   
   /**
    * Determines the set of input files (compilation units) for a compile operation. These
    * are based on the presence of {@link FilesToProcess} annotations on the test method
    * and/or test class. The annotations may seed the environment with a variety of files,
    * but only Java source files (names ending in {@code ".java"} will be passed to the
    * compiler as compilation units.
    * 
    * @param method the test method
    * @param clazz the test class
    * @param fileManager a file manager, for creating the file contents in the environment
    * @return the Java source files to compile
    * @throws IOException if creation of any files in the environment fails
    * 
    * @see JavaCompiler#getTask(java.io.Writer, JavaFileManager, DiagnosticListener, Iterable, Iterable, Iterable)
    */
   private Iterable<JavaFileObject> filesToProcess(Method method, Class<?> clazz,
         TestJavaFileManager fileManager) throws IOException {
      List<FileDefinition> fileDefs = FileDefinition.getFilesToProcess(method, clazz);
      List<JavaFileObject> fileObjects = new ArrayList<JavaFileObject>();
      Set<String> usedPaths = new HashSet<String>();
      for (FileDefinition fileDef : fileDefs) {
         String path = fileDef.getTargetPath();
         if (!usedPaths.contains(path)) {
            // create the file
            JavaFileObject fileObject = createJavaFileObject(fileDef, fileManager, clazz);
            // include source files in the returned list of compilation units
            if (fileObject.getKind() == Kind.SOURCE) {
               fileObjects.add(fileObject);
            }
         }
      }
      return Collections.unmodifiableList(fileObjects);
   }
   
   /**
    * Creates a {@link JavaFileObject} that represents the specified {@link FileDefinition}. The contents
    * of the file are seeded from the contents of a resource.
    * 
    * @param fileDef the file definition (which includes information on both the file's location in the
    *       test file system as well as the location of the corresponding resource)
    * @param fileManager the file manager
    * @param clazz the test class
    * @return a {@link JavaFileObject}
    * @throws IOException if creation of the new file fails
    */
   private JavaFileObject createJavaFileObject(FileDefinition fileDef, TestJavaFileManager fileManager,
         Class<?> clazz) throws IOException {
      String resourceName = fileDef.getResourcePath();
      InputStream in = clazz.getResourceAsStream(resourceName);
      if (in == null) {
         throw new IllegalArgumentException("Resource not found: " + resourceName);
      }
      try {
         return fileManager.createFileObject(fileDef.getTargetLocation(), "", fileDef.getFileName(), Streams.toByteArray(in));
      } finally {
         in.close();
      }
   }
   
   
   
   // TODO: Remove this junk. But keep the useful bits as some of this cruft could be
   // useful as the basis for AnnotationProcessorTestRunnerTest!
/*
   @Retention(RetentionPolicy.RUNTIME)
   public static @interface SimpleAnnotation {
   }

   @OptionsForProcessing({"abc","def","xyz","123"}) @SimpleAnnotation
   public static class SimpleClass {
   }
   
   @SupportedAnnotationTypes("*")
   public static class SimpleProcessor extends AbstractProcessor {
      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         System.out.println("SimpleProcessor has been invoked! Whoopee!");
         System.out.println("Annotations:");
         for (TypeElement annotation : annotations) {
            System.out.println("  " + annotation);
         }
         System.out.println("Root Elements:");
         for (Element rootElement : roundEnv.getRootElements()) {
            System.out.println("  " + rootElement);
            List<? extends AnnotationMirror> mirrors = rootElement.getAnnotationMirrors();
            for (AnnotationMirror mirror : mirrors) {
               TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
               System.out.println("  @" + annotationType.getQualifiedName());
               if (annotationType.getQualifiedName().toString().equals(OptionsForProcessing.class.getCanonicalName())) {
                  AnnotationValue val = mirror.getElementValues().entrySet().iterator().next().getValue();
                  List<Object> stringVals = new ArrayList<Object>();
                  @SuppressWarnings("unchecked")
                  List<AnnotationValue> moarVals = (List<AnnotationValue>) val.getValue();
                  for (AnnotationValue val2 : moarVals) {
                     stringVals.add(val2.getValue());
                  }
                  System.out.println("  * " + Arrays.<List<?>> asList(stringVals));
               }
            }
         }
         System.out.println("Elements annotated with OptionsForProcessing:");
         for (Element element : roundEnv.getElementsAnnotatedWith(annotations.iterator().next())) {
            System.out.println("  " + element);
         }
         return false;
      }
   }
   
   @RunWith(AnnotationProcessorTestRunner.class)
   public static class SimpleTest {
      @Test @Reentrant @ProcessorUnderTest(ElementPrintingProcessor.class) @ClassesToProcess(SimpleClass.class) public void thisIsAReentrantTest(TestEnvironment testEnv) throws IOException {
         System.out.println("Round number: " + testEnv.roundNumber());
         testEnv.processorUnderTest().process(testEnv.annotationTypes(), testEnv.roundEnvironment());
         System.out.println(testEnv.fileManager().getFileForInput(StandardLocation.CLASS_OUTPUT, "", "index.html").getCharContent(true));
      }
      @Test @ProcessorUnderTest(SimpleProcessor.class) @ClassesToProcess(SimpleClass.class) public void thisIsATestWithAProcessor(SimpleProcessor processor, TestEnvironment testEnv) {
         System.out.println("Processor: " + processor);
         System.out.println("Processor under test: " + testEnv.processorUnderTest());
         testEnv.processorUnderTest().process(testEnv.annotationTypes(), testEnv.roundEnvironment());
      }
      @Test @ClassesToProcess(SimpleClass.class) public void thisIsATestForSimpleClass(AbstractProcessor processor, Map<String, String> options, TestEnvironment testEnv) {
         System.out.println("Processor: " + processor);
         System.out.println("Options:");
         for (String option : options.keySet()) {
            System.out.println("  " + option + "=" + options.get(option));
         }
         System.out.println("Annotations:");
         for (TypeElement annotation : testEnv.annotationTypes()) {
            System.out.println("  " + annotation);
         }
         System.out.println("Root Elements:");
         for (Element rootElement : testEnv.roundEnvironment().getRootElements()) {
            System.out.println("  " + rootElement);
            OptionsForProcessing opts = rootElement.getAnnotation(OptionsForProcessing.class);
            if (opts != null) {
               System.out.println("  * " + Arrays.asList(opts.value()));
            }
         }
         TypeElement annotationType = testEnv.processingEnvironment().getElementUtils().getTypeElement(OptionsForProcessing.class.getName());
         System.out.println("Elements annotated with " + annotationType.getQualifiedName());
         for (Element element : testEnv.roundEnvironment().getElementsAnnotatedWith(annotationType)) {
            System.out.println("  " + element);
         }
      }
   }
*/
}
