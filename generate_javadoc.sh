#!/bin/bash
set -e

if [[ -n $(git status -s -uno) ]]; then
	echo "You working tree is dirty. Commit your changes before generating javadoc." >&2
	exit 1
fi

original_branch="$(git branch | grep '^\* ' | awk '{ print $2 }')"
if [[ -z $original_branch ]]; then
	# must have a detached head checked out
	original_branch="$(git rev-parse HEAD)"
fi

git checkout -b regen-javadocs

# Finds all com.bluegosling.* packages (and sub-packages thereof) and generates javadoc for them.
rm -rf javadoc/ 2> /dev/null
mkdir javadoc/
find src -type d \
	| grep com/bluegosling/ \
	| grep -v com/bluegosling/apt/util \
	| sed s/\\//\\./g | sed s/src\\.// \
	| xargs javadoc -source 8 \
	-classpath bin/:lib/commons-collections-testframework-3.2.1.jar:lib/javax.inject.jar:lib/junit-4.10.jar:lib/guava-19.0.jar \
	-d javadoc/ -sourcepath src/ -notimestamp \
	-link https://docs.oracle.com/javase/8/docs/api/ \
	-link http://docs.guava-libraries.googlecode.com/git/javadoc \
	-overview src/com/bluegosling/overview.html

git reset gh-pages
git add javadoc
git commit -m "re-generated javadocs: $(date)"
git checkout gh-pages
git merge --ff-only regen-javadocs
git branch -D regen-javadocs

git checkout -f $original_branch