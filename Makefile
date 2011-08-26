ifeq "$(JAVA_HOME)" ""
	JAVAC_BIN = /usr/bin/javac
else
	JAVAC_BIN = $(JAVA_HOME)/bin/javac
endif

ifeq "$(ANT_HOME)" ""
        ANT_BIN = /usr/bin/ant
else
        ANT_BIN = $(ANT_HOME)/bin/ant
endif

ifeq "$(CMAKE_HOME)" ""
        CMAKE_BIN = /usr/bin/cmake
else
        CMAKE_BIN = $(CMAKE_HOME)/bin/cmake
endif

WHICH_GPP = $(shell which g++)

SHELL=/bin/bash

XTREEMFS_JAR_DIR=$(DESTDIR)/usr/share/java
XTREEMFS_CONFIG_PARENT_DIR=$(DESTDIR)/etc/xos
XTREEMFS_CONFIG_DIR=$(XTREEMFS_CONFIG_PARENT_DIR)/xtreemfs
XTREEMFS_INIT_DIR=$(DESTDIR)/etc/init.d
XTREEMFS_CLIENT_BUILD_DIR=$(shell pwd)/cpp/build
BIN_DIR=$(DESTDIR)/usr/bin
SBIN_DIR=$(DESTDIR)/sbin
MAN_DIR=$(DESTDIR)/usr/share/man/man1
DOC_DIR_SERVER=$(DESTDIR)/usr/share/doc/xtreemfs-server
DOC_DIR_CLIENT=$(DESTDIR)/usr/share/doc/xtreemfs-client
DOC_DIR_TOOLS=$(DESTDIR)/usr/share/doc/xtreemfs-tools
CONTRIB_DIR=$(DESTDIR)/usr/share/xtreemfs
PLUGIN_DIR=$(CONTRIB_DIR)/server-repl-plugin
PLUGIN_CONFIG_DIR=$(XTREEMFS_CONFIG_DIR)/server-repl-plugin

#Configuration of cpp code thirdparty dependencies.
# If you edit the next five variables, make sure you also change them in cpp/CMakeLists.txt.
CLIENT_GOOGLE_PROTOBUF_CPP = cpp/thirdparty/protobuf-2.3.0
CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY = $(CLIENT_GOOGLE_PROTOBUF_CPP)/src/.libs/libprotobuf.a
CLIENT_GOOGLE_TEST_CPP = cpp/thirdparty/gtest-1.5.0
CLIENT_GOOGLE_TEST_CPP_LIBRARY = $(CLIENT_GOOGLE_TEST_CPP)/lib/.libs/libgtest.a
CLIENT_GOOGLE_TEST_CPP_MAIN = $(CLIENT_GOOGLE_TEST_CPP)/lib/.libs/libgtest_main.a
# The two required objects libgtest.a and libgtest_main.a both depend
# on the same target building the Google googletest library.
# Therefore, this target is guarded by a checkfile which will be touched once it was executed.
# This prevents the target from getting executed again as long as the checkfile does not change.
CLIENT_GOOGLE_TEST_CHECKFILE = .googletest_library_already_built

TARGETS = client server foundation
.PHONY:	clean distclean

# Some toplevel configuration
XTFS_BINDIR = $(shell pwd)/bin
export XTFS_BINDIR

all: check_server check_client check_test $(TARGETS)

clean: check_server check_client $(patsubst %,%_clean,$(TARGETS))

distclean: check_server check_client $(patsubst %,%_distclean,$(TARGETS))

install: install-client install-server install-tools

