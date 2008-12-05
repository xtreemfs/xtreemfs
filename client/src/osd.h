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


#ifndef _OSD_H
#define _OSD_H

#include <ne_session.h>

#include <objects.h>

#define OSD_DEFAULT_PORT 32637


/** 
 * Set to deal with a bunch of OSDs at the same time.
 *
 * 
 */
struct OSD_Set {
	int num;
	struct OSD_Proxy **proxy;    /*!< References to proxies */
};

extern struct OSD_Set *OSD_Set_new(int num_osds, char **osd_urls);
extern int OSD_Set_init(struct OSD_Set *os, int num_osds, char **osd_urls);
extern void OSD_Set_delete(struct OSD_Set **os);

extern struct stripe_set *OSD_Set_read(struct OSD_Set *os);
extern int OSD_Set_read_ip(struct OSD_Set *os, struct stripe_set *ss);
extern int OSD_Set_write(struct OSD_Set *os, struct stripe_set *ss);

#endif
