# norootforbuild

%if 0%{?mandriva_version} == 2007 || 0%{?mdkversion} == 200700 || 0%{?rhel_version} == 501 || 0%{?centos_version} == 501 || 0%{?sles_version} == 10
%define client_subpackage 0
%else
%define client_subpackage 1
%endif

Name:           xtreemfs
Version:        _VERSION_
Release:        1
License:        GPL
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          System/Filesystems
Summary:        XtreemFS base package
Source0:        XtreemFS-%{version}.tar.gz

#requires for any distribution
BuildRequires:  ant >= 1.6.5 ant-apache-regexp >= 1.6.5 ant-nodeps >= 1.6.5 java-devel >= 1.6.0
%if %{client_subpackage}
BuildRequires:  gcc-c++ >= 4.1 fuse >= 2.6 fuse-devel >= 2.6 openssl-devel >= 0.9.8 cmake >= 2.6 boost-devel >= 1.35 libattr-devel >= 2
%endif

# openSUSE >=10.2
%if 0%{?suse_version} >= 1020
PreReq:         /usr/sbin/groupadd /usr/sbin/useradd /bin/mkdir /usr/bin/grep /bin/chmod /bin/chown /bin/chgrp /usr/bin/stat
#BuildRequires:  libopenssl-devel >= 0.8
BuildRequires:  pwdutils >= 3
%endif

# Mandriva >=2008
%if 0%{?mandriva_version} >= 2007
Requires(pre):  /usr/sbin/groupadd /usr/sbin/useradd /bin/mkdir /bin/grep /bin/chmod /bin/chown /bin/chgrp /bin/stat
#BuildRequires:  libopenssl-devel >= 0.8 libboost-devel >= 1.42
%endif

# Fedora >=7 with Extras
%if 0%{?fedora_version} >= 7
Requires(pre):  /usr/sbin/groupadd /usr/sbin/useradd /bin/mkdir /bin/grep /bin/chmod /bin/chown /bin/chgrp /usr/bin/stat
#BuildRequires:  openssl-devel >= 0.8
BuildRequires:  kernel redhat-rpm-config
%endif

%description
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

%if %{client_subpackage}
%package client
Summary:        XtreemFS client
Group:          System/Filesystems
#Requires:       %{name} == %{version}-%{release}
Requires:       fuse >= 2.6
Requires:       attr >= 2
Provides:       XtreemFS-client = %{version}
Obsoletes:      XtreemFS-client < %{version}

%description client
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the XtreemFS client module.

%endif

%package backend
Summary:        XtreemFS backend modules and libraries
Group:          System/Filesystems
#Requires:       %{name} == %{version}-%{release}
Requires:       jre >= 1.6.0

%description backend
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the backend modules and libraries shared between the server and tools sub-packages.

%package server
Summary:        XtreemFS server components (DIR, MRC, OSD)
Group:          System/Filesystems
Requires:       %{name}-backend == %{version}-%{release}
Requires:       grep sudo
Requires:       jre >= 1.6.0
Provides:       XtreemFS-server = %{version}
Obsoletes:      XtreemFS-server < %{version}
Requires(post): util-linux

%description server
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

This package contains the XtreemFS server components (DIR, MRC, OSD).
To run the XtreemFS services, a SUN JAVA 6 RUNTIME ENVIROMENT IS REQUIRED! Make sure that Java is installed in /usr/bin, or $JAVA_HOME is set.

%package tools
Summary:        XtreemFS administration tools
Group:          System/Filesystems
Requires:       %{name}-backend == %{version}-%{release}
Requires:       attr >= 2
Requires:       jre >= 1.6.0
Provides:       XtreemFS-tools = %{version}
Obsoletes:      XtreemFS-tools < %{version}

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

%if 0%{?mandriva_version} == 2008 || 0%{?centos_version} >= 501 || 0%{?rhel_version} >= 501 || 0%{?suse_version} == 1030
export CCFLAGS="$CCFLAGS -fPIC"
%endif

%if %{client_subpackage}
make %{?jobs:-j%jobs}
%else
make %{?jobs:-j%jobs} server
%endif

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

%if %{client_subpackage}
make install DESTDIR=$RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{_libdir}/xtreemfs/policies/
%else
make install-server DESTDIR=$RPM_BUILD_ROOT
make install-tools DESTDIR=$RPM_BUILD_ROOT
%endif

# add /etc/xos/xtreemfs/truststore/certs/ folder used for storing certificates
mkdir -p $RPM_BUILD_ROOT/etc/xos/xtreemfs/truststore/certs/

# remove copyright notes (let rpm handle that)
%if %{client_subpackage}
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-client/LICENSE
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-client
%endif
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-server/LICENSE
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-server
rm $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-tools/LICENSE
rmdir $RPM_BUILD_ROOT/usr/share/doc/xtreemfs-tools

%post server
XTREEMFS_CONFIG_DIR=/etc/xos/xtreemfs/

# generate UUIDs
if [ -x $XTREEMFS_CONFIG_DIR/generate_uuid ]; then
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/dirconfig.properties
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/mrcconfig.properties
  $XTREEMFS_CONFIG_DIR/generate_uuid $XTREEMFS_CONFIG_DIR/osdconfig.properties
else
  echo "UUID can't be generated automatically. Please enter a correct UUID in each config file of a xtreemfs service."
fi

#$XTREEMFS_CONFIG_DIR/postinstall_setup.sh
_POSTINSTALL_

