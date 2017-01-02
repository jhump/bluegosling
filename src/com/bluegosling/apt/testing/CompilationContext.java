package com.bluegosling.apt.testing;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * The context for invoking a compiler and running annotation processors. To run a task in the
 * annotation phase of a compilation, create a new context, create a task in that context, and then
 * run the task. Example:
 * <pre>
 * CompilationContext context = new CompilationContext();
 * // seed input files using context.getFileManager()
 * context.newTask().processingFiles(listOfFiles).run(myProcessingTask);
 * </pre>
 */
public class CompilationContext {

   final JavaCompiler compiler;
   final CategorizingDiagnosticCollector diagnosticCollector;
   final TestJavaFileManager fileManager;

   /**
    * Constructs a new context
    */
   public CompilationContext() {
      compiler = ToolProvider.getSystemJavaCompiler();
      diagnosticCollector = new CategorizingDiagnosticCollector();
      fileManager =
            new TestJavaFileManager(compiler.getStandardFileManager(
                  diagnosticCollector.getListener(), null, null));
   }

   /**
    * Returns the system compiler that will be invoked when tasks are run in this context.
    * 
    * @return the system compiler
    */
   public JavaCompiler getCompiler() {
      return compiler;
   }

   /**
    * Returns the diagnostic collector used to collect output from compiler invocations.
    * 
    * @return the diagnostic collector
    */
   public CategorizingDiagnosticCollector getDiagnosticCollector() {
      return diagnosticCollector;
   }

   /**
    * Returns the file manager for this compilation context, used to seed input files or examine
    * output files.
    * 
    * @return the file manager
    */
   public TestJavaFileManager getFileManager() {
      return fileManager;
   }

   /**
    * Returns a new task builder that can execute a task during the annotation processing phase of
    * the compiler.
    * 
    * @return a new task builder
    */
   public TaskBuilder newTask() {
      return new TaskBuilderImpl();
   }

   /**
    * Builds a task to run in this compilation context. Running a task involves invoking the
    * compiler and in turn running a task from within the annotation processing phase.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface TaskBuilder {

      /**
       * Adds option strings to pass to the compiler.
       */
      TaskBuilder withOptions(Iterable<String> options);

      /**
       * Adds option strings to pass to the compiler.
       */
      TaskBuilder withOptions(String... options);

      /**
       * Adds classes to be processed by the annotation processing round of compilation. Note that
       * the classes and their annotations will be available for processing but will not be included
       * in the first round's "root elements" since that indicates source elements to be processed.
       * So you probably instead want {@link #processingFiles(Iterable)}.
       */
      TaskBuilder processingClasses(Iterable<Class<?>> classes);

      /**
       * Adds classes to be processed by the annotation processing round of compilation.
       * 
       * @see #processingClasses(Iterable)
       */
      TaskBuilder processingClasses(Class<?>... classes);

      /**
       * Adds files to the list of things to process during compilation.
       */
      TaskBuilder processingFiles(Iterable<JavaFileObject> files);

      /**
       * Adds files to the list of things to process during compilation.
       */
      TaskBuilder processingFiles(JavaFileObject... files);

      /**
       * Sets the processor under test for the task. The task can either
       * {@linkplain TaskBuilderWithProcessor#run() run the processor} or can interact with it from
       * a {@linkplain #run(CheckedProcessingTask) processing task}.
       */
      TaskBuilderWithProcessor withProcessor(Processor processor);

      /**
       * Invokes the compiler and runs the specified task during the annotation processing phase.
       *
       * @return the value returned by the specified task
       */
      <T, X extends Throwable> T run(CheckedProcessingTask<T, X> task) throws X;

