-include $(TOPDIR)/make.config

TGT = timer_test

CC_INCS        = $(CURRENT_CC_INCS)
CC_COMMON_OBJS = bench_timer.o
CC_LOCAL_OBJS  = main.o
CC_OBJS        = $(CC_LOCAL_OBJS) $(CC_COMMON_OBJS)
CC_LIBS        = -lpthread

CC_FLAGS      += -DTIMINGS

all:	$(TGT)

$(TGT):	$(CC_OBJS)
	$(CC_LD) $(CC_LDFLAGS) -o $(TGT) $^ $(CC_LIBS)

.SECONDEXPANSION:
$(CC_COMMON_OBJS):	$$(patsubst %.o,../../src/%.c,$$@)
	$(CC) $(CC_FLAGS) $(CC_INCS) -c -o $@ $<

%.o:	%.c
	$(CC) $(CC_FLAGS) $(CC_INCS) -c -o $@ $<

clean:
	$(RM) $(CC_OBJS)

distclean:	clean
	$(RM) $(TGT)
	$(RM) *~

