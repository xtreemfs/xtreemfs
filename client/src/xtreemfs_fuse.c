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
* FUSE interface calls for XtreemFS filesystem.
*
*
* Authors: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2006, 2007, 2008
*          Erich Focht <efocht@hpce.nec.com>
*
* Copyright: See COPYING file that comes with this distribution
*
*/
#include <stdint.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <fuse.h>

#ifdef XTREEMOS_ENV
#include <xos_ams.h>
#endif

#ifdef ITAC
#include <xtreemfs_itac.h>
#endif

#ifdef _WIN32
#include <winsock2.h>
#define ENOTCONN WSAENOTCONN
#define boolean json_boolean
#endif

#include "kernel_substitutes.h"

#include "xtreemfs.h"
#include "xtreemfs_conf.h"
#include "xtreemfs_fuse.h"

#include "osd_manager.h"
#include "mrc_comm.h"
#include "metadata_cache.h"

#include "filerw.h"
#include "file.h"

#include "statinfo.h"
#include "stripingpolicy.h"
#include "stripe.h"
#include "xattr.h"
#include "xloc.h"
#include "xcap_inv.h"
#include "xcap.h"

#include "bench_timer.h"
#include "lock_utils.h"
#include "logger.h"

/**
 * Get credentials in FUSE context, i.e. from the process calling the
 * FUSE filesystem function.
 */
static inline void get_fuse_creds(struct creds *c)
{
	struct fuse_context *ctx = fuse_get_context();
#ifdef XTREEMOS_ENV
	AMS_GPASSWD gpwd;
	AMS_GGROUPS ggrp;
#endif

	creds_init(c);

	c->uid    = ctx->uid;
	c->gid    = ctx->gid;
	c->pid    = ctx->pid;
	// add grid credentials below

#ifdef XTREEMOS_ENV
	pthread_mutex_lock(&xtreemos_ams_mutex);
	if (amsclient_invmappinginfo_internal(xtreemos_ams_sock,
					      NULL, c->uid, NULL, c->gid,
					      &gpwd, &ggrp))
	{
		dbg_msg("Cannot get information for (%d,%d) from AMS.\n",
			c->uid, c->gid);
		/* Revert to local method. */
		c->guid = NULL;
		c->ggid = NULL;
		c->subgrp = NULL;
	} else {
		dbg_msg("Global user name: %s\n", gpwd.g_idtoken.g_dn);
		dbg_msg("VO: %s\n", ggrp.g_grptoken.g_vo);
		c->guid = strdup(gpwd.g_idtoken.g_dn);
		c->ggid = strdup(ggrp.g_grptoken.g_vo);
		c->subgrp = strdup(ggrp.g_grptoken.g_subgroup);
	}
	pthread_mutex_unlock(&xtreemos_ams_mutex);
#endif

}


struct xtreemfs_fuse_opts xtreemfs_fuse_opt;

struct fuse_operations xtreemfs_ops = {
	.getattr     = xtreemfs_getattr,
	.readlink    = xtreemfs_readlink,
	//.getdir      = xtreemfs_getdir,
	//.mknod       = xtreemfs_mknod,
	.mkdir       = xtreemfs_mkdir,
	
	.unlink      = xtreemfs_unlink,
	.rmdir       = xtreemfs_rmdir,
	.symlink     = xtreemfs_symlink,
	.rename      = xtreemfs_rename,
	.link        = xtreemfs_link,
	.chmod       = xtreemfs_chmod,
	.chown       = xtreemfs_chown,
	.truncate    = xtreemfs_truncate,
	.utime       = xtreemfs_utime,     // utime is not yet implemented
	                                   // but touch uses it, so keep the
	                                   // fake for now
	.open        = xtreemfs_open,
	.read        = xtreemfs_read,
	.write       = xtreemfs_write,
	//.statfs      = xtreemfs_statfs,
	.flush       = xtreemfs_flush,
	.release     = xtreemfs_release,
	.fsync       = xtreemfs_fsync,
	.setxattr    = xtreemfs_setxattr,
	.getxattr    = xtreemfs_getxattr,
	.listxattr   = xtreemfs_listxattr,
	.removexattr = xtreemfs_removexattr,
	//.opendir     = xtreemfs_opendir,
	.readdir     = xtreemfs_readdir,
	//.releasedir  = xtreemfs_releasedir,
	//.fsyncdir    = xtreemfs_fsyncdir,
	
	.init        = xtreemfs_fuse_init,
	.destroy     = xtreemfs_fuse_destroy,
	
	.access      = xtreemfs_access,
	
	.create      = xtreemfs_create,
	.ftruncate   = xtreemfs_ftruncate,
	//.fgetattr    = xtreemfs_fgetattr
};


