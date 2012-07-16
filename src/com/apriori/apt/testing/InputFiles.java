package com.apriori.apt.testing;

import java.lang.annotation.Target;

import javax.tools.StandardLocation;

/**
 * A set of input files that are loaded from resources. These are used to seed the
 * in-memory file system with contents.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see FilesToProcess
 */
@Target({})
public @interface InputFiles {
   /**
    * The names of the files/resources, relative to {@link #folder()}.
    */
   String[] value();
   
   /**
    * The name of a folder that contains the files to load. For a given file name
    * (the {@code i}<sup>th</sup> file in the array returned by {@link #value()},
    * for example), the following is the path used when trying to load the file
    * as a resource:
    * <pre> folder() + "/" + value()[i]</pre>
    */
   String folder() default "";
   
   /**
    * The location in the in-memory file system where the file will exist.
    * The {@linkplain #folder() folder name} is not used when determining file paths in
    * the in-memory file system. So the resulting path in the file system for a
    * given file name (the {@code i}<sup>th</sup> file in the array returned by 
    * {@link #value()}, for example) is as follows:
    * <pre> location().getName() + "/" + value()[i]</pre>
    */
   StandardLocation location() default StandardLocation.SOURCE_PATH;
}
