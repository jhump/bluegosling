#!/bin/bash

# Finds all com.apriori.* packages (and sub-packages thereof) and generates javadoc for them.
# TODO: remove the filter that excludes com.apriori.testing (it currently causes bogus message about syntax error). Might be fixed by moving to JDK 7 version of javadoc.
# TODO: Move to JDK7 version of javadoc to get new-and-improved javadoc stylesheet/template.
rm -rf javadoc/ 2> /dev/null
mkdir javadoc/
find src -type d | grep com/apriori/ | sed s/\\//\\./g | sed s/src\\.// | grep -v com.apriori.testing | xargs javadoc -source 6 -classpath bin/:lib/javax.inject.jar:lib/junit-4.10.jar -d javadoc/ -sourcepath src/ -notimestamp -link http://docs.oracle.com/javase/6/docs/api/
