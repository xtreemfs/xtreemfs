//
// C++ Interface: fileobj
//
// Description: 
//
//
// Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008
//
// Copyright: See COPYING file that comes with this distribution
//
//

#ifndef __XTRFS_FILEOBJ_H__
#define __XTRFS_FILEOBJ_H__

#include "filerw.h"
#include "fileops.h"

struct fobj_payload {
	enum filerw_op op;	/*!< Operation (read/write) */
	int fobj_id;		/*!< File object ID */
	loff_t offset;		/*!< offset in the file object */
	size_t size;		/*!< transfer size (bytes) */
	size_t rsize;		/*!< real transfer size (as return value) */
	void *buffer;		/*!< pointer to buffer for data */
	struct user_file *fd; 	/*!< file descriptor of open file */
	int use_cache;		/*!< If set try to use cache for operation */
	int new_cache_entry;	/*!< Create a new cache entry at the
				     end of the request. */
	struct fobj_cache_entry *fce;	/*!< Cache entry associated with operation */
};

struct fobj_result {
	size_t num_bytes;
	ssize_t new_size;
	long epoch;
};

struct fobj_sobj_done_data {
	int sobj_id;		/*!< Id of sobj that just finished */
	loff_t offset;		/*!< Offset within that (stripe-)object */
	size_t size;		/*!< Number of bytes to read. */
	size_t num_bytes;	/*!< Number of bytes that had been processed */
	ssize_t new_size;	/*!< Eventually OSD tells us a new size... */
	long epoch;		/*!< ... and epoch. */
	int error;		/*!< Eventually there is an error code. */
};

struct req *fobj_sobj_done_req(int sobj_id,
			       loff_t offset, size_t size, size_t num_bytes,
			       size_t new_size, long epoch,
			       struct req *parent);


/* fileobj related stubs (not worth creating a separate include file) */

void fobj_init(void);
void fobj_close(void);

struct req *fobj_req_create(int type, int fobj_id, loff_t offset,
			    size_t size, void *buffer, struct user_file *fd,
			    int use_cache,
			    struct req *parent);

extern struct work_queue *fobj_wq;

void print_fobj_info(struct req *req);

#endif
