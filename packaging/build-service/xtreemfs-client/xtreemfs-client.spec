Name:           XtreemFS-client
Version:        _VERSION_
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          Networking
Summary:        XtreemFS client
Source0:        %{name}-%{version}.tar.gz
Requires:       fuse >= 2.6 attr python >= 2.4


#requires for different distributions
BuildRequires:  python >= 2.4 gcc-c++ >= 4 fuse >= 2.6 fuse-devel >= 2.6
# openSUSE >=10.2 
%if 0%{?suse_version} >= 1020 
BuildRequires:  libopenssl-devel >= 0.8
%endif

# Mandriva >=2008 
%if 0%{?mandriva_version} >= 2007 
BuildRequires:  libopenssl-devel >= 0.8
%endif

# Fedora >=7 with Extras
%if 0%{?fedora_version} >= 7 
BuildRequires:  openssl-devel >= 0.8 kernel redhat-rpm-config >= 0
%endif


%description
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the XtreemFS client module.

%prep
%setup -q


%build
python scons.py


%install
BIN_DIR="$RPM_BUILD_ROOT/usr/bin"
MAN_DIR="$RPM_BUILD_ROOT/usr/share/man"
XTREEMFS_CONFIG_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/

# copy bins
mkdir -p $BIN_DIR
cp bin/xtfs_* $BIN_DIR

# copy config files
mkdir -p $XTREEMFS_CONFIG_DIR
cp config/default_dir $XTREEMFS_CONFIG_DIR

# copy man-pages
mkdir -p $MAN_DIR
cp -R man/* $MAN_DIR/

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
/etc/xos/
/etc/xos/xtreemfs/*
