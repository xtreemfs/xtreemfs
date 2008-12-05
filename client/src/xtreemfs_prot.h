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
   C Interface: xtreemfs_prot

   Description:
*/

#ifndef __XTRFS_PROT_H__
#define __XTRFS_PROT_H__

#include <ne_request.h>

#include "creds.h"

struct prot_list {
	int max_num_vers;
	int num_vers;
	int *vers;
};

extern struct prot_list *prot_list_new(int init_num);
extern int prot_list_init(struct prot_list *pl, int init_num);
extern void prot_list_del_contents(struct prot_list *pl);
extern void prot_list_destroy(struct prot_list *pl);

extern int prot_list_add_vers(struct prot_list *pl, int vers);

extern int prot_list_cmp(struct prot_list *pl1, struct prot_list *pl2);

extern int prot_list_info(struct prot_list *pl);

extern struct prot_list *json_to_prot_list(struct json_object *jo);
extern int json_to_prot_list_ip(struct prot_list *pl, struct json_object *jo);
extern struct json_object *prot_list_to_json(struct prot_list *pl);

/* These are related to the global protocol list of this client. */
extern struct prot_list *client_vers;
extern struct prot_list *prot_list_create_client_list();
extern struct prot_list *prot_get_versions(ne_session *sess, struct prot_list *pl);

/* The following is for JSON over HTTP channels only */

extern ne_request *prot_getProtocolVersion_req(ne_session *sess,
					       struct creds *creds,
					       struct json_object *req_params);

#endif
