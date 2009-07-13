Name:           XtreemFS-tools
Version:        0.99.3
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          Networking
Summary:        XtreemFS administration tools
Source0:        %{name}-%{version}.tar.gz

%description
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains XtreemFS administration tools.
To run the tools, a SUN JAVA 6 RUNTIME ENVIROMENT IS REQUIRED! Make sure that Java is installed in /usr/bin, or $JAVA_HOME is set.

%prep
%setup

%build

%install
XTREEMFS_JAR_DIR=$RPM_BUILD_ROOT/usr/share/java/
XTREEMFS_CONFIG_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/
BIN_DIR="$RPM_BUILD_ROOT/usr/bin"
MAN_DIR="$RPM_BUILD_ROOT/usr/share/man"

export NO_BRP_CHECK_BYTECODE_VERSION=true

# copy jars
mkdir -p $XTREEMFS_JAR_DIR
cp dist/XtreemFS.jar $XTREEMFS_JAR_DIR/XtreemFS-tools.jar

# copy config files
#mkdir -p $XTREEMFS_CONFIG_DIR
#cp config/default_dir $XTREEMFS_CONFIG_DIR

# copy bins
mkdir -p $BIN_DIR
cp bin/xtfs_* $BIN_DIR

# copy man-pages  
mkdir -p $MAN_DIR  
cp -R man/* $MAN_DIR/

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
/usr/share/java/*.jar
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
#/etc/xos/
#/etc/xos/xtreemfs/