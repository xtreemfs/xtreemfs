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
*  C Implementation: lsvol
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
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

#ifdef ITAC
#include <VT.h>
#include <xtreemfs_itac.h>
#endif

#include <ne_uri.h>

#include <mrc_comm.h>
#include <mrc_request.h>
#include <request.h>
#include <logger.h>

#include "xtreemfs_utils.h"
#include "tool_utils.h"

pthread_attr_t *xpattr;

struct lsvol_params {
	char *mrc_url;
	char *cert_file;
	int long_listing;
	char *dirservice;
};

void
print_help(int argc, char **argv)
{
	char *pname = progname(argv[0]);

	if (pname) {
		printf("%s is used to show information on volumes on a specified MRC.\n", pname);
		printf("Usage:\n\n");
		printf("\t%s <mrc url>\n\n", pname);
		free(pname);
	}
}

int lsvol_params_init(struct lsvol_params *lp)
{
	lp->mrc_url = NULL;
	lp->cert_file = NULL;
	lp->long_listing = 0;
	lp->dirservice = NULL;

	return 0;
}

void lsvol_params_delete(struct lsvol_params **lp)
{
	if (*lp) {
		if ((*lp)->mrc_url)
			free((*lp)->mrc_url);
		free((*lp)->dirservice);
		free(*lp);
		*lp = NULL;
	}
}

struct lsvol_params *
parse_args(int argc, char **argv, int *err)
{
	struct lsvol_params *rv = NULL;
	struct option opts[] = {
		{ "help",       0, NULL, 'h' },
		{ "long",       0, NULL, 'l' },
		{ "dirservice", 1, NULL, 'D' },
		{ "cert",       0, NULL, 'c' },
		{ NULL, 0, NULL, 0 }
	};
	int opt, opt_idx;

	*err = ERR_NO_ERROR;
	
	rv = (struct lsvol_params *)malloc(sizeof(struct lsvol_params));
	if (!rv) {
		*err = ERR_NO_MEM;
		goto finish;
	}

	if (lsvol_params_init(rv)) {
		lsvol_params_delete(&rv);
		*err = ERR_NO_MEM;
		goto finish;
	}

	while(1) {
		opt = getopt_long(argc, argv, "p:s:a:hlD:c:", opts, &opt_idx);
		if(opt == -1)
			break;
		switch(opt) {
		case 'h':
			print_help(argc, argv);
		break;
		case 'l':
			rv->long_listing = 1;
		break;
		case 'D':
			rv->dirservice = strdup(optarg);
		break;
		case 'c':
			rv->cert_file = strdup(optarg);
		break;
		}
	}

	if(optind >= argc) {
		fprintf(stderr, "No volume specification given.\n");
		*err = ERR_NO_VOLUME_URL;
		goto finish;
	}
	
	if(optind + 1 < argc) {
		fprintf(stderr, "Warning! More than one volume URL given. Creating only first ");
		fprintf(stderr, "volume.\n");
	}

	rv->mrc_url = strdup(argv[optind]);
	
finish:
	return rv;
}


