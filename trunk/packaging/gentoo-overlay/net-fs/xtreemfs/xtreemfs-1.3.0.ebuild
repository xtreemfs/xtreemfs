# Copyright 1999-2007 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2 
# $Header: $

inherit java-pkg-2 java-ant-2

DESCRIPTION="XTREEMFS is a distributed and replicated file system for the Internet"
HOMEPAGE="http://www.xtreemfs.org"
SRC_URI="http://xtreemfs.googlecode.com/files/XtreemFS-${PV}.tar.gz"

LICENSE="BSD"
SLOT="0"
KEYWORDS="~amd64 ~x86"
IUSE=""

DEPEND=">=virtual/jdk-1.6.0
	sys-fs/fuse
	sys-fs/e2fsprogs
	sys-apps/attr
	dev-libs/protobuf
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

	keepdir /var/log/xtreemfs/ /var/run/xtreemfs/

	into /usr/
	dobin "${S}"/bin/*
	doman "${S}"/man/man1/*

	for service in dir mrc osd; do
		newinitd "${FILESDIR}"/xtreemfs-${service}.initd xtreemfs-${service}
		newconfd "${FILESDIR}"/xtreemfs-${service}.confd xtreemfs-${service}
	done

	java-pkg_dojar java/servers/dist/XtreemFS.jar java/lib/protobuf-java-2.3.0.jar java/lib/Flease.jar java/lib/BabuDB.jar java/foundation/dist/Foundation.jar
}

pkg_preinst() {
	fowners xtreemfs:xtreemfs /var/log/xtreemfs
	fperms 755 /var/log/xtreemfs
}
