-include $(TOPDIR)/make.config

TGT = striping_policy_test

CC_INCS        += $(JSON_CC_INCS) $(NEON_CC_INCS) $(CURRENT_CC_INCS)
CC_COMMON_OBJS  = stripingpolicy.o
CC_LOCAL_OBJS   = main.o
CC_OBJS         = $(CC_LOCAL_OBJS) $(CC_COMMON_OBJS)
CC_STATIC_LIBS += $(JSON_CC_STATIC_LIBS) $(NEON_CC_STATIC_LIBS)
CC_LIBS        += $(NEON_CC_PARTSH_LIBS) -lpthread



all:	$(TGT)

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

