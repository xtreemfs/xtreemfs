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
#include <stdio.h>
#include <unistd.h>
#include <getopt.h>
#ifndef _WIN32
#include <syslog.h>
#else
#include "platforms.h"
#endif
#include <string.h>
#include <pthread.h>

#include <openssl/evp.h>

#ifdef ITAC
#include <VT.h>
#include <xtreemfs_itac.h>
#endif

#include <xtreemfs.h>
#include <xtreemfs_conf.h>
#include <xtreemfs_fuse.h>
#include <xtreemfs_utils.h>

#include <dirservice.h>
#include <logger.h>
#ifndef _WIN32
#include <mcheck.h>
#endif


void
usage(char *progpath)
{
}

/**
 * Limit stack size per thread.
 *
 * This function has been introduced to come across possible errors
 * in memory handling of threads as soon as possible.
 */
extern pthread_attr_t *xpattr;
int limit_thread_stack(int kbytes)
{
	size_t size, newsize = kbytes * 1024;
	int err;

	xpattr = (pthread_attr_t *)malloc(sizeof(pthread_attr_t));
	pthread_attr_init(xpattr);
	err = pthread_attr_getstacksize(xpattr, &size);
	if (err)
		return err;
	trace_msg("Changing pthread stack size from %ld to %ld\n",
		  size, newsize);
	err = pthread_attr_setstacksize(xpattr, newsize);
	if (err)
		err_msg("setstacksize returned %d\n",err);
	return err;
}

/**
 * Entry point into XtreemFS client.
 */
