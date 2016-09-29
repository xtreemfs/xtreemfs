#!/bin/bash

# --------------------------
# configuration
# --------------------------

# temporary directory for this script
TMP_PATH="$(mktemp -d -t xtreemfs_release_build.XXXXXX)"
if [ -z "$TMP_PATH" ]
then
  echo "Failed to generate temporary directory."
  exit 1
fi

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)

# black list for files/dirs which should NEVER be copied
SOURCE_BLACK_LIST=(
  "contrib/console"
  "contrib/hadoop"
  "doc"
  "etc/xos/xtreemfs/*test"
  "contrib/server-repl-plugin/src/main/resources/config/*-test*"
  "classfiles"
  "cpp/CMakeFiles"
  "java/xtreemfs-pbrpcgen/target"
)

# --------------------------
# begin script
# --------------------------
usage() {
# creates packages for xtreemfs client and server
    cat <<EOF
Usage:
  $0 <build-version> [<xtreemfs home dir>]
EOF
  exit 0
}

# source tarball
build_source_tarball() {
  PACKAGE_PATH="$TMP_PATH/$SOURCE_TARBALL_NAME"

  echo "build source distribution"

  # the OBS build VMs are offline, so copy necessary dependencies to distribution
  mvn --settings $XTREEMFS_HOME_DIR/java/settings.xml \
    --file $XTREEMFS_HOME_DIR/java/pom.xml \
    --activate-profiles xtreemfs-dev \
    --global-settings=$HOME/.m2/settings.xml \
    package org.apache.maven.plugins:maven-dependency-plugin:2.10:go-offline \
    --define maven.repo.local=$(cd $XTREEMFS_HOME_DIR && pwd)/repository \
    --define excludeArtifactIds=maven-deploy-plugin,site-maven-plugin \
    --define skipTests=true
  mvn --settings $XTREEMFS_HOME_DIR/contrib/server-repl-plugin/settings.xml \
    --file $XTREEMFS_HOME_DIR/contrib/server-repl-plugin/pom.xml \
    --activate-profiles xtreemfs-dev \
    --global-settings=$HOME/.m2/settings.xml \
    package org.apache.maven.plugins:maven-dependency-plugin:2.10:go-offline \
    --define maven.repo.local=$(cd $XTREEMFS_HOME_DIR && pwd)/repository \
    --define excludeArtifactIds=maven-deploy-plugin,site-maven-plugin \
    --define skipTests=true

  cleanup_client $PACKAGE_PATH

  # delete all from black-list in temporary dir
  delete_source_black_list $PACKAGE_PATH
  
  # wipe all files and directories from the 'packaging' directory, except
  # for the post install and uuid gen scripts
  mkdir $PACKAGE_PATH/tmp
  mv $PACKAGE_PATH/packaging/generate_uuid $PACKAGE_PATH/tmp
  mv $PACKAGE_PATH/packaging/postinstall_setup.sh $PACKAGE_PATH/tmp
  rm -rf $PACKAGE_PATH/packaging
  mv $PACKAGE_PATH/tmp $PACKAGE_PATH/packaging

  # delete all .svn directories
  delete_svn $PACKAGE_PATH

  # delete UUID from config-files
  grep -v '^uuid\W*=\W*\w\+' $PACKAGE_PATH/etc/xos/xtreemfs/dirconfig.properties > $PACKAGE_PATH/etc/xos/xtreemfs/dirconfig.properties_new
  grep -v '^uuid\W*=\W*\w\+' $PACKAGE_PATH/etc/xos/xtreemfs/mrcconfig.properties > $PACKAGE_PATH/etc/xos/xtreemfs/mrcconfig.properties_new
  grep -v '^uuid\W*=\W*\w\+' $PACKAGE_PATH/etc/xos/xtreemfs/osdconfig.properties > $PACKAGE_PATH/etc/xos/xtreemfs/osdconfig.properties_new
  mv $PACKAGE_PATH/etc/xos/xtreemfs/dirconfig.properties_new $PACKAGE_PATH/etc/xos/xtreemfs/dirconfig.properties
  mv $PACKAGE_PATH/etc/xos/xtreemfs/mrcconfig.properties_new $PACKAGE_PATH/etc/xos/xtreemfs/mrcconfig.properties
  mv $PACKAGE_PATH/etc/xos/xtreemfs/osdconfig.properties_new $PACKAGE_PATH/etc/xos/xtreemfs/osdconfig.properties

  # create archive
  tar -czf "$SOURCE_TARBALL_NAME.tar.gz" -C $TMP_PATH $SOURCE_TARBALL_NAME
}

