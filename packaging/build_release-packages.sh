#!/bin/bash

# --------------------------
# configuration
# --------------------------

# temporary directory for this script
TMP_PATH="/tmp/fsdRLgT24fDM7YqmfFlg85gLVf6aLGA6G"

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
XOS_ADDONS_WHITE_LIST=(
	"src/servers/xtreemos" "xtreemos"
	"src/policies" "policies"
	"AUTHORS" ""
	"COPYING" ""
)

# black list for files/dirs which should NEVER be copied
XOS_ADDONS_BLACK_LIST=(
)

# black list for files/dirs which should NEVER be copied
SOURCE_BLACK_LIST=(
	"doc"
	"bin/xtfs_snap"
	"etc/xos/xtreemfs/*test"
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

	# create archiv
	tar -czf "$SOURCE_TARBALL_NAME.tar.gz" -C $TMP_PATH $SOURCE_TARBALL_NAME
}

build_xtreemos_addons() {

	PACKAGE_PATH="$TMP_PATH/$XOS_ADDONS_PACKAGE_NAME"
	PACKAGE_PATH_TMP="$TMP_PATH/$XOS_ADDONS_PACKAGE_NAME""_tmp"

	echo "build XtreemOS addons package"
	
	create_dir $PACKAGE_PATH
	create_dir $PACKAGE_PATH_TMP
	cp -a $XTREEMFS_HOME_DIR/* $PACKAGE_PATH_TMP
	
	# replace the scons.py softlink + dependencies
	cp $PACKAGE_PATH_TMP/share/scons.py $PACKAGE_PATH_TMP/src/policies/scons.py
	cp -r $PACKAGE_PATH_TMP/share/scons-local-* $PACKAGE_PATH_TMP/src/policies

	# delete all from black-list in temporary dir
	delete_xos_addons_black_list $PACKAGE_PATH_TMP

	# copy white-list to temporary dir
	copy_xos_addons_white_list $PACKAGE_PATH_TMP $PACKAGE_PATH
	
	# delete all .svn directories
	delete_svn $PACKAGE_PATH

	tar czf "$XOS_ADDONS_PACKAGE_NAME.tar.gz" -C $PACKAGE_PATH .
}

function copy_xos_addons_white_list() {
	SRC_PATH=$1
	DEST_PATH=$2

	for (( i = 0 ; i < ${#XOS_ADDONS_WHITE_LIST[@]} ; i=i+2 ))
	do
		SRC="$SRC_PATH/${XOS_ADDONS_WHITE_LIST[$i]}"
		# if directory doesn't exist, create it for copying file
		if [ -d $SRC_PATH/${XOS_ADDONS_WHITE_LIST[i]} ]; then
			mkdir -p "$DEST_PATH/${XOS_ADDONS_WHITE_LIST[i+1]}"
			SRC="$SRC/*"
		else
			TMP_DIRNAME=${XOS_ADDONS_WHITE_LIST[i+1]%/*}
			mkdir -p "$DEST_PATH/$TMP_DIRNAME"
		fi
		cp -a $SRC "$DEST_PATH/${XOS_ADDONS_WHITE_LIST[$i+1]}"
	done
}

function delete_xos_addons_black_list() {
	SRC_PATH=$1

	for (( i = 0 ; i < ${#XOS_ADDONS_BLACK_LIST[@]} ; i++ ))
	do
		rm -Rf "$SRC_PATH/${XOS_ADDONS_BLACK_LIST[i]}"
	done
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

	make -C "$CLEANUP_PATH" distclean
}

function delete_svn() {
	PACKAGE_PATH=$1
	find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf
}

function prepare_build_files() {
    cp -r $BUILD_FILES_DIR/xtreemfs $TARGET_DIR/xtreemfs
    cp -r $BUILD_FILES_DIR/xtreemfs $TARGET_DIR/xtreemfs-testing
    find $TARGET_DIR -type f -exec sed -i "s/_VERSION_/$VERSION/g" {} \;
    
    cp $BUILD_FILES_DIR/meta.xml $TARGET_DIR/
    sed -i "s/_VERSION_/$VERSION/g" $TARGET_DIR/meta.xml
}

function move_packages() {
    cp $SOURCE_TARBALL_NAME.tar.gz $TARGET_DIR/xtreemfs
    cp $SOURCE_TARBALL_NAME.tar.gz $TARGET_DIR/xtreemfs-testing
    mv $XOS_ADDONS_PACKAGE_NAME.tar.gz $TARGET_DIR
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
if [ ! -d "$XTREEMFS_HOME_DIR/src/servers" ] ;
then
	echo "directory is not the xtreemfs home directory"
	usage
fi

BUILD_FILES_DIR=$XTREEMFS_HOME_DIR/packaging/build-service
XOS_ADDONS_PACKAGE_NAME="XtreemFS-XOS-addons-$VERSION"
SOURCE_TARBALL_NAME="XtreemFS-$VERSION"

TARGET_DIR=./dist

# create temporary directory
create_dir $TMP_PATH
create_dir $TARGET_DIR

# build packages
prepare_build_files
build_xtreemos_addons
build_source_tarball
move_packages

# delete temporary directory
rm -Rf $TMP_PATH
