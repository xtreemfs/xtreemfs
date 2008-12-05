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


#include <stdlib.h>
#include <string.h>

#include "osd.h"



struct OSD_Set *
OSD_Set_new(int num_osds, char **osd_urls)
{
	struct OSD_Set *rv = NULL;
	
	rv = (struct OSD_Set *)malloc(sizeof(struct OSD_Set));
	if(rv != NULL) if(OSD_Set_init(rv, num_osds, osd_urls) != 0) OSD_Set_delete(&rv);
	
	return rv;
}

int
OSD_Set_init(struct OSD_Set *os, int num_osds, char **osd_urls)
{
	int rv = 0;
	
	return rv;
}


void
OSD_Set_delete(struct OSD_Set **os)
{
	if(*os) {
		free(*os);
		*os = NULL;
	}
}


struct stripe_set *
OSD_Set_read(struct OSD_Set *os)
{
	struct stripe_set *rv = NULL;
	
	return rv;
}


int
OSD_Set_read_ip(struct OSD_Set *os, struct stripe_set *ss)
{
	int rv = 0;
	
	return rv;
}


int
OSD_Set_write(struct OSD_Set *os, struct stripe_set *ss)
{
	int rv = 0;
	
	return rv;
}
