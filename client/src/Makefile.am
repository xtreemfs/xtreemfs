bin_PROGRAMS = xtfs_mount

xtfs_mount_SOURCES = main.c
xtfs_mount_LDADD = libxtreemfs.a ../json-c-0.7/libjson.la ../neon-0.26.3/src/libneon.la -lfuse
xtfs_mount_CPPFLAGS = -D_FILE_OFFSET_BITS=64 -DFUSE_USE_VERSION=26 -I../json-c-0.7 -I../neon-0.26.3/src

noinst_LIBRARIES = libxtreemfs.a
libxtreemfs_a_SOURCES = xtreemfs.c \
                         mrc_channel.c mrc_request.c \
                         osd_manager.c osd_proxy.c osd_channel.c osd_request.c \
                         workqueue.c signals.c \
                         file_inv.c filerw.c fileobj.c file.c fobj_cache.c \
                         raid_engine.c \
                         statinfo.c \
                         stripingpolicy.c stripe.c sobj_cache.c \
                         xcap.c xcap_inv.c xloc.c xattr.c \
                         acl.c \
                         xtreemfs_fuse.c xtreemfs_utils.c xtreemfs_security.c \
                         request.c mrc_comm.c \
                         creds.c \
                         hashtable.c \
                         radix-tree.c \
                         logger.c bench_timer.c \
                         lock_utils.c
libxtreemfs_a_CPPFLAGS = -D_FILE_OFFSET_BITS=64 -DFUSE_USE_VERSION=26  -I../json-c-0.7 -I../neon-0.26.3/src
libxtreemfs_a_LIBADD = ../json-c-0.7/libjson.la ../neon-0.26.3/src/libneon.la

noinst_HEADERS = fileops.h
