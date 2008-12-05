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
C++ Interface: xtreemfs_security

Description:


Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_SECURITY_H__
#define __XTRFS_SECURITY_H__

#include <ne_ssl.h>

extern int xtreemfs_verify_cert(void *userdata, int failures, const ne_ssl_certificate *cert);

#endif
