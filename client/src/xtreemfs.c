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
*  C Implementation: xtreemfs
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <string.h>
#include <sys/types.h>

#ifndef _WIN32
#include <pwd.h>
#include <grp.h>
#else
#include <windows.h>
#define boolean json_boolean
#endif

#include <pthread.h>

#include <fuse.h>
#include <ne_uri.h>

#include "xtreemfs_utils.h"

#include "xtreemfs.h"
#include "xtreemfs_conf.h"
#include "xcap_inv.h"
#include "sobj_cache.h"
#include "logger.h"
#include "mrc_request.h"
#include "mrc_comm.h"

#include "filerw.h"
#include "stripe.h"

/* Configurations aspects */
char *volume;
char *mrc_url;
int xtreemos_env = 0;

struct xtreemfs_conf conf;

/* Sobj cache */
struct sobj_cache sobj_cache;

/* Translation engine */
struct transl_engine transl_engine;

/* AMS connection */
int xtreemos_ams_sock;
mutex_t xtreemos_ams_mutex;

/* Connection to directory service */
struct dirservice *dirservice = NULL;

/* Entry point for managing current OSDs */
struct OSD_Manager osd_manager;

struct file_inv    file_inv;

struct xcap_inv xcap_inv;

/* Timing */
struct bench_timings read_timings;
struct bench_timings write_timings;
struct bench_timer   read_timer;
struct bench_timer   write_timer;

struct work_queue *client_wq = NULL;


#ifdef ITAC
int itac_filerw_class;
int itac_fobj_class;
int itac_sobj_class;

int itac_filerw_handle_hdl;
int itac_fobj_handle_hdl;
int itac_sobj_handle_hdl;
#endif

ne_ssl_context *ssl_ctx = NULL;
ne_ssl_client_cert *xtreemfs_client_cert = NULL;
ne_ssl_certificate *xtreemfs_ca_cert = NULL;

pthread_attr_t *xpattr;


int
extract_mrc_and_vol(char *vol_url, char **mrc_url, char **vol_name)
{
	int rv = 0;
	ne_uri volume_uri;

	dbg_msg("Analysing volume URL: '%s'\n", vol_url);
	
	ne_uri_parse((const char *)vol_url, &volume_uri);
	
	if(volume_uri.path != NULL) {
		if(volume_uri.path[0] == '/')
			*vol_name = strdup(&volume_uri.path[1]);
		else
			*vol_name = strdup(volume_uri.path);
		free(volume_uri.path);
		
		volume_uri.path = strdup("/");
		*mrc_url = ne_uri_unparse(&volume_uri);
		dbg_msg("MRC URL: '%s'\n", *mrc_url);
	}
	else {
		rv = 1;
	}
	ne_uri_free(&volume_uri);

	return rv;
}


int
xtreemfs_parse_mount_options(int argc, char **argv)
{
	return 0;
}

