# Generated BUILD file for src/com/bluegosling/buildgen
java_library(name='lib',
  sources=globs('*.java'),
  dependencies=['3rdparty:guava',
      '3rdparty:javaparser',
      '3rdparty:jsr305',
      'src/com/bluegosling/concurrent',
      'src/com/bluegosling/concurrent/fluent',
      'src/com/bluegosling/tuples'])

jvm_binary(name='buildgen',
  dependencies=[':lib'],
  main='com.bluegosling.buildgen.BuildGen')
