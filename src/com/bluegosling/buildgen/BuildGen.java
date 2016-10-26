package com.bluegosling.buildgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.bluegosling.buildgen.SourceDependencyAnalysis.Analyzer;
import com.bluegosling.buildgen.SourceDependencyAnalysis.JavaPackage;
import com.bluegosling.buildgen.SourceDependencyAnalysis.PackageDirectory;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.io.Files;

public class BuildGen {
   private static final Predicate<File> BUILD_FILE_FILTER =
         f -> f.isFile() && f.getName().equalsIgnoreCase("BUILD");

         private final Path root;
   private final Set<File> searchPaths;
   private final Settings settings;
   
   public BuildGen(File root) {
      this(root, (Settings) null);
   }
   
   public BuildGen(File root, File... searchPaths) {
      this(root, null, searchPaths);
   }
   
   public BuildGen(File root, Iterable<File> searchPaths) {
      this(root, null, searchPaths);
   }

   public BuildGen(File root, Settings settings) {
      this(root, settings, ImmutableSet.of());
   }

   public BuildGen(File root, Settings settings, File... searchPaths) {
      this(root, settings, Arrays.asList(searchPaths));
   }

   public BuildGen(File root, Settings settings, Iterable<File> searchPaths) {
      this.root = root.toPath();
      this.settings = settings == null ? new Settings.Builder().build() : settings;
      Set<File> paths = ImmutableSet.copyOf(searchPaths);
      this.searchPaths = paths.isEmpty() ? ImmutableSet.of(root) : paths;
   }

   public Model generateModel() {
      SourceDependencyAnalysis analysis = new Analyzer(searchPaths).analyze();
      Set<List<PackageDirectory>> cycles = analysis.getPackageCycles();
      if (!cycles.isEmpty()) {
         throw new BuildGenException(
               "Cannot generate build when there are dependency cycles in packages:\n  "
               + cycles.stream().map(Object::toString).collect(Collectors.joining("\n  ")));
      }
      Model model = new Model(searchPaths);
      for (PackageDirectory dir : analysis.getPackages()) {
         Set<String> deps = new TreeSet<>();
         for (PackageDirectory dep : analysis.getPackageDependencies(dir)) {
            if (dep.equals(dir)) {
               continue;
            }
            String depTarget = asTarget(dep);
            deps.add(depTarget);
            deps.addAll(settings.getImpliedExtraDepsFor(depTarget));
         }
         for (JavaPackage dep : analysis.getExternalPackageDependencies(dir)) {
            String target = settings.get3rdPartyTargetForPackage(dep.getPackageName());
            if (target == null) {
               if (dep.getPackageName().startsWith("java.")
                     || dep.getPackageName().startsWith("javax.")) {
                  continue;
               }
               throw new BuildGenException("No configuration for external package "
                     + dep.getPackageName());
            }
            deps.add(target);
            deps.addAll(settings.getImpliedExtraDepsFor(target));
         }
         String target = asTarget(dir);
         String expandedTarget = target + ":" + dir.getName();
         String mainClass = settings.getMainClassFor(target);
         if (mainClass == null) {
            mainClass = settings.getMainClassFor(expandedTarget);
         }
         TargetType type;
         if (mainClass != null) {
            type = TargetType.JAVA_BINARY;
         } else if (settings.isInTestPath(dir)) {
            type = TargetType.JAVA_TESTS;
         } else {
            type = TargetType.JAVA_LIBRARY;
         }
         model.buildTargetsByPath.put(dir, new Target(dir, type, deps, mainClass));
      }
      return model;
   }
   
   private String asTarget(File dir) {
      return root.relativize(dir.toPath().toAbsolutePath()).normalize().toString();
   }
   
