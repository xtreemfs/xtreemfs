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
* C Interface: xtreemfs
* 
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#ifndef __XTRFS_XTREEMFS_H__
#define __XTRFS_XTREEMFS_H__

#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <fuse.h>

#include "mrc_request.h"
#include "osd_manager.h"
#include "file_inv.h"
#include "file.h"
#include "xcap_inv.h"
#include "sobj_cache.h"
#include "transl_engine.h"
#include "creds.h"

#include "hashtable.h"

#include "bench_timer.h"
#include "logger.h"

#include "platforms.h"

#ifdef XTREEMOS_ENV
#include <xos_ams.h>
#endif

/* Define a pseudo block size */
#define XTREEMFS_PSEUDO_BLOCKSIZE 512

/* XTREEMFS_MODDIR environment variable */
#define XTREEMFS_MODDIR "XTREEMFS_MODDIR"

/* This should be dynamically configurable */
#define NUM_MRCOMM_THREADS 4
#define NUM_SOBJ_THREADS   8
#define NUM_FILERW_THREADS 4
#define NUM_FOBJ_THREADS   8

extern int xtreemos_env;
extern int xtreemos_ams_sock;
extern mutex_t xtreemos_ams_mutex;

extern struct sobj_cache sobj_cache;
extern struct transl_engine transl_engine;

extern struct xtreemfs_conf conf;
extern struct dirservice *dirservice;

/**
 * Get credentials in local context, i.e. from the current process.
 */
static inline void get_local_creds(struct creds *c)
{
#ifdef XTREEMOS_ENV
	AMS_GPASSWD gpwd;
	AMS_GGROUPS ggrp;
#endif

	creds_init(c);

	c->uid = geteuid();
	c->gid = getegid();
	c->pid = getpid();

	// add grid credentials below
#ifdef XTREEMOS_ENV
	pthread_mutex_lock(&xtreemos_ams_mutex);
	if (amsclient_invmappinginfo_internal(xtreemos_ams_sock,
					      NULL, c->uid, NULL, c->gid,
					      &gpwd, &ggrp))
	{
		err_msg("Cannot get information for (%d,%d) from AMS.\n",
			c->uid, c->gid);
		/* Revert to local method. */
		c->guid = NULL;
		c->ggid = NULL;
		c->subgrp = NULL;
	} else {
		dbg_msg("Global user name: %s\n", gpwd.g_idtoken.g_dn);
		dbg_msg("VO: %s\n", ggrp.g_grptoken.g_vo);
		/* dbg_msg("Global group name: %s\n", gpwd.g_idtoken.g_ggid); */
		c->guid = strdup(gpwd.g_idtoken.g_dn);
		c->ggid = strdup(ggrp.g_grptoken.g_vo);
		c->subgrp = strdup(ggrp.g_grptoken.g_subgroup);
	}
	pthread_mutex_unlock(&xtreemos_ams_mutex);
#endif
}

extern struct OSD_Manager osd_manager;

extern struct file_inv	file_inv;
extern struct xcap_inv	xcap_inv;

extern char *mrc_url;
extern char *volume;

extern ne_ssl_context *ssl_ctx;
extern ne_ssl_client_cert *xtreemfs_client_cert;
extern ne_ssl_certificate *xtreemfs_ca_cert;

extern int xtreemfs_init(char *mrc_url);
extern void xtreemfs_exit();

extern char *xtreemfs_full_path(const char *path);

extern void xtreemfs_get_unix_creds(char *ownerId, char *groupId, uid_t *uid, gid_t *gid);
extern int xtreemfs_get_unix_ids(char **userId, char **groupId, uid_t uid, gid_t gid);

extern int xtreemfs_load_mods(struct transl_engine *te, char *mod_dir);
extern void xtreemfs_unload_mods(struct transl_engine *te);

extern int sighandler_install(void);
extern void sighandler_stop(void);

extern struct bench_timings read_timings;
extern struct bench_timings write_timings;
extern struct bench_timer   read_timer;
extern struct bench_timer   write_timer;

extern struct bench_timings neon_timings;
extern struct bench_timer   neon_timer;

extern struct work_queue *client_wq;

#endif
