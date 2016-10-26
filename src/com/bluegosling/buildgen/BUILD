# Generated BUILD file for src/com/bluegosling/buildgen
java_library(name='lib',
  sources=globs('*.java'),
  dependencies=['src/com/bluegosling/concurrent',
      'src/com/bluegosling/concurrent/fluent',
      'src/com/bluegosling/tuples',
      '3rdparty:guava',
      '3rdparty:jsr305',
      '3rdparty:javaparser'])
jvm_binary(name='buildgen',
  dependencies=[':lib'],  main='com.bluegosling.buildgen.BuildGen')