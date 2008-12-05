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
C++ Interface: mrc_request

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef _MRC_REQUEST_H
#define _MRC_REQUEST_H

#include <pthread.h>

#include <ne_string.h>

#include "stripe.h"
#include "request.h"
#include "list.h"
#include "acl.h"
#include "xattr.h"
#include "statinfo.h"
#include "xtreemfs.h"

/**
 * MRC Commands
 * The strings are in mrc_request.c and must be in sync with the commands!
 */
typedef enum {
	COM_UNDEFINED = 0,
	
	/* Initial file system */
	COM_INIT_FS,

	/* Volume commands */
	COM_CREATE_VOLUME,
	COM_VOLUME_INFO,
	COM_DELETE_VOLUME,
	COM_GET_VOLUMES,
	
	/* Replica commands */
	COM_ADD_REPLICA,
	
	/* General entities command */
	COM_ACCESS_MODE,
	COM_CHECK_ACCESS,
	COM_CHANGE_OWNER,
	COM_SET_ACL_ENTRIES,
	COM_REM_ACL_ENTRIES,

	COM_GET_XATTR,
	COM_SET_XATTRS,
	COM_REM_XATTRS,
	COM_STAT,
	COM_MOVE,

	/* Directory commands */
	COM_CREATE_DIR,
	COM_READ_DIR,
	COM_READ_DIR_AND_STAT,
	
	/* File commands */
	COM_CREATE_FILE,
	COM_DELETE,
	COM_OPEN,
	COM_UPD_FILESIZE,
	
	/* Links */
	COM_CREATE_SYMLINK,
	COM_CREATE_LINK,
	COM_RENEW
} MRC_Com_t;

extern char *mrc_function[];

/**
 * Data structures describing different payloads of an MRC request.
 * The data structures have a generic pattern to make it easy to
 * associate the right structure with the right request. This
 * minimises chances of introducing errors by using the wrong data
 * structure.
 * The pattern looks like
 *      MRC_Req_<func_name>_data
 * where <func_name> is created from the function name by make all
 * chars lower case and inserting a '_' between words.
 * The answer to a request has the same structure:
 *      MRC_Req_<func_name>_resp
 */ 

struct MRC_Req_add_replica_data {
	char *fileid;
	struct striping_policy *sp;
	int num_osds;
	char **osds;
};

struct MRC_Req_create_file_data {
	char *path;
	void *xAttrs;
	struct striping_policy *sp;
	void *acl;
	int mode;
	int open;
};

struct MRC_Req_create_file_resp {
	char *xcap;
	char *xloc;
};

struct MRC_Req_move_data {
	char *from;
	char *to;
};

struct MRC_Req_open_data {
	char *path;
	char *access_mode;
};

struct MRC_Req_open_resp {
	char *xcap;
	char *xloc;
};

struct MRC_Req_create_symlink_data {
	char *from;
	char *to;
};

struct MRC_Req_create_link_data {
	char *from;
	char *to;
};

struct MRC_Req_upd_fs_data {
	struct xcap *xcap;
	off_t new_size;
	int epoch;
};

struct MRC_Req_renew_data {
	struct xcap *xcap;
	off_t new_size;
	int epoch;
};

struct MRC_Req_renew_resp {
	struct list_head *xlocs_list;
	struct xcap *xcap;
};

struct MRC_Req_change_accmode_data {
	char *path;
	int mode;
};

struct MRC_Req_check_access_data {
	char *path;
	char *mode;
};

struct MRC_Req_check_access_resp {
	int grant;
};

struct MRC_Req_change_owner_data {
	char *path;
	char *userId;
	char *groupId;
};

struct MRC_Req_set_acl_entries_data {
	char *path;
};

struct MRC_Req_rem_acl_entries_data {
	char *path;
	void *entities;
};

struct MRC_Req_get_xattr_data {
	char *path;
	char *key;
};

struct MRC_Req_get_xattr_resp {
	char *value;
};

struct MRC_Req_set_xattrs_data {
	char *path;
	struct xattr_list *attrs;
};

struct MRC_Req_rem_xattrs_data {
	char *path;
	char *key;
};

struct MRC_Req_stat_data {
	char *path;
	int inclReplicas;
	int inclXAttrs;
	int inclACLs;
};

