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
*  C Implementation: xtreemfs_security
*
* Description: 
*
*
* Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <ne_session.h>
#include <ne_ssl.h>

#include "xtreemfs_security.h"
#include "logger.h"

int xtreemfs_verify_cert(void *userdata, int failures, const ne_ssl_certificate *cert)
{
	const char *hostname = userdata;

	dbg_msg("Entering.\n");
	dbg_msg("Failures: %x\n", failures);
		
	if (failures & NE_SSL_IDMISMATCH) {
		const char *id = ne_ssl_cert_identity(cert);
		if (id)
			dbg_msg("Server certificate was issued to '%s' not '%s'.\n",
				id, hostname);
		else
			dbg_msg("The certificate was not issued for '%s'\n", hostname);
	}

	if (failures & NE_SSL_UNTRUSTED)
		dbg_msg("The certificate is not signed by a trusted Certificate Authority.");

	dbg_msg("Leaving.\n");
	
	return 0 ;
}
