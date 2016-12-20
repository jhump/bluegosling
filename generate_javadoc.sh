#!/bin/bash
set -e

# Finds all com.bluegosling.* packages (and sub-packages thereof) and generates javadoc for them.
rm -rf javadoc/ 2> /dev/null
mkdir javadoc/
find src -type d \
	| grep com/bluegosling/ \
	| sed s/\\//\\./g | sed s/src\\.// \
	| xargs javadoc -source 8 \
	-classpath bin/:lib/commons-collections-testframework-3.2.1.jar:lib/javax.inject.jar:lib/junit-4.10.jar:lib/guava-19.0.jar:lib/javaparser-core-2.4.0.jar \
	-d javadoc/ -sourcepath src/ -notimestamp \
	-link https://docs.oracle.com/javase/8/docs/api/ \
	-link http://docs.guava-libraries.googlecode.com/git/javadoc \
	-overview src/com/bluegosling/overview.html
