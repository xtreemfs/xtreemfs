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


#ifndef __XTRFS_MRC_H__
#define __XTRFS_MRC_H__
/*
 * $Id$
 *
 * Description: 
 *
 *
 * Authors:
 *      Erich Focht <efocht at hpce dot nec dot com>, (c) 2007
 *      Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
 *
 * Copyright: See COPYING file that comes with this distribution
 *
 */

#include "request.h"
#include "mrc_request.h"

#define MRC_DEFAULT_PORT    32636

extern struct work_queue *mrccomm_wq;

void print_mrccomm_info(struct req *req);

void mrccomm_init(char *mrc_url, int num_threads);

struct req *mrccomm_req_create(int *err, struct MRC_payload *p,
			       struct req *parent, int wait);


#endif /* __XTRFS_MRC_H__ */
