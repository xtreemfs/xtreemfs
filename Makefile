ifeq "$(JAVA_HOME)" ""
	JAVAC_BIN = $(shell which javac)
else
	JAVAC_BIN = $(JAVA_HOME)/bin/javac
endif

ifeq "$(MAVEN_HOME)" ""
        MVN_BIN = $(shell which mvn)
else
        MVN_BIN = $(MAVEN_HOME)/bin/mvn
endif

ifeq "$(CMAKE_HOME)" ""
        CMAKE_BIN = cmake
else
        CMAKE_BIN = $(CMAKE_HOME)/bin/cmake
endif

SHELL := $(shell which bash)
WHICH_GPP = $(shell which g++)
WHICH_CLANGPP = $(shell which clang++)

ifeq "$(shell uname)" "SunOS"
  PROTOBUF_DISABLE_64_BIT_SOLARIS = "--disable-64bit-solaris"
endif

# Paths used during compilation.
XTREEMFS_CLIENT_BUILD_DIR=$(shell pwd)/cpp/build
XTREEMFS_BINARIES_DIR = $(shell pwd)/bin

# Install paths relative to DESTDIR.
XTREEMFS_JAR_DIR=$(DESTDIR)/usr/share/java
XTREEMFS_CONFIG_PARENT_DIR=$(DESTDIR)/etc/xos
XTREEMFS_CONFIG_DIR=$(XTREEMFS_CONFIG_PARENT_DIR)/xtreemfs
XTREEMFS_INIT_DIR=$(DESTDIR)/etc/init.d
XTREEMFS_SHARE_DIR=$(DESTDIR)/usr/share/xtreemfs
LIB_DIR?=/usr/lib/xtreemfs
XTREEMFS_LIB_DIR=$(DESTDIR)$(LIB_DIR)
BIN_DIR=$(DESTDIR)/usr/bin
SBIN_DIR=$(DESTDIR)/sbin
MAN_DIR=$(DESTDIR)/usr/share/man/man1
DOC_DIR_SERVER=$(DESTDIR)/usr/share/doc/xtreemfs-server
DOC_DIR_CLIENT=$(DESTDIR)/usr/share/doc/xtreemfs-client
DOC_DIR_TOOLS=$(DESTDIR)/usr/share/doc/xtreemfs-tools
PLUGIN_CONFIG_DIR=$(XTREEMFS_CONFIG_DIR)/server-repl-plugin

#Configuration of cpp code thirdparty dependencies.
# If you edit the next five variables, make sure you also change them in cpp/CMakeLists.txt.
CLIENT_GOOGLE_PROTOBUF_CPP = cpp/thirdparty/protobuf-2.5.0
CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY = $(CLIENT_GOOGLE_PROTOBUF_CPP)/src/.libs/libprotobuf.a
CLIENT_GOOGLE_TEST_CPP = cpp/thirdparty/gtest-1.7.0
CLIENT_GOOGLE_TEST_CPP_LIBRARY = $(CLIENT_GOOGLE_TEST_CPP)/lib/.libs/libgtest.a
CLIENT_GOOGLE_TEST_CPP_MAIN = $(CLIENT_GOOGLE_TEST_CPP)/lib/.libs/libgtest_main.a
# The two required objects libgtest.a and libgtest_main.a both depend
# on the same target building the Google googletest library.
# Therefore, this target is guarded by a checkfile which will be touched once it was executed.
# This prevents the target from getting executed again as long as the checkfile does not change.
CLIENT_GOOGLE_TEST_CHECKFILE = .googletest_library_already_built

XTREEMFS_JNI_LIBRARY = libjni-xtreemfs.so

TARGETS = client server foundation flease
.PHONY:	clean distclean

all: check_server check_client check_test $(TARGETS)

clean: check_server check_client $(patsubst %,%_clean,$(TARGETS))

distclean: check_server check_client $(patsubst %,%_distclean,$(TARGETS))

install: install-client install-server install-tools install-libs

