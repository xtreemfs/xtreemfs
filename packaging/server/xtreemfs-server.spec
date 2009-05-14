Name:           XtreemFS-server
Version:        0.99.0
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          Networking
Summary:        XtreemFS server components (DIR, MRC, OSD)
Source0:        %{name}-%{version}.tar.gz
Requires:       grep
Requires:       sudo

%description
XtreemFS is a distributed, object-based file system. More information can be found at www.xtreemfs.org.

This package contains the XtreemFS server components (DIR, MRC, OSD).
To run the XtreemFS services, a SUN JAVA 6 RUNTIME ENVIROMENT IS REQUIRED! Make sure that Java is installed in /usr/bin, or $JAVA_HOME is set.

%prep
%setup

%build

%install
XTREEMFS_JAR_DIR=$RPM_BUILD_ROOT/usr/share/java/
XTREEMFS_CONFIG_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/
XTREEMFS_INIT_DIR="$RPM_BUILD_ROOT/etc/init.d/"
BIN_DIR="$RPM_BUILD_ROOT/usr/bin"
MAN_DIR="$RPM_BUILD_ROOT/usr/share/man"

export NO_BRP_CHECK_BYTECODE_VERSION=true

# copy jars
mkdir -p $XTREEMFS_JAR_DIR
cp dist/XtreemFS.jar $XTREEMFS_JAR_DIR
cp lib/*.jar $XTREEMFS_JAR_DIR

# copy config files
mkdir -p $XTREEMFS_CONFIG_DIR
cp config/*.properties $XTREEMFS_CONFIG_DIR
# copy generate_uuid script
cp packaging/generate_uuid $XTREEMFS_CONFIG_DIR

# copy init.d files
mkdir -p $XTREEMFS_INIT_DIR
cp init.d-scripts/xtreemfs-* $XTREEMFS_INIT_DIR
chmod a+x $XTREEMFS_INIT_DIR/xtreemfs-*

# copy bins
mkdir -p $BIN_DIR
cp bin/xtfs_* $BIN_DIR

# copy man-pages  
mkdir -p $MAN_DIR  
cp -R man/* $MAN_DIR/

%post
XTREEMFS_CONFIG_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/

# generate UUIDs
if [ -x $XTREEMFS_CONFIG_DIR/generate_uuid ]; then
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/dirconfig.properties
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/mrcconfig.properties
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/osdconfig.properties
else
  echo "UUID can't be generated automatically. Please enter a correct UUID in each config file of a xtreemfs service."
fi

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
/usr/share/java/*.jar
/etc/init.d/xtreemfs-*
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
/etc/xos/
/etc/xos/xtreemfs/*