      /**
       * Invokes the compiler and runs the specified task during the annotation processing phase. If
       * there is more than one round of processing, the task will be invoked each time.
       *
       * @return the value returned by final invocation of the specified task
       */
      <T, X extends Throwable> T runReentrant(CheckedProcessingTask<T, X> task) throws X;
   }

   /**
    * Builds a task to run in this compilation context. For tasks with a processor under test, this
    * interface provides an additional {@linkplain #run() convenience method} that simply invokes
    * the processor under test.
    *
    * <p>Other methods are overridden only to constrain the return type (covariance) for method
    * chaining. If {@link #run(CheckedProcessingTask)} is invoked, the specified task will be
    * executed and the {@link TestEnvironment} will indicate the processor under test with which the
    * task can interact.
    */
   public interface TaskBuilderWithProcessor extends TaskBuilder {

      // override builder return type so this wider interface is returned

      @Override
      TaskBuilderWithProcessor withOptions(Iterable<String> options);

      @Override
      TaskBuilderWithProcessor withOptions(String... options);

      @Override
      TaskBuilderWithProcessor processingClasses(Iterable<Class<?>> classes);

      @Override
      TaskBuilderWithProcessor processingClasses(Class<?>... classes);

      @Override
      TaskBuilderWithProcessor processingFiles(Iterable<JavaFileObject> files);

      @Override
      TaskBuilderWithProcessor processingFiles(JavaFileObject... files);

      /**
       * Invokes the compiler and runs the processor under test during the annotation processing
       * phase.
       *
       * @return the value returned by the {@linkplain Processor#process(Set, RoundEnvironment)
       *         processor under test}
       */
      boolean run() throws Throwable;
   }

   private class TaskBuilderImpl implements TaskBuilderWithProcessor {
      private final List<String> options = new ArrayList<>();
      private final Set<Class<?>> classesToProcess = new HashSet<>();
      private final Set<JavaFileObject> filesToProcess = new HashSet<>();
      private Processor processorUnderTest;

      TaskBuilderImpl() {
      }

      @Override
      public TaskBuilderImpl withOptions(Iterable<String> opts) {
         Iterables.addAll(this.options, opts);
         return this;
      }

      @Override
      public TaskBuilderImpl withOptions(String... opts) {
         return withOptions(Arrays.asList(opts));
      }

      @Override
      public TaskBuilderImpl processingClasses(Iterable<Class<?>> classes) {
         Iterables.addAll(this.classesToProcess, classes);
         return this;
      }

      @Override
      public TaskBuilderImpl processingClasses(Class<?>... classes) {
         return processingClasses(Arrays.asList(classes));
      }

      @Override
      public TaskBuilderImpl processingFiles(Iterable<JavaFileObject> files) {
         Iterables.addAll(this.filesToProcess, files);
         return this;
      }

      @Override
      public TaskBuilderImpl processingFiles(JavaFileObject... files) {
         return processingFiles(Arrays.asList(files));
      }

      @Override
      public TaskBuilderImpl withProcessor(Processor processor) {
         this.processorUnderTest = requireNonNull(processor);
         return this;
      }

      private boolean run(Processor processor) throws Throwable {
         // if no classes or files specified, just use Object as a dummy class to process
         // (just to get us into a processing environment)
         Set<String> classNames = classesToProcess.isEmpty() && filesToProcess.isEmpty()
               ? new LinkedHashSet<>(Arrays.asList(Object.class.getCanonicalName()))
               : classesToProcess.stream()
                     .map(Class::getCanonicalName)
                     .collect(Collectors.toCollection(LinkedHashSet::new));
         JavaCompiler.CompilationTask task =
               compiler.getTask(null, fileManager, diagnosticCollector.getListener(), options,
                     classNames, filesToProcess);
         TrackingProcessor trackingProcessor = new TrackingProcessor(processor);
         task.setProcessors(Collections.singleton(trackingProcessor));
         boolean success;
         Throwable t;
         try {
            success = task.call();
            t = trackingProcessor.getThrowable();
         } catch (Throwable th) {
            success = false;
            t = trackingProcessor.getThrowable();
            if (t == null) {
               t = th;
            }
         }
         if (t == null && !success) {
            throw new CompilationFailedException("Compilation task failed",
                  diagnosticCollector.getDiagnostics(Diagnostic.Kind.ERROR));
         }
         if (t != null) {
            if (t instanceof ProcessorInvocationException) {
               throw t.getCause(); // unwrap
            }
            throw t;
         }
         Boolean ret = trackingProcessor.getProcessorReturn();
         if (ret == null) {
            throw new CompilationFailedException("Compilation never invoked processor",
                  diagnosticCollector.getDiagnostics(Diagnostic.Kind.ERROR));
         }
         return ret;
      }

      @Override
      public boolean run() throws Throwable {
         if (processorUnderTest == null) {
            throw new IllegalStateException("processor under test never specified");
         }
         return run(processorUnderTest);
      }

      @SuppressWarnings("unchecked")
      // we know type constraints on throwable, so casting is okay
      private <T, X extends Throwable> T run(CheckedProcessingTask<T, X> task, boolean reentrant)
            throws X {
         ProcessingTaskProcessor<T> processor =
               new ProcessingTaskProcessor<T>(task, processorUnderTest, reentrant);
         try {
            run(processor);
         } catch (Throwable e) {
            Throwable thrownFromTask = processor.getThrowable();
            if (thrownFromTask != null) {
               // we know the task can only throw X or unchecked, so this
               // unchecked cast is safe regarding exception types that can
               // "escape" this method per its signature
               throw (X) thrownFromTask;
            } else if (e instanceof Error) {
               throw (Error) e;
            } else if (e instanceof RuntimeException) {
               throw (RuntimeException) e;
            } else {
               throw new RuntimeException(e);
            }
         }
         return processor.getValue();
      }

      @Override
      public <T, X extends Throwable> T run(CheckedProcessingTask<T, X> task) throws X {
         return run(task, false);
      }

      @Override
      public <T, X extends Throwable> T runReentrant(CheckedProcessingTask<T, X> task) throws X {
         return run(task, true);
      }
   }

   /**
    * A {@link Processor} that delegates to another processor and keeps track of the value(s)
    * returned or exceptions thrown from the {@link #process(Set, RoundEnvironment)} method.
    */
   private class TrackingProcessor implements Processor {
      private final Processor processor;
      private Boolean processorReturn;
      private Throwable throwable;

      TrackingProcessor(Processor processor) {
         this.processor = processor;
      }

      @Override
      public Set<String> getSupportedOptions() {
         return processor.getSupportedOptions();
      }

      @Override
      public Set<String> getSupportedAnnotationTypes() {
         return processor.getSupportedAnnotationTypes();
      }

      @Override
      public SourceVersion getSupportedSourceVersion() {
         return processor.getSupportedSourceVersion();
      }

      @Override
      public void init(ProcessingEnvironment processingEnv) {
         processor.init(processingEnv);
      }

      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         try {
            boolean ret = processor.process(annotations, roundEnv);
            processorReturn = ret;
            return ret;
         } catch (Throwable t) {
            throwable = t;
            throw t;
         }
      }

      @Override
      public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member, String userText) {
         return processor.getCompletions(element, annotation, member, userText);
      }

      public Boolean getProcessorReturn() {
         return processorReturn;
      }

      public Throwable getThrowable() {
         return throwable;
      }
   }

   /**
    * A {@link Processor} that wraps a given {@link ProcessingTask}. The task will be executed
    * during the {@linkplain Processor#process(Set, RoundEnvironment) processing step}.
    *
    * <p>Other methods (like {@link #getSupportedAnnotationTypes()}) are delegated to the processor
    * under test. If there is no processor under test, they return "empty" defaults. For source
    * version, the default is {@link SourceVersion#RELEASE_6}.
    */
   private class ProcessingTaskProcessor<T> implements Processor {
      private final CheckedProcessingTask<T, ?> task;
      private final Processor processorUnderTest;
      private final boolean reentrant;
      private T taskValue;
      private Throwable taskThrowable;
      private ProcessingEnvironment processingEnv;
      private int roundNumber;

      ProcessingTaskProcessor(CheckedProcessingTask<T, ?> task, Processor processorUnderTest,
            boolean reentrant) {
         this.task = requireNonNull(task);
         this.processorUnderTest = processorUnderTest;
         this.reentrant = reentrant;
      }

      @Override
      public Set<String> getSupportedOptions() {
         return processorUnderTest == null
               ? Collections.emptySet()
               : processorUnderTest.getSupportedOptions();
      }

      @Override
      public Set<String> getSupportedAnnotationTypes() {
         return processorUnderTest == null
               ? Collections.singleton("*")
               : processorUnderTest.getSupportedAnnotationTypes();
      }

      @Override
      public SourceVersion getSupportedSourceVersion() {
         return processorUnderTest == null
               ? SourceVersion.latestSupported()
               : processorUnderTest.getSupportedSourceVersion();
      }

      @Override
      public void init(ProcessingEnvironment env) {
         this.processingEnv = env;
         if (processorUnderTest != null) {
            processorUnderTest.init(env);
         }
      }

      @Override
      public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
         roundNumber++;
         if (roundNumber > 1 && !reentrant) {
            return false;
         }
         try {
            taskValue =
                  task.run(new TestEnvironment(fileManager, diagnosticCollector, processingEnv,
                        roundEnv, roundNumber, annotations, processorUnderTest, task));
         } catch (Throwable t) {
            taskThrowable = t;
            throw new ProcessorInvocationException(t);
         }
         return false;
      }

      @Override
      public Iterable<? extends Completion> getCompletions(Element element,
            AnnotationMirror annotation, ExecutableElement member, String userText) {
         return processorUnderTest == null
               ? Collections.emptySet()
               : processorUnderTest.getCompletions(element, annotation, member, userText);
      }

      public T getValue() {
         return taskValue;
      }

      public Throwable getThrowable() {
         return taskThrowable;
      }
   }
}