function delete_source_black_list() {
  SRC_PATH=$1

  for (( i = 0 ; i < ${#SOURCE_BLACK_LIST[@]} ; i++ ))
  do
    rm -Rf $SRC_PATH/${SOURCE_BLACK_LIST[i]}
  done
}

function create_dir() {
  CREATE_DIR=$1
  if [ -d "$CREATE_DIR" ]; then
    rm -Rf $CREATE_DIR
  fi
  mkdir -p $CREATE_DIR
}

function cleanup_client() {
  CLEANUP_PATH=$1
  echo "cleanup client"

  # copy to temporary dir
  create_dir $CLEANUP_PATH
  cp -a $XTREEMFS_HOME_DIR/* "$CLEANUP_PATH"

  make -C "$CLEANUP_PATH" clean
}

function delete_svn() {
  PACKAGE_PATH=$1
  find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf
  find $PACKAGE_PATH -name ".git" -print0 | xargs -0 rm -rf
  find $PACKAGE_PATH -name ".gitignore" -delete
}

function prepare_build_files() {
    cp -r $BUILD_FILES_DIR/xtreemfs $TARGET_DIR/xtreemfs
    cp -r $BUILD_FILES_DIR/xtreemfs $TARGET_DIR/xtreemfs-testing
    find $TARGET_DIR -type f -exec sed -i "s/_VERSION_/$VERSION/g" {} \;
    # write contents of postinstall_setup.sh into the packages' spec files:
    find $TARGET_DIR -type f -exec sed -i -e "/_POSTINSTALL_/r $BUILD_FILES_DIR/../postinstall_setup.sh" -e '/_POSTINSTALL_/d' {} \;

    cp $BUILD_FILES_DIR/meta.xml $TARGET_DIR/
    sed -i "s/_VERSION_/$VERSION/g" $TARGET_DIR/meta.xml
}

function move_packages() {
    cp $SOURCE_TARBALL_NAME.tar.gz $TARGET_DIR/xtreemfs
    cp $SOURCE_TARBALL_NAME.tar.gz $TARGET_DIR/xtreemfs-testing
    mv $SOURCE_TARBALL_NAME.tar.gz $TARGET_DIR
    echo $VERSION > $TARGET_DIR/VER
}

VERSION=
XTREEMFS_HOME_DIR=

# parse command line options
if [ -z "$1" ]; then
  usage
fi
if [ -z "$2" ]; then
  XTREEMFS_HOME_DIR="."
else
  XTREEMFS_HOME_DIR="$2"
fi

VERSION="$1"
if [ ! -d "$XTREEMFS_HOME_DIR/java/xtreemfs-servers" ] ;
then
  echo "directory is not the xtreemfs home directory"
  usage
fi

# Set version in client and server version management files.
set_version_output=$("$XTREEMFS_HOME_DIR/packaging/set_version.sh" -n "$VERSION")
if [ "$(echo "$set_version_output" | cut -d' ' -f1)" != "OK" ]
then
  cat <<EOF
Automatically setting the version was not successful.

Output of $XTREEMFS_HOME_DIR/packaging/set_version.sh -n $VERSION:
$set_version_output
EOF
  exit 1
fi

BUILD_FILES_DIR=$XTREEMFS_HOME_DIR/packaging/build-service
SOURCE_TARBALL_NAME="XtreemFS-$VERSION"

TARGET_DIR=./dist

# create temporary directory
create_dir $TMP_PATH
create_dir $TARGET_DIR

# build packages
prepare_build_files
#build_xtreemos_addons
build_source_tarball
move_packages

# delete temporary directory
rm -Rf $TMP_PATH
