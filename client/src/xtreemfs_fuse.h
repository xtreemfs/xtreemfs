/* Copyright (c) 2007, 2008  Matthias Hess, Erich Focht
   This file is part of XtreemFS.

   XtreemFS is part of XtreemOS, a Linux-based Grid Operating
   System, see <http://www.xtreemos.eu> for more details. The
   XtreemOS project has been developed with the financial support
   of the European Commission's IST program under contract
   #FP6-033576.

   XtreemFS is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   XtreemFS is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of 
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
C Interface: xtreemfs

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2006

Copyright: See COPYING file that comes with this distribution

*/

#include <syslog.h>
#include <fuse.h>

#define XTREEMFS_ENTER() trace_msg("Entering.\n")
#define XTREEMFS_LEAVE() trace_msg("Leaving.\n")

extern struct fuse_operations xtreemfs_ops;

extern int xtreemfs_getattr(const char *, struct stat *);
extern int xtreemfs_readlink(const char *, char *, size_t);
extern int xtreemfs_getdir(const char *, fuse_dirh_t, fuse_dirfil_t);
extern int xtreemfs_mknod(const char *, mode_t, dev_t);
extern int xtreemfs_mkdir(const char *, mode_t);

extern int xtreemfs_unlink(const char *);
extern int xtreemfs_rmdir(const char *);
extern int xtreemfs_symlink(const char *, const char *);
extern int xtreemfs_rename(const char *, const char *);
extern int xtreemfs_link(const char *, const char *);
extern int xtreemfs_chmod(const char *, mode_t);
extern int xtreemfs_chown(const char *, uid_t, gid_t);
extern int xtreemfs_truncate(const char *, off_t);
extern int xtreemfs_utime(const char *, struct utimbuf *);
extern int xtreemfs_open(const char *, struct fuse_file_info *);
extern int xtreemfs_read(const char *, char *, size_t, off_t, struct fuse_file_info *);
extern int xtreemfs_write(const char *, const char *, size_t, off_t, struct fuse_file_info *);
extern int xtreemfs_statfs(const char *, struct statvfs *);
extern int xtreemfs_flush(const char *, struct fuse_file_info *);
extern int xtreemfs_release(const char *, struct fuse_file_info *);
extern int xtreemfs_fsync(const char *, int, struct fuse_file_info *);
extern int xtreemfs_setxattr(const char *, const char *, const char *, size_t, int);
extern int xtreemfs_getxattr(const char *, const char *, char *, size_t);
extern int xtreemfs_listxattr(const char *, char *, size_t);
extern int xtreemfs_removexattr(const char *, const char *);
extern int xtreemfs_opendir(const char *, struct fuse_file_info *);
extern int xtreemfs_readdir(const char *, void *, fuse_fill_dir_t, off_t, struct fuse_file_info *);
extern int xtreemfs_releasedir(const char *, struct fuse_file_info *);
extern int xtreemfs_fsyncdir(const char *, int, struct fuse_file_info *);

extern void *xtreemfs_fuse_init(struct fuse_conn_info *);
extern void xtreemfs_fuse_destroy(void *);

extern int xtreemfs_access(const char *, int);

extern int xtreemfs_create(const char *, mode_t, struct fuse_file_info *);
extern int xtreemfs_ftruncate(const char *, off_t, struct fuse_file_info *);
extern int xtreemfs_fgetattr(const char *, struct stat *, struct fuse_file_info *);
