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
*  C Implementation: mrc_request
*
* Description: 
*
*
* Authors: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*          Erich Focht <efocht at hpce dot nec dot de>, Copyright (c) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

#include "mrc.h"
#include "mrc_request.h"
#include "request.h"

#include "logger.h"
#include "kernel_substitutes.h"

/**
 * Allocate a mrc payload strcture and initialize it.
 */
static struct MRC_payload *MRC_payload_new(MRC_Com_t cmd)
{
	struct MRC_payload *p = NULL;

	p = (struct MRC_payload *)
		malloc(sizeof(struct MRC_payload));
	if (p) {
		p->command = cmd;
		p->uf = NULL;
		p->creds = NULL;
	}
	return p;
}

/**
 * Free a mrc payload structure and its command specific data.
 *
 * The payload specific data should be freed by the caller of the request!
 */
void MRC_payload_free(void *p)
{
	free(p);
}

/*--------- Generic functions ---------- */

struct req *MRC_Request__addReplica(int *err, struct creds *c,
				    char *fileid, struct striping_policy *sp,
				    int num_osds, char **osds,
				    struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct MRC_Req_add_replica_data *data = NULL;
	struct req *req = NULL;
	
	p = MRC_payload_new(COM_ADD_REPLICA);
	if (!p) {
		*err = -ENOMEM;
		goto finish;
	}
	p->creds = c;
	data = &p->data.add_replica;
	data->fileid = fileid;
	data->sp = sp;
	data->num_osds = num_osds;
	data->osds = osds;
	
	req = mrccomm_req_create(err, p, parent, wait);
finish:
	return req;
}

struct req *MRC_Request__createDir(int *err,
				   char *dirPath, void *xAttrs, void *acl,
				   int um, struct creds *c,
				   struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct MRC_Req_create_dir_data *data = NULL;
	struct req *req = NULL;

	p = MRC_payload_new(COM_CREATE_DIR);
	if (!p) {
		*err = -ENOMEM;
		goto finish;
	}
	p->creds = c;
	data = &p->data.create_dir;
	data->dirPath = dirPath;
	data->xAttrs  = xAttrs;
	data->acl     = acl;
	data->umask   = um;

	req = mrccomm_req_create(err, p, parent, wait);
 finish:
	return req;
}

struct req *MRC_Request__createFile(int *err,
				    struct MRC_Req_create_file_resp *resp,
				    char *filePath, void *xAttrs,
				    struct striping_policy *sp,
				    void *acl, int mode, int do_open,
				    struct creds *c,
				    struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct MRC_Req_create_file_data *data = NULL;
	struct req *req = NULL;

	p = MRC_payload_new(COM_CREATE_FILE);
	if (!p) {
		*err = -ENOMEM;
		goto finish;
	}
	p->creds = c;
	data = &p->data.create_file;

	data->path      = filePath;
	data->xAttrs    = xAttrs;
	data->sp        = sp;
	data->acl       = acl;
	data->mode      = mode;
	data->open      = do_open;

	p->resp.createfile = resp;

	req = mrccomm_req_create(err, p, parent, wait);
 finish:
	return req;
}

struct req *MRC_Request__createVolume(int *err,
				      char *volumeName,
				      int osdSelectionPolicyId,
				      struct striping_policy *def_sp,
				      int acPolicyId,
				      int partitioningPolicyId,
				      struct acl_list *acl, struct creds *c,
				      struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct MRC_Req_create_volume_data *data = NULL;
	struct req *req = NULL;

	p = MRC_payload_new(COM_CREATE_VOLUME);
	if (!p) {
		*err = -ENOMEM;
		goto finish;
	}
	p->creds = c;
	data = &p->data.create_vol;

	data->volumeName            = volumeName;
	data->osdSelectionPolicyId  = osdSelectionPolicyId;
	data->defaultStripingPolicy = def_sp;
	data->acPolicyId            = acPolicyId;
	data->partitioningPolicyId  = partitioningPolicyId;
	data->acl                   = acl;

	req = mrccomm_req_create(err, p, parent, wait);
 finish:
	return req;
}

