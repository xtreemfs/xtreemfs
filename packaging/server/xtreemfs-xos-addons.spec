Name:           XtreemFS-XOS-addons
Version:        0.99.2
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#BuildRequires:  fastjar
Group:          Networking
Summary:        XtreemOS server addons for XtreemFS
Source0:        %{name}-%{version}.tar.gz
Requires:       tar

%description
XtreemFS is an object-based file system that is designed for federated IT infrastructures that are connected by wide-area networks. This package contains optional XtreemOS add-ons (DIR, MRC, OSD).

NOTE: REQUIRES SUN JAVA 6 RUNTIME ENVIREMENT!

%prep
%setup

%build

%install
TARGET_DIR=$RPM_BUILD_ROOT/etc/xos/xtreemfs/policies

# copy content
mkdir -p $TARGET_DIR
cp -r * $TARGET_DIR

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
/etc/xos
/etc/xos/xtreemfs
/etc/xos/xtreemfs/policies/*
