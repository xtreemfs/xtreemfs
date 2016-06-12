# Copyright 1999-2007 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2
# $Header: $



# Use XTREEMFS_ESVN_REPO_URI to track a different branch
ESVN_REPO_URI=${XTREEMFS_ESVN_REPO_URI:-http://xtreemfs.googlecode.com/svn/trunk/}
ESVN_PROJECT=${ESVN_REPO_URI##*/svnroot/}
ESVN_PROJECT=${ESVN_PROJECT%/*}
if [[ ${PV} == "9999" ]] ; then
  inherit java-pkg-2 java-ant-2 subversion
elif [[ ${PV} == "8888_beta" ]] ; then
  inherit java-pkg-2 java-ant-2 subversion
  ESVN_REPO_URI=${XTREEMFS_ESVN_REPO_URI:-http://xtreemfs.googlecode.com/svn/tags/XtreemFS-unstable}
  ESVN_PROJECT=${ESVN_REPO_URI##*/svnroot/}
  ESVN_PROJECT=${ESVN_PROJECT%/*}
else
  inherit java-pkg-2 java-ant-2
  SRC_URI="http://xtreemfs.googlecode.com/files/XtreemFS-${PV}.tar.gz"
fi


DESCRIPTION="XtreemFS is a distributed and replicated file system for the Internet"
HOMEPAGE="http://www.xtreemfs.org"


LICENSE="BSD"
SLOT="0"
IUSE=""

LICENSE="BSD"
SLOT="0"
KEYWORDS="~amd64 ~x86"
IUSE=""

DEPEND=">=virtual/jdk-1.6.0
  >=dev-util/cmake-2.6
  sys-fs/fuse
  sys-fs/e2fsprogs
  dev-java/ant-core
  sys-apps/attr
  >=dev-libs/boost-1.39.0"
RDEPEND="${DEPEND}"

S="${WORKDIR}"/XtreemFS-${PV}/

pkg_setup() {
  enewuser xtreemfs -1 -1 /var/lib/xtreemfs
}

src_compile() {
  export LANG=en_US.utf8
  export LC_ALL=${LANG}
  emake ANT_HOME="" || die "emake failed!"
}

src_install() {
  insinto /etc/xtreemfs/
  doins "${S}"/etc/xos/xtreemfs/*.properties

  insinto /etc/xtreemfs/server-repl-plugin/
  doins "${S}"contrib/server-repl-plugin/config/dir.properties
  doins "${S}"contrib/server-repl-plugin/config/mrc.properties

  keepdir /var/log/xtreemfs/ /var/run/xtreemfs/

  into /usr/
  dobin "${S}"/bin/*
  doman "${S}"/man/man1/*

  for service in dir mrc osd; do
    newinitd "${FILESDIR}"/xtreemfs-${service}.initd xtreemfs-${service}
    newconfd "${FILESDIR}"/xtreemfs-${service}.confd xtreemfs-${service}
  done

  java-pkg_jarinto /usr/share/${PN}/java/servers/dist
  java-pkg_dojar java/servers/dist/XtreemFS.jar

  java-pkg_jarinto /usr/share/${PN}/java/lib
  java-pkg_dojar java/lib/protobuf-java-2.5.0.jar java/lib/BabuDB.jar java/lib/commons-codec-1.3.jar java/lib/jdmktk.jar java/lib/jdmkrt.jar java/lib/JavaReedSolomon.jar

  java-pkg_jarinto /usr/share/${PN}/java/foundation/dist
  java-pkg_dojar java/foundation/dist/Foundation.jar

  java-pkg_jarinto /usr/share/${PN}/java/flease/dist
  java-pkg_dojar java/flease/dist/Flease.jar

  java-pkg_jarinto /usr/share/${PN}/server-repl-plugin/
  java-pkg_dojar contrib/server-repl-plugin/replication.jar

  # Set the XTREEMFS environment variable
  echo -n "XTREEMFS=/usr/share/${PN}" > "${T}/90xtreemfs"
  doenvd "${T}/90xtreemfs"

}

pkg_preinst() {
  fowners xtreemfs:xtreemfs /var/log/xtreemfs
  fperms 755 /var/log/xtreemfs
}

pkg_postinst() {
        ${S}/packaging/generate_uuid /etc/xtreemfs/dirconfig.properties
        ${S}/packaging/generate_uuid /etc/xtreemfs/mrcconfig.properties
        ${S}/packaging/generate_uuid /etc/xtreemfs/osdconfig.properties

}