int
xtreemfs_getattr(const char *path, struct stat *stbuf)
{
	int err = 0;
	struct req *req;
	struct creds c;
	struct xtreemfs_statinfo si;
	struct fileid_struct *fileid = NULL;
#ifdef XTREEMOS_ENV
	AMS_GPASSWD gpwd;
	AMS_GGROUPS pgrp;
	/* struct tcp_info tcp_info; */
#endif
	char *fullpath = NULL;
	
	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	trace_msg("Getting attribute for path '%s'\n", path);

	xtreemfs_statinfo_init(&si);
		
	fullpath = xtreemfs_full_path(path);

	/* Check if we have a local entry in file inventory for it */
	dbg_msg("Checking for fileid in inventory.\n");
	fileid = file_inv_get_by_path(&file_inv, fullpath, 1);
	if (fileid) {
		dbg_msg("Fileid found in inv. Locking it.\n");
		spin_lock(&fileid->flush);
	}

#if 0
	if (!metadata_cache_get(&md_cache, path, &si)) {
		req = MRC_Request__stat(&err, &si, fullpath, 0, 0, 0, &c,
						NULL, 1);
		if (req)
			err = execute_req(mrccomm_wq, req);
	else
			err = -ENOMEM;
	} else {
		dbg_msg("Entity found in metadata cache\n");
	}
#else
	req = MRC_Request__stat(&err, &si, fullpath, 0, 0, 0, &c,
				NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

#endif

	if (!err) {
		dbg_msg("Entity found on MRC!\n");
		dbg_msg("POSIX access mode: %ld\n", si.posixAccessMode);
		/* Looks like we already have that entity on the mrc! */
		memset((void *)stbuf, 0, sizeof(struct stat));
		switch(si.objType) {
			case OBJ_FILE:
				stbuf->st_mode = S_IFREG;
				break;
			case OBJ_DIR:
				stbuf->st_mode = S_IFDIR;
				break;
#ifndef _WIN32
			case OBJ_SYMLINK:
				stbuf->st_mode = S_IFLNK;
				break;
#endif
			default: /* Should never happen! */
				break;
		}
		stbuf->st_mode  |= si.posixAccessMode;
		stbuf->st_nlink  = si.linkCount;
		stbuf->st_size   = si.size;
		xtreemfs_get_unix_creds(si.ownerId, si.groupId, &stbuf->st_uid, &stbuf->st_gid);

#ifdef XTREEMOS_ENV
		pthread_mutex_lock(&xtreemos_ams_mutex);

#if 0
		/* Let's see if the socket is still open */
		getsockopt(xtreemos_ams_sock, SOL_SOCKET, TCP_INFO,
			   (void *)&tcp_info, sizeof(tcp_info));
		if (tcp_info.tcpi_state != TCP_ESTABLISHED) {
			xtreemos_ams_sock = amsclient_connect_open();
			if (xtreemos_ams_sock < 0) {
				fprintf(stderr, "Cannot reopen socket! This is a desaster!\n");
			}
		} else {
			dbg_msg("Socket is established and all is good!\n");
		}
#endif
		if (amsclient_mappinginfo_internal(xtreemos_ams_sock,
						   si.ownerId, si.groupId, si.groupId,
						   &gpwd, &pgrp) < 0) {
			dbg_msg("Cannot get local user and local group from AMS.\n");
		} else {
			stbuf->st_uid = gpwd.l_idtoken.g_mappeduid;
			stbuf->st_gid = gpwd.l_idtoken.g_mappedgid;
			dbg_msg("Local user:  %d\n", stbuf->st_uid);
			dbg_msg("Local group: %d\n", stbuf->st_gid);
		}
		pthread_mutex_unlock(&xtreemos_ams_mutex);
#endif

		// stbuf->st_uid    = 0;
		// stbuf->st_gid    = 0;
		stbuf->st_atime  = si.atime;
		stbuf->st_ctime  = si.ctime;
		stbuf->st_mtime  = si.mtime;
			
		if (fileid) {
			dbg_msg("Found fileid '%s' in cache.\n", fullpath);
			dbg_msg("Use count is %d\n", fileid_struct_read_uc(fileid));
			/* Check if we have to update the fileid */
			if (fileid->sz.size != si.size) {
				if (atomic_read(&fileid->sz.needs_updating) &&
				    si.epoch <= fileid->sz.epoch) {
					/* We believe we have the right size */
					dbg_msg("cached size is good: %lld\n", fileid->sz.size);
					stbuf->st_size = fileid->sz.size;
				} else {
					/* MRC has the real size */
					stbuf->st_size = si.size;
					fileid->sz.size = si.size;
					fileid->sz.epoch = si.epoch;
					// getattr has no order...
					// so don't change sz.order
					dbg_msg("set: size=%lld\n", fileid->sz.size);
					atomic_set(&fileid->sz.needs_updating, 0);
				}
			} else {
				dbg_msg("File size %lld is correct for our file!\n",
					fileid->sz.size);
			}
			dbg_msg("Unlocking fileid for '%s'\n", fullpath);
			fileid_struct_dec_uc(fileid); /* From get operation */
			/* spin_unlock(&fileid->lock); */
		} else {
			dbg_msg("Not found in inventory.\n");
		}
#ifndef _WIN32
		/* Now that we have the file size, we can set the block
		   count */
		stbuf->st_blocks =
			(stbuf->st_size ?
				stbuf->st_size - 1
				/ XTREEMFS_PSEUDO_BLOCKSIZE + 1
				: 0 );
#endif
	} else {
		err = -abs(err);
		dbg_msg("Error in request.\n");
	}

	if (fileid)
		spin_unlock(&fileid->flush);

	free(fullpath);

	dbg_msg("Freeing stat info.\n");
	xtreemfs_statinfo_clear(&si);
	dbg_msg("Leaving with error code %d\n", err);

	creds_delete_content(&c);
	XTREEMFS_LEAVE();
	return -abs(err);
}

int
xtreemfs_readlink(const char *path, char *linkbuf, size_t size)
{
	int err = 0;
	struct req *req;
	struct creds c;
	struct xtreemfs_statinfo si;
	
	char *fullpath = NULL;
	
	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	trace_msg("Getting attribute for path '%s'\n", path);

	xtreemfs_statinfo_init(&si);
	fullpath = xtreemfs_full_path(path);
	
	req = MRC_Request__stat(&err, &si, fullpath, 0, 0, 0, &c,
					NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	if (!err) {
		dbg_msg("Entity found on MRC!\n");

		/* Looks like we already have that entity on the mrc! */
		if (si.objType == OBJ_SYMLINK) {
			strncpy(linkbuf, si.linkTarget, size);
		}
	}

	dbg_msg("Path '%s', linkbuf == '%s', size == %d\n", path, linkbuf, size);
	dbg_msg("readlink not implemented, yet\n");
	xtreemfs_statinfo_clear(&si);
	free(fullpath);

	creds_delete_content(&c);	
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_getdir(const char *path, fuse_dirh_t h, fuse_dirfil_t filler)
{
	int rv = 0;

	XTREEMFS_ENTER();
	dbg_msg("getdir not implemented, yet\n");
	XTREEMFS_LEAVE();
	
	return rv;
}

int
xtreemfs_mknod(const char *path, mode_t mode, dev_t rdev)
{
	int rv = 0;

	XTREEMFS_ENTER();
	dbg_msg("mknod not implemented, yet\n");
	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_mkdir(const char *path, mode_t mode)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath;
	mode_t um;
	
	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);
	
	dbg_msg("Mkdir for full path '%s' in mode %d\n", fullpath, mode);
	
	um = umask(0);
	umask(um);
	
	req = MRC_Request__createDir(&err, fullpath, NULL, NULL, mode /* 0755 */,
				     &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	free(fullpath);
	creds_delete_content(&c);
	XTREEMFS_LEAVE();
	
	return -abs(err);
}

static void notify_osd_del(struct xloc_list *xlocs, struct xcap *xcap)
{
	struct list_head *iter;
	struct xlocs *xl;
	int i;
	struct OSD_Proxy *op;
	struct OSD_Channel_resp channel_resp;

	list_for_each(iter, &xlocs->head) {
		xl = container_of(iter, struct xlocs, head);
		/* We need only the first OSD from each list. */
		i = 0;
		/* for (i=0; i<xl->num_osds; i++) { */
			op = OSD_Manager_get_proxy(&osd_manager, xl->osds[i], dirservice);
			if (!op) {
				dbg_msg("Cannot get proxy for OSD '%s'\n", xl->osds[i]);
				break;
			}
			OSD_Proxy_del(op, xcap->fileID,
				      xlocs, xcap,
				      &channel_resp);
		/* } */
	}
}

/**
 * Delete a file from the file system.
 */
int
xtreemfs_unlink(const char *path)
{
	int err = 0;
	struct req *req;
	struct creds c;
	struct MRC_Req_delete_resp resp = { .xcap = NULL, .xlocs = NULL };
	char *fullpath;
	struct fileid_struct *fileid;

	
	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	fileid = file_inv_get_by_path(&file_inv, fullpath, 1);
	
	/* We will mark the fileid to indicate that it is to be 
	   deleted. If there are still processes using that file
	   (use count > 0) we do nothing. Otherwise the file will
	   be deleted from the MRC, all the OSDs and from the file
	   inventory.
	   The file will also be erased if we do not have it in 
	   our inventory. This means simply passing the command to
	   the MRC and OSDs.
	   The test for the '2': We have one because of 'file_inv_get_by_path'
	   and another one because the fileid struct is given away in
	   the inventory.
	*/
	if (fileid) {
		atomic_set(&fileid->to_be_erased, 1);		
		dbg_msg("File use count: %d\n", fileid_struct_read_uc(fileid));
	}

        /* always delete the file from the MRC, even if it is still open.
	   This is necessary to be POSIX compliant.
	 */
        req = MRC_Request__delete(&err, fullpath, &resp, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;
        
	if (!fileid || fileid_struct_read_uc(fileid) <= 2) {

		/* Now notify all relevant OSDs of the delete. */
		if (resp.xlocs)
			notify_osd_del(resp.xlocs, resp.xcap);

		/* Remove fileid, replica and user files from inventory */
		if (fileid) {
			dbg_msg("Removing file '%s' from inventory.\n", fileid->fileid);
			fileid_struct_dec_uc(fileid);	/* From get op. */
			/* spin_unlock(&fileid->lock); */
			file_inv_remove(&file_inv, fileid);
		}
		
		if (resp.xlocs)
			xloc_list_delete(resp.xlocs);
		if (resp.xcap)
			xcap_delete(resp.xcap);
       	} else {
		fileid_struct_dec_uc(fileid);		/* From get operation */
		err_msg("Cannot delete fileid '%s'",
			fileid->fileid);
		if (fileid_struct_read_uc(fileid) > 0)
			err_msg("File is still in use.\n");
		if (fileid) {
			/* spin_unlock(&fileid->lock); */
		}
	}

	free(fullpath);
	creds_delete_content(&c);

	XTREEMFS_LEAVE();
	return -abs(err);
}

int
xtreemfs_rmdir(const char *path)
{
	int err = 0;
	struct req *req;
	struct creds c;
	struct MRC_Req_delete_resp resp;
	char *fullpath;
	
	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	req = MRC_Request__delete(&err, fullpath, &resp, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_symlink(const char *from, const char *to)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath;

	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(to);
	dbg_msg("From in FUSE '%s' and as a fullname '%s'\n", from, fullpath);
	dbg_msg("To in FUSE '%s'\n", to);
	req = MRC_Request__createSymbolicLink(&err, fullpath, from, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_rename(const char *from, const char *to)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath_from, *fullpath_to;
	
	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	get_fuse_creds(&c);
	fullpath_from = xtreemfs_full_path(from);
	fullpath_to   = xtreemfs_full_path(to);
	
	dbg_msg("Renaming '%s' to '%s (%s -> %s)'\n", from, to,
		fullpath_from, fullpath_to);

	req = MRC_Request__move(&err, fullpath_from, fullpath_to,
				&c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	if (err) {
		dbg_msg("Error in rename mrc (%d)\n", err);
		goto out;
	}

	/* Must rename entity in our internal database */
	file_inv_rename(&file_inv, fullpath_from, fullpath_to);

 out:
	creds_delete_content(&c);
	free(fullpath_to);
	free(fullpath_from);
	XTREEMFS_LEAVE();
	
	return -abs(err);
}

int
xtreemfs_link(const char *from, const char *to)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath_from = NULL;
	char *fullpath_to = NULL;

	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	fullpath_from = xtreemfs_full_path(from);
	fullpath_to = xtreemfs_full_path(to);
	get_fuse_creds(&c);
	req = MRC_Request__createLink(&err, fullpath_to, fullpath_from, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	creds_delete_content(&c);
	free(fullpath_from);
	free(fullpath_to);
	XTREEMFS_LEAVE();
	
	return -abs(err);
}

int
xtreemfs_chmod(const char *path, mode_t mode)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath = NULL;
	
	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);
	fullpath = xtreemfs_full_path(path);
	get_fuse_creds(&c);
	
	dbg_msg("Changing mode to %d\n", mode);
	req = MRC_Request__changeAccessMode(&err, fullpath, mode, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;
	
	if (err) {
		dbg_msg("Cannot wait for request.\n");
		goto finish;
	}
	
finish:
	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();
	return -abs(err);
}

int
xtreemfs_chown(const char *path, uid_t uid, gid_t gid)
{
	int err = 0;
	char *fullpath;
	char *userId, *groupId;
	struct req *req;
	struct creds c;
	
	XTREEMFS_ENTER();
	metadata_cache_flush(&md_cache);

	dbg_msg("About to change ownership for %s to %d.%d\n", path, uid, gid);

	fullpath = xtreemfs_full_path(path);
	get_fuse_creds(&c);
	xtreemfs_get_unix_ids(&userId, &groupId, uid, gid);
	if (userId || groupId) {
		dbg_msg("Found user %s in group %s\n", userId, groupId);
		req = MRC_Request__changeOwner(&err, fullpath,
					       userId, groupId,
					       &c, NULL, 1);
		if (req)
			err = execute_req(mrccomm_wq, req);
		else
			err = -ENOMEM;

		if (err) {
			dbg_msg("Cannot wait for request.\n");
			err = -abs(err);	
			goto finish;
		}
	} else {
		dbg_msg("Could not find user and group name for %d.%d\n", uid, gid);
	}

finish:
	creds_delete_content(&c);
	free(userId);
	free(groupId);
	free(fullpath);
	
	XTREEMFS_LEAVE();
	return -abs(err);
}

int xtreemfs_do_truncate(struct fileid_struct *fileid,
			 struct xloc_list *xloc_list,
			 struct xcap *xcap_s,
			 off_t size,
			 struct OSD_Channel_resp *channel_resp)
{
	int err = 0;
	struct list_head *iter;
	struct xlocs *xl;

	struct OSD_Proxy *op;
	/* struct OSD_Channel_resp channel_resp = {
		.new_size = -1,
		.epoch = -1,
	}; */


	XTREEMFS_ENTER();
	dbg_msg("Truncating file to new size %d\n", size);

	if (fileid) {
		fileid->sz.size = size;
	} else {
		err_msg("Must have a fileid struct with each user file!\n");
		err = -EIO;
		goto finish;
	}

	/* Notify first OSD for each list entry */
	list_for_each(iter, &xloc_list->head) {
 		xl = container_of(iter, struct xlocs, head);
 		dbg_msg("Getting OSD proxy for '%s'\n", xl->osds[0]);
		op = OSD_Manager_get_proxy(&osd_manager, xl->osds[0], dirservice);
		if (!op) {
			dbg_msg("Cannot get proxy for OSD '%s'\n", xl->osds[0]);
			break;
		}
		OSD_Proxy_trunc(op, fileid->fileid, size,
				xloc_list, xcap_s,
				channel_resp);
	}

#if 0
	if (channel_resp.new_size != -1) {
		dbg_msg("Got a new size from truncate operation.\n");
		new_size = channel_resp.new_size;
		new_epoch = channel_resp.epoch;
		fileid->sz.size = new_size;
		fileid->sz.epoch = new_epoch;
		xcap_s->truncateEpoch = new_epoch;
	} else {
		new_size = size;
		new_epoch = fileid->sz.epoch;
	}
#endif

finish:	
	XTREEMFS_LEAVE();

	return err;
}

int
xtreemfs_truncate(const char *path, off_t size)
{
	int err = 0;
	char *fullpath = NULL;
	char *mode = "t";
	struct MRC_Req_open_resp open_resp = { NULL, NULL };
	struct fileid_struct *fileid;
	struct req *req;
	struct creds c;
	struct xcap *xcap_s = NULL;
	struct xloc_list *xloc_list = NULL;
		
	struct OSD_Channel_resp channel_resp = {
		.new_size = -1,
		.epoch = -1,
		.location = NULL
	};

	off_t new_size;
	long new_epoch;

	XTREEMFS_ENTER();
	get_fuse_creds(&c);

	fullpath = xtreemfs_full_path(path);

	req = MRC_Request__open(&err, &open_resp, fullpath, mode, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	if (err)
		goto finish;

	xcap_s = str_to_xcap(open_resp.xcap);
	xcap_s->creds.uid = c.uid;
	xcap_s->creds.gid = c.gid;
	xcap_s->creds.pid = c.pid;
	
	xloc_list = str_to_xlocs_list(open_resp.xloc);

	dbg_msg("xloc: '%s'\n", open_resp.xloc);
	dbg_msg("xcap: '%s'\n", open_resp.xcap);

	/* See, if we already have an entry for this file in the
	   inventory */
	fileid = file_inv_get_by_path(&file_inv, fullpath, 1);
	if (!fileid) {
		dbg_msg("File not found in inventory.\n");
		fileid = fileid_struct_new(xcap_s->fileID, fullpath, 0, NULL, 0);
		file_inv_add_fileid(&file_inv, fileid, 1);
		fileid->sz.size = size;
		fileid->sz.epoch = xcap_s->truncateEpoch;
		// Not neccessary as we do that right away!
		// atomic_set(&fileid->sz.needs_updating, 1);
	} else {   /* Set only new size */
		fileid->sz.size = size;
		// atomic_set(&fileid->sz.needs_updating, 1);
		fileid_struct_dec_uc(fileid);
		atomic_inc(&fileid->sz.order);
		atomic_set(&fileid->sz.needs_updating, 0);
		/* spin_unlock(&fileid->lock); */
	}

	err = xtreemfs_do_truncate(fileid, xloc_list, xcap_s, size,
				   &channel_resp);
	if (err) {
		err_msg("Error while doing trunc operation on the OSDs.\n");
		goto finish;
	}

	/* Well let's see if we got a new size from another client
	   during the truncate operations... */
	if (channel_resp.new_size != -1) {
		dbg_msg("Adjusting size after truncate op.\n");
		new_size = channel_resp.new_size;
		new_epoch = channel_resp.epoch;
		fileid->sz.size = new_size;
		fileid->sz.epoch = new_epoch;
		atomic_inc(&fileid->sz.order);
		atomic_set(&fileid->sz.needs_updating, 0);
		// xcap_s->truncateEpoch = new_epoch;
	} else {
		new_size = size;
		new_epoch = xcap_s->truncateEpoch;
	}
	
	/* Update file size */
	req = MRC_Request__updFileSize(&err, xcap_s, new_size, new_epoch,
				     &c, NULL, atomic_read(&fileid->sz.order), 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	if (err)
		goto finish;

	/* If the new epoch in xcap is different from file.epoch: */
	if (new_epoch != xcap_s->truncateEpoch)
		fileid->sz.epoch = xcap_s->truncateEpoch;

	// send notification to OSDs (for now: delete the file)
	// dbg_msg("Notify OSDs with '%s' and '%s'\n", resp.xcap, resp.xloc);
	/* notify_osd_del(xloc_list, xcap_s); */

finish:
	if (xloc_list)
		xloc_list_delete(xloc_list);
	if (xcap_s)
		xcap_delete(xcap_s);

	creds_delete_content(&c);

	free(open_resp.xloc);
	free(open_resp.xcap);
	free(channel_resp.location);
	free(fullpath);

	dbg_msg("File size after operation: %d\n", fileid->sz.size);

	XTREEMFS_LEAVE();

	return -abs(err);
}

int xtreemfs_ftruncate(const char *path, off_t size,
		  struct fuse_file_info *fi)
{
	int err = 0;
	char *fullpath = NULL;
	struct creds c;
	struct fileid_struct *fileid;
	struct user_file *uf;
	struct xloc_list *xloc_list;
	struct xcap *xcap_s;
	struct req *req;

	struct OSD_Channel_resp channel_resp = {
		.new_size = -1,
		.epoch = -1,
	};

	off_t new_size;
	long new_epoch;

	XTREEMFS_ENTER();
	get_fuse_creds(&c);

	fullpath = xtreemfs_full_path(path);

	uf = (struct user_file *)(uintptr_t)fi->fh;
	if (!uf) {
		dbg_msg("No user file structure!\n");
		err = -ENOENT;
		goto finish;
	}

	trace_msg("Truncating '%s' to new size %d\n", path, size);

	fileid = uf->file->fileid_s;
	if (fileid) {
		fileid->sz.size = size;
		atomic_inc(&fileid->sz.order);
		atomic_set(&fileid->sz.needs_updating, 0);
	} else {
		err_msg("Must have a fileid struct with each user file!\n");
		err = -EIO;
		goto finish;
	}

	xloc_list = uf->xloc_list;
	xcap_s    = uf->xcap;

	err = xtreemfs_do_truncate(fileid, xloc_list, xcap_s, size,
				   &channel_resp);
	if (err) {
		err_msg("Error while doing truncate with the OSDs.\n");
		goto finish;
	}

	/* Because we have a regular (open) file we need to renew
	   the capability in the inventory. */
		/* Tell the MRC about the new size */
	if (channel_resp.new_size != -1) {
		new_size = channel_resp.new_size;
		new_epoch = channel_resp.epoch;
		fileid->sz.size = new_size;
		fileid->sz.epoch = new_epoch;
		atomic_inc(&fileid->sz.order);
		atomic_set(&fileid->sz.needs_updating, 0);
		// xcap_s->truncateEpoch = new_epoch;
	} else {
		new_size = size;
		new_epoch = fileid->sz.epoch;
	}

	/* Update file size */
	req = MRC_Request__updFileSize(&err, xcap_s, new_size, new_epoch,
				     &c, NULL, atomic_read(&fileid->sz.order), 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;
	if (err)
		goto finish;

#if 0
	err = xcap_inv_renew_immediate(&xcap_inv, fileid->fileid, xcap_s,
				       new_size, new_epoch,
				       atomic_read(&fileid->sz.order));
#endif
	/* Lets see if truncateEpoch was update in the renewal process */
	if (new_epoch != xcap_s->truncateEpoch)
		fileid->sz.epoch = xcap_s->truncateEpoch;

finish:
	dbg_msg("File size after operation: %d\n", fileid->sz.size);

	creds_delete_content(&c);
	free(fullpath);

	XTREEMFS_LEAVE();

	return -abs(err);
}


int
xtreemfs_utime(const char *path, struct utimbuf *ubuf)
{
	int rv = 0;

	XTREEMFS_ENTER();
	dbg_msg("utime not implemented, yet\n");
	XTREEMFS_LEAVE();

	return rv;
}

static struct user_file *xtreemfs_create_user_file(char *fullpath,
						   int flags,
						   struct xcap *xcap,
						   struct xlocs *xloc,
						   struct xloc_list *xloc_list,
						   int *err)
{
	struct user_file *uf = NULL;
	struct xtreemfs_statinfo si;
	struct req *req;
	struct creds c;
	struct fileid_struct *fileid = NULL;
	/* int err; */

	XTREEMFS_ENTER();

	get_fuse_creds(&c);

	/* Make sure we block on flush operations! If we find the
	   fileid in our inventory it will be locked! */
	fileid = file_inv_get_by_path(&file_inv, fullpath, 1);

	/* Do not allow flushing during the stat operation */
	if (fileid)
		spin_lock(&fileid->flush);

	/* Before creating a new user file, let's get the file info
	   from the MRC. This way we can make sure that we have the
	   right file info (especially the size!) in our inventory. */
	xtreemfs_statinfo_init(&si);
	req = MRC_Request__stat(err, &si, fullpath, 0, 0, 0, &c, NULL, 1);
	if (req)
		*err = execute_req(mrccomm_wq, req);
	else
		*err = -ENOMEM;

	if (*err) {
		dbg_msg("Error while executing request: %d\n", *err);
		xtreemfs_statinfo_clear(&si);
		*err = -abs(*err);
		goto out;
	}

	/* Release fileid from get_by_path op */
	if (fileid) {
		/* spin_unlock(&fileid->lock); */
	}

	uf = user_file_new(xcap, xloc, xloc_list);
	if (!uf) {
		*err = -EIO;
		err_msg("Cannot create new user file.\n");
		goto out;
	}

	/* This inc op indicated that we have the user_file added
	   to the fh entry of the fuse_file_info structure.
	   This is used to indicate that the new file will remain
	   open until we close it.
	 */
	spin_lock(&uf->lock);
	atomic_inc(&uf->use_count);
	dbg_msg("Created new user file.\n");
		
	file_inv_add_user_file(&file_inv, fullpath, flags, uf,
			       conf.caching, dirservice);
		
	dbg_msg("File use count now: %d\n", atomic_read(&uf->use_count));
		
	// pthread_spin_lock(&xcap_inv.lock);
	trace_msg("Adding xcap to inventory.\n");
	xcap_inv_add(&xcap_inv, xcap, 1);
	// pthread_spin_unlock(&xcap_inv.lock);
		
	if (!*err) {	/* Stat request was successful */
		uf->file->fileid_s->sz.size = si.size;
		uf->file->fileid_s->sz.epoch = xcap->truncateEpoch;
	}
	spin_unlock(&uf->lock);
		
	xtreemfs_statinfo_clear(&si);
		
	dbg_msg("Address of user file structure: %p\n", uf);

out:
	if (fileid) {
		fileid_struct_dec_uc(fileid);	/* From get operation */
		spin_unlock(&fileid->flush);
	}

	creds_delete_content(&c);
	XTREEMFS_LEAVE();

	return uf;
}

/**
 * FUSE interface to opening a file.
 */
int xtreemfs_open(const char *path, struct fuse_file_info *fi)
{
	int err = 0;
	char *fullpath = NULL;
	struct MRC_Req_open_resp resp = { .xcap = NULL, .xloc = NULL };
	struct xcap *xcap;
	struct xlocs *xloc;
	struct xloc_list *xloc_list;

	char *open_mode[] = { "r", "w", "a", "ga", "c", "t", "sr" };
	int open_mode_idx = -1;
	struct user_file *uf = NULL;
	struct req *req;
	struct creds c;

	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);
	dbg_msg("Opening path '%s' flags=%d\n", fullpath, fi->flags);
	
	switch(fi->flags & O_ACCMODE) {
		case O_RDONLY:
			open_mode_idx = 0;
		break;
		/* XtreemFS does not distinguish between read/write and
		 * write only... */
		case O_WRONLY:
		case O_RDWR:
			open_mode_idx = 1;
		break;
	}
	if (fi->flags & O_APPEND)
		open_mode_idx = 2;

#if 0
	if (fi->flags & O_CREAT) {
		dbg_msg("Creating file.!\n");
		open_mode_idx = 4;
	}
#endif

#ifdef O_DIRECT
	if (fi->flags & O_DIRECT) {
		dbg_msg("direct I/O flag set for %s\n",path);
		fi->direct_io = 1;
	}
#endif

	req = MRC_Request__open(&err, &resp, fullpath,
				open_mode[open_mode_idx], &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

	if (err)
		goto out;

	uf = (struct user_file *)(uintptr_t)fi->fh;

	if (!uf) {
		xcap = str_to_xcap(resp.xcap);
		creds_copy(&xcap->creds, &c);

		xloc = xloc_list_get_idx_str(resp.xloc, 0);
		xloc_list = str_to_xlocs_list(resp.xloc);
		dbg_msg("Xlocation string: %s\n", xloc->repr);

		uf = xtreemfs_create_user_file(fullpath, fi->flags,
					       xcap, xloc, xloc_list,
					       &err);
		fi->fh = (uint64_t)(uintptr_t)uf;
		dbg_msg("Fileid use count after uf creation: %d\n",
			fileid_struct_read_uc(uf->file->fileid_s));
	} else {		
		dbg_msg("Reopening existing user file: %p\n", uf);
		
		/* Set new xcap and xloc from MRC on the already
		   existing file. */
		// pthread_spin_lock(&uf->lock);
		if (resp.xcap) {
			xcap_copy_values_from_str(uf->xcap, resp.xcap);
			uf->file->fileid_s->sz.epoch = uf->xcap->truncateEpoch;
		}
		if (resp.xloc) {
			xloc = xloc_list_get_idx_str(resp.xloc, 0);
			// free(uf->xloc);
			if (uf->xloc)
				xlocs_delete(uf->xloc);
			uf->xloc = xloc;
			dbg_msg("Exchanged x-location string: %s\n", uf->xloc->repr);
		}
		atomic_inc(&uf->use_count);
		// pthread_spin_unlock(&uf->lock);
		dbg_msg("File '%s' with uf = %p\n", path, uf);
	}

out:
	if (resp.xloc)
		free(resp.xloc);
	if (resp.xcap)
		free(resp.xcap);

	creds_delete_content(&c);

	free(fullpath);

	XTREEMFS_LEAVE();
	return -abs(err);
}

int
xtreemfs_read(const char *path, char *rbuf, size_t size, off_t offset,
	      struct fuse_file_info *fi)
{
	int rv = 0, err = 0;
	struct user_file *uf = (struct user_file *)(uintptr_t)(fi->fh);
	struct fileid_struct *fileid = uf->file->fileid_s;
	struct req *req;
	size_t rsize;
	size_t tsize = 0;
	off_t file_size;
		
	XTREEMFS_ENTER();
#ifdef ITAC
	VT_begin(itac_fuse_read_hdl);
#endif

	dbg_msg("uf=%p\n", uf);
	dbg_msg("Reading at %ld with size %d\n", offset, (int)size);

	if (size == 0) {
		dbg_msg("Size is 0!\n");
		rv = 0;
		goto out;
	}
	
	spin_lock(&uf->lock);
	atomic_inc(&uf->use_count);
	
	// access to size should be "atomic" (i.e. protected by lock)
	// pthread_spin_lock(&fileid->lock);
	file_size = fileid->sz.size;
	// pthread_spin_unlock(&fileid->lock);

	/* See if we can read at all */
	if (offset < file_size) {
		if (offset + size > file_size) {
			rsize = file_size - offset;
			dbg_msg("Corrected read size to %ld\n", rsize);
		} else
			rsize = size;
	} else {
		dbg_msg("attempted access outside the known filesize!\n");
		dbg_msg("fileid=%s offset=%ld known filesize=%ld\n",
			fileid->fileid, offset, (long)file_size);
		rv = 0; // illegal seek ??
		goto finish;
	}
	
	bench_timer_start(&read_timer);

	req = filerw_req_create(REQ_FILERW_READ, offset, rsize,
				(void *)rbuf, uf, &tsize, NULL);
	if (!req) {
		dbg_msg("filerw_req_create returned an error: %d\n",err);
		rv = -EIO;
		goto finish;
	}
	err = execute_req(filerw_wq, req);
		
	dbg_msg("returned from wait (uf=%p) with err=%d\n", uf, err);
	
	bench_timer_stop(&read_timer);
	bench_timings_add_ms(&read_timings, tsize / bench_timer_ms(&read_timer));
	
	if (!err) {
		rv = tsize;
	} else {
		err_msg("Error %d after waiting for request.\n", err);
		rv = -abs(err);
	}
	
finish:
	atomic_dec(&uf->use_count);
	spin_unlock(&uf->lock);
out:

#ifdef ITAC
	VT_end(itac_fuse_read_hdl);
#endif
	dbg_msg("Returning %d\n", rv);
	XTREEMFS_LEAVE();
	return rv;
}

int
xtreemfs_write(const char *path, const char *wbuf, size_t size, off_t offset,
	       struct fuse_file_info *fi)
{
	int rv = 0, err = 0;
	struct user_file *uf = (struct user_file *)(uintptr_t)fi->fh;
	struct req *req;
	size_t num_bytes = 0;

	XTREEMFS_ENTER();
#ifdef ITAC
	VT_begin(itac_fuse_write_hdl);
#endif

	spin_lock(&uf->lock);
	atomic_inc(&uf->use_count);

	bench_timer_start(&write_timer);
	
	req = filerw_req_create(REQ_FILERW_WRITE, offset, size,
				(void *)wbuf, uf,
				&num_bytes, NULL);
	if (req) {
		err = execute_req(filerw_wq, req);
		dbg_msg("returned from wait (uf=%p) with err=%d\n", uf, err);
	} else {
		err_msg("Could not create request for filerw.\n");
		rv = -ENOMEM;
		goto finish;
	}

	dbg_msg("FileRW request %p created for uf=%p.\n",req,uf);
	
	// block and wait for the request to finish
	dbg_msg("Waiting for fileRW req %p (uf=%p).\n", req, uf);

	/** Write data to an open file
	 *
	 * Write should return exactly the number of bytes requested
	 * except on error.  An exception to this is when the 'direct_io'
	 * mount option is specified (see read operation).
	 */

	bench_timer_stop(&write_timer);
	bench_timings_add_ms(&write_timings,
			     size / bench_timer_ms(&write_timer));
	if (err) {
		err_msg("Error %d in 'req_wait'\n", err);
		rv = -abs(err);
	} else
		rv = (int)size;

	dbg_msg("done.\n");
	dbg_msg("File size after write op: %lld\n", uf->file->fileid_s->sz.size);

finish:
	atomic_dec(&uf->use_count);
	spin_unlock(&uf->lock);
#ifdef ITAC
	VT_end(itac_fuse_write_hdl);
#endif
	XTREEMFS_LEAVE();
	return rv;
}

int
xtreemfs_statfs(const char *path, struct statvfs *sbuf)
{
	int rv = 0;

	XTREEMFS_ENTER();
	dbg_msg("statfs not implemented, yet\n");
	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_flush(const char *path, struct fuse_file_info *fi)
{
	int err = 0;
	struct user_file *uf;
	struct fileid_struct *fileid;
	struct creds c;

#ifdef ITAC
	VT_begin(itac_fuse_flush_hdl);
#endif

	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	dbg_msg("Flushing '%s'\n", path);
	
	uf = (struct user_file *)(uintptr_t)fi->fh;
	if (!uf || !uf->file)
		goto finish;
	atomic_inc(&uf->use_count);
	
	fileid = uf->file->fileid_s;

	/* Indicate that we are flushing the file */
	spin_lock(&fileid->flush);

	dbg_msg("File size in flush: %ld\n", (int)fileid->sz.size);

	/* Do not allow any changes during flush operation. The size
	   affects all user files. */
	spin_lock(&fileid->lock);
	if (atomic_read(&fileid->sz.needs_updating)) {
		dbg_msg("File %s needs updating, size=%ld\n",
			fileid->path, fileid->sz.size);
	
		// pthread_spin_lock(&xcap_inv.lock);
		err = xcap_inv_renew_immediate(&xcap_inv,
					       fileid->fileid, uf->xcap,
					       fileid->sz.size, fileid->sz.epoch,
					       NEW_FILEID_ORDER(uf), 1);
		atomic_set(&fileid->sz.needs_updating, 0);
		// pthread_spin_unlock(&xcap_inv.lock);
	}

	spin_unlock(&fileid->lock);
	spin_unlock(&fileid->flush);
finish:
	creds_delete_content(&c);

	if (uf && uf->file)
		atomic_dec(&uf->use_count);
#ifdef ITAC
	VT_end(itac_fuse_flush_hdl);
#endif
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_release(const char *path, struct fuse_file_info *fi)
{
	int err = 0;
	struct user_file *uf = (struct user_file *)(uintptr_t)fi->fh;
	struct fileid_struct *fileid;

#ifdef ITAC
	VT_begin(itac_fuse_release_hdl);
#endif
	XTREEMFS_ENTER();
	dbg_msg("Releasing path '%s'\n", path);

	if (uf) {
		fileid = uf->file->fileid_s;
		if (atomic_read(&fileid->sz.needs_updating)) {
			dbg_msg("updating filesize first... = %lld\n",
				fileid->sz.size);
			err = xtreemfs_flush(path, fi);
		}
		if (file_inv_release_user_file(&file_inv, uf) == 1)
			fi->fh = (uintptr_t)NULL;

	} else
		dbg_msg("No file handle in release operation!\n");
	
	XTREEMFS_LEAVE();
#ifdef ITAC
	VT_end(itac_fuse_release_hdl);
#endif
	return -abs(err);
}

int
xtreemfs_fsync(const char *path, int isdatasync, struct fuse_file_info *fi)
{
	int rv = 0;
	struct user_file *uf = (struct user_file *)(uintptr_t)fi->fh;
	struct fileid_struct *fileid;

	XTREEMFS_ENTER();
	if (uf) {
		fileid = uf->file->fileid_s;
		if (atomic_read(&fileid->sz.needs_updating)) {
			dbg_msg("updating filesize first... = %ld\n",
				fileid->sz.size);
			rv = xtreemfs_flush(path, fi);
		}
	}	
	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_setxattr(const char *path, const char *name, const char *value, size_t size, int flags)
{
	int err = 0;
	struct req *req;
	char *fullpath = NULL;
	struct creds c;
	struct xattr_list xattr_list;

	XTREEMFS_ENTER();

	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	dbg_msg("Set new value '%s' for key '%s' (size is %d)\n", value, name, size);

	xattr_list_init(&xattr_list);

	/* TODO: This requires more care for binary data! The solution below
	   can only be accepted for text values! */
	xattr_list_add_values(&xattr_list, (char *)name, (char *)value, size);

	req = MRC_Request__setXAttrs(&err, fullpath, &xattr_list, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;
	
out:
	xattr_list_del_contents(&xattr_list);
	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();

	return err;
}

int
xtreemfs_getxattr(const char *path, const char *name, char *value, size_t size)
{
	int rv = 0;
	struct req *req;
	struct MRC_Req_get_xattr_resp resp = { .value = NULL };
	int err = 0;
	char *fullpath = NULL;
	struct creds c;

	XTREEMFS_ENTER();

	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	dbg_msg("Getting value for key '%s' and file '%s'\n", name, fullpath);

	req = MRC_Request__getXAttr(&err, fullpath, (char *)name, &resp, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;

		if (err) {
			rv = err;
			goto out;
		}

	if (!err) {
		if (resp.value) {
			dbg_msg("Return value: %s\n", resp.value);
			if (strlen(resp.value) < size && size > 0) {
				strcpy(value, resp.value);
				rv = strlen(resp.value);
			} else if (size == 0) {
				rv = strlen(resp.value);
			} else {
				err = -ERANGE;
				rv = err;
			}
		}
	} else {
		rv = err;
		goto out;
	}

out:
	free(resp.value);
	free(fullpath);
	creds_delete_content(&c);
	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_listxattr(const char *path, char *list, size_t size)
{
	int rv = 0;
	int err = 0;
	struct req *req;
	char *fullpath = NULL;
	struct creds c;
	struct xtreemfs_statinfo si;
	char *key_list = NULL;
	size_t key_list_size;

	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	dbg_msg("Getting attribute list for '%s' in buffer of length %d\n",
		fullpath, size);

	xtreemfs_statinfo_init(&si);

	req = MRC_Request__stat(&err, &si, fullpath, 0, 1, 0, &c,
				NULL, 1);
	if (!req) {
		err = -ENOMEM;
		goto out;
	}

	err = execute_req(mrccomm_wq, req);

	if (!err) {
		if (si.xAttrs) {
			key_list = xattr_list_key_list(si.xAttrs, &key_list_size);
			if (key_list) {
				dbg_msg("Got a key list of size %d!\n", key_list_size);
				dbg_msg("First key: '%s'\n", key_list);
				if (key_list_size < size && size > 0) {
					memcpy((void *)list, (void *)key_list, key_list_size);
					rv = key_list_size;
				} else if (size == 0) {
					rv = key_list_size;
				} else
					rv = -ERANGE;
			} else {
				rv = -1;
			}
		}
	} else {
		rv = -abs(err);
	}

out:
	free(fullpath);
	xtreemfs_statinfo_clear(&si);
	creds_delete_content(&c);
	free(key_list);
	dbg_msg("Returning %d\n", rv);

	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_removexattr(const char *path, const char *name)
{
	int rv = 0;
	int err = 0;
	struct req *req;
	char *fullpath = NULL;
	struct creds c;

	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	dbg_msg("remove xattr '%s' for '%s'\n", name, fullpath);

	req = MRC_Request__removeXAttrs(&err, fullpath, (char *)name, &c, NULL, 1);
	if (!req) {
		rv = err;
		goto out;
	}
	err = execute_req(mrccomm_wq, req);

out:
	free(fullpath);
	creds_delete_content(&c);
	XTREEMFS_LEAVE();

	return rv;
}

int
xtreemfs_opendir(const char *path, struct fuse_file_info *fi)
{
	int rv = 0;
	
	XTREEMFS_ENTER();
	dbg_msg("opendir not implemented, yet\n");
	XTREEMFS_LEAVE();
	
	return rv;
}

int
xtreemfs_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
		 off_t offset, struct fuse_file_info *fi)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath = NULL;
	struct MRC_Req_readDirAndStat_resp resp;
	int i;
	
	XTREEMFS_ENTER();
	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	req = MRC_Request__readDir(&err, &resp, fullpath, &c, NULL, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = -ENOMEM;
	if (err)
		goto out;

#if 0
	/* We use for now a very conservative metadata caching policy, in which
	   the cache is only used for the entries of the latest readDirAndStat.
	   First the entries in the cache, because the data is used as soon as it
	   is handed over to the filler. 
    */
	metadata_cache_flush(&md_cache);
	metadata_cache_insert_entries(&md_cache, path,
		resp.num_entries, resp.dir_entries, resp.stat_entries);
#endif
	for (i = 0; i < resp.num_entries; i++)
		filler(buf, resp.dir_entries[i], NULL, 0);
	filler(buf, ".", NULL, 0);
	filler(buf, "..", NULL, 0);

	
	for (i = 0; i < resp.num_entries; i++) {
		free(resp.dir_entries[i]);
#if 0
		xtreemfs_statinfo_clear(&(resp.stat_entries[i]));
#endif
	}
#if 0
	free(resp.stat_entries);
#endif
	free(resp.dir_entries);

 out:
	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_releasedir(const char *path, struct fuse_file_info *fi)
{
	int rv = 0;
	
	XTREEMFS_ENTER();
	dbg_msg("releasedir not implemented, yet\n");
	XTREEMFS_LEAVE();
	
	return rv;
}

int
xtreemfs_fsyncdir(const char *path, int isdatasync, struct fuse_file_info *fi)
{
	int rv = 0;
	
	XTREEMFS_ENTER();
	dbg_msg("fsyncdir not implemented, yet\n");
	XTREEMFS_LEAVE();
	
	return rv;
}

int
xtreemfs_create(const char *path, mode_t mode, struct fuse_file_info *fi)
{
	int err = 0;
	struct req *req;
	struct creds c;
	char *fullpath = NULL;
	struct user_file *uf = NULL;
	int do_open = 0;
	struct MRC_Req_create_file_resp resp = {
		.xcap = NULL,
		.xloc = NULL
	};

	XTREEMFS_ENTER();

	uf = (struct user_file *)(uintptr_t)fi->fh;

	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);
	
	dbg_msg("Create path '%s' in mode %d", fullpath, mode);
	dbg_msg("Flags: %d", fi->flags);

	if (!uf) {
		dbg_msg("Open after create.\n");
		do_open = 1;
	}
 
	req = MRC_Request__createFile(&err, &resp,
				      fullpath, NULL, NULL, NULL,
				      mode, do_open, &c, NULL, 1);
	if (!req) {
		err_msg("Error during file creation.\n");
		err = -ENOENT;
		goto out;
	}

	err = execute_req(mrccomm_wq, req);

	if(!err) {
		if (do_open) {
			struct xcap *xcap;
			struct xlocs *xloc;
			struct xloc_list *xloc_list;

			dbg_msg("Xcap string: '%s'\n", resp.xcap);
			xcap = str_to_xcap(resp.xcap);
			creds_copy(&xcap->creds, &c);

			xloc = xloc_list_get_idx_str(resp.xloc, 0);
			xloc_list = str_to_xlocs_list(resp.xloc);
			dbg_msg("Xlocation string: %s\n", xloc->repr);

			uf = xtreemfs_create_user_file(fullpath, fi->flags,
					       xcap, xloc, xloc_list,
					       &err);
			fi->fh = (uint64_t)(uintptr_t)uf;
			dbg_msg("File '%s' created with uf = %p\n", path, fi->fh);
			dbg_msg("Use count after uf creation: %d\n",
				fileid_struct_read_uc(uf->file->fileid_s));
		} else {
			err_msg("Whoops! Trying to create an already existing file.\n");
		}
	} else {
		if (err == 13)
			err = -abs(err);
	}
 out:
	creds_delete_content(&c);
	if (resp.xloc)
		free(resp.xloc);
	if (resp.xcap)
		free(resp.xcap);

	free(fullpath);
	XTREEMFS_LEAVE();

	return -abs(err);
}

int
xtreemfs_fgetattr(const char *path, struct stat *stbuf, struct fuse_file_info *fi)
{
	int rv = 0;
	
	XTREEMFS_ENTER();
	dbg_msg("fgetattr not implemented, yet\n");
	XTREEMFS_LEAVE();
	
	return rv;
}

void *
xtreemfs_fuse_init(struct fuse_conn_info *conn)
{
	void *rv = NULL;

	XTREEMFS_ENTER();

	dbg_msg("Volume is '%s'\n", volume);
	dbg_msg("MRC is '%s'\n", mrc_url);

#ifdef XTREEMOS_ENV
	pthread_mutex_init(&xtreemos_ams_mutex, NULL);
	xtreemos_ams_sock = amsclient_connect_open();
	if (xtreemos_ams_sock < 0) {
		dbg_msg("Cannot open connection to AMS.\n");
	}
#endif

	xtreemfs_load_mods(&transl_engine, conf.mod_dir);
	metadata_cache_init(&md_cache);

#if 0
	raid_engine_init(&raid_engine);
	xtreemfs_load_mods(&raid_engine);
#endif

	OSD_Manager_init(&osd_manager);

	file_inv_init(&file_inv);
	sobj_init(NUM_SOBJ_THREADS);

	dbg_msg("Starting multiple queues\n");
	fobj_init();
	filerw_init();
	mrccomm_init(mrc_url, NUM_MRCOMM_THREADS);
	xcap_inv_init(&xcap_inv);

	if (conf.monitoring) {
		dbg_msg("Start monitoring interface.\n");
	}
	
	XTREEMFS_LEAVE();
	
	return rv;
}

void
xtreemfs_fuse_destroy(void *handle)
{
	
	XTREEMFS_ENTER();

#ifdef XTREEMOS_ENV
	amsclient_connect_close(xtreemos_ams_sock);
	pthread_mutex_destroy(&xtreemos_ams_mutex);
#endif
	timing_msg("Avg. write bandwidth in stripe stage: %g kB/s\n",
		   bench_timings_avg_ms(&write_timings));
	timing_msg("Avg. write bandwidth in osd proxy: %g kB/s\n",
		   bench_data_collection_avg(&osd_proxy_bandwidth));
	timing_msg("Avg. write bandwidth in neon statge: %g kB/s\n",
		   bench_timings_avg_ms(&neon_timings));
	
	metadata_cache_clear(&md_cache);
	xtreemfs_unload_mods(&transl_engine);

	OSD_Manager_clear(&osd_manager);
	file_inv_clear(&file_inv);
	xcap_inv_clear(&xcap_inv);
	
	mrccomm_destroy();
	ne_sock_exit();

	XTREEMFS_LEAVE();
	
}

int
xtreemfs_access(const char *path, int mode)
{
	struct req *req;
	struct creds c;
	struct MRC_Req_check_access_resp resp;
	char *fullpath;
	char mode_str[12];
	int i;
	int err = 0;
	
	XTREEMFS_ENTER();

	get_fuse_creds(&c);
	fullpath = xtreemfs_full_path(path);

	/* Convert mode to string */
	i = 0;
	if (mode & R_OK)
		mode_str[i++] = 'r';
	if (mode & W_OK)
		mode_str[i++] = 'w';
	if (mode & X_OK)
		mode_str[i++] = 'x';
	if (mode & F_OK)
		mode_str[i++] = 'f';

	mode_str[i] = '\0';

	req = MRC_Request__checkAccess(&err, &resp, fullpath, mode_str, &c, NULL, 1);
	if (!req) {
		dbg_msg("Error while checking file access.\n");
		err = -EIO;
		goto out;
	}
	err = execute_req(mrccomm_wq, req);
	if(!err) {
		if (!resp.grant) {
			dbg_msg("Access denied.\n");
			err = -EACCES;
		}
	}

out:
	creds_delete_content(&c);
	free(fullpath);
	XTREEMFS_LEAVE();
	return -abs(err);
}
