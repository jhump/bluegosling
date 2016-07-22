package com.bluegosling.processors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import org.junit.Test;

import com.bluegosling.apt.testing.CategorizingDiagnosticCollector;
import com.bluegosling.apt.testing.CompilationContext;
import com.bluegosling.apt.testing.CompilationFailedException;

public class ValueTypeProcessorTest {
   @Test public void properUsage() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "import java.util.Objects;\n"
            + "import java.util.concurrent.TimeUnit;\n"
            + "\n"
            + "@ValueType final class Foo extends Object {\n"
            + "  static volatile Foo theOne = null;\n"
            + "\n"
            + "  final String name;"
            + "  final int duration;"
            + "  final TimeUnit unit;\n"
            + "\n"
            + "  Foo(String name, int duration, TimeUnit unit) {\n"
            + "    this.name = name;\n"
            + "    this.duration = duration;\n"
            + "    this.unit = unit;\n"
            + "  }\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo && ((Foo) o).name.equals(this.name);\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return Objects.hash(name, unit.toNanos(duration));\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return name + \"(\" + duration + \" \" + unit + \")\";\n"
            + "  }\n"
            + "}\n";
      expectSuccess(file);
   }
   
   @Test public void properUsage_empty() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo {\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n";
      expectSuccess(file);
   }
   
   @Test public void error_notClass() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType interface Foo {\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must be classes (not interfaces, enums, or annotation types).",
                  3, 12));

      file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType enum Foo {\n"
            + "  INSTANCE;\n"
            + "}\n";
      errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must be classes (not interfaces, enums, or annotation types).",
                  3, 12));

      file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType @interface Foo {\n"
            + "}\n";
      errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must be classes (not interfaces, enums, or annotation types).",
                  3, 13));
   }

   @Test public void error_nonFinalClass() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType class Foo {\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must be final.", 3, 12));
   }

   @Test public void error_extends() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo extends Bar {\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n"
            + "\n"
            + "class Bar {\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types may not have a superclass, other than java.lang.Object.",
                  3, 18));
   }

   @Test public void error_nonFinalField() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo {\n"
            + "  private String bar;\n"
            + "  private int baz;\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types cannot have non-final member fields.", 4, 18),
            new Message("Value types cannot have non-final member fields.", 5, 15));
   }

   @Test public void error_noEquals() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo {\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must not use the default implementation for equals(Object).",
                  3, 18));
   }

   @Test public void error_noHashCode() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo {\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public String toString() {\n"
            + "    return Foo.class.getName();\n"
            + "  }\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must not use the default implementation for hashCode().",
                  3, 18));
   }
   
   @Test public void error_noToString() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType final class Foo {\n"
            + "  @Override public boolean equals(Object o) {\n"
            + "    return o instanceof Foo;\n"
            + "  }\n"
            + "  @Override public int hashCode() {\n"
            + "    return 0;\n"
            + "  }\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types must not use the default implementation for toString().",
                  3, 18));
   }
   
   @Test public void error_multiple() throws Throwable {
      String file = "import com.bluegosling.util.ValueType;\n"
            + "\n"
            + "@ValueType class Foo extends Bar {\n"
            + "  private String bar;\n"
            + "  private int baz;\n"
            + "}\n"
            + "\n"
            + "class Bar {\n"
            + "}\n";
      List<Diagnostic<JavaFileObject>> errors = expectFailure(file);
      expectMessages(errors,
            new Message("Value types cannot have non-final member fields.", 4, 18),
            new Message("Value types cannot have non-final member fields.", 5, 15),
            new Message("Value types must be final."
                  + " Value types may not have a superclass, other than java.lang.Object."
                  + " Value types must not use the default implementation for equals(Object)."
                  + " Value types must not use the default implementation for hashCode()."
                  + " Value types must not use the default implementation for toString().",
                  3, 12));
   }
   
   private void expectSuccess(String fooContents) throws Throwable {
      CompilationContext ctx = new CompilationContext();
      JavaFileObject file = ctx.getFileManager()
            .createJavaFileObject(StandardLocation.SOURCE_PATH, "Foo", Kind.SOURCE, fooContents);
      CategorizingDiagnosticCollector diags = ctx.getDiagnosticCollector();
      boolean ret = ctx.newTask().processingFiles(file).withProcessor(new ValueTypeProcessor())
            .run();
      assertFalse(ret); // processor always returns false; never "claims" annotations
      assertTrue(diags.getDiagnostics(Diagnostic.Kind.ERROR).isEmpty());
   }
   
   private List<Diagnostic<JavaFileObject>> expectFailure(String fooContents)
         throws Throwable {
      CompilationContext ctx = new CompilationContext();
      JavaFileObject file = ctx.getFileManager()
            .createJavaFileObject(StandardLocation.SOURCE_PATH, "Foo", Kind.SOURCE, fooContents);
      CategorizingDiagnosticCollector diags = ctx.getDiagnosticCollector();
      try {
         ctx.newTask().processingFiles(file).withProcessor(new ValueTypeProcessor()).run();
         fail("Expecting compilation to fail but it did not");
      } catch (CompilationFailedException e) {
         // expected
      }
      // make sure we have errors that all point to our source file
      List<Diagnostic<JavaFileObject>> errors = diags.getDiagnostics(Diagnostic.Kind.ERROR);
      assertFalse(errors.isEmpty());
      for (Diagnostic<JavaFileObject> error : errors) {
         assertSame(file, error.getSource());
      }
      return errors;
   }
   
   private void expectMessages(List<Diagnostic<JavaFileObject>> errors, Message... messages) {
      Iterator<Diagnostic<JavaFileObject>> iter = errors.iterator();
      for (Message msg : messages) {
         Diagnostic<?> d = iter.next();
         assertEquals(msg.message, d.getMessage(null));
         assertEquals(msg.lineNumber, d.getLineNumber());
         assertEquals(msg.columnNumber, d.getColumnNumber());
      }
      assertFalse(iter.hasNext());
   }
   
   private static class Message {
      final String message;
      final int lineNumber;
      final int columnNumber;
      
      Message(String message, int lineNumber, int columnNumber) {
         this.message = message;
         this.lineNumber = lineNumber;
         this.columnNumber = columnNumber;
      }
   }
}
