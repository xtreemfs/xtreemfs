#!/bin/sh

cd required-libs

./add_to_local_maven_repo.sh

cd ..

mvn eclipse:eclipse