struct MRC_Req_delete_resp {
	struct xcap *xcap;
	struct xloc_list *xlocs;
};

struct MRC_Req_create_dir_data {
	char *dirPath;
	void *xAttrs;
	void *acl;
	int umask;
};

/**
 * \todo Accomodate more parameters
 */
struct MRC_Req_create_volume_data {
	char *volumeName;
	int osdSelectionPolicyId;
	struct striping_policy *defaultStripingPolicy;
	int acPolicyId;
	int partitioningPolicyId;
	void *acl;
};

struct MRC_Req_get_volumes_resp {
	int num_vols;
	char **ids;
	char **names;
};

struct MRC_Req_readDir_resp {
	int num_entries;
	char **dir_entries;
};

struct MRC_Req_readDirAndStat_resp {
	int num_entries;
	char **dir_entries;
	struct xtreemfs_statinfo *stat_entries;
};

/**
 * Payload structure for all MRC requests, used instead of MRC request data
 * 
 */
struct MRC_payload {
	MRC_Com_t command;	/*!< Command of this request */
	struct creds *creds;	/*!< Credentials for executing request */
	struct user_file *uf;	/*!< Some commands carry user_file struct */
	unsigned int order;	/*!< Request order (set if uf is not NULL) */
	union {
		struct MRC_Req_add_replica_data add_replica;
		struct MRC_Req_create_file_data create_file;
		struct MRC_Req_create_volume_data create_vol;
		struct MRC_Req_change_accmode_data chg_accmode;
		struct MRC_Req_check_access_data chk_access;
		struct MRC_Req_change_owner_data chg_owner;
		struct MRC_Req_set_acl_entries_data set_acl_entries;
		struct MRC_Req_rem_acl_entries_data rem_acl_entries;
		struct MRC_Req_get_xattr_data get_xattr;
		struct MRC_Req_set_xattrs_data set_xattrs;
		struct MRC_Req_rem_xattrs_data rem_xattrs;
		struct MRC_Req_move_data move;
		struct MRC_Req_open_data open;
		struct MRC_Req_create_symlink_data create_symlink;
		struct MRC_Req_create_link_data create_link;
		struct MRC_Req_upd_fs_data upd_fs;
		struct MRC_Req_renew_data renew;
		struct MRC_Req_stat_data stat;
		struct MRC_Req_create_dir_data create_dir;
		void *data;
	} data;
	union {
		struct MRC_Req_create_file_resp *createfile;
		struct MRC_Req_open_resp *open;
		struct MRC_Req_delete_resp *delete;
		struct MRC_Req_get_volumes_resp *get_volumes;
		struct MRC_Req_readDir_resp *readdir;
		struct MRC_Req_readDirAndStat_resp *readdirandstat;
		struct MRC_Req_renew_resp *renew;
		struct xtreemfs_statinfo *statinfo;
		struct MRC_Req_get_xattr_resp *get_xattr;
		struct MRC_Req_check_access_resp *chk_access;
	} resp;
};

void MRC_payload_free(void *p);

/** 
 * Functions to build an MRC request. These are the only allowed constructors
 * for MRC requests!
 *
 * Each function has a set of specific arguments, which end up in the
 * request's payload, and some common arguments like:
 * @param *err pointer to the error return code
 * @param *creds pointer to credentials structure
 * @param *parent pointer to the parent requests (can be NULL)
 * @param wait flag showing whether there will be a waiter for the request
 *             or not, avoids too early freeing of the request structure.
 * @return *req pointer to request structure
 */

struct req *MRC_Request__initFileSystem(int *err, struct creds *c,
					struct req *parent, int wait);

struct req *MRC_Request__addReplica(int *err, struct creds *c,
				    char *fileid, struct striping_policy *sp,
				    int num_osds, char **osds,
				    struct req *parent, int wait);

struct req *MRC_Request__createDir(int *err,
				   char *dirPath, void *xAttrs, void *acl,
				   int um, struct creds *c,
				   struct req *parent, int wait);

struct req *MRC_Request__createFile(int *err,
				    struct MRC_Req_create_file_resp *resp,
				    char *filePath, void *xAttrs,
				    struct striping_policy *sp,
				    void *acl, int mode, int do_open,
				    struct creds *c,
				    struct req *parent, int wait);