struct req *MRC_Request__delete(int *err, char *path,
				struct MRC_Req_delete_resp *resp,
				struct creds *c, struct req *parent, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_DELETE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.data = (void *)path; // will be deleted in xtreemfs_unlink
	p->resp.delete = resp;

	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__deleteVolume(int *err, char *volumeName,
				      struct creds *c, struct req *parent,
				      int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_DELETE_VOLUME);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.data = (void *)volumeName;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__getVolumes(int *err,
				    struct MRC_Req_get_volumes_resp *resp,
				    struct creds *c, struct req *parent,
				    int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_GET_VOLUMES);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->resp.get_volumes = resp;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__changeAccessMode(int *err,
					  char *path, int mode,
					  struct creds *c, struct req *parent,
					  int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_ACCESS_MODE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.chg_accmode.path = path;
	p->data.chg_accmode.mode = mode;
	req = mrccomm_req_create(err, p, parent, wait);	
out:
	return req;
}

struct req *MRC_Request__checkAccess(int *err,
				     struct MRC_Req_check_access_resp *resp,
				     char *path, char *mode,
				     struct creds *c, struct req *parent,
				     int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_CHECK_ACCESS);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}

	p->creds = c;
	p->data.chk_access.path = path;
	p->data.chk_access.mode = mode;
	p->resp.chk_access = resp;

	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req; 
}


struct req *MRC_Request__changeOwner(int *err,
				     char *path, char *userId, char *groupId,
				     struct creds *c, struct req *parent,
				     int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_CHANGE_OWNER);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.chg_owner.path    = path;
	if (userId)
		p->data.chg_owner.userId  = userId;
	else
		p->data.chg_owner.userId  = NULL;

	if (groupId)
		p->data.chg_owner.groupId = groupId;
	else
		p->data.chg_owner.groupId = NULL;

	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req;
}

struct req *MRC_Request__setACLEntries(int *err,
				       char *path, void *entries,
				       struct creds *c, struct req *parent,
				       int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_ACCESS_MODE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	
out:
	return req;
}

struct req *MRC_Request__removeACLEntries(int *err,
					  char *path, void *entities,
					  struct creds *c, struct req *parent,
					  int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_ACCESS_MODE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	
out:
	return req;	
}


struct req *MRC_Request__getXAttr(int *err,
				  char *path, char *key,
				  struct MRC_Req_get_xattr_resp *resp,
				  struct creds *c, struct req *parent,
				  int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_GET_XATTR);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	dbg_msg("Creating payload entries '%s' for '%s'\n", key, path);
	p->creds = c;
	p->data.get_xattr.path = path;
	p->data.get_xattr.key  = key;
	p->resp.get_xattr      = resp;

	req = mrccomm_req_create(err, p, parent, wait);	
out:
	return req;
}

struct req *MRC_Request__setXAttrs(int *err,
				   char *path,
				   struct xattr_list *attrs,
				   struct creds *c, struct req *parent,
				   int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_SET_XATTRS);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.set_xattrs.path = path;
	p->data.set_xattrs.attrs = attrs;

	req = mrccomm_req_create(err, p, parent, wait);	
out:
	return req;
}


struct req *MRC_Request__removeXAttrs(int *err,
				      char *path, char *key,
				      struct creds *c, struct req *parent,
				      int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	
	p = MRC_payload_new(COM_REM_XATTRS);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->data.rem_xattrs.path = path;
	p->data.rem_xattrs.key = key;

	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req;
}



