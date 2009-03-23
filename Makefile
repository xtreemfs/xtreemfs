ifeq "$(ANT_HOME)" ""
	ANT_HOME = /usr
endif

TARGETS = client server
.PHONY:	clean distclean

# Some toplevel configuration
XTFS_BINDIR = $(shell pwd)/client/bin
export XTFS_BINDIR

all: check $(TARGETS)

clean: check $(patsubst %,%_clean,$(TARGETS))

distclean: check $(patsubst %,%_distclean,$(TARGETS))

check:
	@if [ "/usr" = $(ANT_HOME) -a ! -f "/usr/bin/ant" ]; then echo "Apache Ant version 1.6.5 or newer required"; exit 1; fi
	@echo using Ant installation at $(ANT_HOME)
	
.PHONY:	client client_clean client_distclean
client:
	python client/scons.py -C client
client_clean:
	python client/scons.py -C client -c
client_distclean:
	python client/scons.py -C client -c

.PHONY: server server_clean server_distclean
server: check
	$(ANT_HOME)/bin/ant  -f servers/build.xml jar
server_clean: check
	$(ANT_HOME)/bin/ant  -f servers/build.xml clean || exit 1;
server_distclean: check
	$(ANT_HOME)/bin/ant  -f servers/build.xml clean || exit 1;