install-client:

	@if [ ! -f bin/mount.xtreemfs ]; then echo "PLEASE RUN 'make client' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_CLIENT)
	@cp LICENSE $(DOC_DIR_CLIENT)

	@mkdir -p $(BIN_DIR)
	@cp   -a  bin/*.xtreemfs bin/xtfsutil $(BIN_DIR)
	          #bin/xtfs_vivaldi
	          
	@ln -s $(BIN_DIR)/mount.xtreemfs $(SBIN_DIR)/mount.xtreemfs
	@ln -s $(BIN_DIR)/umount.xtreemfs $(SBIN_DIR)/umount.xtreemfs

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
	@cp java/lib/*.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(PLUGIN_DIR)
	@cp -r contrib/server-repl-plugin/lib $(PLUGIN_DIR)
	@cp contrib/server-repl-plugin/replication.jar $(PLUGIN_DIR)

	@mkdir -p $(XTREEMFS_CONFIG_DIR)
#	@cp etc/xos/xtreemfs/*config.properties $(XTREEMFS_CONFIG_DIR)

	@mkdir -p $(PLUGIN_CONFIG_DIR)
	@cp contrib/server-repl-plugin/config/dir.properties $(PLUGIN_CONFIG_DIR)
	@cp contrib/server-repl-plugin/config/mrc.properties $(PLUGIN_CONFIG_DIR)
	
	# delete UUID from config-files
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/dirconfig.properties > $(XTREEMFS_CONFIG_DIR)/dirconfig.properties
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/mrcconfig.properties > $(XTREEMFS_CONFIG_DIR)/mrcconfig.properties
	@grep -v '^uuid\W*=\W*\w\+' etc/xos/xtreemfs/osdconfig.properties > $(XTREEMFS_CONFIG_DIR)/osdconfig.properties

	@cp packaging/generate_uuid $(XTREEMFS_CONFIG_DIR)
	@cp packaging/postinstall_setup.sh $(XTREEMFS_CONFIG_DIR)
	@chmod a+x $(XTREEMFS_CONFIG_DIR)/postinstall_setup.sh

	@mkdir -p $(XTREEMFS_INIT_DIR)
	@cp etc/init.d/xtreemfs-* $(XTREEMFS_INIT_DIR)
	@chmod a+x $(XTREEMFS_INIT_DIR)/xtreemfs-*

	@echo "to complete the server installation, please execute $(XTREEMFS_CONFIG_DIR)/postinstall_setup.sh" 

install-tools:

	@if [ ! -f java/servers/dist/XtreemFS.jar ]; then echo "PLEASE RUN 'make server' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_TOOLS)
	@cp LICENSE $(DOC_DIR_TOOLS)

	@mkdir -p $(XTREEMFS_JAR_DIR)
	@cp java/servers/dist/XtreemFS.jar $(XTREEMFS_JAR_DIR)
	@cp java/foundation/dist/Foundation.jar $(XTREEMFS_JAR_DIR)
	@cp java/lib/*.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(BIN_DIR)
	@cp   -a  `ls bin/xtfs_* | grep -v xtfs_.*mount` $(BIN_DIR)

	@mkdir -p $(MAN_DIR)
	@cp -R man/man1/xtfs_* $(MAN_DIR)

uninstall:

	@rm -rf $(DOC_DIR_SERVER)
	@rm -rf $(DOC_DIR_CLIENT)
	@rm -rf $(DOC_DIR_TOOLS)
	
	@rm -rf $(BIN_DIR)/xtfs*
	@rm -rf $(BIN_DIR)/*.xtreemfs
	
	@rm -f $(SBIN_DIR)/mount.xtreemfs
	@rm -f $(SBIN_DIR)/umount.xtreemfs

	@rm -f $(XTREEMFS_JAR_DIR)/XtreemFS.jar
	@rm -f $(XTREEMFS_JAR_DIR)/BabuDB*.jar
	@rm -f $(XTREEMFS_JAR_DIR)/yidl.jar

	@rm -f $(XTREEMFS_INIT_DIR)/xtreemfs-*
	
	@rm -rf $(MAN_DIR)/xtfs*
	@rm -rf $(MAN_DIR)/*.xtreemfs*
	
	@rm -rf $(CONTRIB_DIR)

	@echo "uninstall complete"
	
purge: uninstall

	@rm -rf $(XTREEMFS_CONFIG_DIR)
	@echo "purge complete"

check_server:
	@if [ ! -e $(JAVAC_BIN) ]; then echo "javac not found! Make sure a JDK is installed and set JAVA_HOME."; exit 1; fi;
	@if [ $(shell $(JAVAC_BIN) -version 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 6 ]; then echo "java version >= 1.6.0 required!"; exit 1; fi;
	@echo "java ok"

	@if [ ! -e $(ANT_BIN) ]; then echo "ant not found! Make sure ant is installed and set ANT_HOME."; exit 1; fi;
	@echo "ant ok"

check_client:
	@if [ ! $(WHICH_GPP) ]; then echo "g++ not found";exit 1; fi;
	@if [ ! $(CMAKE_BIN) ]; then echo "cmake not found";exit 1; fi;
	@echo "g++ ok"
	

check_test:
	@if [[ $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 3 && $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f1) -lt 3 ]]; then echo "python >= 2.4 required!"; exit 1; fi;
	@echo "python ok"

.PHONY:	client client_clean client_distclean client_thirdparty_clean client_package_macosx

CLIENT_THIRDPARTY_REQUIREMENTS = $(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY)
ifdef BUILD_CLIENT_TESTS
	CLIENT_THIRDPARTY_REQUIREMENTS += $(CLIENT_GOOGLE_TEST_CPP_LIBRARY) $(CLIENT_GOOGLE_TEST_CPP_MAIN)
endif

client_thirdparty: $(CLIENT_THIRDPARTY_REQUIREMENTS)

$(CLIENT_GOOGLE_PROTOBUF_CPP_LIBRARY): $(CLIENT_GOOGLE_PROTOBUF_CPP)/src/**
	@echo "client_thirdparty: Configuring and building required Google protobuf library..."
	@cd $(CLIENT_GOOGLE_PROTOBUF_CPP) && ./configure >/dev/null
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
	$(CMAKE_BIN) -Hcpp -B$(XTREEMFS_CLIENT_BUILD_DIR) --check-build-system CMakeFiles/Makefile.cmake 0 $(CLIENT_DEBUG)
	@$(MAKE) -C $(XTREEMFS_CLIENT_BUILD_DIR)	
	@cp   -a $(XTREEMFS_CLIENT_BUILD_DIR)/*.xtreemfs $(XTFS_BINDIR)
	@cp   -a $(XTREEMFS_CLIENT_BUILD_DIR)/xtfsutil $(XTFS_BINDIR)
client_clean: check_client client_thirdparty_clean
	@rm -rf $(XTREEMFS_CLIENT_BUILD_DIR)
client_distclean: check_client client_thirdparty_distclean
	@rm -rf $(XTREEMFS_CLIENT_BUILD_DIR)

CLIENT_PACKAGE_MACOSX_OUTPUT_DIR = XtreemFS_Client_MacOSX.mpkg
CLIENT_PACKAGE_MACOSX_OUTPUT_FILE = XtreemFS_installer.dmg
client_package_macosx:
# Clean everything first to ensure we package a clean client.
	@$(MAKE) client_distclean
# We call $(MAKE) instead of specifying the targets as requirements as its not possible to define dependencies between these two and this breaks in case of parallel builds.
	@$(MAKE) client
	@echo "Running the Apple Packagemaker..."
	@/Developer/usr/bin/packagemaker -d packaging/macosx/XtreemFS_MacOSX_Package.pmdoc/ -o $(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)
	@echo "Creating a DMG file..."
	@if [ -f "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)" ]; then echo "Removing previous file $(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)."; rm "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"; fi
	@hdiutil create -fs HFS+ -srcfolder "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)" -volname "XtreemFS Client for MacOSX" "$(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"
	@if [ -d "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)" ]; then echo "Cleaning up temporary files..."; rm -r "$(CLIENT_PACKAGE_MACOSX_OUTPUT_DIR)"; fi
	@echo "Package file created: $(CLIENT_PACKAGE_MACOSX_OUTPUT_FILE)"

.PHONY: foundation foundation_clean
foundation:
	$(ANT_BIN) -D"file.encoding=UTF-8" -f java/foundation/build-1.6.5.xml jar
foundation_clean:
	$(ANT_BIN)  -D"file.encoding=UTF-8" -f java/foundation/build-1.6.5.xml clean || exit 1;
foundation_distclean:
	$(ANT_BIN) -D"file.encoding=UTF-8" -f java/foundation/build-1.6.5.xml clean || exit 1;

.PHONY: server server_clean server_distclean
server: check_server foundation
	$(ANT_BIN) -D"file.encoding=UTF-8" -f java/servers/build-1.6.5.xml jar
server_clean: check_server
	$(ANT_BIN) -D"file.encoding=UTF-8" -f java/servers/build-1.6.5.xml clean || exit 1;
server_distclean: check_server
	$(ANT_BIN) -D"file.encoding=UTF-8" -f java/servers/build-1.6.5.xml clean || exit 1;

test: check_test client server
	python $(XTFS_BINDIR)/../tests/xtestenv.py -c $(XTFS_BINDIR)/../tests/test_config.py short
