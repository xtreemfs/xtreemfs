MODULES = raid0 raid5

-include $(TOPDIR)/make.config

CURRENT_CC_INCS = -I$(TOPDIR)/src

CC_FLAGS        += -fPIC
CC_INCS         = $(CURRENT_CC_INCS) $(JSON_CC_INCS) $(NEON_CC_INCS)

raid0_CC_OBJS = raid0.o
raid5_CC_OBJS = raid5.o

all:	$(MODULES)

.PHONY:	raid0
raid0:	raid0.so

.PHONY:	raid5
raid5:	raid5.so

raid0.so:	$(raid0_CC_OBJS)
	$(CC_LD) $(CC_LDFLAGS) -shared -o $(@).so $(raid0_CC_OBJS)

raid5.so:	$(raid5_CC_OBJS)
	$(CC_LD) $(CC_LDFLAGS) -shared -o $(@).so $(raid5_CC_OBJS)

%.o:	%.c
	$(CC) $(CC_FLAGS) $(CC_INCS) -c $<

clean:
	$(RM) $(raid0_CC_OBJS) $(raid5_CC_OBJS)

distclean:	clean
	$(RM) $(MODULES)
