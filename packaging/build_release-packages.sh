#!/bin/bash

# --------------------------
# configuration
# --------------------------

# temporary directory for this script
TMP_PATH="/tmp/fsdRLgT24fDM7YqmfFlg85gLVf6aLGA6G"

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
SERVER_WHITE_LIST=(
	"servers/build.xml" ""
	"servers/bin" "bin"
	"servers/config" "config"
	"servers/init.d-scripts" "init.d-scripts"
	"servers/lib" "lib"
	"servers/man" "man"
	"servers/dist" "dist"
	"packaging/generate_uuid" "packaging"
)

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
CLIENT_WHITE_LIST=(
	"client/generate_src_and_proj.bat" ""
	"client/include" "include"
	"client/scons.py" ""
	"client/SConstruct" ""
	"client/proj" "proj"
	"client/share" "share"
	"client/src" "src"
	"client/bin" "bin"
	"client/man" "man"
	"servers/config/default_dir" "config/default_dir"
)

# black list for files/dirs which should NEVER be copied
SERVER_BLACK_LIST=(
	"servers/bin/generate_xtreemfs_java.py"
	"servers/config/*test"
)

# black list for files/dirs which should NEVER be copied
CLIENT_BLACK_LIST=(
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

# client package
cleanup_client() {
	CLEANUP_PATH=$1
	echo "cleanup client"

	# copy to temporary dir
	create_dir $CLEANUP_PATH
	cp -a $XTREEMFS_HOME_DIR/* "$CLEANUP_PATH"

	make -C "$CLEANUP_PATH" distclean
}

build_client_package() {
	PACKAGE_PATH="$TMP_PATH/$CLIENT_PACKAGE_NAME"
	CLEANUP_PATH="$TMP_PATH/$CLIENT_PACKAGE_NAME""_compile"

	cleanup_client $CLEANUP_PATH

	echo "build client package"

	# copy to temporary dir
	create_dir $PACKAGE_PATH

	# delete all from black-list in temporary dir
	delete_client_black_list $CLEANUP_PATH

	# copy white-list to temporary dir
	copy_client_white_list $CLEANUP_PATH $PACKAGE_PATH

	# delete all .svn directories
	find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf

	# create archiv
	#tar -cjf "$CLIENT_PACKAGE_NAME.tar.bz2" -C $TMP_PATH $CLIENT_PACKAGE_NAME
	tar -czf "$CLIENT_PACKAGE_NAME.tar.gz" -C $TMP_PATH $CLIENT_PACKAGE_NAME
}

# server package
compile_server() {
	COMPILE_PATH=$1
	echo "compile server"

	create_dir $COMPILE_PATH

	# copy to temporary dir
	cp -a $XTREEMFS_HOME_DIR/* "$COMPILE_PATH"

	# compile
	ant jar -q -buildfile "$COMPILE_PATH/servers/build.xml"
}

build_server_package() {
	PACKAGE_PATH="$TMP_PATH/$SERVER_PACKAGE_NAME"
	COMPILE_PATH="$TMP_PATH/$SERVER_PACKAGE_NAME""_compile"

	compile_server $COMPILE_PATH

	echo "build server package"

	create_dir $PACKAGE_PATH

	# delete UUID from config-files
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/servers/config/dirconfig.properties > $COMPILE_PATH/servers/config/dirconfig.properties_new
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/servers/config/mrcconfig.properties > $COMPILE_PATH/servers/config/mrcconfig.properties_new
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/servers/config/osdconfig.properties > $COMPILE_PATH/servers/config/osdconfig.properties_new
	mv $COMPILE_PATH/servers/config/dirconfig.properties_new $COMPILE_PATH/servers/config/dirconfig.properties
	mv $COMPILE_PATH/servers/config/mrcconfig.properties_new $COMPILE_PATH/servers/config/mrcconfig.properties
	mv $COMPILE_PATH/servers/config/osdconfig.properties_new $COMPILE_PATH/servers/config/osdconfig.properties

	# delete all from black-list in temporary dir
	delete_server_black_list $COMPILE_PATH

	# copy white-list to temporary dir
	copy_server_white_list $COMPILE_PATH $PACKAGE_PATH

	# delete all .svn directories
	find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf

	# create archiv
	#tar -cjf "$SERVER_PACKAGE_NAME.tar.bz2" -C $TMP_PATH $SERVER_PACKAGE_NAME
	tar -czf "$SERVER_PACKAGE_NAME.tar.gz" -C $TMP_PATH $SERVER_PACKAGE_NAME
}

function copy_server_white_list() {
	SRC_PATH=$1
	DEST_PATH=$2

	for (( i = 0 ; i < ${#SERVER_WHITE_LIST[@]} ; i=i+2 ))
	do
		SRC="$SRC_PATH/${SERVER_WHITE_LIST[$i]}"
		# if directory doesn't exist, create it for copying file
		if [ -d $SRC_PATH/${SERVER_WHITE_LIST[i]} ]; then
			mkdir -p "$DEST_PATH/${SERVER_WHITE_LIST[i+1]}"
			SRC="$SRC/*"
		else
			TMP_DIRNAME=${SERVER_WHITE_LIST[i+1]%/*}
			mkdir -p "$DEST_PATH/$TMP_DIRNAME"
		fi
		cp -a $SRC "$DEST_PATH/${SERVER_WHITE_LIST[$i+1]}"
	done
}

function copy_client_white_list() {
	SRC_PATH=$1
	DEST_PATH=$2

	for (( i = 0 ; i < ${#CLIENT_WHITE_LIST[@]} ; i=i+2 ))
	do
		SRC="$SRC_PATH/${CLIENT_WHITE_LIST[$i]}"
		# if directory doesn't exist, create it for copying file
		if [ -d $SRC_PATH/${CLIENT_WHITE_LIST[i]} ]; then
			mkdir -p "$DEST_PATH/${CLIENT_WHITE_LIST[i+1]}"
			SRC="$SRC/*"
		else
			TMP_DIRNAME=${CLIENT_WHITE_LIST[i+1]%/*}
			mkdir -p "$DEST_PATH/$TMP_DIRNAME"
		fi
		cp -a $SRC "$DEST_PATH/${CLIENT_WHITE_LIST[$i+1]}"
	done
}

function delete_server_black_list() {
	SRC_PATH=$1

	for (( i = 0 ; i < ${#SERVER_BLACK_LIST[@]} ; i++ ))
	do
		rm -Rf "$SRC_PATH/${SERVER_BLACK_LIST[i]}"
	done
}

function delete_client_black_list() {
	SRC_PATH=$1

	for (( i = 0 ; i < ${#CLIENT_BLACK_LIST[@]} ; i++ ))
	do
		rm -Rf "$SRC_PATH/${CLIENT_BLACK_LIST[i]}"
	done
}

function create_dir() {
	CREATE_DIR=$1
	if [ -d "$CREATE_DIR" ]; then
		rm -Rf $CREATE_DIR
	fi
	mkdir -p $CREATE_DIR
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
if [ ! -d "$XTREEMFS_HOME_DIR/servers" -o ! -d "$XTREEMFS_HOME_DIR/client" ] ;
then
	echo "directory is not the xtreemfs home directory"
	usage
fi

CLIENT_PACKAGE_NAME="XtreemFS-client-$VERSION"
SERVER_PACKAGE_NAME="XtreemFS-server-$VERSION"

# create temporary directory
create_dir $TMP_PATH

# build packages
build_client_package
build_server_package

# delete temporary directory
rm -Rf $TMP_PATH
