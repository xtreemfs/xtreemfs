/* Copyright (c) 2007, 2008  Erich Focht, Matthias Hess
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


#ifndef __XTRFS_FILERW_H__
#define __XTRFS_FILERW_H__

/**
  \file filerw.h

  File read/write operations related definitions.

  @author Erich Focht <efocht@hpce.nec.com>

*/

#include "file.h"
#include "fileobj.h"
#include "request.h"

#ifndef min
#define min(a,b) (((a) < (b)) ? (a) : (b))
#endif

struct filerw_payload {
	loff_t offset;		/*!< offset in the full file */
	size_t size;		/*!< transfer size (bytes) */
	void *buffer;		/*!< pointer to buffer for data */
	struct user_file *fd; 	/*!< file descriptor of open file */
};

struct filerw_response {
	size_t *bytes_ptr;	/*!< Pointer to result: number of bytes that
				  have actually been read or written */
	ssize_t new_size;
	long epoch;
};

/**
 * Payload for a filerw_fobj_done request (_finish handler).
 */
struct filerw_fobj_done_data {
	int fobj_id;
	int offset;
	int num_bytes;
	ssize_t new_size;
	long epoch;
};

extern struct work_queue *filerw_wq;

void filerw_init(void);
void filerw_close(void);

struct req *filerw_req_create(int type, loff_t offset, size_t size,
			      void *buffer, struct user_file *fd,
			      size_t *tsize, struct req *parent);

struct req *filerw_fobj_done_req(int fobj_id, int offset, int num_bytes,
				 ssize_t new_size, long epoch,
				 struct req *parent);

void print_filerw_info(struct req *req);


#endif // __XTRFS_FILERW_H__
