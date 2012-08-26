# norootforbuild

Name:           xtreemfs-check-packages
Version:        _VERSION_
Release:        1
License:        BSD
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Group:          System/Filesystems
Summary:        XtreemFS minimal package checks
Source0:        XtreemFS-%{version}.tar.gz
BuildRequires:  xtreemfs-client == %{version}
BuildRequires:  xtreemfs-server == %{version}
BuildRequires:  xtreemfs-tools == %{version}

%description
XtreemFS is a distributed and replicated file system for the internet. For more details, visit www.xtreemfs.org.

%prep
%setup -q -n XtreemFS-%{version}

%build


%install


%clean
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root)
%doc LICENSE

%changelog