int
main(int argc, char **argv)
{
	struct req *req;
	struct creds c;
	struct lsvol_params *lp = NULL;
	struct MRC_Req_get_volumes_resp req_resp;
	char *mrc_url_str = NULL;
	ne_uri mrc_uri;
	struct xtreemfs_statinfo si;
	struct dirservice *ds = NULL;
	char cert_pw[128];

	int i, j, err = 0;
	

#ifdef ITAC
 	VT_initialize(&argc, &argv);

	VT_classdef("fuse",   &itac_fuse_class);
	VT_classdef("osd",    &itac_osd_class);
	VT_classdef("filerw", &itac_filerw_class);
	VT_classdef("fobj",   &itac_fobj_class);
	VT_classdef("sobj",   &itac_sobj_class);

	VT_classdef("spin",   &itac_spin_class);

	VT_funcdef("read",    itac_fuse_class,   &itac_fuse_read_hdl);
	VT_funcdef("write",   itac_fuse_class,   &itac_fuse_write_hdl);
	VT_funcdef("flush",   itac_fuse_class,   &itac_fuse_flush_hdl);
	VT_funcdef("release", itac_fuse_class,   &itac_fuse_release_hdl);

	VT_funcdef("exec",    itac_osd_class,    &itac_osd_exec_hdl);
	VT_funcdef("put",     itac_osd_class,    &itac_osd_put_hdl);
	VT_funcdef("get",     itac_osd_class,    &itac_osd_get_hdl);

	VT_funcdef("handle",  itac_filerw_class, &itac_filerw_handle_hdl);
	VT_funcdef("handle",  itac_fobj_class,   &itac_fobj_handle_hdl);
	VT_funcdef("handle",  itac_sobj_class,   &itac_sobj_handle_hdl);
#endif

	ne_sock_init();

	if(argc < 2) {
		print_help(argc, argv);
		exit(1);
	}

	/* For the time being, disable logging */
	// logit.level = LOGLEV_NO_LOG;

	memset((void *)&mrc_uri, 0, sizeof(mrc_uri));

	lp = parse_args(argc, argv, &err);
	if (err != ERR_NO_ERROR)
		exit(1);


	if (lp->cert_file) {
		xtreemfs_client_cert = ne_ssl_clicert_read(lp->cert_file);
		if (!xtreemfs_client_cert) {
			fprintf(stderr, "Cannot read certificate file '%s'\n",
				lp->cert_file);
			err = ERR_get_error();
			fprintf(stderr, "Reason: %d\n", err);
			goto finish;
		}
		if (ne_ssl_clicert_encrypted(xtreemfs_client_cert)) {
			int try=0;
			dbg_msg("Certificate is password protected!\n");
			do {
				EVP_read_pw_string(cert_pw, 128, "Password: ", 0);
				try++;
			} while(ne_ssl_clicert_decrypt(xtreemfs_client_cert, cert_pw) &&
				try < 3);
			if (ne_ssl_clicert_encrypted(xtreemfs_client_cert)) {
				fprintf(stderr, "Password does not match certificate.\n");
				exit(1);
			}
			/* Clear password */
			memset((void *)cert_pw, 0, 128);
		} else {
			dbg_msg("Certificate is not password protected.\n");
		}
	}

	if (lp->dirservice) {
		ds = dirservice_new(lp->dirservice, xtreemfs_client_cert);
		if (ds) {
			if (dirservice_connect(ds)) {
				fprintf(stderr, "Cannot connect to directory service.\n");
				exit(1);
			}
		}
	}
	
	memset((void *)&mrc_uri, 0, sizeof(ne_uri));
	if(str_to_uri_ds(lp->mrc_url, &mrc_uri, MRC_DEFAULT_PORT, ds)) {
		fprintf(stderr, "Cannot parse MRC URL '%s'\n", argv[1]);
		exit(1);
	}
	
	mrc_url_str = extract_mrc(&mrc_uri);
	
	mrccomm_init(mrc_url_str, 1);

	get_local_creds(&c);
	req = MRC_Request__getVolumes(&err, &req_resp, &c, NULL, 1);
	err = execute_req(mrccomm_wq, req);
	if (err)
		goto finish;
	
	for(i=0; i<req_resp.num_vols; i++) {
		if (lp->long_listing != 0) {
			req = MRC_Request__stat(&err, &si,
			      req_resp.names[i], 1, 0, 0, &c, NULL, 1);
			if (err)
				continue;
			err = req_wait(req);
			req_put(req);
			if (err) {
			}

			printf("Volume '%s'\n", req_resp.names[i]);
			for (j=0; j<strlen(req_resp.names[i])+strlen("Volume ''"); j++)
				printf("-");
			printf("\n");
			printf("\tID:       %s\n", req_resp.ids[i]);
			printf("\tOwner:    %s\n", si.ownerId);
			printf("\tGroup:    %s\n", si.groupId);
			/* printf("\tSize:     %ld\n", si.size); */
			printf("\tAccess:   %d\n", si.posixAccessMode);
			
			xtreemfs_statinfo_clear(&si);
		} else {
			printf("%s  ->  %s\n", req_resp.names[i], req_resp.ids[i]);
		}
	}
	
	for(i=0; i<req_resp.num_vols; i++) {
		free(req_resp.ids[i]);
		free(req_resp.names[i]);
	}
	free(req_resp.ids);
	free(req_resp.names);
	
finish:
	if (lp)
		lsvol_params_delete(&lp);

	if (ds) {
		dirservice_disconnect(ds);
		dirservice_destroy(ds);
	}

	free(mrc_url_str);
	ne_uri_free(&mrc_uri);

#ifdef ITAC
	VT_finalize();
#endif

	return err;
}
