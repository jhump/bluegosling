package com.apriori.apt.testing;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * A collector of diagnostic messages emitted by the compiler. Unlike
 * {@link DiagnosticCollector}, this class categorizes the diagnostic messages by
 * kind.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class CategorizingDiagnosticCollector {

   /**
    * The list of all diagnostics received so far.
    */
   private final List<Diagnostic<JavaFileObject>> allDiagnostics;
   
   /**
    * Lists of all diagnostics received so far, categorized by kind.
    */
   private final Map<Diagnostic.Kind, List<Diagnostic<JavaFileObject>>> categorizedDiagnostics;

   /**
    * Constructs a new collector. Diagnostics will be printed to {@code stderr}.
    */
   public CategorizingDiagnosticCollector() {
      allDiagnostics = new ArrayList<Diagnostic<JavaFileObject>>();
      categorizedDiagnostics = new HashMap<Diagnostic.Kind, List<Diagnostic<JavaFileObject>>>();
   }

   /**
    * Returns a {@link DiagnosticListener} that prints diagnostic messages and records
    * them to the collector's internal structures. Diagnostics will be printed to
    * standard error.
    * 
    * @return the listener
    */
   DiagnosticListener<JavaFileObject> getListener() {
      return getListener(null);
   }
   
   /**
    * Returns a {@link DiagnosticListener} that prints diagnostic messages and records
    * them to the collector's internal structures. Diagnostics will be printed to the
    * specified writer. 
    * 
    * @return the listener
    * @param writer the writer, for printing diagnostics
    */
   DiagnosticListener<JavaFileObject> getListener(Writer writer) {
      final Writer output = writer == null ? new OutputStreamWriter(System.err) : writer;
      return new DiagnosticListener<JavaFileObject>() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            @SuppressWarnings("unchecked")
            Diagnostic<JavaFileObject> fileDiagnostic = (Diagnostic<JavaFileObject>) diagnostic;
            synchronized (CategorizingDiagnosticCollector.this) {
               allDiagnostics.add(fileDiagnostic);
               List<Diagnostic<JavaFileObject>> category = categorizedDiagnostics.get(diagnostic.getKind());
               if (category == null) {
                  category = new ArrayList<Diagnostic<JavaFileObject>>();
                  categorizedDiagnostics.put(diagnostic.getKind(), category);
               }
               category.add(fileDiagnostic);
            }
            try {
               output.write(diagnostic.getMessage(null) + "\n");
               output.flush();
            } catch (IOException e) {
               // Don't really want to throw here since we've captured
               // the diagnostic. Wish there was something better to do...
               e.printStackTrace();
            }
         }
      };
   }

   /**
    * Gets a view of all diagnostics collected so far.
    * 
    * @return a collection of diagnostics
    */
   public synchronized Collection<Diagnostic<JavaFileObject>> getAllDiagnostics() {
      return Collections.unmodifiableList(allDiagnostics);
   }
   
   /**
    * Gets a view of diagnostics of a given kind collected so far. 
    * 
    * @param kind the kind of diagnostic to return
    * @return a collection of diagnostics
    */
   public synchronized Collection<Diagnostic<JavaFileObject>> getDiagnostics(Diagnostic.Kind kind) {
      List<Diagnostic<JavaFileObject>> category = categorizedDiagnostics.get(kind);
      if (category == null) {
         return Collections.emptyList();
      } else {
         return Collections.unmodifiableList(category);
      }
   }
}
