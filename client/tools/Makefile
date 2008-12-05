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

include $(TOPDIR)/make.config

TGTS = xtfs_mkvol xtfs_lsvol xtfs_rmvol

# This is cumbersome because it sets the real targets
# explicitly. This is better be done automatically because
# it makes adding new tools easier. The general idea behind
# this is to set the targets above and then leave the rest
# untouched.

ifdef XTFS_BINDIR
REAL_TGTS = $(patsubst %,$(XTFS_BINDIR)/%,$(TGTS))
REAL_LSVOL = $(XTFS_BINDIR)/xtfs_lsvol
REAL_MKVOL = $(XTFS_BINDIR)/xtfs_mkvol
REAL_RMVOL = $(XTFS_BINDIR)/xtfs_rmvol
else
REAL_TGTS = $(TGTS)
REAL_LSVOL = xtfs_lsvol
REAL_MKVOL = xtfs_mkvol
REAL_RMVOL = xtfs_rmvol
endif

all: $(REAL_TGTS)

SRCDIR = $(TOPDIR)/src
LIB = $(SRCDIR)/libxtreemfs.a

CC_FLAGS += $(FUSE_CC_FLAGS)
CC_INCS  += -I$(SRCDIR) $(NEON_CC_INCS) $(JSON_CC_INCS) $(CURRENT_CC_INCS)

#### Common macros for tools ####

TOOLS_INCS        = -I$(SRCDIR) $(NEON_CC_INCS) $(JSON_CC_INCS)
TOOLS_STATIC_LIBS = -L$(SRCDIR) -lxtreemfs \
                    $(NEON_CC_STATIC_LIBS) $(JSON_CC_STATIC_LIBS)
TOOLS_LIBS = $(NEON_CC_PARTSH_LIBS)

ifdef ITAC
CC_FLAGS += $(ITAC_CC_FLAGS) -DITAC
CC_INCS  += $(ITAC_CC_INCS)
TOOLS_LIBS += $(ITAC_CC_LIBS)
endif

ifeq ($(XTREEMOS_ENV),y)
TOOLS_LIBS  += $(XTREEMOS_CC_LIBS)
endif

TOOLS_OBJS = lsvol.o mkvol.o rmvol.o mount.xtreemfs.o \
             tool_utils.o

$(REAL_LSVOL):  lsvol.o tool_utils.o $(LIB)
	$(call PARTST_LD,$@,$^,$(TOOLS_STATIC_LIBS),$(TOOLS_LIBS))

$(REAL_MKVOL):  mkvol.o tool_utils.o $(LIB)
	$(call PARTST_LD,$@,$^,$(TOOLS_STATIC_LIBS),$(TOOLS_LIBS))

$(REAL_RMVOL):  rmvol.o tool_utils.o $(LIB)
	$(call PARTST_LD,$@,$^,$(TOOLS_STATIC_LIBS),$(TOOLS_LIBS))

addrepl:	addrepl.o tool_utils.o $(LIB)
	$(call PARTST_LD,$@,$^,$(TOOLS_STATIC_LIBS),$(TOOLS_LIBS))

.SECONDEXPANSION:
$(TOOLS_OBJS):	$$(patsubst %.o,%.c,$$@)
	$(CC) $(CC_FLAGS) $(CC_INCS) $(TOOLS_INCS) -c -o $@ $<

#### Rule for building library (should not be needed)

$(LIB):
	make -C $(SRCDIR)

#### General rule for all 'local' source files ####

%.o:	%.c
	$(CC) $(CC_FLAGS) $(CC_INCS) -c -o $@ $<

clean:
	$(RM) $(TOOLS_OBJS) *.o

distclean:	clean
	$(RM) $(REAL_TGTS)
	$(RM) *~
