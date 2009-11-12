# norootforbuild

%if 0%{?centos_version} == 501 || 0%{?mandriva_version} == 2007 || 0%{?mdkversion} == 200700 || 0%{?rhel_version} == 501 || 0%{?sles_version} == 10 
%define client_subpackage 0
%else
%define client_subpackage 1
%endif

Name:           xtreemfs
Version:        _VERSION_
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          Networking
Summary:        XtreemFS base package
Source0:        XtreemFS-%{version}.tar.gz

#requires for different distributions
BuildRequires:  ant >= 1.6.5 java-devel >= 1.6.0
%if %{client_subpackage}
BuildRequires:  python >= 2.4 gcc-c++ >= 4.2 fuse >= 2.6 fuse-devel >= 2.6 openssl-devel >= 0.9.8
%endif

# openSUSE >=10.2 
%if 0%{?suse_version} >= 1020 
#BuildRequires:  libopenssl-devel >= 0.8
BuildRequires:  pwdutils >= 3
%endif

# Mandriva >=2008 
%if 0%{?mandriva_version} >= 2007 
#BuildRequires:  libopenssl-devel >= 0.8
%endif

# Fedora >=7 with Extras
%if 0%{?fedora_version} >= 7 
#BuildRequires:  openssl-devel >= 0.8
BuildRequires:  kernel redhat-rpm-config
%endif

%description
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

%if %{client_subpackage}
%package client
Summary:    XtreemFS client
Group:      Networking
#Requires:   %{name} == %{version}-%{release}
Requires:   fuse >= 2.6
Provides:   XtreemFS-client
Obsoletes:  XtreemFS-client

%description client
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the XtreemFS client module.
%endif

%package backend
Summary:    XtreemFS backend modules and libraries
Group:      Networking
#Requires:   %{name} == %{version}-%{release}
Requires:   jre >= 1.6.0

%description backend
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the backend modules and libraries shared between the server and tools sub-packages.

%package server
Summary:    XtreemFS server components (DIR, MRC, OSD)
Group:      Networking
Requires:   %{name}-backend == %{version}-%{release}
Requires:   grep sudo
Requires:   jre >= 1.6.0
Provides:   XtreemFS-server
Obsoletes:  XtreemFS-server

%description server
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the XtreemFS server components (DIR, MRC, OSD).
To run the XtreemFS services, a SUN JAVA 6 RUNTIME ENVIROMENT IS REQUIRED! Make sure that Java is installed in /usr/bin, or $JAVA_HOME is set.

%package tools
Summary:    XtreemFS administration tools
Group:      Networking
Requires:   %{name}-backend == %{version}-%{release}
Requires:   python >= 2.6
Requires:   attr
Requires:   jre >= 1.6.0
Provides:   XtreemFS-tools
Obsoletes:  XtreemFS-tools

%description tools
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains XtreemFS administration tools.
To run the tools, a SUN JAVA 6 RUNTIME ENVIROMENT IS REQUIRED! Make sure that Java is installed in /usr/bin, or $JAVA_HOME is set.

%prep
%setup -q -n XtreemFS-%{version}


%build
export ANT_OPTS=-D"file.encoding=UTF-8"
export CFLAGS="$RPM_OPT_FLAGS -fno-strict-aliasing"
export CXXFLAGS=$CFLAGS

%if %{client_subpackage}
make %{?jobs:-j%jobs}
%else
make %{?jobs:-j%jobs} server
#python share/scons.py
%endif

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

%if %{client_subpackage}
make install DESTDIR=$RPM_BUILD_ROOT
%else
make install-server DESTDIR=$RPM_BUILD_ROOT
make install-tools DESTDIR=$RPM_BUILD_ROOT
%endif

# remove copyright notes (let rpm handle that)
%if %{client_subpackage}
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-client/COPYING
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-client
%endif
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-server/COPYING
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-server
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-tools/COPYING
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-tools

# remove test program
rm -f $RPM_BUILD_ROOT/usr/bin/xtfs_test

%clean
rm -rf $RPM_BUILD_ROOT

%if %{client_subpackage}
%files client
%defattr(-,root,root)
/usr/bin/*.xtreemfs
/usr/bin/xtfs_*mount
/usr/bin/xtfs_vivaldi
/usr/share/man/man1/*.xtreemfs*
%dir /etc/xos/
%dir /etc/xos/xtreemfs/
%config(noreplace) /etc/xos/xtreemfs/default_dir
#/usr/share/doc/xtreemfs-client/
%doc COPYING
%endif

%files backend
%defattr(-,root,root)
/usr/share/java/XtreemFS.jar
/usr/share/java/yidl.jar
/usr/share/java/BabuDB.jar
%doc COPYING

%files server
%defattr(-,root,root)
/etc/init.d/xtreemfs-*
%dir /etc/xos/
%dir /etc/xos/xtreemfs/
%config(noreplace) /etc/xos/xtreemfs/*.properties
/etc/xos/xtreemfs/generate_uuid
/etc/xos/xtreemfs/postinstall_setup.sh
#/usr/share/doc/xtreemfs-server/
%doc COPYING

%files tools
%defattr(-,root,root)
%if %{client_subpackage}
# these files only appear if the client package is build
%exclude /usr/bin/xtfs_*mount
%exclude /usr/bin/xtfs_vivaldi
%endif
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
#/usr/share/doc/xtreemfs-tools/
%doc COPYING

%changelog