install-client:

	@if [ ! -f $(XTREEMFS_BINARIES_DIR)/mkfs.xtreemfs ]; then echo "PLEASE RUN 'make client' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_CLIENT)
	@cp LICENSE $(DOC_DIR_CLIENT)

	@mkdir -p $(BIN_DIR)
	@cp   -p  $(XTREEMFS_BINARIES_DIR)/*.xtreemfs $(XTREEMFS_BINARIES_DIR)/xtfsutil $(BIN_DIR)

# mount -t xtreemfs will be recognized when binaries are present in /sbin/. Only applicable if the Fuse Client was built.
	@[ -f $(XTREEMFS_BINARIES_DIR)/mount.xtreemfs ] && mkdir -p $(SBIN_DIR); true
	@[ -f $(XTREEMFS_BINARIES_DIR)/mount.xtreemfs ] && ln -s $(BIN_DIR)/mount.xtreemfs $(SBIN_DIR)/mount.xtreemfs; true
	@[ -f $(XTREEMFS_BINARIES_DIR)/mount.xtreemfs ] && ln -s $(BIN_DIR)/umount.xtreemfs $(SBIN_DIR)/umount.xtreemfs; true

	@mkdir -p $(XTREEMFS_CONFIG_DIR)
	@cp etc/xos/xtreemfs/default_dir $(XTREEMFS_CONFIG_DIR)

	@mkdir -p $(MAN_DIR)
	@cp -R man/man1/*.xtreemfs* $(MAN_DIR)
	@cp -R man/man1/xtfsutil.* $(MAN_DIR)

install-server:

	@if [ ! -f java/servers/dist/XtreemFS.jar ]; then echo "PLEASE RUN 'make server' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_SERVER)
	@cp LICENSE $(DOC_DIR_SERVER)

	@mkdir -p $(XTREEMFS_JAR_DIR)
	@cp java/servers/dist/XtreemFS.jar $(XTREEMFS_JAR_DIR)
	@cp java/foundation/dist/Foundation.jar $(XTREEMFS_JAR_DIR)
	@cp java/flease/dist/Flease.jar $(XTREEMFS_JAR_DIR)
	@cp java/lib/*.jar $(XTREEMFS_JAR_DIR)
	@cp contrib/server-repl-plugin/BabuDB_replication_plugin.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(XTREEMFS_CONFIG_DIR)
#	@cp etc/xos/xtreemfs/*config.properties $(XTREEMFS_CONFIG_DIR)
# delete UUID from config-files
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/dirconfig.properties > $(XTREEMFS_CONFIG_DIR)/dirconfig.properties
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/mrcconfig.properties > $(XTREEMFS_CONFIG_DIR)/mrcconfig.properties
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/osdconfig.properties > $(XTREEMFS_CONFIG_DIR)/osdconfig.properties

	@mkdir -p $(PLUGIN_CONFIG_DIR)
	@cp contrib/server-repl-plugin/config/dir.properties $(PLUGIN_CONFIG_DIR)
	@cp contrib/server-repl-plugin/config/mrc.properties $(PLUGIN_CONFIG_DIR)

	@cp packaging/generate_uuid $(XTREEMFS_CONFIG_DIR)
	@cp packaging/postinstall_setup.sh $(XTREEMFS_CONFIG_DIR)
	@chmod a+x $(XTREEMFS_CONFIG_DIR)/postinstall_setup.sh

# Generating init.d scripts based on template.
	@etc/init.d/generate_initd_scripts.sh
	@mkdir -p $(XTREEMFS_INIT_DIR)
	@cp etc/init.d/xtreemfs-{dir,mrc,osd} $(XTREEMFS_INIT_DIR)
	@chmod a+x $(XTREEMFS_INIT_DIR)/xtreemfs-*

	@mkdir -p $(XTREEMFS_SHARE_DIR)
	@cp contrib/xtreemfs-osd-farm/xtreemfs-osd-farm $(XTREEMFS_SHARE_DIR)

	@echo "to complete the server installation, please execute $(XTREEMFS_CONFIG_DIR)/postinstall_setup.sh"

install-tools:

	@if [ ! -f java/servers/dist/XtreemFS.jar ]; then echo "PLEASE RUN 'make server' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_TOOLS)
	@cp LICENSE $(DOC_DIR_TOOLS)

	@mkdir -p $(XTREEMFS_JAR_DIR)
	@cp java/servers/dist/XtreemFS.jar $(XTREEMFS_JAR_DIR)
	@cp java/foundation/dist/Foundation.jar $(XTREEMFS_JAR_DIR)
	@cp java/flease/dist/Flease.jar $(XTREEMFS_JAR_DIR)
	@cp java/lib/*.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(BIN_DIR)
	@cp   -p  `ls $(XTREEMFS_BINARIES_DIR)/xtfs_* | grep -v xtfs_.*mount` $(BIN_DIR)

	@mkdir -p $(MAN_DIR)
	@cp -R man/man1/xtfs_* $(MAN_DIR)

install-libs:
# This could also install a shared libxtreemfs
# but for now it only installs the libjni-xtreemfs if it exists.
	@mkdir -p $(XTREEMFS_LIB_DIR)
	@if [ -f $(XTREEMFS_CLIENT_BUILD_DIR)/$(XTREEMFS_JNI_LIBRARY) ]; then \
		cp $(XTREEMFS_CLIENT_BUILD_DIR)/$(XTREEMFS_JNI_LIBRARY) $(XTREEMFS_LIB_DIR)/$(XTREEMFS_JNI_LIBRARY); \
	fi

uninstall:

	@rm -rf $(DOC_DIR_SERVER)
	@rm -rf $(DOC_DIR_CLIENT)
	@rm -rf $(DOC_DIR_TOOLS)

	@rm -rf $(BIN_DIR)/xtfs*
	@rm -rf $(BIN_DIR)/*.xtreemfs

	@rm -f $(SBIN_DIR)/mount.xtreemfs
	@rm -f $(SBIN_DIR)/umount.xtreemfs

	@rm -f $(XTREEMFS_LIB_DIR)/$(XTREEMFS_JNI_LIBRARY)
	@rmdir $(XTREEMFS_LIB_DIR)

	@rm -f $(XTREEMFS_JAR_DIR)/XtreemFS.jar
	@rm -f $(XTREEMFS_JAR_DIR)/Foundation.jar
	@rm -f $(XTREEMFS_JAR_DIR)/Flease.jar
	@rm -f $(XTREEMFS_JAR_DIR)/BabuDB.jar
	@rm -f $(XTREEMFS_JAR_DIR)/commons-codec-1.3.jar
	@rm -f $(XTREEMFS_JAR_DIR)/jdmkrt.jar
	@rm -f $(XTREEMFS_JAR_DIR)/jdmktk.jar
	@rm -f $(XTREEMFS_JAR_DIR)/protobuf-java-2.5.0.jar
	@rm -f $(XTREEMFS_JAR_DIR)/BabuDB_replication_plugin.jar

	@rm -f $(XTREEMFS_INIT_DIR)/xtreemfs-*

	@rm -rf $(MAN_DIR)/xtfs*
	@rm -rf $(MAN_DIR)/*.xtreemfs*

	@echo "uninstall complete"

purge: uninstall

	@rm -rf $(XTREEMFS_CONFIG_DIR)
	@echo "purge complete"

check_server:
	@if [ ! -e $(JAVAC_BIN) ]; then echo "javac not found! Make sure a JDK is installed and set JAVA_HOME."; exit 1; fi;
	@if [ $(shell $(JAVAC_BIN) -version 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 6 ]; then echo "java version >= 1.6.0 required!"; exit 1; fi;
	@echo "java ok"

	@if [ ! -e $(MVN_BIN) ]; then echo "mvn not found! Make sure mvn is installed and set MAVEN_HOME."; exit 1; fi;
	@echo "mvn ok"

check_client:
	@if [ ! $(WHICH_GPP) -a ! $(WHICH_CLANGPP) ]; then echo "C++ compiler not found";exit 1; fi;
	@if [ ! $(CMAKE_BIN) ]; then echo "cmake not found";exit 1; fi;
	@echo "C++ ok"


check_test:
	@if [[ $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 3 && $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f1) -lt 3 ]]; then echo "python >= 2.4 required!"; exit 1; fi;
	@echo "python ok"

.PHONY:	client client_clean client_distclean client_thirdparty_clean client_package_macosx

# Client section.
CLIENT_THIRDPARTY_REQUIREMENTS = $(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY)
ifdef BUILD_CLIENT_TESTS
	CLIENT_THIRDPARTY_REQUIREMENTS += $(CLIENT_GOOGLE_TEST_CPP_LIBRARY) $(CLIENT_GOOGLE_TEST_CPP_MAIN)
	CMAKE_BUILD_CLIENT_TESTS = -DBUILD_CLIENT_TESTS=true
endif

# Tell CMake if it should skip building the jni library
ifdef SKIP_JNI
	CMAKE_SKIP_JNI = -DSKIP_JNI=true
endif

# Do not use env variables to control the CMake behavior as stated in http://www.cmake.org/Wiki/CMake_FAQ#How_can_I_get_or_set_environment_variables.3F
# Instead define them via -D, so they will be cached.
ifdef BOOST_ROOT
	CMAKE_BOOST_ROOT = -DBOOST_ROOT="$(BOOST_ROOT)" -DBoost_NO_SYSTEM_PATHS=ON
endif

# Fix boost problems on centos 6 (see https://public.kitware.com/Bug/view.php?id=15270)
ifdef NO_BOOST_CMAKE
	CMAKE_NO_BOOST_CMAKE = -DBoost_NO_BOOST_CMAKE=BOOL:ON
endif

# Tell CMake if it should ignore a missing Fuse.
ifdef SKIP_FUSE
	CMAKE_SKIP_FUSE = -DSKIP_FUSE=true
endif

# Trigger building the experimental LD_PRELOAD library
ifdef BUILD_PRELOAD
	CMAKE_BUILD_PRELOAD = -DBUILD_PRELOAD=true
endif

client_thirdparty: $(CLIENT_THIRDPARTY_REQUIREMENTS)

$(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY): $(CLIENT_GOOGLE_PROTOBUF_CPP)/src/**
	@echo "client_thirdparty: Configuring and building required Google protobuf library..."
	@cd $(CLIENT_GOOGLE_PROTOBUF_CPP) && LIBS=-lpthread ./configure --with-pic $(PROTOBUF_DISABLE_64_BIT_SOLARIS) >/dev/null
	@$(MAKE) -C $(CLIENT_GOOGLE_PROTOBUF_CPP) >/dev/null
	@echo "client_thirdparty: ...completed building required Google protobuf library."
	@touch $(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY)

$(CLIENT_GOOGLE_TEST_CPP_LIBRARY): $(CLIENT_GOOGLE_TEST_CHECKFILE)
	@touch $(CLIENT_GOOGLE_TEST_CPP_LIBRARY)

$(CLIENT_GOOGLE_TEST_CPP_MAIN): $(CLIENT_GOOGLE_TEST_CHECKFILE)
	@touch $(CLIENT_GOOGLE_TEST_CPP_MAIN)

$(CLIENT_GOOGLE_TEST_CHECKFILE): $(CLIENT_GOOGLE_TEST_CPP)/include/** $(CLIENT_GOOGLE_TEST_CPP)/src/**
	@echo "client_thirdparty: Configuring and building required Google googletest library..."
	@cd $(CLIENT_GOOGLE_TEST_CPP) && ./configure >/dev/null
	@$(MAKE) -C $(CLIENT_GOOGLE_TEST_CPP) >/dev/null
	@touch $(CLIENT_GOOGLE_TEST_CPP_LIBRARY)
	@echo "client_thirdparty: ...completed building required Google googletest library."
	@touch $(CLIENT_GOOGLE_TEST_CHECKFILE)

client_thirdparty_clean:
	@if [ -f $(CLIENT_GOOGLE_PROTOBUF_CPP)/Makefile ]; then echo "Cleaning required Google protobuf library sources..."; $(MAKE) -C $(CLIENT_GOOGLE_PROTOBUF_CPP) clean >/dev/null; fi
	@if [ -f $(shell pwd)/$(CLIENT_GOOGLE_TEST_CPP)/Makefile ]; then echo "Cleaning required Google googletest library sources..."; $(MAKE) -C $(shell pwd)/$(CLIENT_GOOGLE_TEST_CPP) clean >/dev/null; fi
	@if [ -f $(CLIENT_GOOGLE_TEST_CHECKFILE) ]; then rm $(CLIENT_GOOGLE_TEST_CHECKFILE); fi
	@echo "...finished cleaning thirdparty sources."

client_thirdparty_distclean:
	@echo "client_thirdparty: Dist-Cleaning required Google protobuf library sources..."
	@if [ -f $(shell pwd)/$(CLIENT_GOOGLE_PROTOBUF_CPP)/Makefile ]; then $(MAKE) -C $(shell pwd)/$(CLIENT_GOOGLE_PROTOBUF_CPP) distclean >/dev/null; fi
	@echo "client_thirdparty: Dist-Cleaning required Google googletest library sources..."
	@if [ -f $(shell pwd)/$(CLIENT_GOOGLE_TEST_CPP)/Makefile ]; then $(MAKE) -C $(shell pwd)/$(CLIENT_GOOGLE_TEST_CPP) distclean >/dev/null; fi
	@if [ -f $(CLIENT_GOOGLE_TEST_CHECKFILE) ]; then rm $(CLIENT_GOOGLE_TEST_CHECKFILE); fi
	@echo "client_thirdparty: ...finished distcleaning thirdparty sources."

client_debug: CLIENT_DEBUG = -DCMAKE_BUILD_TYPE=Debug
client_debug: client

client: check_client client_thirdparty
	$(CMAKE_BIN) -Hcpp -B$(XTREEMFS_CLIENT_BUILD_DIR) --check-build-system CMakeFiles/Makefile.cmake 0 $(CLIENT_DEBUG) $(CMAKE_BOOST_ROOT) $(CMAKE_BUILD_CLIENT_TESTS) $(CMAKE_SKIP_FUSE) ${CMAKE_BUILD_PRELOAD} ${CMAKE_SKIP_JNI} ${CMAKE_GENERATE_JNI} ${CMAKE_NO_BOOST_CMAKE}
	@$(MAKE) -C $(XTREEMFS_CLIENT_BUILD_DIR)
	@cd $(XTREEMFS_CLIENT_BUILD_DIR); for i in *.xtreemfs xtfsutil; do [ -f $(XTREEMFS_BINARIES_DIR)/$$i ] && rm -f $(XTREEMFS_BINARIES_DIR)/$$i; done; true
	@cp   -p $(XTREEMFS_CLIENT_BUILD_DIR)/*.xtreemfs $(XTREEMFS_BINARIES_DIR)
	@cp   -p $(XTREEMFS_CLIENT_BUILD_DIR)/xtfsutil $(XTREEMFS_BINARIES_DIR)

client_clean: check_client client_thirdparty_clean
	@cd $(XTREEMFS_CLIENT_BUILD_DIR) &>/dev/null && { for i in *.xtreemfs xtfsutil; do [ -f $(XTREEMFS_BINARIES_DIR)/$$i ] && rm -f $(XTREEMFS_BINARIES_DIR)/$$i; done }; true
	@rm -rf $(XTREEMFS_CLIENT_BUILD_DIR)

client_distclean: check_client client_thirdparty_distclean
	@cd $(XTREEMFS_CLIENT_BUILD_DIR) &>/dev/null && { for i in *.xtreemfs xtfsutil; do [ -f $(XTREEMFS_BINARIES_DIR)/$$i ] && rm -f $(XTREEMFS_BINARIES_DIR)/$$i; done }; true
	@rm -rf $(XTREEMFS_CLIENT_BUILD_DIR)

CLIENT_PACKAGE_MACOSX_OUTPUT_DIR = XtreemFS_Client_MacOSX.mpkg
CLIENT_PACKAGE_MACOSX_OUTPUT_FILE = XtreemFS_Client_MacOSX_installer.dmg
client_package_macosx:
ifeq ($(CMAKE_BOOST_ROOT),)
	@echo No BOOST_ROOT environment variable is specified. This will probably fail. Please set it first.; exit 1
endif
	@./packaging/set_version.sh -i
# Clean everything first to ensure we package a clean client.
	@$(MAKE) client_distclean
# We call $(MAKE) instead of specifying the targets as requirements as its not possible to define dependencies between these two and this breaks in case of parallel builds.
	@$(MAKE) client SKIP_SET_SVN_VERSION=1
	@echo "Running the Apple Packagemaker..."
	@/Developer/usr/bin/packagemaker -d packaging/macosx/XtreemFS_MacOSX_Package.pmdoc/ -o $(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)
	@echo "Creating a DMG file..."
	@if [ -f "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)" ]; then echo "Removing previous file $(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)."; rm "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"; fi
	@hdiutil create -fs HFS+ -srcfolder "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)" -volname "XtreemFS Client for MacOSX" "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"
	@if [ -d "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)" ]; then echo "Cleaning up temporary files..."; rm -r "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)"; fi
	@echo "Package file created: $(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"

.PHONY: parent parent_clean parent_distclean
parent:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/pom.xml --define skipTests --non-recursive install
parent_clean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/pom.xml clean || exit 1;
parent_distclean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/pom.xml clean || exit 1;

.PHONY: flease flease_clean flease_distclean
flease: parent foundation
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-flease/pom.xml --define skipTests install
flease_clean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-flease/pom.xml clean || exit 1;
flease_distclean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-flease/pom.xml clean || exit 1;

.PHONY: foundation foundation_clean foundation_distclean
foundation: parent pbrpcgen
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-foundation/pom.xml --define skipTests install
foundation_clean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-foundation/pom.xml clean || exit 1;
foundation_distclean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-foundation/pom.xml clean || exit 1;

pbrpcgen: $(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY)
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-pbrpcgen/pom.xml --define skipTests install
pbrpcgen_clean:
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-pbrpcgen/pom.xml clean || exit 1

.PHONY: server server_clean server_distclean
server: check_server parent flease foundation
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-servers/pom.xml --define skipTests install
server_clean: check_server
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-servers/pom.xml clean || exit 1;
server_distclean: check_server
	$(MVN_BIN) --settings java/settings.xml --activate-profiles xtreemfs-dev --file java/xtreemfs-servers/pom.xml clean || exit 1;

.PHONY: hadoop-client hadoop-client_clean hadoop-client_distclean
hadoop-client: parent foundation server
	$(MVN_BIN) --settings contrib/hadoop/settings.xml --activate-profiles xtreemfs-hadoop-client-dev --file contrib/hadoop/pom.xml --define skipTests install
	@echo -e "\n\nHadoop Client was successfully compiled. You can find it here:\n\n\tcontrib/hadoop/target/xtreemfs-hadoop-client-<VERSION>.jar\n\nSee the XtreemFS User Guide how to add it in Hadoop.\n"
hadoop-client_clean:
	$(MVN_BIN) --settings contrib/hadoop/settings.xml --activate-profiles xtreemfs-hadoop-client-dev --file contrib/hadoop/pom.xml clean || exit 1
hadoop-client_distclean:
	$(MVN_BIN) --settings contrib/hadoop/settings.xml --activate-profiles xtreemfs-hadoop-client-dev --file contrib/hadoop/pom.xml clean || exit 1

test: check_test client server
	python ./tests/xtestenv -c ./tests/test_config.py short

interfaces: pbrpcgen client_thirdparty
	$(MAKE) -C interface

.PHONY: jni-client-generate
jni-client-generate: CMAKE_GENERATE_JNI = -DGENERATE_JNI=true
jni-client-generate: client
	@echo Attention: To ensure the JNI code is generated, call make distclean prior to make jni-client-generate