int
xtreemfs_init(char *vol_url)
{
	int rv = 0;
	ne_uri vol_uri;
	struct req *req;
	struct creds creds;
	struct MRC_Req_get_volumes_resp resp;
	int err = 0;
	int i;

	sighandler_install();

	creds_init(&creds);

	ne_sock_init();
	if (str_to_uri_ds(vol_url, &vol_uri, MRC_DEFAULT_PORT, dirservice)) {
		rv = 1;
		goto out;
	}
	
	mrc_url = extract_mrc(&vol_uri);
	volume  = extract_volume(&vol_uri);
	ne_uri_free(&vol_uri);

	if (!volume) {
		rv = 4;
		goto out;
	}

	/* Before we start we need to check if the volume is available on
	   the MRC. For that we need to start the MRC communication
	   infrastructure. This will be replaced by a MRC utility
	   function that avoids starting the whole infrastructure if
	   it is not needed.
	*/

	mrccomm_init(mrc_url, NUM_MRCOMM_THREADS);

	/* Now see if we can get volume information */
	get_local_creds(&creds);
	req = MRC_Request__getVolumes(&err, &resp, &creds, NULL, 1);
	if (!req) {
		rv = 1;
		goto out;
	}
	err = execute_req(mrccomm_wq, req);

	// req_del(req);
	if (!err) {
		for(i=0; i<resp.num_vols; i++) {
			if (!strcmp(resp.names[i], volume))
				break;
		}
		if (resp.num_vols == 0 || i == resp.num_vols)
			rv = 2;

		/* And delete all */
		for(i=0; i<resp.num_vols; i++) {
			free(resp.ids[i]);
			free(resp.names[i]);
		}
		free(resp.ids);
		free(resp.names);

		if (rv)
			goto out;
	} else {
		rv = 3;
		goto out;
	}
	mrccomm_destroy();

	/* If timing is enabled then the timers are initialized here. */

	bench_timings_init(&read_timings);
	bench_timings_init(&write_timings);
	bench_timer_init(&read_timer);
	bench_timer_init(&write_timer);
	
	bench_timings_init(&neon_timings);
	bench_timer_init(&neon_timer);
	
	bench_data_collection_init(&osd_proxy_bandwidth);
	
out:
	creds_delete_content(&creds);
	return rv;
}


void
xtreemfs_exit()
{
	sighandler_stop();

	bench_timings_clear(&read_timings);
	bench_timings_clear(&write_timings);
	bench_timings_clear(&neon_timings);
	bench_timings_clear(&osd_proxy_bandwidth);

	sobj_close();
	fobj_close();
	filerw_close();

	dbg_msg("Exiting xtreemfs.\n");

	// MRC_Proxy_stop(&mrc_proxy);
	ne_sock_exit();
	free(mrc_url);
	free(volume);
}

char *
xtreemfs_full_path(const char *path)
{
	return create_fullname(volume, path);
}

#if 0
void
xtreemfs_set_unix_creds(struct MRC_Request *mr)
{
	struct fuse_context *ctx;

	ctx = fuse_get_context();
	mr->uid = ctx->uid;
	mr->gid = ctx->gid;
	mr->pid = ctx->pid;
	
	dbg_msg("uid: %d\n", ctx->uid);
	dbg_msg("gid: %d\n", ctx->gid);
	dbg_msg("pid: %d\n", ctx->pid);
}
#endif

#ifndef _WIN32
void
xtreemfs_get_unix_creds(char *ownerId, char *groupId, uid_t *uid, gid_t *gid)
{
	char username[256];
	char groupname[256];
	char *p;
	int i, j, l;
	struct passwd passwd, *pwd_res;
	char pwd_str[1024];
	struct group  group, *grp_res;
	char grp_str[1024];
		
	dbg_msg("ownerId:  '%s'\n", ownerId);
	
	l = strlen(ownerId);
	j = 0;
	while((ownerId[j] == ' ' || ownerId[j] == '\t') && j < l) j++;
	
	p = username;
	i = 0;
	while(ownerId[j] != ' ' && ownerId[j] != '\t' && ownerId[j] != '\0' && i<256 && j<l) {
		p[i] = ownerId[j];
		i++;
		j++;
	}
	p[i] = '\0';
	
	l = strlen(groupId);
	j = 0;
	while((groupId[j] == ' ' || groupId[j] == '\t') && j < l) j++;

	p = groupname;
	i = 0;
	while(groupId[j] != ' ' && groupId[j] != '\t' && groupId[j] != '\0' && i<256 && j<l) {
		p[i] = groupId[i];
		i++;
		j++;
	}
	p[i] = '\0';
	
	dbg_msg("User name '%s'; group name '%s'\n", username, groupname);

	*uid = 0;
	*gid = 0;

	getpwnam_r(username, &passwd, pwd_str, 1024, &pwd_res);
	if (pwd_res)
		*uid = pwd_res->pw_uid;

	getgrnam_r(groupname, &group, grp_str, 1024, &grp_res);
	if (grp_res)
		*gid = grp_res->gr_gid;
}

