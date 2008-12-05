/* Copyright (c) 2007, 2008  Matthias Hess
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
 * C Implementation: rmvol
 *
 * Description: Remove an XtreemFS volume.
 *
 *
 * Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
 *       & Erich Focht <efocht at hpce dot nec dot com>
 *
 * Copyright: See COPYING file that comes with this distribution
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <getopt.h>

#include <mrc_request.h>
#include <mrc_comm.h>
#include <mrc_request.h>
#include <logger.h>
#include <xtreemfs.h>
#include "xtreemfs_utils.h"
#include "tool_utils.h"


struct rmvol_params {
	char *vol_url;
	ne_uri volume_url;
	char *cert_file;
	char *dirservice;
};

pthread_attr_t *xpattr;

int rmvol_params_init(struct rmvol_params *rp)
{
	int err = 0;

	rp->vol_url    = NULL;
	rp->cert_file  = NULL;
	rp->dirservice = NULL;

	return err;
}

void rmvol_params_delete(struct rmvol_params *rp)
{
	free(rp->vol_url);
	free(rp->cert_file);
	free(rp->dirservice);

	if (&rp->volume_url)
		ne_uri_free(&rp->volume_url);
}

struct rmvol_params *rmvol_params_new()
{
	struct rmvol_params *rv = NULL;

	rv = (struct rmvol_params *)malloc(sizeof(struct rmvol_params));
	if (rv && rmvol_params_init(rv)) {
		rmvol_params_delete(rv);
		rv = NULL;
	}

	return rv;
}

struct rmvol_params *parse_args(int argc, char **argv)
{
	struct rmvol_params *rv = NULL;
	int err = ERR_NO_ERROR;
	struct option opts[] = {
		{ "cert",            1, NULL, 'c' },
		{ "dirservice",      1, NULL, 'D' },
		{ "help",            0, NULL, 'h' },
		{ NULL, 0, NULL, 0 }
	};
	int opt, opt_idx;
	char *real_volume_url = NULL;
	struct dirservice *ds = NULL;

	rv = rmvol_params_new();
	if (!rv) {
		err = -ENOMEM;
		goto finish;
	}

	while(1) {
		opt = getopt_long(argc, argv, "c:hD:", opts, &opt_idx);
		if(opt == -1) break;
		
		switch(opt) {
			case 0:
			break;

			case 'c':
				rv->cert_file = strdup(optarg);
			break;

			case 'h':

			break;

			case 'D':
				rv->dirservice = strdup(optarg);
			break;
		}
	}

	/* Now we need to have exactly one more non-optional argument. */
	
	if(optind >= argc) {
		fprintf(stderr, "No volume specification given.\n");
		err = ERR_NO_VOLUME_URL;
		goto finish;
	}
	
	if(optind + 1 < argc) {
		fprintf(stderr, "Warning! More than one volume URL given. Removing only first ");
		fprintf(stderr, "volume.\n");
	}

	if (rv->dirservice) {
		ds = dirservice_new(rv->dirservice, xtreemfs_client_cert);
		if (ds)
			dirservice_connect(ds);
	}

	if (str_to_uri_ds(argv[optind], &rv->volume_url, 0, ds)) {
		fprintf(stderr, "Cannot parse the given volume url '%s'\n",
			argv[optind]);
		err = ERR_PARSE_ERROR;
		goto finish;
	}

finish:
	if (ds) {
		dirservice_disconnect(ds);
		dirservice_destroy(ds);
	}

	if (err && rv) {
		rmvol_params_delete(rv);
		rv = NULL;
	}

	return rv;
}

int
main(int argc, char **argv)
{
	struct req *req;
	struct creds c;
	struct rmvol_params *rp = NULL;
	ne_uri volume_url;
	char *mrc_url = NULL;
	char *vol_name = NULL;
	int err = 0;

	if(argc < 2) {
		fprintf(stderr, "Too few arguments.\n");
		exit(1);
	}

	rp = parse_args(argc, argv);

	mrc_url = extract_mrc(&rp->volume_url);
	vol_name = extract_volume(&rp->volume_url);

	if (!vol_name) {
		fprintf(stderr, "No volume name given.\n");
		exit(1);
	}

	/* For the time being, disable logging */
	logit.level = LOGLEV_DEBUG;

	mrccomm_init(mrc_url, 1);
	get_local_creds(&c);
		
	req = MRC_Request__deleteVolume(&err, vol_name, &c, NULL, 1);
	if (!req) {
		err = 1;
		fprintf(stderr, "Could not create MRC request!\n");
		goto finish;
	}
	err = execute_req(mrccomm_wq, req);
	if (err) {
		fprintf(stderr, "Delete volume request returned %d\n", err);
	}

finish:
	free(mrc_url);
	free(vol_name);
	rmvol_params_delete(rp);

	return err;
}
