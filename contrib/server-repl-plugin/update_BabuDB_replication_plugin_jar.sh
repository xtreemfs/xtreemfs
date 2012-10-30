#!/bin/bash

# Copyright (c) 2012 Michael Berlin, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

set -e

trap onexit 1 2 3 15 ERR

function onexit() {
    local exit_status=${1:-$?}
    echo ERROR: Exiting $0 with $exit_status
    exit $exit_status
}

replication_dir_in_babudb_trunk="java/replication"

cat <<EOF
This script updates the binary .jar file which contains the BabuDB replication plugin.

EOF

if [ -z "$BABUDB" ]
then
  known_babudb_dirs="../../../../googlecode-svn-babudb/trunk"
  for dir in $known_babudb_dirs
  do
    if [ -d "$dir" ]
    then
      BABUDB="$dir"
    fi
  done
fi

if [ -z "$BABUDB" ]
then
  echo "The environment variable BABUDB was not set. Please point it to a checkout directory of the SVN trunk of the BabuDB project (svn checkout http://babudb.googlecode.com/svn/trunk/ babudb)."
  exit 1
fi

if [ ! -d "$BABUDB" ]
then
  echo "The environment variable BABUDB does not point to an existing directory. BABUDB = ${BABUDB}"
  exit 1
fi

echo "Updating the .jar files required by the BabuDB replication code first..."
${BABUDB}/${replication_dir_in_babudb_trunk}/update_jar_dependencies.sh
echo "Finished updating the .jar files required by the BabuDB replication code."

echo "compiling BabuDB replication plugion (BabuDB_replication_plugin.jar)"
babudb_replication_buildfile="${BABUDB}/${replication_dir_in_babudb_trunk}/build.xml"
babudb_replication_jar_source="${BABUDB}/${replication_dir_in_babudb_trunk}/../dist/replication/BabuDB_replication_plugin.jar"
babudb_replication_jar_dest=$(dirname "$0")

# ant clean -f "$babudb_replication_buildfile" >/dev/null
ant jar -f "$babudb_replication_buildfile" >/dev/null
cp -a "$babudb_replication_jar_source" "$babudb_replication_jar_dest"

echo "finished compiling BabuDB replication plugion (BabuDB_replication_plugin.jar)"