int
main(int argc, char **argv)
{	
	char cert_pw[128];
	int err;
	char *mrc_id = NULL;
	char *mrc_name = NULL;

	char *vol_id = NULL;
	char *real_volume_url = NULL;
	int len;
	int mem_trace = 0;

#ifdef ITAC
 	VT_initialize(&argc, &argv);
 	dbg_msg("VT initialized.\n");

	VT_classdef("fuse",   &itac_fuse_class);
	VT_classdef("osd",    &itac_osd_class);
	VT_classdef("filerw", &itac_filerw_class);
	VT_classdef("fobj",   &itac_fobj_class);
	VT_classdef("sobj",   &itac_sobj_class);

	VT_classdef("spin",   &itac_spin_class);

	VT_funcdef("read",    itac_fuse_class,   &itac_fuse_read_hdl);
	VT_funcdef("write",   itac_fuse_class,   &itac_fuse_write_hdl);

	VT_funcdef("exec",    itac_osd_class,    &itac_osd_exec_hdl);
	VT_funcdef("put",     itac_osd_class,    &itac_osd_put_hdl);
	VT_funcdef("get",     itac_osd_class,    &itac_osd_get_hdl);

	VT_funcdef("handle",  itac_filerw_class, &itac_filerw_handle_hdl);
	VT_funcdef("handle",  itac_fobj_class,   &itac_fobj_handle_hdl);
	VT_funcdef("handle",  itac_sobj_class,   &itac_sobj_handle_hdl);
#endif

	/* Get configuration from command line options and a configuration
	   file, eventually. */
	if (xtreemfs_conf_get_ip(&conf, argc, argv)) {
		err = 1;
		dbg_msg("Cannot parse arguments.\n");
		goto out2;
	}
#ifndef _WIN32
	if (conf.mem_trace) {
		mem_trace = 1;
		mtrace();
	}
#endif

	if (conf.volume_url) {
		ne_sock_init();
		openlog("xtreemfs", LOG_PID, LOG_USER);
		logit.type = syslog_log;
		if (conf.logfile) {
			dbg_msg("Logging to file '%s'\n", conf.logfile);
			closelog();
			logit.logfile = fopen(conf.logfile, "w");
			logit.type = file_log;
		}

		err_msg("debugging level is %d\n", conf.debugging);
		if (conf.debugging) {
			logit.level = conf.debugging;
			if (logit.level < LOGLEV_ERROR)
				logit.level = LOGLEV_ERROR;
			fprintf(stderr, "Set debug level to %d\n",
				conf.debugging);
		}

		// increasing stack size in the hope to hit the problem sooner
		// when problem is found, reduce to 1MB or 512KB (EF)
		if (conf.stack_size
		    && limit_thread_stack(conf.stack_size))
			exit(1);

		/* Check for node certificate */
		if (conf.ssl_cert) {
			dbg_msg("SSL certificate file '%s'\n", conf.ssl_cert);
			xtreemfs_client_cert = ne_ssl_clicert_read(conf.ssl_cert);
			if (!xtreemfs_client_cert) {
				fprintf(stderr, "Cannot read certificate file '%s'\n",
					conf.ssl_cert);
				exit(1);
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
#if 0
			xtreemfs_ca_cert = ne_ssl_cert_read("/home/mhess/.xtreemfs/cacert.pem");
			if (!xtreemfs_ca_cert)
				dbg_msg("Cannot read CA cert...\n");
#endif
		}

		/* Get mrc id, either uuid or url */
		mrc_id = extract_hostid(conf.volume_url);
		if (mrc_id) {
			dbg_msg("MRC ID from command line is '%s'\n", mrc_id);
		}

		vol_id = extract_volumeid(conf.volume_url);
		if (vol_id) {
			dbg_msg("Volume ID from command line is '%s'\n", vol_id);
		}

		/* If a directory service is specified, then the volume url has the
		   format <mrc uuid>/<volume name>. */
		if (conf.dirservice) {
			dirservice = dirservice_new(conf.dirservice, xtreemfs_client_cert);
			if (dirservice) {
				if (dirservice_connect(dirservice)) {
					err_msg("Cannot connect to directory service.\n");
					exit(1);
				}
				mrc_name = dirservice_resolve_uuid(dirservice, mrc_id, MRC_DEFAULT_PORT);
				dirservice_disconnect(dirservice);
				if (mrc_name) {
					dbg_msg("MRC name from DS: '%s'\n", mrc_name);
				}
			} else {
				dbg_msg("Cannot create dirservice '%s'\n", conf.dirservice);
			}
		} else {
			mrc_name = strdup(mrc_id);
		}

		/* Regardless of directory service, we should now have a complete URL
		   for the MRC. We can then put together the real volume URL. */
		len = strlen(mrc_name) + 1 + strlen(vol_id) + 1;
		real_volume_url = (char *)malloc(sizeof(char) * len);
		if (!real_volume_url) {
			err = -ENOMEM;
			goto out;
		}
		snprintf(real_volume_url, len, "%s/%s", mrc_name, vol_id);

		dbg_msg("Real volume URL is: '%s'\n", real_volume_url);

		free(mrc_name);
		free(mrc_id);
		free(vol_id);

		ne_sock_exit();

		err = xtreemfs_init(real_volume_url);
		if (err) {
			switch(err) {
				case 2:
					fprintf(stderr, "No volume '%s' on MRC.\n",
						volume);
				break;
				case 3:
					fprintf(stderr, "Cannot connect to MRC.\n");
				break;
				case 4:
					fprintf(stderr, "No volume specified.\n");
				break;
				default:
					fprintf(stderr, "Cannot mount volume!\n");
				break;
			}
			goto out;
		}
	}
	dbg_msg("About to start fuse main.\n");
	if (conf.mem_trace)
		mtrace();

	/* This actually starts the file system and mounts it in the correct
	   place. */
	fuse_main(conf.fargs.argc, conf.fargs.argv, &xtreemfs_ops, NULL);

	/* Clean up the mess that we produced. */
	if (conf.mem_trace)
		muntrace();

	if (dirservice)
		dirservice_destroy(dirservice);

	dbg_msg("Fuse main finished.\n");

out:
	xtreemfs_exit();

out2:
	log_end(&logit);

	xtreemfs_conf_del_contents(&conf);

	if (!err) {
		free(conf.volume_url);
		free(conf.logfile);
	}

	free(real_volume_url);
#ifndef _WIN32
	if (mem_trace)
		muntrace();
#endif

#ifdef ITAC
	VT_finalize();
	dbg_msg("VT finished.\n");
#endif
	return err;
}
