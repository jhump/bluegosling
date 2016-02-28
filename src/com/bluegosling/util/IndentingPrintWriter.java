package com.bluegosling.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A utility for printing indented content. Indentation level can be increased or decreased and all
 * text written will be properly indented by that amount. Any time a newline is written to this
 * printer, the subsequent line is automatically indented.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class IndentingPrintWriter extends PrintWriter {
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
   public IndentingPrintWriter(Writer writer, int indentSize) {
      super(writer, true);
      this.indentSize = indentSize;
   }
   
   /**
    * Creates a new printer that sends all output to the specified stream.
    * 
    * @param out the stream to which printed output is written using UTF8 for character encoding
    * @param indentSize the number of spaces in a single level of indentation
    */
   public IndentingPrintWriter(OutputStream out, int indentSize) {
      this(out, Charset.forName("UTF-8"), indentSize);
   }
   
   /**
    * Creates a new printer that sends all output to the specified stream.
    * 
    * @param out the stream to which printed output is written
    * @param charset the character set used to encode output
    * @param indentSize the number of spaces in a single level of indentation
    */
   public IndentingPrintWriter(OutputStream out, Charset charset, int indentSize) {
      this(new OutputStreamWriter(out, charset), indentSize);
   }

   @Override
   public void println() {
      write('\n');
      flush();
   }
   
   @Override
   public void write(char[] buf, int offs, int len) {
      try {
         int last = offs + len;
         int previousPos = offs;
         int pos;
         while ((pos = indexOf(buf, '\n', previousPos, last)) >= 0) {
            if (atStartOfNewLine) {
               out.write(indent);
            }
            out.write(buf, previousPos, pos - previousPos + 1);
            atStartOfNewLine = true;
            previousPos = pos + 1;
         }
         if (previousPos < last) {
            if (atStartOfNewLine) {
               out.write(indent);
            }
            out.write(buf, previousPos, last - previousPos);
            atStartOfNewLine = false;
         }
      } catch (InterruptedIOException e) {
         Thread.currentThread().interrupt();
         setError();
      } catch (IOException e) {
         setError();
      }
   }

   private int indexOf(char[] buf, char search, int start, int end) {
      for (int i = start; i < end; i++) {
         if (buf[i] == search) {
            return i;
         }
      }
      return -1;
   }
   
   @Override
   public void write(int ch) {
      try {
         if (atStartOfNewLine) {
            out.write(indent);
         }
         out.write(ch);
         atStartOfNewLine = ch == '\n';
      } catch (InterruptedIOException e) {
         Thread.currentThread().interrupt();
         setError();
      } catch (IOException e) {
         setError();
      }
   }

   @Override
   public void write(String s, int offs, int len) {
      try {
         int last = offs + len;
         int previousPos = offs;
         int pos;
         while ((pos = s.indexOf('\n', previousPos)) >= 0 && pos < last) {
            if (atStartOfNewLine) {
               out.write(indent);
            }
            out.write(s, previousPos, pos - previousPos + 1);
            atStartOfNewLine = true;
            previousPos = pos + 1;
         }
         if (previousPos < last) {
            if (atStartOfNewLine) {
               out.write(indent);
            }
            out.write(s, previousPos, last - previousPos);
            atStartOfNewLine = false;
         }
      } catch (InterruptedIOException e) {
         Thread.currentThread().interrupt();
         setError();
      } catch (IOException e) {
         setError();
      }
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
      if (indentLevel == 0) {
         throw new IllegalStateException("already completely unindented");
      }
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
