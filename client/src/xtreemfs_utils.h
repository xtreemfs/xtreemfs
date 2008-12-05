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
   C Interface: xtreemfs_utils

   Description: 


   Author: Matthias Hess <matthiash@acm.org>, (C) 2007

   Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_UTILS_H__
#define __XTRFS_UTILS_H__

#include <ne_uri.h>

#include <dirservice.h>
#include <mrc.h>

extern char *supported_schemes[];

extern int str_to_uri(char *str, ne_uri *uri, int def_port);
extern int str_to_uri_ds(char *str, ne_uri *uri, int def_port, struct dirservice *ds);

extern char *extract_hostid(char *path);
extern char *extract_volumeid(char *path);

extern char *extract_mrc(ne_uri *uri);
extern char *extract_volpath(ne_uri *uri);
extern char *extract_volume(ne_uri *uri);
extern char *extract_path(ne_uri *uri);
extern char *extract_pseudo_uuid(char *name);

extern char *create_fullname(const char *volname, const char *path);

extern char *create_vol_url(struct dirservice *ds, char *vol_path);
extern char *resolve_uuid(struct dirservice *ds, char *uuid);

extern char *current_basedir();

extern char *sha1_digest_to_str(unsigned char *digest);

extern int print_neon_buffer(ne_buffer *buf);

#endif
