package com.bluegosling.apt.testing;

import com.bluegosling.util.IoStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;

/**
 * Information about the current test, processing, and round environments. This represents the
 * context of a test run by an {@link AnnotationProcessorTestRunner}. It includes references to the
 * current {@link ProcessingEnvironment}, {@link RoundEnvironment}, {@link JavaFileManager}, etc. An
 * instance of this class can be injected into a test method so that the test logic can interact
 * with the environment.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TestEnvironment {
   private final TestJavaFileManager fileManager;
   private final CategorizingDiagnosticCollector diagnosticCollector;
   private final ProcessingEnvironment processingEnv;
   private final RoundEnvironment roundEnv;
   private final int roundNumber;
   private final Set<? extends TypeElement> annotationTypes;
   private final Processor processorUnderTest;
   private final Object testObject;
   
   TestEnvironment(TestJavaFileManager fileManager,
         CategorizingDiagnosticCollector diagnosticCollector, ProcessingEnvironment processingEnv,
         RoundEnvironment roundEnv, int roundNumber, Set<? extends TypeElement> annotationTypes,
         Processor processorUnderTest, Object testObject) {
      this.fileManager = fileManager;
      this.diagnosticCollector = diagnosticCollector;
      this.processingEnv = processingEnv;
      this.roundEnv = roundEnv;
      this.roundNumber = roundNumber;
      this.annotationTypes = annotationTypes;
      this.processorUnderTest = processorUnderTest;
      this.testObject = testObject;
   }
   
   /**
    * Gets the current {@link JavaFileManager}.
    * 
    * @return the file manager
    */
   public TestJavaFileManager fileManager() {
      return fileManager;
   }

   /**
    * Gets the current collector of diagnostics emitted by the compiler and by annotation
    * processors.
    * 
    * @return the diagnostic collector
    */
   public CategorizingDiagnosticCollector diagnosticCollector() {
      return diagnosticCollector;
   }
   
   /**
    * Gets the current processing environment.
    * 
    * @return the processing environment
    */
   public ProcessingEnvironment processingEnvironment() {
      return processingEnv;
   }

   /**
    * Gets the round environment for the current round of processing.
    * 
    * @return the round environment
    */
   public RoundEnvironment roundEnvironment() {
      return roundEnv;
   }

   /**
    * Gets the number of the current round (first round is number one).
    * 
    * @return the current round number
    */
   public int roundNumber() {
      return roundNumber;
   }

   /**
    * Gets the set of annotation types for the current round, as {@link TypeElement}s. If the test
    * defined a processor (using {@link ProcessorUnderTest @ProcessorUnderTest} or
    * {@link InitializeProcessorField @InitializeProcessorField} annotations), then this will be
    * filtered to just the annotations supported by that processor.
    * 
    * <p>If no processor is defined, this will be all annotations in the current set of classes and
    * input files to process. If a processor is later created programmatically inside of a test
    * method, this set can then be filtered to only the supported annotation types using
    * {@link #filterAnnotationTypesFor(Processor)}.
    * 
    * @return the set of annotation types for the current round of processing
    * 
    * @see #processorUnderTest()
    * @see #filterAnnotationTypesFor(Processor)
    */
   public Set<? extends TypeElement> annotationTypes() {
      return annotationTypes;
   }
   
   /**
    * Filters the current set of annotation types to only those supported by the specified
    * processor.
    * 
    * @param processor the processor
    * @return the filtered set of annotation types for the current round of processing
    * 
    * @see #annotationTypes()
    */
   public Set<? extends TypeElement> filterAnnotationTypesFor(Processor processor) {
      Set<String> supported = processor.getSupportedAnnotationTypes();
      Set<TypeElement> filteredTypes = new LinkedHashSet<TypeElement>();
      for (TypeElement element : annotationTypes()) {
         if (supported.contains(element.getQualifiedName().toString())) {
            filteredTypes.add(element);
         }
      }
      return Collections.unmodifiableSet(filteredTypes);
   }
   
   /**
    * Gets the current processor under test or {@code null} if there isn't one.
    * 
    * @return the processor under test
    */
   public Processor processorUnderTest() {
      return processorUnderTest;
   }
   
   /**
    * Invokes the current processor under test by calling its {@link Processor#process(Set,
    * RoundEnvironment)} method. The current round environment and annotation types are passed to
    * this method.
    * 
    * @return the result returned by the processor
    * @throws NullPointerException if there is no current processor under test
    * 
    * @see #processorUnderTest()
    */
   public boolean invokeProcessor() {
      return processorUnderTest.process(annotationTypes, roundEnv);
   }
   
   /**
    * Validates the contents of the specified file by comparing them to the contents of the
    * specified resource. This method compares character contents vs. raw binary contents, so
    * calling this method is equivalent to the following:
    * <pre>testEnv.validateGeneratedFile(file, resourcePath, false);</pre>
    * 
    * @param file the output file to validate
    * @param resourcePath the resource that contains the "golden" contents
    * @throws IOException if an exception occurs while reading the specified file or the specified
    *       resource
    * 
    * @see ValidateGeneratedFiles
    */
   public void validateGeneratedFile(FileObject file, String resourcePath) throws IOException {
      validateGeneratedFile(file, resourcePath, false);
   }
   
   /**
    * Validates the contents of the specified file by comparing them to the contents of the
    * specified resource.
    * 
    * @param file the output file to validate
    * @param resourcePath the resource that contains the "golden" contents
    * @param binary if true then the raw byte contents of the specified file and resource are
    *       compared; otherwise the files are interpreted as character data and the result strings
    *       are compared
    * @throws IOException if an exception occurs while reading the specified file or the specified
    *       resource
    * 
    * @see ValidateGeneratedFiles
    */
   public void validateGeneratedFile(FileObject file, String resourcePath, boolean binary)
         throws IOException {
      InputStream in = testObject.getClass().getResourceAsStream(resourcePath);
      if (in == null) {
         throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }
      try {
         if (binary) {
            byte genBytes[] = IoStreams.toByteArray(in);
            byte goldenBytes[];
            if (file instanceof TestJavaFileObject) {
               goldenBytes = ((TestJavaFileObject) file).getByteContents();
            } else {
               InputStream goldenIn = file.openInputStream();
               try {
                  goldenBytes = IoStreams.toByteArray(goldenIn);
               } finally {
                  goldenIn.close();
               }
            }
            org.junit.Assert.assertArrayEquals("Output " + file.getName()
                  + " does not match contents of resource " + resourcePath,
                  genBytes, 
                  goldenBytes);
         } else {
            org.junit.Assert.assertEquals("Output " + file.getName()
                  + " does not match contents of resource " + resourcePath,
                  IoStreams.toString(in, TestJavaFileObject.DEFAULT_CHARSET), 
                  file.getCharContent(true));
         }
      } finally {
         in.close();
      }
   }
}
