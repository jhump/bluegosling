package com.bluegosling.apt.testing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaFileManager.Location;

import com.bluegosling.reflect.caster.Caster;

import javax.tools.StandardLocation;

/**
 * A definition of a test file. A test file exists in the test file system, and also has
 * an analog in the test class's resources. The contents of the resource may be used to
 * either seed the test file system contents (for setting up input files) or to verify
 * the test file system contents (for checking generated output files).
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */ 
class FileDefinition {
   
   /**
    * The folder in which the resource belongs.
    * 
    * @see InputFiles#folder()
    * @see OutputFiles#folder()
    */
   private final String sourceFolder;

   /**
    * The name of the file. This name is relative to the {@linkplain #sourceFolder folder}
    * when finding the associated resource. In the test file system this name will be
    * relative to the {@linkplain #targetLocation target location}.
    * 
    * @see InputFiles#value()
    * @see OutputFiles#value()
    */
   private final String fileName;

   /**
    * The location in the test file system where the file resides.
    * 
    * @see InputFiles#location()
    * @see OutputFiles#location()
    */
   private final Location targetLocation;

   /**
    * Creates a new file definition.
    * 
    * @param sourceFolder the folder in which the resource exists
    * @param fileName the name of the file
    * @param targetLocation the location in the test file system where the file resides
    */
   FileDefinition(String sourceFolder, String fileName, Location targetLocation) {
      this.sourceFolder = sourceFolder;
      this.fileName = fileName;
      this.targetLocation = targetLocation;
   }

   /**
    * The name of the file. The name can contain more than just a simple name and is allowed
    * to contain more than one path element.
    * 
    * @return the file name
    */
   public String getFileName() {
      return fileName;
   }
   
   /**
    * The full path to the resource that represents this file. This path can be used in
    * conjunction with {@link Class#getResourceAsStream(String)} to load resource
    * contents.
    * 
    * @return the full path to the resource
    */
   public String getResourcePath() {
      return TestJavaFileManager.removeMultipleSlashes(sourceFolder + "/" + fileName);
   }
   
   /**
    * The location of the file in the test file system.
    * 
    * @return the location
    */
   public Location getTargetLocation() {
      return targetLocation;
   }
   
   /**
    * The full path to the file in the test file system.
    * 
    * @return the full path to the test file
    */
   public String getTargetPath() {
      return TestJavaFileManager.removeMultipleSlashes(targetLocation.getName() + "/" + fileName);
   }
   
   /**
    * A view of sets of files. This has the same structure as the two annotations used for
    * defining file sets in tests.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @see FilesToProcess
    * @see ValidateGeneratedFiles
    */
   private interface FileSets {
      FileSet[] value();
      boolean incremental();
   }
   
   /**
    * A view of a single set of files. This has the same structure as the two annotations
    * used for describing files in tests.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @see InputFiles
    * @see OutputFiles
    */
   private interface FileSet {
      String[] value();
      String folder();
      StandardLocation location();
   }
   
   /**
    * Gets a list of all {@link FileDefinition}s that represent input files to process for the specified
    * test. The returned list may include duplicate test files in the event that annotations on both
    * the test method and the test class refer to the same test file. In this case, the specification
    * from the method is first and the one from the class is later in the list.
    * 
    * @param testMethod the test method
    * @param testClass the test class
    * @return the list of all {@link FileDefinition}s
    * 
    * @see FilesToProcess
    */
   public static List<FileDefinition> getFilesToProcess(Method testMethod, Class<?> testClass) {
      return getFilesFromAnnotations(testMethod, testClass, FilesToProcess.class);
   }
   
   /**
    * Gets a list of all {@link FileDefinition}s that represent output files to validate for the specified
    * test. The returned list may include duplicate test files in the event that annotations on both
    * the test method and the test class refer to the same test file. In this case, the specification
    * from the method is first and the one from the class is later in the list.
    * 
    * @param testMethod the test method
    * @param testClass the test class
    * @return the list of all {@link FileDefinition}s
    * 
    * @see ValidateGeneratedFiles
    */
   public static List<FileDefinition> getFilesToValidate(Method testMethod, Class<?> testClass) {
      return getFilesFromAnnotations(testMethod, testClass, ValidateGeneratedFiles.class);
   }
   
   /**
    * Extracts a list of files from annotations.
    * 
    * @param testMethod the test method
    * @param testClass the test class
    * @param annotationType the annotation, whose structure must be compatible with that of
    *    {@link FileSets}
    * @return the list of files
    */
   private static List<FileDefinition> getFilesFromAnnotations(Method testMethod, Class<?> testClass,
         Class<? extends Annotation> annotationType) {
      List<FileDefinition> files = new ArrayList<>();
      FileSets forMethod = Caster.castToInterface(testMethod.getAnnotation(annotationType),
            FileSets.class, true);
      boolean includeClass = true;
      if (forMethod != null) {
         if (!forMethod.incremental()) {
            includeClass = false;
         }
         addFilesToProcess(forMethod, files);
      }
      if (includeClass) {
         FileSets forClass = Caster.castToInterface(testClass.getAnnotation(annotationType),
               FileSets.class, true);
         if (forClass != null) {
            addFilesToProcess(forClass, files);
         }
      }
      return Collections.unmodifiableList(files);
   }
   
   /**
    * Appends all files in the specified {@link FileSets} to the specified list.
    * 
    * @param fileSets the sets of files to add
    * @param files the list to which files are added
    */
   private static void addFilesToProcess(FileSets fileSets, List<FileDefinition> files) {
      for (FileSet inputFiles : fileSets.value()) {
         for (String file : inputFiles.value()) {
            files.add(new FileDefinition(inputFiles.folder(), file, inputFiles.location()));
         }
      }
   }
}
