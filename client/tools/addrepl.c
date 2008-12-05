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
*  C Implementation: addrepl
*
* Description: 
*
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <getopt.h>

#include <ne_uri.h>

#include <mrc_comm.h>
#include <mrc_request.h>
#include <request.h>
#include <logger.h>

#include "xtreemfs_utils.h"
#include "tool_utils.h"

/* Because of missing separation this has to go here. The workqueue, for
   instance requires it. */
pthread_attr_t *xpattr = NULL;

struct add_repl_params {
	/* General params */
	char *mrc_url;
	char *volname;
	char *pathname;
	struct striping_policy *sp;
	int num_osds;
	char **osds;
};


int
add_repl_params_init(struct add_repl_params *params)
{
	int err = 0;
	
	memset((void *)params, 0, sizeof(*params));
	
	params->mrc_url = NULL;
	params->volname = NULL;
	params->pathname = NULL;
	params->sp = striping_policy_new();
	params->num_osds = 0;
	params->osds = NULL;
	
	return err;
}

void
add_repl_params_delete(struct add_repl_params *params)
{
	int i;
	
	free(params->mrc_url);
	free(params->volname);
	free(params->pathname);
	free(params->sp);
	if(params->osds) {
		for(i=0; i<params->num_osds; i++)
			free(params->osds[i]);
		free(params->osds);
	}
}


int
check_osd_location(char *osd_loc_str)
{
	int err = 0;
	
	return err;
}

struct add_repl_params *
parse_args(int argc, char **argv)
{
	struct add_repl_params *rv = NULL;
	struct option opts[] = {
		{ "striping-policy", 1, NULL, 'p' },
		{ "osds",            1, NULL, 'o' },
		{ "help",            0, NULL, 'h' },
		{ NULL,              0, NULL,  0  }
	};
	int opt, opt_idx;
#if 0
	char *mrc_url_str = NULL;
	char *vol_name = NULL;
	char *path = NULL;
#endif
	int err = 0;
	int i;
	
	ne_uri path_uri;

	rv = (struct add_repl_params *)malloc(sizeof(*rv));
	if(rv == NULL) {
		err = ERR_NO_MEM;
		goto finish;
	}
	
	if(add_repl_params_init(rv)) {
		err = ERR_NO_MEM;
		add_repl_params_delete(rv);
		goto finish;
	}
	
	while(1) {
		opt = getopt_long(argc, argv, "p:o:h", opts, &opt_idx);
		if(opt == -1) break;
		
		switch(opt) {
		case 'p':
			analyse_striping_policy_str_ip(optarg, &err, rv->sp);
			
			switch(err) {
			case ERR_ONLY_POLICY:
				fprintf(stderr, "Only striping policy name given! The format");
				fprintf(stderr, "string requires stripe size and width, too.\n");
			break;
			case ERR_NO_STRIPE_WIDTH:
				fprintf(stderr, "Missing stripe width.\n");
			break;
			case ERR_STRIPE_SIZE_NOT_A_NUMBER:
				fprintf(stderr, "Stripe size must be a number.\n");
			break;
			case ERR_STRIPE_WIDTH_NOT_A_NUMBER:
				fprintf(stderr, "Stripe width must be a number.\n");
			break;
			case ERR_TOO_MANY_ARGUMENTS:
				fprintf(stderr, "Illegal striping policy format string, because");
				fprintf(stderr, "there are more than three arguments.\n");
			break;
			case ERR_UNKNOWN_POLICY:
				fprintf(stderr, "The policy name that you specified is not ");
				fprintf(stderr, "supported. Please check the name again.\n");
			default:
			break;
			}
		break;
		
		}
		if(err != 0) break;
	}
	
	memset((void *)&path_uri, 0, sizeof(ne_uri));
	if(str_to_uri(argv[optind], &path_uri)) {
		fprintf(stderr, "Cannot parse MRC URL '%s'\n", argv[optind]);
		err = -1;
		goto finish;
	}
	
	rv->mrc_url  = extract_mrc(&path_uri);
	rv->volname  = extract_volume(&path_uri);
	rv->pathname = extract_path(&path_uri);

	/* Remaining args are interpreted as OSDs */
	
	rv->num_osds = argc - optind - 1;	
	if(rv->num_osds != rv->sp->width) {
		fprintf(stderr, "Number of specified OSDs differs from number required ");
		fprintf(stderr, "by striping policy! The policy requires %d OSD(s) ", rv->sp->width);
		fprintf(stderr, "but %d have been specified.\n", rv->num_osds);
		err = -1;
		goto finish;
	}
	
	rv->osds = (char **)malloc(sizeof(char *) * rv->num_osds);
	if(rv->osds == NULL) {
		fprintf(stderr, "Cannot allocate memory for OSD locations.\n");
		err = -1;
		goto finish;
	}
	memset((void *)rv->osds, 0, sizeof(char *) * rv->num_osds);
	
	for(i=optind+1; i<argc; i++) {
		if(check_osd_location(argv[i])) {
			fprintf(stderr, "'%s' is not a valid location for an OSD!\n", argv[i]);
			err = -1;
			goto finish;
		} else {
			rv->osds[i-optind-1] = strdup(argv[i]);
		}
	}
	
	
finish:
	if(err != 0) {
		add_repl_params_delete(rv);
		free(rv);
		rv = NULL;
	}
	
	ne_uri_free(&path_uri);
	
	return rv;
}


int
main(int argc, char **argv)
{
	int err;
	struct add_repl_params *params = NULL;
	struct req *req;
	struct creds c;
	char *fullname = NULL;
	
	struct xtreemfs_statinfo si;
	
#if 0	
	struct MRC_Req_get_volumes_resp req_resp;	
	char *fileid;
#endif
	
	ne_sock_init();
	params = parse_args(argc, argv);
	
	if(argc == 1) {
		ne_sock_exit();
		exit(1);
	}
	
	
	/* For the time being, disable logging */
	// logit.level = LOGLEV_NO_LOG;

	mrccomm_init(params->mrc_url, 1);	
	get_local_creds(&c);
	
	/* First get fileid of path */
	
	fullname = create_fullname(params->volname, params->pathname);
	req = MRC_Request__stat(&err, &si,
			      fullname, 0, 0,
			      0, &c, NULL, 1);
	if(err) {
		fprintf(stderr, "Internal error. Cannot create stat request.\n");
		goto finish;
	}
	err = req_wait(req);
	req_put(req);
	if(err) {
		if(err == -2) {
			fprintf(stderr, "No such file '%s' on volume '%s'.\n",
				params->pathname, params->volname);
		}
		goto finish;
	}
	
	req = MRC_Request__addReplica(&err, &c, si.fileId,
				      params->sp, params->num_osds, params->osds,
				      NULL, 1);
	if (err)
		goto finish;
	err = req_wait(req);
	req_put(req);
	if (err)
		goto finish;

finish:
	free(fullname);
	if(params) {
		add_repl_params_delete(params);
		free(params);
	}
	ne_sock_exit();
	return 0;
}
