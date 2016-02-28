#!/bin/bash

# Finds all com.bluegosling.* packages (and sub-packages thereof) and generates javadoc for them.
rm -rf javadoc/ 2> /dev/null
mkdir javadoc/
find src -type d | grep com/bluegosling/ | grep -v com/bluegosling/apt/util | sed s/\\//\\./g | sed s/src\\.// | xargs javadoc -source 8 -classpath bin/:lib/javax.inject.jar:lib/junit-4.10.jar -d javadoc/ -sourcepath src/ -notimestamp -link https://docs.oracle.com/javase/8/docs/api/ -overview src/com/bluegosling/overview.html
