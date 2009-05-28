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

TARGETS = client server
.PHONY:	clean distclean

# Some toplevel configuration
XTFS_BINDIR = $(shell pwd)/bin
export XTFS_BINDIR

all: check_server check_client check_test $(TARGETS)

clean: check_server check_client $(patsubst %,%_clean,$(TARGETS))

distclean: check_server check_client $(patsubst %,%_distclean,$(TARGETS))

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
	@if [[ $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f2) -lt 5 && $(shell python -V 2>&1 | head -n1 | cut -d" " -f2 | cut -d. -f1) -lt 3 ]]; then echo "python >= 2.5 required!"; exit 1; fi;
	@echo "python ok"

.PHONY:	client client_clean client_distclean
client: check_client
	python src/client/scons.py -C src/client
client_clean: check_client
	python src/client/scons.py -C src/client -c
client_distclean: check_client
	python src/client/scons.py -C src/client -c

.PHONY: server server_clean server_distclean
server: check_server
	$(ANT_BIN) -f src/servers/build.xml jar
server_clean: check_server
	$(ANT_BIN) -f src/servers/build.xml clean || exit 1;
server_distclean: check_server
	$(ANT_BIN) -f src/servers/build.xml clean || exit 1;

test: check_test client server
	$(XTFS_BINDIR)/xtfs_test --autotest
