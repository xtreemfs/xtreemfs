#!/bin/bash

# --------------------------
# configuration
# --------------------------

# temporary directory for this script
TMP_PATH="/tmp/fsdRLgT24fDM7YqmfFlg85gLVf6aLGA6G"

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
SERVER_WHITE_LIST=(
	"bin/xtfs_cleanup" "bin/xtfs_cleanup"
	"bin/xtfs_mrcdbtool" "bin/xtfs_mrcdbtool"
	"bin/xtfs_scrub" "bin/xtfs_scrub"
	"man/man1/xtfs_mrcdbtool.1" "man/man1/xtfs_mrcdbtool.1"
	"man/man1/xtfs_scrub.1" "man/man1/xtfs_scrub.1"
	"man/man1/xtfs_cleanup.1" "man/man1/xtfs_cleanup.1"
	"etc/xos/xtreemfs/default_dir" "config/default_dir"
	"etc/xos/xtreemfs/dirconfig.properties" "config/dirconfig.properties"
	"etc/xos/xtreemfs/mrcconfig.properties" "config/mrcconfig.properties"
	"etc/xos/xtreemfs/osdconfig.properties" "config/osdconfig.properties"
	"etc/init.d" "init.d-scripts"
	"src/servers/lib" "lib"
	"src/servers/dist" "dist"
	"packaging/generate_uuid" "packaging"
	"AUTHORS" ""
	"COPYING" ""
)

# white list for files/dirs which should be copied
# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
CLIENT_WHITE_LIST=(
	"src/client/generate_src_and_proj.bat" ""
	"src/client/include" "include"
	"src/client/proj" "proj"
	"src/client/scons-local-1.2.0.d20090223" "scons-local-1.2.0.d20090223"
	"src/client/scons.py" ""
	"src/client/SConstruct" ""
	"src/client/share" "share"
	"src/client/src" "src"
        "bin/xtfs_umount" ""
	"man/man1/xtfs_lsvol.1" "man/man1/xtfs_lsvol.1"
	"man/man1/xtfs_mkvol.1" "man/man1/xtfs_mkvol.1"
	"man/man1/xtfs_mount.1" "man/man1/xtfs_mount.1"
	"man/man1/xtfs_rmvol.1" "man/man1/xtfs_rmvol.1"
	"man/man1/xtfs_sp.1" "man/man1/xtfs_sp.1"
	"man/man1/xtfs_stat.1" "man/man1/xtfs_stat.1"
	"man/man1/xtfs_umount.1" "man/man1/xtfs_umount.1"
	"etc/xos/xtreemfs/default_dir" "config/default_dir"
	"AUTHORS" ""
	"COPYING" ""
)

# source (relative from XTREEMFS_HOME_DIR) and destination (in package)
XOS_ADDONS_WHITE_LIST=(
	"src/servers/xtreemos" "xtreemos"
	"src/policies" "policies"
	"AUTHORS" ""
	"COPYING" ""
)

# black list for files/dirs which should NEVER be copied
SERVER_BLACK_LIST=(
	"servers/lib/test"
)

# black list for files/dirs which should NEVER be copied
CLIENT_BLACK_LIST=(
)

# black list for files/dirs which should NEVER be copied
XOS_ADDONS_BLACK_LIST=(
)

# black list for files/dirs which should NEVER be copied
SOURCE_BLACK_LIST=(
	"packaging"
	"doc"
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

	cleanup_client $PACKAGE_PATH

	echo "build source distribution"

	# delete all from black-list in temporary dir
	delete_source_black_list $PACKAGE_PATH

	# delete all .svn directories
	find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf

	# create archiv
	tar -czf "$SOURCE_TARBALL_NAME.tar.gz" -C $TMP_PATH $SOURCE_TARBALL_NAME
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
	ant jar -q -buildfile "$COMPILE_PATH/src/servers/build.xml"
}

build_server_package() {
	PACKAGE_PATH="$TMP_PATH/$SERVER_PACKAGE_NAME"
	COMPILE_PATH="$TMP_PATH/$SERVER_PACKAGE_NAME""_compile"

	compile_server $COMPILE_PATH

	echo "build server package"

	create_dir $PACKAGE_PATH

	# delete UUID from config-files
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/etc/xos/xtreemfs/dirconfig.properties > $COMPILE_PATH/etc/xos/xtreemfs/dirconfig.properties_new
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/etc/xos/xtreemfs/mrcconfig.properties > $COMPILE_PATH/etc/xos/xtreemfs/mrcconfig.properties_new
	grep -v '^uuid\W*=\W*\w\+' $COMPILE_PATH/etc/xos/xtreemfs/osdconfig.properties > $COMPILE_PATH/etc/xos/xtreemfs/osdconfig.properties_new
	mv $COMPILE_PATH/etc/xos/xtreemfs/dirconfig.properties_new $COMPILE_PATH/etc/xos/xtreemfs/dirconfig.properties
	mv $COMPILE_PATH/etc/xos/xtreemfs/mrcconfig.properties_new $COMPILE_PATH/etc/xos/xtreemfs/mrcconfig.properties
	mv $COMPILE_PATH/etc/xos/xtreemfs/osdconfig.properties_new $COMPILE_PATH/etc/xos/xtreemfs/osdconfig.properties

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

build_xtreemos_addons() {

	PACKAGE_PATH="$TMP_PATH/$XOS_ADDONS_PACKAGE_NAME"
	PACKAGE_PATH_TMP="$TMP_PATH/$XOS_ADDONS_PACKAGE_NAME""_tmp"

	echo "build XtreemOS addons package"
	
	create_dir $PACKAGE_PATH
	create_dir $PACKAGE_PATH_TMP
	cp -a $XTREEMFS_HOME_DIR/* $PACKAGE_PATH_TMP
	
	# replace the scons.py softlink + dependencies
	rm $PACKAGE_PATH_TMP/src/policies/scons.py
	cp $PACKAGE_PATH_TMP/src/client/scons.py $PACKAGE_PATH_TMP/src/policies/scons.py
	cp -r $PACKAGE_PATH_TMP/src/client/scons-local-* $PACKAGE_PATH_TMP/src/policies

	# delete all from black-list in temporary dir
	delete_xos_addons_black_list $PACKAGE_PATH_TMP

	# copy white-list to temporary dir
	copy_xos_addons_white_list $PACKAGE_PATH_TMP $PACKAGE_PATH
	
	# delete all .svn directories
	find $PACKAGE_PATH -name ".svn" -print0 | xargs -0 rm -rf
	
	exit 1

	tar czf "$XOS_ADDONS_PACKAGE_NAME.tar.gz" -C $PACKAGE_PATH .
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
		rm -Rf "$SRC_PATH/${SOURCE_BLACK_LIST[i]}"
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
if [ ! -d "$XTREEMFS_HOME_DIR/src/servers" -o ! -d "$XTREEMFS_HOME_DIR/src/client" ] ;
then
	echo "directory is not the xtreemfs home directory"
	usage
fi

CLIENT_PACKAGE_NAME="XtreemFS-client-$VERSION"
SERVER_PACKAGE_NAME="XtreemFS-server-$VERSION"
XOS_ADDONS_PACKAGE_NAME="XtreemFS-XOS-addons-$VERSION"
SOURCE_TARBALL_NAME="XtreemFS-$VERSION"

# create temporary directory
create_dir $TMP_PATH

# build packages
build_client_package
build_server_package
build_xtreemos_addons
build_source_tarball

# delete temporary directory
rm -Rf $TMP_PATH
