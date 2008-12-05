TOPDIR ?= ../../..
-include $(TOPDIR)/make.config

TGT = wqtest

CC_INCS        += $(JSON_CC_INCS) $(NEON_CC_INCS) $(CURRENT_CC_INCS)
CC_COMMON_OBJS  = workqueue.o request.o logger.o
CC_LOCAL_OBJS   = main.o stage1.o stage2.o
CC_OBJS         = $(CC_LOCAL_OBJS) $(CC_COMMON_OBJS)
CC_STATIC_LIBS += $(JSON_CC_STATIC_LIBS) $(NEON_CC_STATIC_LIBS)
CC_LIBS        += $(JSON_CC_PARTSH_LIBS) $(NEON_CC_PARTSH_LIBS)

CC_FLAGS       += -D_FILE_OFFSET_BITS=64 -g -O0
CC_LDFLAGS     += -g -O0

all:

$(TGT):	$(CC_OBJS)
	$(call PARTST_LD,$@,$^,$(CC_STATIC_LIBS),$(CC_LIBS))

.SECONDEXPANSION:
$(CC_COMMON_OBJS):	$$(patsubst %.o,../../../src/%.c,$$@)
	$(CC) $(CC_FLAGS) $(CC_INCS) -c -o $@ $<

%.o:	%.c
	$(CC) $(CC_FLAGS) $(CC_INCS) -c -o $@ $<

clean:
	$(RM) $(CC_OBJS)

distclean:	clean
	$(RM) $(TGT)
	$(RM) *~