   public static void main(String args[]) throws IOException {
      String settingsFile = null;
      File root = new File(".").getCanonicalFile();
      while (args.length > 0 && (args[0].equals("--settings") || args[0].equals("--root"))) {
         if (args.length < 2) {
            System.err.printf("Option %s requires an argument", args[0]);
            usage();
         }
         if (args[0].equals("--settings")) {
            settingsFile = args[1];
         } else {
            root = new File(args[1]);
         }
         // shift these arguments away
         int newLen = args.length - 2;
         String newArgs[] = new String[newLen];
         System.arraycopy(args, 2, newArgs, 0, newLen);
         args = newArgs;
      }
      
      if (args.length > 0 && args[0].equals("--help")) {
         usage();
      }
      
      if (args.length == 0) {
         args = new String[] { "." };
      }
      
      List<File> paths = Lists.transform(Arrays.asList(args), File::new);
      for (File path : paths) {
         if (!path.exists()) {
            System.err.printf(
                  "The given path, %s, does not exist so could not be analyzed.\n", path);
            System.exit(1);
         }
         if (!path.isDirectory()) {
            System.err.printf(
                  "The given path, %s, is not a directory.\n", path);
            System.exit(1);
         }
         if (!isSubPath(root, path)) {
            System.err.printf(
                  "The given path, %s, is not in the specified root, %s.\n", path, root);
            System.exit(1);
         }
      }
      System.out.println("Analyzing sources...");
      try {
         Settings settings = loadSettings(settingsFile);
         Model model = new BuildGen(root, settings, paths).generateModel();
         System.out.println("Removing existing BUILD files...");
         for (File buildFile : Files.fileTreeTraverser().preOrderTraversal(root)
               .filter(BUILD_FILE_FILTER)) {
            if (!settings.isInIgnorePath(buildFile)) {
               buildFile.delete();
            }
         }
         System.out.println("Writing new BUILD files...");
         for (Entry<File, Target> entry : model.buildTargetsByPath.entrySet()) {
            if (!settings.isInIgnorePath(entry.getKey())) {
               File buildFile = new File(entry.getKey(), "BUILD");
               try (FileWriter writer = new FileWriter(buildFile)) {
                  entry.getValue().writeBuildFile(writer);
               }
            }
         }
         System.out.println("Done!");
      } catch (BuildGenException e) {
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
   
   private static void usage() {
      System.err.printf(
            "Usage:\n"
            + "  %s [--settings settings-file] [--root root-dir] [directory...]\n"
            + "All Java source files contained in all given directories, are analyzed. If not\n"
            + "directories are specified, the current directory is analyzed. All build targets\n"
            + "will use paths relative to the given root directory. If no root directory is given\n"
            + "then the current directory is assumed. The given settings file is used to control"
            + "some of the BUILD file generation, including mapping unknown packages to external"
            + "(e.g. 3rd party) library targets and distinguishing test from source folders (e.g."
            + "folders where targets need to be test targets, not library or binary targets). Any\n"
            + "BUILD files in the directories will be removed and replaced by newly generated\n"
            + "files. If any dependencies cannot be resolved or if any package dependency\n"
            + "cycles are found, the process fails. When the process fails, existing BUILD\n"
            + "files will be left in tact.\n",
            SourceDependencyAnalysis.class.getName());
      System.exit(1);
   }
   
   private static boolean isSubPath(File parent, File possibleChild) {
      return possibleChild.toPath().toAbsolutePath().normalize()
            .startsWith(parent.toPath().toAbsolutePath().normalize());
   }
   
   private static Settings loadSettings(String settingsFile) throws IOException {
      Settings.Builder builder = new Settings.Builder();
      if (settingsFile == null) {
         File f = new File("buildgen.properties");
         if (!f.exists()) {
            return builder.build();
         }
         settingsFile = f.getAbsolutePath();
      }
      Properties props = new Properties();
      props.load(new FileInputStream(settingsFile));
      for (Entry<Object, Object> entry : props.entrySet()) {
         String key = entry.getKey().toString();
         String value = entry.getValue().toString();
         if (key.startsWith("3rd-party-package.")) {
            String packageName = key.substring("3rd-party-package.".length());
            builder.add3rdPartyPackage(packageName, value.trim());
         } else if (key.startsWith("binary-target.")) {
            String binaryTargetName = key.substring("binary-target.".length());
            builder.addJavaBinary(binaryTargetName, value.trim());
         } else if (key.startsWith("extra-deps.")) {
            String depWithImpliedExtras = key.substring("extra-deps.".length());
            for (String extra : value.split(",")) {
               builder.addImpliedDep(depWithImpliedExtras, extra.trim());
            }
         } else if (key.equals("test-paths")) {
            for (String path : value.split(",")) {
               builder.addTestPath(path.trim());
            }
         } else if (key.equals("ignore-paths")) {
            for (String path : value.split(",")) {
               builder.addIgnorePath(path.trim());
            }
         } else {
            throw new BuildGenException("Invalid key in settings file %s:\n"
                  + "%s is not a recognized setting name", settingsFile, key);
         }
      }
      return builder.build();
   }
   
   public static class Model {
      final Set<File> paths;
      final Map<File, Target> buildTargetsByPath;
      
      Model(Set<File> paths) {
         this.paths = Collections.unmodifiableSet(paths);
         this.buildTargetsByPath = new TreeMap<>();
      }
      
      public Map<File, Target> buildTargetsByPath() {
         return Collections.unmodifiableMap(buildTargetsByPath);
      }
   }
   
   public enum TargetType {
      JAVA_LIBRARY,
      JAVA_BINARY,
      JAVA_TESTS
   }
   
   public static class Target {
      public final File path;
      public final TargetType type;
      public final String mainClass;
      public final Set<String> deps;
      
      Target(File path, TargetType type, Set<String> deps, String mainClass) {
         assert (type == TargetType.JAVA_BINARY) == (mainClass != null);
         this.path = path;
         this.type = type;
         this.deps = Collections.unmodifiableSet(deps);
         this.mainClass = mainClass;
      }

      public void writeBuildFile(FileWriter writer) {
         try (Formatter fmt = new Formatter(writer)) {
            if (type == TargetType.JAVA_BINARY) {
               writeTarget(fmt, "lib", TargetType.JAVA_LIBRARY);
               fmt.format("\n"
                     + "jvm_binary(name='%s',\n"
                     + "  dependencies=[':lib'],\n"
                     + "  main='%s')\n",
                     path.getName(),
                     mainClass);
            } else {
               writeTarget(fmt, path.getName(), type);
            }
         }
      }
      
      private void writeTarget(Formatter fmt, String targetName, TargetType type) {
         fmt.format("# Generated BUILD file for %s\n"
               + "%s(name='%s',\n"
               + "  sources=globs('*.java'),\n"
               + "  dependencies=[%s])\n",
               path,
               type.name().toLowerCase(),
               targetName,
               deps.stream()
                     .map(s -> "'" + s + "'")
                     .collect(Collectors.joining(",\n      ")));
      }
   }
   
   public static class Settings {
      final SetMultimap<String, String> impliedExtraDeps;
      final PackageTrieMap<String> packageTo3rdPartyTarget;
      final Map<String, String> binaryTargetToMainClass;
      final Set<File> testPaths;
      final Set<File> ignorePaths;
      
      Settings(Builder builder) {
         this.impliedExtraDeps = Multimaps.unmodifiableSetMultimap(builder.impliedExtraDeps);
         this.packageTo3rdPartyTarget = builder.packageTo3rdPartyTarget.asUnmodifiable();
         this.binaryTargetToMainClass =
               Collections.unmodifiableMap(builder.binaryTargetToMainClass);
         this.testPaths = Collections.unmodifiableSet(builder.testPaths);
         this.ignorePaths = Collections.unmodifiableSet(builder.ignorePaths);
      }
      
      public boolean isInTestPath(File path) {
         return isInPath(testPaths, path);
      }

      public boolean isInIgnorePath(File path) {
         return isInPath(ignorePaths, path);
      }
      
      private boolean isInPath(Set<File> paths, File query) {
         for (File p : paths) {
            if (isSubPath(p, query)) {
               // given file is inside the test path
               return true;
            }
         }
         return false;
      }

      public Set<String> getImpliedExtraDepsFor(String dep) {
         return impliedExtraDeps.get(dep);
      }
      
      public String getMainClassFor(String binaryTarget) {
         return binaryTargetToMainClass.get(binaryTarget);
      }
      
      public String get3rdPartyTargetForPackage(String packageName) {
         return packageTo3rdPartyTarget.get(packageName);
      }
      
      public static class Builder {
         SetMultimap<String, String> impliedExtraDeps = HashMultimap.create();
         PackageTrieMap<String> packageTo3rdPartyTarget = new PackageTrieMap<>();
         Map<String, String> binaryTargetToMainClass = new HashMap<>();
         Set<File> testPaths = new HashSet<>();
         Set<File> ignorePaths = new HashSet<>();
         boolean reset;
         
         private void maybeReset() {
            if (reset) {
               impliedExtraDeps = HashMultimap.create(impliedExtraDeps);
               packageTo3rdPartyTarget = packageTo3rdPartyTarget.deepCopy();
               binaryTargetToMainClass = new HashMap<>(binaryTargetToMainClass);
               testPaths = new HashSet<>(testPaths);
               ignorePaths = new HashSet<>(ignorePaths);
               reset = false;
            }
         }

         public Builder addTestPath(String path) {
            return addTestPath(new File(path));
         }

         public Builder addTestPath(File path) {
            maybeReset();
            testPaths.add(path);
            return this;
         }
         
         public Builder addIgnorePath(String path) {
            return addIgnorePath(new File(path));
         }
         
         public Builder addIgnorePath(File path) {
            maybeReset();
            ignorePaths.add(path);
            return this;
         }

         public Builder addImpliedDep(String dep, String impliedExtraDep) {
            maybeReset();
            impliedExtraDeps.put(dep, impliedExtraDep);
            return this;
         }
         
         public Builder add3rdPartyPackage(String packageName, String thirdPartyTargetName) {
            maybeReset();
            packageTo3rdPartyTarget.put(packageName, thirdPartyTargetName);
            return this;
         }
         
         public Builder addJavaBinary(String targetName, String mainClass) {
            maybeReset();
            binaryTargetToMainClass.put(targetName, mainClass);
            return this;
         }
         
         public Settings build() {
            Settings ret = new Settings(this);
            reset = true;
            return ret;
         }
      }
   }
}
