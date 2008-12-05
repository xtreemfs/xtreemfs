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
 *  C Implementation: mkvol
 *
 * Description: 
 *
 *
 * Author: Matthias Hess <mhess at hpce dot nec dot de>, Copyright 2007
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

#include <sys/types.h>
#include <sys/stat.h>

#include <ne_ssl.h>
#include <openssl/evp.h>

#include <mrc_request.h>
#include <mrc_comm.h>
#include <stripingpolicy.h>
#include <logger.h>
#include <xtreemfs.h>

#ifdef ITAC
#include <VT.h>
#include <xtreemfs_itac.h>
#endif

#ifdef XTREEMOS_ENV
#include <xos_ams.h>
#endif

#include "xtreemfs_utils.h"
#include "tool_utils.h"

struct volume_params {
	char *mrc_url;
	char *name;
	char *scheme;
	int osdSelectionPolicy;
	struct striping_policy *defStripingPolicy;
	int acPolicyId;
	int def_mode;
	char *acMode;
	int partitioningPolicyId;
	void *acl;
	char *cert_file;
	int debug;
	char *dirservice;
};

pthread_attr_t *xpattr;

int
volume_params_init(struct volume_params *vp)
{
	int rv = 0;
	
	vp->name                 = NULL;
	vp->scheme               = NULL;
	vp->osdSelectionPolicy   = 1;
	vp->defStripingPolicy = striping_policy_new();
	if(vp->defStripingPolicy) {
		vp->defStripingPolicy->id = SPOL_RAID_0;
		vp->defStripingPolicy->stripe_size = 4096;
		vp->defStripingPolicy->width = 1;
	}
	else
		rv = 1;
	vp->acPolicyId           = 2;
	vp->acMode               = NULL;
	vp->partitioningPolicyId = 1;
	vp->acl                  = NULL;

	vp->debug                = 0;
	vp->dirservice           = NULL;
	vp->cert_file            = NULL;

	return rv;
}

void
volume_params_delete(struct volume_params *vp)
{
	free(vp->mrc_url);
	free(vp->name);
	free(vp->acMode);
	free(vp->cert_file);
	free(vp);
}


void
print_help(int argc, char **argv)
{
	char *pname = progname(argv[0]);
	
	printf("%s is used to create a new volume on a specified MRC.\n", pname);
	printf("Usage:\n\n");
	printf("\t%s [options] <volume url>\n\n", pname);
	printf(
"Options\n"
"\t-p/--striping-policy=policy  policy is a string definig the striping policy.\n"
"\t                             It has the following format:\n"
"\t                             name,size,width\n"
"\t                             name   is selected from 'RAID0', 'RAID1' or 'RAID5'\n"
"\t                             size   gives the size of each stripe in kB\n"
"\t                             width  is an integer specifying the striping width.\n"
"\t                             Default setting for the striping policy is RAID0,4,1\n"
"\t                             meaning raid0 with 1 OSD and 4 kB stripe size.\n" 
"\n"
"\t-s/--osd-selection=id        Choose how to select OSDs.\n"
"\t                             1: 'random OSD selection'\n"
"\t                             2: 'proximity-based OSD selection'\n"
"\t                             default setting for this option is 1.\n"
"\n"
"\t-a/--access-policy=id        Choose the access control mechanism for this volume.\n"
"\t                             Allowed values for id are\n"
"\t                             1: Everyone is allowed to access the volume\n"
"\t                             2: Access is based on POSIX ACLs\n"
"\t                             3: Access is based on volume ACLs\n"
"\t                             Default setting for this option is 2\n"
"\n"
"\t-m/--mode=mode               Specify the default mode for the chosen access policy.\n"
"\t                             If no mode is given, the default mode for the policy\n"
"\t                             will be used.\n"
"\t-c/--cert=file               Use this certificate to connect to the MRC. If you\n"
"\t                             are using a certificate, the scheme must be 'https'\n"
"\t-D/--dirservice=ds           Use directory service specified by 'ds'.\n"
"\t-h/--help                    Print this help text.\n"
"\n"
"The volume location is given by the URI <volume url> which has the following generic\n"
"pattern: <scheme>://<mrc host>[:<mrc port>]/<volume name>\n"
"\tThe only supported <scheme> right now is 'http'.\n"
"\tThe MRC host can be given either as a hostname or an IPv4 address. If no port is\n"
"\tspecified, the default MRC port of 32636 is assumed. The volume name <volume name>\n"
"\tfollows the usual rules of filenames.\n"
"\n"
);

	free(pname);
}