struct req *MRC_Request__initFileSystem(int *err, struct creds *c,
					struct req *parent, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;

	p = MRC_payload_new(COM_INIT_FS);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__move(int *err, char *sourcePath, char *targetPath,
			      struct creds *c, struct req *parent, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p = NULL;
	struct MRC_Req_move_data *data;

	p = MRC_payload_new(COM_MOVE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	data = &p->data.move;
	data->from = sourcePath;
	data->to = targetPath;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__open(int *err, struct MRC_Req_open_resp *resp,
			      char *path, char *accessMode,
			      struct creds *c, struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct MRC_Req_open_data *data = NULL;
	struct req *req = NULL;

	p = MRC_payload_new(COM_OPEN);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->resp.open = resp;
	data = &p->data.open;
	data->path = path;
	data->access_mode = accessMode;

	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__readDir(int *err, struct MRC_Req_readDir_resp *resp,
				 char *path, struct creds *c,
				 struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct req *req = NULL;

	p = MRC_payload_new(COM_READ_DIR);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->resp.readdir = resp;
	p->data.data = path;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__readDirAndStat(int *err, struct MRC_Req_readDirAndStat_resp *resp,
				 char *path, struct creds *c,
				 struct req *parent, int wait)
{
	struct MRC_payload *p;
	struct req *req = NULL;

	p = MRC_payload_new(COM_READ_DIR_AND_STAT);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->resp.readdirandstat = resp;
	p->data.data = path;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}


struct req *MRC_Request__updFileSize(int *err,
				     struct xcap *xc, off_t new_size, int epoch,
				     struct creds *c, struct req *parent,
				     int order, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p;
	struct MRC_Req_upd_fs_data *nd;

	p = MRC_payload_new(COM_UPD_FILESIZE);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}

	nd = &p->data.upd_fs;
	p->creds = c;
	p->order = order;
	nd->xcap = xc;
	nd->new_size = new_size;
	nd->epoch = epoch;
	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req;
}


struct req *MRC_Request__renew(int *err, struct MRC_Req_renew_resp *resp,
			       struct xcap *xc, off_t new_size, int epoch,
			       struct creds *c, struct req *parent, int order, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p;
	struct MRC_Req_renew_data *rd;

	p = MRC_payload_new(COM_RENEW);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	rd = &p->data.renew;
	p->creds = c;
	p->resp.renew = resp;
	p->order = order;
	rd->xcap = xc;
	rd->new_size = new_size;
	rd->epoch = epoch;
	req = mrccomm_req_create(err, p, parent, wait);
 out:
	return req;
}

struct req *MRC_Request__stat(int *err, struct xtreemfs_statinfo *si,
			      char *path, int inclReplicas, int inclXAttrs,
			      int inclACLs, struct creds *c,
			      struct req *parent, int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p;
	struct MRC_Req_stat_data *sd;

	p = MRC_payload_new(COM_STAT);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}
	p->creds = c;
	p->resp.statinfo = si;
	sd = &p->data.stat;
	
	sd->path         = path;
	sd->inclReplicas = inclReplicas;
	sd->inclXAttrs   = inclXAttrs;
	sd->inclACLs     = inclACLs;

	dbg_msg("Path: %s\n", path);
	req = mrccomm_req_create(err, p, parent, wait);
	if (*err) {
		err_msg("Error while executing request stat (%d)\n", *err);
	}
 out:
	return req;
}

struct req *MRC_Request__createSymbolicLink(int *err,
					    char *linkPath, const char *targetPath,
					    struct creds *c, struct req *parent,
					    int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p;
	struct MRC_Req_create_symlink_data *csd;

	p = MRC_payload_new(COM_CREATE_SYMLINK);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}

	p->creds = c;
	csd = &p->data.create_symlink;
	csd->from = linkPath;
	csd->to = (char *)targetPath;

	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req;
}

struct req *MRC_Request__createLink(int *err,
				    char *linkPath, char *targetPath,
				    struct creds *c, struct req *parent,
				    int wait)
{
	struct req *req = NULL;
	struct MRC_payload *p;
	struct MRC_Req_create_link_data *cld;

	p = MRC_payload_new(COM_CREATE_LINK);
	if (!p) {
		*err = -ENOMEM;
		goto out;
	}

	p->creds = c;
	cld = &p->data.create_link;
	cld->from = linkPath;
	cld->to = targetPath;

	req = mrccomm_req_create(err, p, parent, wait);
out:
	return req;
}


// functione below are not yet implemented.

#if 0
int
MRC_Request__addReplica(struct MRC_Request *mr, char *fileId, char *stripingPolicy, void *osd_list)
{
	return 0;
}

int
MRC_Request__createSymbolicLink(struct MRC_Request *mr, char *linkPath, char *targetPath)
{
	return 0;
}

int
MRC_Request__getServerConfiguration(struct MRC_Request *mr)
{
	return 0;
}

int
MRC_Request__query(struct MRC_Request *mr, char *path, char *queryString)
{
	return 0;
}

int
MRC_Request__removeXAttrs(struct MRC_Request *mr, char *path, void *keys)
{
	return 0;
}

int
MRC_Request__setXAttrs(struct MRC_Request *mr, char *path, void *attrs)
{
	return 0;
}
#endif
