package com.apriori.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

// TODO: javadoc
public final class Streams {
   private Streams() {}
   
   public static long copyBytes(InputStream in, OutputStream out) throws IOException {
      byte buffer[] = new byte[8192];
      long totalBytesRead = 0;
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
         totalBytesRead += bytesRead;
         out.write(buffer, 0, bytesRead);
      }
      return totalBytesRead;
   }
   
   public static long copyText(Readable in, Appendable out) throws IOException {
      CharBuffer buffer = CharBuffer.allocate(8192);
      long totalCharsRead = 0;
      int charsRead;
      while ((charsRead = in.read(buffer)) != -1) {
         totalCharsRead += charsRead;
         buffer.flip();
         out.append(buffer);
         buffer.clear();
      }
      return totalCharsRead;
   }
   
   public static String toString(Readable in) throws IOException {
      StringWriter out = new StringWriter();
      copyText(in, out);
      return out.toString();
   }
   
   public static String toString(InputStream in, Charset charset) throws IOException {
      return new String(toByteArray(in), charset);
   }
   
   public static byte[] toByteArray(Readable in, Charset charset) throws IOException {
      return toString(in).getBytes(charset);
   }
   
   public static byte[] toByteArray(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      copyBytes(in, out);
      return out.toByteArray();
   }
}
