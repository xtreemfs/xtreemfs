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

WHICH_GPP = $(shell which g++)

SHELL=/bin/bash

XTREEMFS_JAR_DIR=$(DESTDIR)/usr/share/java
XTREEMFS_CONFIG_PARENT_DIR=$(DESTDIR)/etc/xos
XTREEMFS_CONFIG_DIR=$(XTREEMFS_CONFIG_PARENT_DIR)/xtreemfs
XTREEMFS_INIT_DIR=$(DESTDIR)/etc/init.d
BIN_DIR=$(DESTDIR)/usr/bin
MAN_DIR=$(DESTDIR)/usr/share/man/man1
DOC_DIR_SERVER=$(DESTDIR)/usr/share/doc/xtreemfs-server
DOC_DIR_CLIENT=$(DESTDIR)/usr/share/doc/xtreemfs-client
DOC_DIR_TOOLS=$(DESTDIR)/usr/share/doc/xtreemfs-tools

TARGETS = client server
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
	@cp COPYING $(DOC_DIR_CLIENT)

	@mkdir -p $(BIN_DIR)
	@cp   -at $(BIN_DIR) \
	          bin/*.xtreemfs \
	          bin/xtfs_*mount \
	          bin/xtfs_vivaldi

	@mkdir -p $(XTREEMFS_CONFIG_DIR)
	@cp etc/xos/xtreemfs/default_dir $(XTREEMFS_CONFIG_DIR)

	@mkdir -p $(MAN_DIR)
	@cp -R man/man1/*.xtreemfs* $(MAN_DIR)

install-server:

	@if [ ! -f src/servers/dist/XtreemFS.jar ]; then echo "PLEASE RUN 'make server' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_SERVER)
	@cp COPYING $(DOC_DIR_SERVER)

	@mkdir -p $(XTREEMFS_JAR_DIR)
	@cp src/servers/dist/XtreemFS.jar $(XTREEMFS_JAR_DIR)
	@cp src/servers/lib/BabuDB*.jar $(XTREEMFS_JAR_DIR)
	@cp src/servers/lib/yidl.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(XTREEMFS_CONFIG_DIR)
#	@cp etc/xos/xtreemfs/*config.properties $(XTREEMFS_CONFIG_DIR)
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

	@if [ ! -f src/servers/dist/XtreemFS.jar ]; then echo "PLEASE RUN 'make server' FIRST!"; exit 1; fi

	@mkdir -p $(DOC_DIR_TOOLS)
	@cp COPYING $(DOC_DIR_TOOLS)

	@mkdir -p $(XTREEMFS_JAR_DIR)
	@cp src/servers/dist/XtreemFS.jar $(XTREEMFS_JAR_DIR)
	@cp src/servers/lib/BabuDB*.jar $(XTREEMFS_JAR_DIR)
	@cp src/servers/lib/yidl.jar $(XTREEMFS_JAR_DIR)

	@mkdir -p $(BIN_DIR)
	@cp   -at $(BIN_DIR) \
	          `ls bin/xtfs_* | grep -v xtfs_.*mount`

	@mkdir -p $(MAN_DIR)
	@cp -R man/man1/xtfs_* $(MAN_DIR)

uninstall:

	@rm -rf $(DOC_DIR_SERVER)
	@rm -rf $(DOC_DIR_CLIENT)
	@rm -rf $(DOC_DIR_TOOLS)
	
	@rm -rf $(BIN_DIR)/xtfs_*
	@rm -rf $(BIN_DIR)/*.xtreemfs

	@rm -f $(XTREEMFS_JAR_DIR)/XtreemFS.jar
	@rm -f $(XTREEMFS_JAR_DIR)/BabuDB*.jar
	@rm -f $(XTREEMFS_JAR_DIR)/yidl.jar

	@rm -f $(XTREEMFS_INIT_DIR)/xtreemfs-*
	
	@rm -rf $(MAN_DIR)/xtfs_*
	@rm -rf $(MAN_DIR)/*.xtreemfs*

	@echo "uninstall complete"
	
purge:

	@rm -rf $(DOC_DIR_SERVER)
	@rm -rf $(DOC_DIR_CLIENT)
	@rm -rf $(DOC_DIR_TOOLS)
	
	@rm -rf $(BIN_DIR)/xtfs_*
	@rm -rf $(BIN_DIR)/*.xtreemfs

	@rm -f $(XTREEMFS_JAR_DIR)/XtreemFS.jar
	@rm -f $(XTREEMFS_JAR_DIR)/BabuDB*.jar
	@rm -f $(XTREEMFS_JAR_DIR)/yidl.jar

	@rm -rf $(XTREEMFS_CONFIG_DIR)
	@rm -f $(XTREEMFS_INIT_DIR)/xtreemfs-*
	
	@rm -rf $(MAN_DIR)/xtfs_*
	@rm -rf $(MAN_DIR)/*.xtreemfs*

	@echo "purge complete"

check_server:
	@if [ ! -e $(JAVAC_BIN) ]; then echo "javac not found! Make sure a JDK is installed and set JAVA_HOME."; exit 1; fi;
	@if [ $(shell $(JAVAC_BIN) -version 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 6 ]; then echo "java version >= 1.6.0 required!"; exit 1; fi;
	@echo "java ok"

	@if [ ! -e $(ANT_BIN) ]; then echo "ant not found! Make sure ant is installed and set ANT_HOME."; exit 1; fi;
	@echo "ant ok"

check_client:
	@if [ ! $(WHICH_GPP) ]; then echo "g++ not found";exit 1; fi;
	@echo "g++ ok"

check_test:
	@if [[ $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 3 && $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f1) -lt 3 ]]; then echo "python >= 2.4 required!"; exit 1; fi;
	@echo "python ok"

.PHONY:	client client_clean client_distclean
client: check_client
	python share/scons.py
client_clean: check_client
	python share/scons.py -c
client_distclean: check_client
	python share/scons.py -c

.PHONY: server server_clean server_distclean
server: check_server
	$(ANT_BIN) -f src/servers/build.xml jar
server_clean: check_server
	$(ANT_BIN) -f src/servers/build.xml clean || exit 1;
server_distclean: check_server
	$(ANT_BIN) -f src/servers/build.xml clean || exit 1;

test: check_test client server
	$(XTFS_BINDIR)/xtfs_test --autotest
