Name:           XtreemFS-client
Version:        0.10.1
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          Networking
Summary:        XtreemFS client
Source0:        %{name}-%{version}.tar.bz2
Requires:       fuse >= 2.6 attr


#requires for different distributions
BuildRequires:  make >= 3.8.1 gcc >= 4 fuse >= 2.6 fuse-devel >= 2.6 libxml2-devel >= 2.6

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
BuildRequires:  openssl-devel >= 0.8 kernel
%endif


%description
XtreemFS is an object-based file system that is designed for federated IT infrastructures that are connected by wide-area networks. This packages containes the XtreemFS client component.


%prep
%setup -q


%build
make


%install
BIN_DIR="$RPM_BUILD_ROOT/usr/bin"
MAN_DIR="$RPM_BUILD_ROOT/usr/share/man"
XTREEMFS_CONFIG_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/  

# copy bins
mkdir -p $BIN_DIR
#cp tools/xtfs_* $BIN_DIR
#cp src/xtfs_* $BIN_DIR

#cd ..
cp bin/xtfs_* $BIN_DIR

# copy config files
mkdir -p $XTREEMFS_CONFIG_DIR
cp config/* $XTREEMFS_CONFIG_DIR

# copy man-pages
mkdir -p $MAN_DIR
cp -R man/* $MAN_DIR/

%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root)
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
/etc/xos/xtreemfs/*
