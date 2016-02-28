package com.bluegosling.apt.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;

import javax.tools.FileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * A simple in-memory file. This is used by {@link TestJavaFileManager} to provide an in-memory file
 * system for use in unit tests for annotation processors.
 * 
 * <p>When writing contents to a file, calling code <em>must</em> close and/or flush the output
 * stream for changes to actually be "written" to the in-memory contents. Opening an output stream
 * for a file effectively locks it, so only one output stream can be opened at any given time for a
 * file. Open input streams read consistent data. So if the file is changed while one thread is
 * reading its contents, the reading thread will not see the updates (unless it were to close and
 * re-open a new input stream for the file).
 * 
 * <p>The in-memory contents of the file are stored as a byte array, not a character array. Using
 * the {@link FileObject} interface to open a reader or writer for the file assume UTF-8 character
 * set for encoding and decoding.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TestJavaFileObject extends SimpleJavaFileObject {
   /**
    * The default character set when defining file contents as text: UTF-8.
    */
   static final Charset DEFAULT_CHARSET = TestJavaFileManager.DEFAULT_CHARSET;

   private final TestJavaFileManager fileManager;
   private final String filePath;
   private final boolean readOnly;
   private byte fileContents[];
   private long lastModified;
   private FileObjectOutputStream outputStream;
   private volatile boolean deleted;
   
   TestJavaFileObject(TestJavaFileManager fileManager, String filePath) {
      this(fileManager, filePath, false);
   }
   
   TestJavaFileObject(TestJavaFileManager fileManager, String filePath, boolean readOnly) {
      this(fileManager, filePath, new byte[0], readOnly);
   }
   
   TestJavaFileObject(TestJavaFileManager fileManager, String filePath, String contents) {
      this(fileManager, filePath, contents, DEFAULT_CHARSET);
   }
   
   TestJavaFileObject(TestJavaFileManager fileManager, String filePath, String contents,
         Charset charset) {
      this(fileManager, filePath, contents.getBytes(charset));
   }
   
   TestJavaFileObject(TestJavaFileManager fileManager, String filePath, byte fileContents[]) {
      this(fileManager, filePath, fileContents, false);
   }

   private TestJavaFileObject(TestJavaFileManager fileManager, String filePath, byte fileContents[],
         boolean readOnly) {
      super(buildUri(filePath), determineKind(filePath));
      this.fileManager = fileManager;
      this.filePath = filePath;
      this.readOnly = readOnly;
      this.fileContents = fileContents.length > 0 ? fileContents.clone() : fileContents;
      this.lastModified = System.currentTimeMillis();
   }
   
   static URI buildUri(String filePath) {
      return URI.create("string://" + filePath);
   }
   
   static Kind determineKind(String filePath) {
      for (Kind candidate : Kind.values()) {
         if (candidate != Kind.OTHER) {
            if (filePath.endsWith(candidate.extension)) {
               return candidate;
            }
         }
      }
      return Kind.OTHER;
   }
   
   synchronized void setFileContents(byte contents[]) {
      fileContents = contents;
      lastModified = System.currentTimeMillis();
   }
   
   boolean isDeleted() {
      return deleted;
   }
   
   TestJavaFileManager getFileManager() {
      return fileManager;
   }
   
   @Override
   public synchronized boolean delete() {
      if (readOnly) {
         return false;
      }
      boolean ret = fileManager.delete(filePath);
      deleted = true;
      if (outputStream != null) {
         closeOutputStream();
      }
      return ret;
   }
   
   @Override
   public String getName() {
      return filePath;
   }
   
   @Override
   public long getLastModified() {
      return lastModified;
   }

   @Override
   public synchronized String getCharContent(boolean ignoreEncodingErrors) {
      return new String(fileContents, DEFAULT_CHARSET);
   }
   
   /**
    * Gets the binary contents of the file. This is similar to {@link #getCharContent(boolean)}
    * except that it returns bytes instead of characters.
    * 
    * @return the contents of the file as an array of bytes
    */
   public synchronized byte[] getByteContents() {
      return fileContents.clone();
   }
   
   @Override
   public synchronized InputStream openInputStream() {
      return new ByteArrayInputStream(fileContents);
   }

   @Override
   public synchronized Reader openReader(boolean ignoreEncodingErrors) {
      return new InputStreamReader(openInputStream(), DEFAULT_CHARSET);
   }

   public synchronized Reader openReader(Charset charset) {
      return new InputStreamReader(openInputStream(), charset);
   }

   @Override
   public synchronized OutputStream openOutputStream() throws IOException {
      if (readOnly) {
         throw new IOException("File " + filePath + " is read-only");
      }
      if (outputStream != null) {
         throw new IOException("File " + filePath + " is already opened for writing");
      }
      int defaultSize = fileContents.length > 100 ? fileContents.length : 100;
      outputStream = new FileObjectOutputStream(defaultSize);
      fileManager.openedForWriting(this);
      return outputStream;
   }
   
   @Override
   public synchronized Writer openWriter() throws IOException {
      return new OutputStreamWriter(openOutputStream(), DEFAULT_CHARSET);
   }

   public synchronized Writer openWriter(Charset charset) throws IOException {
      return new OutputStreamWriter(openOutputStream(), charset);
   }

   synchronized void closeOutputStream() {
      outputStream = null;
      fileManager.closedForWriting(this);
   }
   
   synchronized void flush() throws IOException {
      if (outputStream != null) {
         outputStream.flush();
      }
   }
   
   /**
    * An output stream that flushes the written bytes to the {@link TestJavaFileObject}'s in-memory
    * file contents on {@linkplain #flush() flush} and {@linkplain #close() close} operations.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class FileObjectOutputStream extends OutputStream {
      private final ByteArrayOutputStream out; 
      private boolean closed;
      
      FileObjectOutputStream(int initialCapacity) {
         out = new ByteArrayOutputStream(initialCapacity);
      }
      
      private void checkState() throws IOException {
         if (isDeleted()) {
            throw new IOException("File has been removed");
         } else if (closed) {
            throw new IOException("Stream has been closed");
         }
      }
      
      @Override
      public synchronized void flush() throws IOException {
         checkState();
         setFileContents(out.toByteArray());
      }
      
      @Override
      public synchronized void close() throws IOException {
         if (!closed) {
            checkState();
            setFileContents(out.toByteArray());
            closed = true;
            closeOutputStream();
         }
      }
      
      @Override
      public synchronized void write(int b) throws IOException {
         checkState();
         out.write(b);
      }
      
      @Override
      public synchronized void write(byte bytes[]) throws IOException {
         checkState();
         out.write(bytes);
      }
      
      @Override
      public synchronized void write(byte bytes[], int offset, int length) throws IOException {
         checkState();
         out.write(bytes, offset, length);
      }
   }
}
