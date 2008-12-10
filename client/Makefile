#  Copyright (c) 2007, 2008  Matthias Hess, Erich Focht
#  This file is part of XtreemFS.
#
#  XtreemFS is part of XtreemOS, a Linux-based Grid Operating
#  System, see <http://www.xtreemos.eu> for more details. The
#  XtreemOS project has been developed with the financial support
#  of the European Commission's IST program under contract
#  #FP6-033576.
#
#  XtreemFS is free software: you can redistribute it and/or
#  modify it under the terms of the GNU General Public License as
#  published by the Free Software Foundation, either version 2 of
#  the License, or (at your option) any later version.
#
#  XtreemFS is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of 
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with XtreemFS.  If not, see <http://www.gnu.org/licenses/>.


TOPDIR = $(shell pwd)
export TOPDIR
-include make.config

XTFS_BINDIR = $(shell pwd)/bin
export XTFS_BINDIR

TARGETS = client tools tests json neon
.PHONY:	clean distclean

all: $(TARGETS)

clean: $(patsubst %,%_clean,$(TARGETS))

distclean: $(patsubst %,%_distclean,$(TARGETS))


.PHONY:	client client_clean client_distclean
client: json neon
	$(MAKE) -C src all || exit 1;
client_clean:
	$(MAKE) -C src clean || exit 1;
client_distclean:
	$(MAKE) -C src distclean || exit 1;


.PHONY: tools tools_clean tools_distclean
tools: json neon
	$(MAKE) -C tools all || exit 1;
tools_clean:
	$(MAKE) -C tools clean || exit 1;
tools_distclean:
	$(MAKE) -C tools distclean || exit 1;


.PHONY:	tests tests_clean tests_distclean
tests: json neon
	$(MAKE) -C tests all || exit 1;
tests_clean:
	$(MAKE) -C tests clean || exit 1;
tests_distclean:
	$(MAKE) -C tests distclean || exit 1;

.PHONY: docs
docs:
	doxygen

### JSON library targets

.PHONY: json json_clean json_distclean
$(JSON_LIBDIR)/libjson.a:
	cd $(JSON_BASEDIR) && \
	./configure && \
	make || exit 1
json: $(JSON_LIBDIR)/libjson.a
json_clean:
	$(MAKE) -C $(JSON_BASEDIR) clean || exit 1
json_distclean:
	$(MAKE) -C $(JSON_BASEDIR) distclean || exit 1

### NEON library targets

.PHONY: neon neon_clean neon_distclean
$(NEON_LIBDIR)/libneon.a:
	cd $(NEON_BASEDIR) && \
	./configure --with-ssl=openssl \
                    --without-libxml2  \
                    --without-expat    \
                    --disable-webdav   \
                    --enable-threadsafe-ssl=posix \
                    CFLAGS="-O3 -funroll-loops $(CC_FLAGS)" && \
	make || exit 1
neon: $(NEON_LIBDIR)/libneon.a
neon_clean:
	$(MAKE) -C $(NEON_BASEDIR) clean || exit 1
neon_distclean:
	$(MAKE) -C $(NEON_BASEDIR) distclean || exit 1