%if 0%{?suse_version}
%fillup_and_insserv -f xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%restart_on_update xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%endif
%if 0%{?fedora_version}
/sbin/chkconfig --add xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%endif
%if 0%{?mandriva_version}
%_post_service xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%endif

%preun server
%if 0%{?suse_version}
%stop_on_removal xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%endif
%if 0%{?fedora_version}
# 0 packages after uninstall -> pkg is about to be removed
  if [ "$1" = "0" ] ; then
    /sbin/service xtreemfs-dir stop >/dev/null 2>&1
    /sbin/service xtreemfs-mrc stop >/dev/null 2>&1
    /sbin/service xtreemfs-osd stop >/dev/null 2>&1
    /sbin/chkconfig --del xtreemfs-dir xtreemfs-mrc xtreemfs-osd
  fi
%endif
%if 0%{?mandriva_version}
%_preun_service xtreemfs-dir xtreemfs-mrc xtreemfs-osd
%endif

%postun server
%if 0%{?suse_version}
%insserv_cleanup
%endif
%if 0%{?fedora_version}
# >=1 packages after uninstall -> pkg was updated -> restart
if [ "$1" -ge "1" ] ; then
  /sbin/service xtreemfs-dir condrestart >/dev/null 2>&1 || :
  /sbin/service xtreemfs-mrc condrestart >/dev/null 2>&1 || :
  /sbin/service xtreemfs-osd condrestart >/dev/null 2>&1 || :
fi
%endif
%if 0%{?mandriva_version}
%endif

%if %{client_subpackage}
%post client
#XTREEMFS_CONFIG_DIR=/etc/xos/xtreemfs/
#$XTREEMFS_CONFIG_DIR/postinstall_setup.sh
_POSTINSTALL_

#%if 0%{?suse_version}
#%fillup_and_insserv -f xtreemfs-vivaldi
#%restart_on_update xtreemfs-vivaldi
#%endif
#%if 0%{?fedora_version}
#/sbin/chkconfig --add xtreemfs-vivaldi
#%endif
#%if 0%{?mandriva_version}
#%_post_service xtreemfs-vivaldi
#%endif

%preun client
#%if 0%{?suse_version}
#%stop_on_removal xtreemfs-vivaldi
#%endif
#%if 0%{?fedora_version}
## 0 packages after uninstall -> pkg is about to be removed
#  if [ "$1" = "0" ] ; then
#    /sbin/service xtreemfs-vivaldi stop >/dev/null 2>&1
#    /sbin/chkconfig --del xtreemfs-vivaldi
#  fi
#%endif
#%if 0%{?mandriva_version}
#%_preun_service xtreemfs-vivaldi
#%endif

%postun client
#%if 0%{?suse_version}
#%insserv_cleanup
#%endif
#%if 0%{?fedora_version}
## >=1 packages after uninstall -> pkg was updated -> restart
#if [ "$1" -ge "1" ] ; then
#  /sbin/service xtreemfs-vivaldi condrestart >/dev/null 2>&1 || :
#fi
#%endif
#%if 0%{?mandriva_version}
#%endif
%endif

%clean
rm -rf $RPM_BUILD_ROOT

%if %{client_subpackage}
%files client
%defattr(-,root,root)
/usr/bin/*.xtreemfs
/usr/bin/xtfsutil
#/usr/bin/xtfs_vivaldi
%dir %{_libdir}/xtreemfs
%dir %{_libdir}/xtreemfs/policies
#/etc/init.d/xtreemfs-vivaldi
/usr/share/man/man1/*.xtreemfs*
/usr/share/man/man1/xtfsutil*
%dir /etc/xos/
%dir /etc/xos/xtreemfs/
%config(noreplace) /etc/xos/xtreemfs/default_dir
#/usr/share/doc/xtreemfs-client/
%doc LICENSE

%defattr(-,root,root)
%endif

%files backend
%defattr(-,root,root)
/usr/share/java/XtreemFS.jar
/usr/share/java/Foundation.jar
/usr/share/java/protobuf-java-2.3.0.jar
/usr/share/java/Flease.jar
/usr/share/java/BabuDB.jar

%doc LICENSE

%files server
%defattr(-,root,root)
/etc/init.d/xtreemfs-*
#%exclude /etc/init.d/xtreemfs-vivaldi
%dir /etc/xos/
%dir /etc/xos/xtreemfs/
%dir %attr(0750,root,root) /etc/xos/xtreemfs/truststore/
%dir %attr(0750,root,root) /etc/xos/xtreemfs/truststore/certs/
%config(noreplace) %attr(0750,root,root) /etc/xos/xtreemfs/*.properties
/etc/xos/xtreemfs/generate_uuid
/etc/xos/xtreemfs/postinstall_setup.sh
%dir /etc/xos/xtreemfs/server-repl-plugin/
/etc/xos/xtreemfs/server-repl-plugin/dir.properties
/etc/xos/xtreemfs/server-repl-plugin/mrc.properties
%dir /usr/share/xtreemfs/
%dir /usr/share/xtreemfs/server-repl-plugin/
/usr/share/xtreemfs/server-repl-plugin/replication.jar
%dir /usr/share/xtreemfs/server-repl-plugin/lib/
/usr/share/xtreemfs/server-repl-plugin/lib/*.jar
%doc LICENSE

%files tools
%defattr(-,root,root)
%if %{client_subpackage}
# these files only appear if the client package is built
#%exclude /usr/bin/xtfs_vivaldi
%endif
/usr/bin/xtfs_*
/usr/share/man/man1/xtfs_*
#/usr/share/doc/xtreemfs-tools/
%doc LICENSE

%changelog
