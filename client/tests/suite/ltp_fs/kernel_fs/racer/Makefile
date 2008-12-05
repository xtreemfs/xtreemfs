TARGETS=$(wildcard *.sh)

all:
 
install:
	@set -e; for i in $(TARGETS) ; do ln -f $$i ../../bin/$$i ; done

clean:
