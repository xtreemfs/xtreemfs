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

TOPDIR ?= ..
-include $(TOPDIR)/make.config

TGT = xtfs_mount
ifdef XTFS_BINDIR
REAL_TARGET = $(patsubst %,$(XTFS_BINDIR)/%,$(TGT))
else
REAL_TARGET = $(TGT)
endif

LIB = libxtreemfs.a

CURRENT_CC_INCS = -I$(TOPDIR)/src

CC_FLAGS += $(FUSE_CC_FLAGS)
CC_INCS  += $(NEON_CC_INCS) $(JSON_CC_INCS) $(CURRENT_CC_INCS)
XTREEMFS_LIB_OBJS = xtreemfs.o \
                    mrc_channel.o mrc_request.o \
                    osd_manager.o osd_proxy.o osd_channel.o osd_request.o \
                    dirservice.o \
                    workqueue.o signals.o \
                    file_inv.o filerw.o fileobj.o file.o fobj_cache.o \
                    statinfo.o \
                    stripingpolicy.o stripe.o sobj_cache.o \
                    transl_engine.o transl_mod.o \
                    lease.o obj_set.o \
                    xcap.o xcap_inv.o xloc.o xattr.o \
                    acl.o \
                    xtreemfs_fuse.o xtreemfs_utils.o xtreemfs_security.o xtreemfs_conf.o xtreemfs_prot.o \
                    request.o mrc_comm.o \
                    creds.o \
                    hashtable.o \
                    radix-tree.o \
                    logger.o bench_timer.o \
                    lock_utils.o platforms.o metadata_cache.o

ifdef ITAC
XTREEMFS_LIB_OBJS += xtreemfs_itac.o
endif

CC_OBJS           = main.o $(XTREEMFS_LIB_OBJS)
#                    monitor.o xtreemfs_net_utils.o net_utils.o
CC_LIBS          += $(FUSE_CC_LIBS) $(NEON_CC_PARTSH_LIBS)
#CC_LIBS          += $(FUSE_CC_LIBS) $(NEON_CC_PARTSH_LIBS) -lefence
CC_STATIC_LIBS    = $(NEON_CC_STATIC_LIBS) $(JSON_CC_STATIC_LIBS)

ifdef ITAC
CC_FLAGS += $(ITAC_CC_FLAGS) -DITAC
CC_INCS  += $(ITAC_CC_INCS)
CC_LIBS  += $(ITAC_CC_LIBS)
endif

ifeq ($(MSYSTEM), MINGW32)
CC_INCS  += -I../win32/fuse4win/include -I../win32/pthreads/include 
CC_LIBS  =  -L../win32/fuse4win -lfuse_static -L../win32/pthreads/lib -lpthreadGCE2 -L../win32/openssl-0.9.8i/  -lssl -lcrypto  -L/c/Programme/MinGW/lib/ -lstdc++ -lwsock32  -ladvapi32 -lgdi32
endif

CC_FLAGS += -DCONFIG_SMP -fPIC
CC_LDFLAGS += -rdynamic

all:	$(REAL_TARGET) $(LIB) mods

tools:  mkvol lsvol rmvol


ifeq ($(MSYSTEM), MINGW32)
$(REAL_TARGET):	$(CC_OBJS)
	$(CC) $(CC_OBJS) $(CC_STATIC_LIBS) $(CC_LIBS) -o $(REAL_TARGET)
else
$(REAL_TARGET):	$(CC_OBJS)
	$(call PARTST_LD,$@,$^,$(CC_STATIC_LIBS),$(CC_LIBS))
endif


%.o:	%.c
	$(CC) $(CC_FLAGS) $(CC_INCS) -c $<

clean:
	$(RM) $(CC_OBJS) $(LIB)
	$(MAKE) -C mods clean

distclean:	clean
	$(RM) $(REAL_TARGET) $(LIB)
	$(RM) *~
	$(MAKE) -C mods distclean

.PHONY:	mods
mods:
	$(MAKE) -C mods all

.PHONY: docs
docs:
	doxygen

#### Rules for building a xtreemfs library ####
.PHONY:	lib
lib:	$(LIB)

$(LIB): $(XTREEMFS_LIB_OBJS)
	$(AR) rv $@ $^


