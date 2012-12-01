package com.apriori.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

// TODO: doc!
public class IndentingPrinter {
   
   private final Writer writer;
   private final int indentSize;
   private int indentLevel;
   private String indent = "";
   private boolean atStartOfNewLine = true;
   
   public IndentingPrinter(Writer writer, int indentSize) {
      this.writer = writer;
      this.indentSize = indentSize;
   }
   
   public IndentingPrinter(OutputStream out, int indentSize) {
      this(new OutputStreamWriter(out), indentSize);
   }
   
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
   
   public void println(String msg, Object... args) throws IOException {
      print(msg + "\n", args);
      flush();
   }
   
   public void flush() throws IOException {
      writer.flush();
   }
   
   public void println() throws IOException {
      println("");
   }
   
   public void indent() {
      indentLevel++;
      setIndent();
   }
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