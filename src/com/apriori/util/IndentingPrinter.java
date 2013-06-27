package com.apriori.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A utility for printing indented content. Indentation level can be increased or decreased and all
 * text written will be properly indented by that amount.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class IndentingPrinter {
   
   private final Writer writer;
   private final int indentSize;
   private int indentLevel;
   private String indent = "";
   private boolean atStartOfNewLine = true;
   
   /**
    * Creates a new printer that sends all output to the specified writer.
    * 
    * @param writer the writer to which printed output is written
    * @param indentSize the number of spaces in a single level of indentation
    */
   public IndentingPrinter(Writer writer, int indentSize) {
      this.writer = writer;
      this.indentSize = indentSize;
   }
   
   /**
    * Creates a new printer that sends all output to the specified stream.
    * 
    * @param out the stream to which printed output is written using UTF8 for character encoding
    * @param indentSize the number of spaces in a single level of indentation
    */
   public IndentingPrinter(OutputStream out, int indentSize) {
      this(out, Charset.forName("UTF-8"), indentSize);
   }
   
   /**
    * Creates a new printer that sends all output to the specified stream.
    * 
    * @param out the stream to which printed output is written
    * @param charset the character set used to encode output
    * @param indentSize the number of spaces in a single level of indentation
    */
   public IndentingPrinter(OutputStream out, Charset charset, int indentSize) {
      this(new OutputStreamWriter(out, charset), indentSize);
   }
   
   /**
    * Prints indented content. Before each new line, spaces for the current level of indentation are
    * written before other content.
    * 
    * @param msg a {@linkplain java.util.Formatter format string}
    * @param args optional format arguments
    * @throws IOException if an exception is thrown while writing output
    */
   public void print(String msg, Object... args) throws IOException {
      String output = String.format(msg, args);
      int previousPos = 0;
      int pos;
      while ((pos = output.indexOf('\n', previousPos)) >= 0) {
         if (atStartOfNewLine) {
            writer.write(indent);
         }
         String piece = output.substring(previousPos, pos + 1);
         writer.write(piece);
         atStartOfNewLine = true;
         previousPos = pos + 1;
      }
      String piece = output.substring(previousPos);
      if (!piece.isEmpty()) {
         if (atStartOfNewLine) {
            writer.write(indent);
         }
         writer.write(piece);
         atStartOfNewLine = false;
      }
   }
   
   /**
    * Prints indented content and ends it with a newline character. Before each new line, spaces for
    * the current level of indentation are written before other content.
    * 
    * @param msg a {@linkplain java.util.Formatter format string}
    * @param args optional format arguments
    * @throws IOException if an exception is thrown while writing output
    */
   public void println(String msg, Object... args) throws IOException {
      print(msg + "\n", args);
      flush();
   }
   
   /**
    * Ensures that all content printed so far is flushed to underlying writer or output stream.
    * 
    * @throws IOException if flushing/writing the content results in an exception
    */
   public void flush() throws IOException {
      writer.flush();
   }
   
   /**
    * Prints an empty blank line.
    * 
    * @throws IOException if an exception is thrown while writing output
    */
   public void println() throws IOException {
      println("");
   }
   
   /**
    * Gets the current level of indentation.
    * 
    * @return the current level of indentation
    */
   public int getIndentLevel() {
      return indentLevel;
   }
   
   /**
    * Increases the current level of indentation by one.
    */
   public void indent() {
      indentLevel++;
      setIndent();
   }
   
   /**
    * Decreases the current level of indentation by one.
    */
   public void outdent() {
      indentLevel--;
      setIndent();
   }
   
   private void setIndent() {
      if (indentLevel <= 0) {
         indent = "";
         return;
      }
      int len = indentLevel * indentSize;
      StringBuilder sb = new StringBuilder(len);
      for (int i = 0; i < len; i++) {
         sb.append(' ');
      }
      indent = sb.toString();
   }
}