int xtreemfs_get_unix_ids(char **userId, char **groupId, uid_t uid, gid_t gid)
{
	int err = 0;
	struct passwd passwd, *pwd_res;
	char pwd_str[1024];
	struct group  group, *grp_res;
	char grp_str[1024];
	
	*userId  = NULL;
	*groupId = NULL;

	if ((int)gid >= 0) {
		getgrgid_r(gid, &group, grp_str, 1024, &grp_res);
		if (!grp_res) {
			err = 1;
			dbg_msg("Group not found.\n");
			goto finish;
		}
		*groupId = strdup(grp_res->gr_name);
	}

	if ((int)uid >= 0) {
		getpwuid_r(uid, &passwd, pwd_str, 1024, &pwd_res);
		if (!pwd_res) {
			err = 1;
			dbg_msg("Password entry not found.\n");
			goto finish;
		}
		*userId = strdup(pwd_res->pw_name);
	}

finish:
	return err;
}

#else /* _WIN32 */
void
xtreemfs_get_unix_creds(char *ownerId, char *groupId, uid_t *uid, gid_t *gid)
{
	*uid = 0;
	*gid = 0;
}

int xtreemfs_get_unix_ids(char **userId, char **groupId, uid_t uid, gid_t gid)
{
	char buffer[100];
	DWORD len = 100;
	GetUserName(buffer, &len);
	*userId = strdup(buffer);
	*groupId = strdup("");
	return 0;
}

#endif /* _WIN32 */

int xtreemfs_load_mods(struct transl_engine *te, char *mod_dir)
{
	int err = 0;
	char check_dir[1024];
	char *xtreemfs_mod_dir = NULL;
	char *xtreemfs_start_dir = NULL;

	/* If the user has set the 'mod_dir' options it will override all
	   other settings. If it is not set we look for the 'XTREEMFS_MODDIR'
	   environment variable and then we try to find modules by the
	   base directory of the user space program. */

	transl_engine_init(&transl_engine, mod_dir);

	/* First let's check if we have a 'mod_dir' argument. */
	if (!mod_dir) {
		char *envp = NULL;

		envp = getenv(XTREEMFS_MODDIR);
		if (envp) {
			dbg_msg("XTREEMFS_MODDIR = '%s'\n", envp);
			xtreemfs_mod_dir = strdup(envp);
		} else {
			char *p = NULL;

			/* We do not have an environment variable at hand. So
			   we must try to figure out a module directory by
			   the path of the executable. */
			if (!xtreemfs_start_dir)
				xtreemfs_start_dir = current_basedir();
			if (!xtreemfs_start_dir) {
				err = 1;
				goto out;
			}

			/* First check '<basedir>/mods' */
			snprintf(check_dir, 1024, "%s/mods", xtreemfs_start_dir);
			dbg_msg("Trying dir '%s'\n", check_dir);
			if (!transl_engine_load_mods(te, check_dir)) {
				goto out;
			}

			/* The check '<basedir>/../mods' */
			strncpy(check_dir, xtreemfs_start_dir, 1024);
			p = rindex(check_dir, '/');
			if (p) {
				*p = '\0';
				p = strncat(check_dir, "/mods", 1024);
				if (!p) {
					err = 2;
					goto out;
				}
				dbg_msg("Trying dir '%s'\n", check_dir);
				if (!transl_engine_load_mods(te, check_dir)) {
					goto out;
				}
			}
		}
	}
out:
	if (xtreemfs_mod_dir)
			free(xtreemfs_mod_dir);
	if (xtreemfs_start_dir)
			free(xtreemfs_start_dir);
	return err;
}

void xtreemfs_unload_mods(struct transl_engine *te)
{
	transl_engine_unload_mods(te);
}