struct req *MRC_Request__delete(int *err, char *path, 
				struct MRC_Req_delete_resp *resp,
				struct creds *c, struct req *parent, int wait);

struct req *MRC_Request__move(int *err, char *sourcePath, char *targetPath,
			      struct creds *c, struct req *parent, int wait);

struct req *MRC_Request__open(int *err, struct MRC_Req_open_resp *resp,
			      char *path, char *accessMode,
			      struct creds *c, struct req *parent, int wait);

struct req *MRC_Request__readDir(int *err, struct MRC_Req_readDir_resp *resp,
				 char *path, struct creds *c,
				 struct req *parent, int wait);

struct req *MRC_Request__readDirAndStat(int *err, struct MRC_Req_readDirAndStat_resp *resp,
				 char *path, struct creds *c,
				 struct req *parent, int wait);

struct req *MRC_Request__updFileSize(int *err,
				     struct xcap *xc, off_t new_size, int epoch,
				     struct creds *c, struct req *parent,
				     int order, int wait);

struct req *MRC_Request__renew(int *err, struct MRC_Req_renew_resp *resp,
			       struct xcap *xc, off_t new_size, int epoch,
			       struct creds *c, struct req *parent,
			       int order, int wait);

struct req *MRC_Request__stat(int *err, struct xtreemfs_statinfo *si,
			      char *path, int inclReplicas, int inclXAttrs,
			      int inclACLs, struct creds *c,
			      struct req *parent, int wait);

struct req *MRC_Request__createVolume(int *err,
				      char *volumeName,
				      int osdSelectionPolicyId,
				      struct striping_policy *def_sp,
				      int acPolicyId,
				      int partitioningPolicyId,
				      struct acl_list *acl, struct creds *c,
				      struct req *parent, int wait);

struct req *MRC_Request__deleteVolume(int *err, char *volumeName,
				      struct creds *c, struct req *parent,
				      int wait);

struct req *MRC_Request__getVolumes(int *err,
				    struct MRC_Req_get_volumes_resp *resp,
				    struct creds *c, struct req *parent,
				    int wait);

struct req *MRC_Request__changeAccessMode(int *err,
					  char *path, int mode,
					  struct creds *c, struct req *parent,
					  int wait);

struct req *MRC_Request__checkAccess(int *err,
				     struct MRC_Req_check_access_resp *resp,
				     char *path, char *mode,
				     struct creds *c, struct req *parent,
				     int wait);

struct req *MRC_Request__changeOwner(int *err,
				     char *path, char *userId, char *groupId,
				     struct creds *c, struct req *parent,
				     int wait);

struct req *MRC_Request__setACLEntries(int *err,
				       char *path, void *entries,
				       struct creds *c, struct req *parent,
				       int wait);

struct req *MRC_Request__getXAttr(int *err,
				  char *path, char *key,
				  struct MRC_Req_get_xattr_resp *resp,
				  struct creds *c, struct req *parent,
				  int wait);

struct req *MRC_Request__setXAttrs(int *err,
				   char *path, struct xattr_list *attrs,
				   struct creds *c, struct req *parent,
				   int wait);

struct req *MRC_Request__removeXAttrs(int *err,
				      char *path, char *key,
				      struct creds *c, struct req *parent,
				      int wait);

struct req *MRC_Request__createLink(int *err,
				    char *linkPath, char *targetPath,
				    struct creds *c, struct req *parent,
				    int wait);

struct req *MRC_Request__createSymbolicLink(int *err,
					    char *linkPath, const char *targetPath,
					    struct creds *c,
					    struct req *parent, int wait);

//--------------------------------------------
// request creators below are not changed, yet
//--------------------------------------------
#if 0
extern int MRC_Request__addReplica(struct MRC_Request *mr, char *fileId, char *stripingPolicy, void *osd_list);

extern int MRC_Request__createSymbolicLink(struct MRC_Request *mr, char *linkPath, char *targetPath);

extern int MRC_Request__getServerConfiguration(struct MRC_Request *mc);

extern int MRC_Request__query(struct MRC_Request *mc, char *path, char *queryString);

extern int MRC_Request__removeXAttrs(struct MRC_Request *mc, char *path, void *keys);

extern int MRC_Request__setXAttrs(struct MRC_Request *mc, char *path, void *attrs);
#endif

#endif
