#!/bin/bash

set -e

XTREEMFS_DIR="$1"
TGZ_ARCHIVE="cpp.tgz"

MOUNT="$PWD"

cd "$XTREEMFS_DIR"
tar czf "${MOUNT}/${TGZ_ARCHIVE}" --exclude="build" --exclude="thirdparty" "cpp"
cd "$MOUNT"

tar zxf "$TGZ_ARCHIVE"

find . -name '*.cpp' >/dev/null

grep -R 'test' . >/dev/null