#!/bin/bash
set -e
cd "$(dirname $0)"
./pants run src/com/bluegosling/buildgen -- --settings buildgen.properties src test
