package com.apriori.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Utility methods for working with streams. There are helper methods for binary streams
 * ({@link InputStream} and {@link OutputStream}) and for text streams (
 * {@link java.util.Reader}/{@link Readable} and {@link java.util.Writer}/{@link Appendable}).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Streams {
   private Streams() {}

   /**
    * Copies binary data by reading from one stream and writing to another.
    * 
    * @param in the source of data
    * @param out the target to which data is copied
    * @return the total number of bytes copied
    * @throws IOException if an exception is thrown trying to read or write from the streams
    */
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
   
   /**
    * Copies text data by reading from one stream and writing to another.
    * 
    * @param in the source of data
    * @param out the target to which data is copied
    * @return the total number of characters copied
    * @throws IOException if an exception is thrown trying to read or write from the streams
    */
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

   /**
    * Fully consumes the specified stream and returns the results as a string.
    * 
    * @param in the stream to read
    * @return the contents of the stream as a string
    * @throws IOException if an exception is thrown trying to read from the stream
    */
   public static String toString(Readable in) throws IOException {
      StringWriter out = new StringWriter();
      copyText(in, out);
      return out.toString();
   }
   
   /**
    * Fully consumes the specified stream and returns the results as a string.
    * 
    * @param in the stream to read
    * @param charset the character set used to decode binary data as characters
    * @return the contents of the stream as a string
    * @throws IOException if an exception is thrown trying to read from the stream
    */
   public static String toString(InputStream in, Charset charset) throws IOException {
      return new String(toByteArray(in), charset);
   }
   
   /**
    * Fully consumes the specified stream and returns the results as a byte array.
    * 
    * @param in the stream to read
    * @param charset the character set used to encode text as bytes
    * @return the contents of the stream as an array of bytes
    * @throws IOException if an exception is thrown trying to read from the stream
    */
   public static byte[] toByteArray(Readable in, Charset charset) throws IOException {
      return toString(in).getBytes(charset);
   }
   
   /**
    * Fully consumes the specified stream and returns the results as a byte array.
    * 
    * @param in the stream to read
    * @return the contents of the stream as an array of bytes
    * @throws IOException if an exception is thrown trying to read from the stream
    */
   public static byte[] toByteArray(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      copyBytes(in, out);
      return out.toByteArray();
   }
}