struct volume_params *
parse_args(int argc, char **argv)
{
	struct volume_params *rv = NULL;
	struct option opts[] = {
		{ "striping-policy", 1, NULL, 'p' },
		{ "osd-selection",   1, NULL, 's' },
		{ "access-policy",   1, NULL, 'a' },
		{ "mode",            1, NULL, 'm' },
		{ "cert",            1, NULL, 'c' },
		{ "debug",           0, NULL, 'd' },
		{ "dirservice",      1, NULL, 'D' },
		{ "help",            0, NULL, 'h' },
		{ NULL, 0, NULL, 0 }
	};
	int opt, opt_idx;
	ne_uri volume_uri;
	char *real_volume_url = NULL;
	struct dirservice *ds = NULL;
	char cert_pw[128];

	int err = 0;
	int i;
	
	rv = (struct volume_params *)malloc(sizeof(struct volume_params));
	if(rv == NULL) {
		err = ERR_NO_MEM;
		goto finish;
	}
	if(volume_params_init(rv) != 0) {
		err = ERR_NO_MEM;
		goto finish;
	}
	
	while(1) {
		opt = getopt_long(argc, argv, "p:s:a:m:c:hdD:", opts, &opt_idx);
		if(opt == -1) break;
		
		switch(opt) {
		case 0:
		
		break;
		
		case 'p':
			analyse_striping_policy_str_ip(optarg, &err, rv->defStripingPolicy);
			
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
		
		case 's':
			rv->osdSelectionPolicy = atoi(optarg);
		break;

		case 'a':
			rv->acPolicyId = atoi(optarg);
		break;

		case 'm':
			rv->acMode = strdup(optarg);
		break;

		case 'c':
			rv->cert_file = strdup(optarg);
		break;

		case 'd':
			rv->debug = 1;
		break;

		case 'D':
			rv->dirservice = strdup(optarg);
		break;

		case 'h':
			print_help(argc, argv);
		break;
		}
		
		if(err != 0) break; 
	}

	/* Now we need to have exactly one more non-optional argument. */
	
	if(optind >= argc) {
		fprintf(stderr, "No volume specification given.\n");
		err = ERR_NO_VOLUME_URL;
		goto finish;
	}
	
	if(optind + 1 < argc) {
		fprintf(stderr, "Warning! More than one volume URL given. Creating only first ");
		fprintf(stderr, "volume.\n");
	}
	
	
	/* Now try to analyse the volume url */
#if 0	
	/* Do we have a scheme? */
	if(strstr(argv[optind], "://") == NULL) {
		real_volume_url = (char *)malloc(strlen(argv[optind]) + strlen("http://") + 1);
		real_volume_url = strcpy(real_volume_url, "http://");
		real_volume_url = strcat(real_volume_url, argv[optind]);
	}
	else {
		real_volume_url = strdup(argv[optind]);
	}
	
	if(ne_uri_parse((const char *)real_volume_url, &volume_uri) != 0) {
		fprintf(stderr, "Cannot parse the given volume url '%s'\n",
			argv[optind]);
		err = ERR_PARSE_ERROR;
		goto finish;
	}
#endif

	if (rv->cert_file) {
		xtreemfs_client_cert = ne_ssl_clicert_read(rv->cert_file);
		if (!xtreemfs_client_cert) {
			fprintf(stderr, "Cannot read certificate file '%s'\n",
				rv->cert_file);
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

	if (rv->dirservice) {
		ds = dirservice_new(rv->dirservice, xtreemfs_client_cert);
		if (ds)
			dirservice_connect(ds);
	}
		
	if (str_to_uri_ds(argv[optind], &volume_uri, 0, ds)) {
		fprintf(stderr, "Cannot parse the given volume url '%s'\n",
			argv[optind]);
		err = ERR_PARSE_ERROR;
		goto finish;
	}
	
	/* Scheme supported? */
	for(i=0; supported_schemes[i] != NULL; i++) {
		if(!strcmp(volume_uri.scheme, supported_schemes[i])) break;
	}
	if(supported_schemes[i] == NULL) {
		fprintf(stderr, "Scheme '%s' is not supported.\n", volume_uri.scheme);
		err = ERR_UNSUPPORTED_SCHEME;
		goto finish;
	} else {
		rv->scheme = strdup(volume_uri.scheme);
	}
		
	/* Port given? */
	if(volume_uri.port == 0)
		volume_uri.port = MRC_DEFAULT_PORT;
	
	/* Exploit a trick to get MRC url. */
	if(volume_uri.path != NULL && strcmp(volume_uri.path, "/")) {
		if(volume_uri.path[0] == '/')
			rv->name = strdup(&volume_uri.path[1]);
		else
			rv->name = strdup(volume_uri.path);
		free(volume_uri.path);
		
		volume_uri.path = strdup("/");
		rv->mrc_url = ne_uri_unparse(&volume_uri);
	}
	else {
		fprintf(stderr, "No volume given!\n");
		err = ERR_NO_VOLUME_URL;
		goto uri_free;
	}

uri_free:
	ne_uri_free(&volume_uri);
	
finish:
	if (ds) {
		dirservice_disconnect(ds);
		dirservice_destroy(ds);
	}

	if(err != ERR_NO_ERROR) {
		if(rv != NULL)
			volume_params_delete(rv);
		rv = NULL;
	}
	free(real_volume_url);
		
	return rv;
}

int
mode_str_to_int(char *ms)
{
	int rv = 0;
	int j, l;
	char *endp;

	if (!ms)
		goto finish;

	l = strlen(ms);
	
	/* Skip white space */
	j = 0;
	while (j < l && (ms[j] == ' ' || ms[j] == '\t')) j++;

	/* If we start with a number, assume it is octal */
	if (ms[j] >= '0' && ms[j] <= '9') {
		rv = (int)strtol(&ms[j], &endp, 8);
		if (*endp != '\0') rv = -1;
	} else {    /* Assume we have a string like 'u=<mode>,g=<mode>,...' */

	}

finish:
	return rv;
}

int
main(int argc, char **argv)
{
	struct volume_params *vp;
	struct req *req;
	struct creds c;
	struct acl_list acl_list;
	/* For later use: struct acl_list *al; */
	int default_mode;
	int err = 0;
	mode_t um;

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
	
#ifdef XTREEMOS_ENV
	pthread_mutex_init(&xtreemos_ams_mutex, NULL);
	xtreemos_ams_sock = amsclient_connect_open();
	if (xtreemos_ams_sock < 0) {
		err_msg("Cannot open connection to AMS.\n");
	}
#endif

	logit.level = LOGLEV_NO_LOG;

	vp = parse_args(argc, argv);
	if(vp == NULL) {
		ne_sock_exit();
		exit(1);
	}

	if (!vp->debug)
		/* For the time being, disable logging */
		logit.level = LOGLEV_NO_LOG;
	else
		logit.level = vp->debug;
	
	/* Read certificate, eventually */
	if(vp->cert_file != NULL) {
		if(strcmp(vp->scheme, "https")) {
			fprintf(stderr, "If you specify a certificate file you must also ");
			fprintf(stderr, "have a 'https' connection!\n");
			ne_sock_exit();
			err = 1;
			goto out;
		}
		/* If we have a certificate, it was already decrypted in parse_args */
	}

	mrccomm_init(vp->mrc_url, 1);
	get_local_creds(&c);

	if(vp->acPolicyId == 2) {   /* POSIX ACL based policy */
		acl_list_init(&acl_list);
		if (vp->acMode) {
			default_mode = mode_str_to_int(vp->acMode);
		} else {
			um = umask(0);
			umask(um);
			default_mode = (S_IRWXU | S_IRWXG | S_IRWXO) & (~um);
		}
		mode_to_acl_list(default_mode, &acl_list);
		/* And add a mask entry (still missing) */
		vp->acl = &acl_list;
	}
	
	req = MRC_Request__createVolume(&err,
				  vp->name, vp->osdSelectionPolicy,
				  vp->defStripingPolicy,
				  vp->acPolicyId,
				  vp->partitioningPolicyId,
				  vp->acl, &c, NULL, 1);
	if (!req) {
		fprintf(stderr, "Could not create MRC request!\n");
		goto out;
	}
	err = execute_req(mrccomm_wq, req);
	if (err) {
		if (err == 17 || err == -17) {
			fprintf(stderr, "Volume '%s' alread exists ", vp->name);
			fprintf(stderr, "on MRC '%s'.\n", vp->mrc_url);
		} else {
			fprintf(stderr, "Due to an unknown reason (%d) I cannot create ", err);
			fprintf(stderr, "volume '%s'. ", vp->name);
			fprintf(stderr, "Sorry.\n");
		}
	}

 out:
 	if(xtreemfs_client_cert != NULL)
 		ne_ssl_clicert_free(xtreemfs_client_cert);
 
#ifdef XTREEMOS_ENV
	amsclient_connect_close(xtreemos_ams_sock);
	pthread_mutex_destroy(&xtreemos_ams_mutex);
#endif
 
	ne_sock_exit();
	
	volume_params_delete(vp);

#ifdef ITAC
	VT_finalize();
#endif
	
	return err;
}
