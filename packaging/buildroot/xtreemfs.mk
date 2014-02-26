#################
#
# XtreemFS package for client and server targets.
#
#################

XTREEMFS_VERSION=master
XTREEMFS_SITE="https://code.google.com/p/xtreemfs/"
XTREEMFS_SITE_METHOD=git

define XTREEMFS_USERS
	xtreemfs -1 xtreemfs -1 * /var/lib/xtreemfs -
endef

define XTREEMFS_BUILD_CMDS
	XTREEMFS_CLIENT_BUILD_DIR=cpp/build
# google-protobuf always tries to run its unit tests with a given 'protoc'. Make sure that these tests always pass by setting the 'protoc' path to '/bin/true'.
	(cd $(@D)/cpp/thirdparty/protobuf-2.5.0 && $(TARGET_MAKE_ENV) CC="$(TARGET_CC)" CXX="$(TARGET_CXX)" LD="$(TARGET_LD) -L$(STAGING_DIR)/lib -L$(STAGING_DIR)/usr/lib" ./configure --host=x86_64-unknown-linux-gnu --target=i686-unknown-linux-gnu --with-protoc=/bin/true)
	$(TARGET_MAKE_ENV) make -C $(@D)/cpp/thirdparty/protobuf-2.5.0
	(cd $(@D) && CC="$(TARGET_CC)" CXX="$(TARGET_CXX)" LD="$(TARGET_LD) -L$(STAGING_DIR)/lib -L$(STAGING_DIR)/usr/lib" cmake -Hcpp -B$(@D)/cpp/build --check-build-system CMakeFiles/Makefile.cmake 0  -DBOOST_ROOT=$(STAGING_DIR)/usr) -DSKIP_FUSE=true
	echo $(@D)
	$(TARGET_MAKE_ENV) make -C $(@D)/cpp/build
	cp -p $(@D)/cpp/build/*.xtreemfs $(@D)/bin
	cp -p $(@D)/cpp/build/xtfsutil $(@D)/bin
	BOOST_ROOT=$(STAGING_DIR)/usr $(TARGET_MAKE_ENV) $(MAKE) CC="$(TARGET_CC)" CXX="$(TARGET_CXX)" LD="$(TARGET_LD) -L$(STAGING_DIR)/lib -L$(STAGING_DIR)/usr/lib" -C $(@D) server
endef

define XTREEMFS_INSTALL_TARGET_CMDS
	$(TARGET_MAKE_ENV) $(MAKE) DESTDIR=$(TARGET_DIR) -C $(@D) install-client install-server
	$(@D)/etc/init.d/generate_initd_scripts.sh
	sed 's/java -ea /jamvm /'g $(@D)/etc/init.d/xtreemfs-dir > $(TARGET_DIR)/etc/init.d/xtreemfs-dir
	sed 's/java -ea /jamvm /'g $(@D)/etc/init.d/xtreemfs-mrc > $(TARGET_DIR)/etc/init.d/xtreemfs-mrc
	sed 's/java -ea /jamvm /'g $(@D)/etc/init.d/xtreemfs-osd > $(TARGET_DIR)/etc/init.d/xtreemfs-osd
endef

$(eval $(generic-package))
