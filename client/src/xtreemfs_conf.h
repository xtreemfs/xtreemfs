/* Copyright (c) 2007, 2008  Matthias Hess

   Author: Matthias Hess

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
   C Interface: xtreemfs_conf

   Description:
*/

#ifndef __XTRFS_CONF_H__
#define __XTRFS_CONF_H__

#include <fuse.h>

struct xtreemfs_fuse_opts {

} xtreemfs_fuse_opt;

extern struct xtreemfs_fuse_opts xtreemfs_fuse_opt;


struct xtreemfs_conf {
	char *uuid;		/*!< UUID of this host */

	char *basedir;		/*!< Where to store and read configs, modules etc */
	char *conf_file;

	char *volume_url;

	/* For development purposes only. The directory service will
	   be integrated into the volume description when the functionality
	   works. */
	char *dirservice;
	char *logfile;
	int   debugging;
	int   tracing;
	int   num_threads;
	int   stack_size;
	int   mem_trace;
#ifdef XTREEMOS_ENV
	int   xtreemos;
#endif
	/* Monitoring */
	int   monitoring;	/*!< Enable monitoring if != 0 */
	int   mon_port;
	int   req_ids;		/*!< Use request ids for debugging */

	/* Module handling */
	char *mod_dir;		/*!< Module directory. If not given explicitly
				     it will be created as '<basedir>/mods' */

	int   caching;		/*!< Enable caching if != 0 */
	int   clustermode;
	int   singlequeue;
	char *ssl_cert;		/*!< Client host certificate */

	struct fuse_args fargs;	/*!< Arguments for FUSE. */
};
extern struct xtreemfs_conf conf;

extern struct fuse_opt xtreemfs_opts[];

struct xtreemfs_conf *xtreemfs_conf_new();
int xtreemfs_conf_init(struct xtreemfs_conf *xc);
void xtreemfs_conf_del_contents(struct xtreemfs_conf *xc);
void xtreemfs_conf_destroy(struct xtreemfs_conf *xc);

struct xtreemfs_conf *xtreemfs_conf_read(char *filename);
int xtreemfs_conf_read_ip(struct xtreemfs_conf *xc, char *filename);
int xtreemfs_conf_write(struct xtreemfs_conf *xc, char *filename);

int xtreemfs_conf_parse_args_ip(struct xtreemfs_conf *xc, int args, char **argv);

int xtreemfs_conf_get_ip(struct xtreemfs_conf *xc, int argc, char **argv);

int xtreemfs_conf_print(struct xtreemfs_conf *xc);

#endif
