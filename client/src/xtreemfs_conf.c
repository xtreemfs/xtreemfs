/* Copyright (c) 2007, 2008  Matthias Hess

   Author: Matthias Hess

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
   C Implementation: xtreemfs_conf

   Description:
 */

#include <stdlib.h>
#include <stdio.h>
#include <stddef.h>
#include <string.h>

#include <fuse.h>

#include "xtreemfs.h"
#include "xtreemfs_conf.h"
#include "logger.h"

#define iswhite(x) ((x) == ' ' || (x) == '\t')

#define XTREEMFS_OPT(t, p, v) { t, offsetof(struct xtreemfs_conf, p), v }

/* Options that are passed via FUSE arguments */

struct fuse_opt xtreemfs_opts[] = {
	XTREEMFS_OPT("uuid=%s",        uuid,        0),
	XTREEMFS_OPT("volume_url=%s",  volume_url,  0),
	XTREEMFS_OPT("basedir=%s",     basedir,     0),
	XTREEMFS_OPT("conf=%s",        conf_file,   0),
	XTREEMFS_OPT("dirservice=%s",  dirservice,  0),

	/* Debugging and logging options */
	XTREEMFS_OPT("logfile=%s",     logfile,     0),
	XTREEMFS_OPT("debug=%d",       debugging,   0),
	XTREEMFS_OPT("logging=%d",     tracing,     0),
	XTREEMFS_OPT("mem_trace=%d",   mem_trace,   0),

#ifdef XTREEMOS_ENV
	/* XtreemOS integration */
	XTREEMFS_OPT("xtreemos=%d",    xtreemos,    0),
#endif

	/* Monitoring */
	XTREEMFS_OPT("monitoring=%d",  monitoring,  0),
	XTREEMFS_OPT("request_ids=%d", req_ids,     0),
	
	/* Runtime adjustments */
	XTREEMFS_OPT("num_threads=%d", num_threads, 0),
	XTREEMFS_OPT("stack_size=%d",  stack_size,  0),
	XTREEMFS_OPT("caching=%d",     caching,     0),
	XTREEMFS_OPT("clustermode",    clustermode, 1),
	XTREEMFS_OPT("singlequeue",    singlequeue, 1),

	/* Security options */
	XTREEMFS_OPT("ssl_cert=%s",    ssl_cert,    0),
	FUSE_OPT_END
};

struct xtreemfs_conf *xtreemfs_conf_new()
{
	struct xtreemfs_conf *rv = NULL;

	rv = (struct xtreemfs_conf *)malloc(sizeof(struct xtreemfs_conf));
	if (rv && xtreemfs_conf_init(rv)) {
		xtreemfs_conf_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int xtreemfs_conf_init(struct xtreemfs_conf *xc)
{
	int err = 0;

	xc->uuid     = NULL;

	return err;
}

void xtreemfs_conf_del_contents(struct xtreemfs_conf *xc)
{
	fuse_opt_free_args(&xc->fargs);

	free(xc->uuid);
}

void xtreemfs_conf_destroy(struct xtreemfs_conf *xc)
{
	xtreemfs_conf_del_contents(xc);
	free(xc);
}

static inline char *remove_whitespace(char *lp)
{
	while (iswhite(*lp) && *lp != '\0')
		lp++;

	return lp;
}

/**
 * Match a key against a template from fuse opts.
 *
 * @param templ fuse opt template
 * @param key   key to match
 * @return 0 in case they do not match, 1 otherwise
 */
static int match_template(const char *templ, char *key)
{
	int rv = 0;
	char *endp = NULL;
	char *d = (char *)templ;
	int p=0;

	dbg_msg("Matching '%s' against '%s'\n",
		key, templ);
	endp = strchr(templ, '=');
	if (endp) {
		while(d != endp) {
			if (*d != key[p])
				break;
			p++;
			d++;
		}
		if (d == endp)
			rv = 1;
	} else {	/* No '=' in template */
		rv = !strcmp(templ, key);
	}

	dbg_msg("Returning %d\n", rv);
	return rv;
}

/**
 * Analye one configuration line.
 *
 * @return    <0 on error, line code otherwise
 *            1 comment
 *            2 section
 *            3 key/value pair
 *          255 something else
 *           negative values indicate error
 */
static int analyze_line(char *line_buf, char **section, char **key, char **value)
{
	int rv = 0;	/* Assume error */
	char *lp = line_buf;
	char *hp = NULL;
	int len;

	/* Remove any trailing white space */
	lp = remove_whitespace(lp);

	if (*lp == '#') {
		rv = 1;
		goto out;
	}

	if (*lp == '\0') { 	/* Empty line */
		rv = 4;
		goto out;
	}

	if (*lp == '[') {	/* Section start */
		lp++;
		lp = remove_whitespace(lp);
		hp = lp;
		len = 0;
		while (!iswhite(*hp) && *hp != ']' && *hp != '\0') {
			hp++;
			len++;
		}

		/* We got a problem if *hp == '\0' */
		if (*hp == '\0') {
			rv = -1;
			goto out;
		}

		/* Yep, we have section name. Let's see if it is correctly
		   closed */
		hp = remove_whitespace(hp);
		if (*hp == ']') {	/* Yipiee! All went well. */
			if (*section)
				free(*section);
			*section = (char *)malloc(len+1);
			memcpy((void *)(*section), lp, len);
			(*section)[len] = '\0';
			rv = 2;
			goto out;
		} else {
			rv = -1;
			goto out;
		}
	}

	/* The last we got here should be a key/value pair */
	len = 0;
	hp = lp;
	while (!iswhite(*hp) && *hp != '=' && *hp != '\0') {
		hp++;
		len++;
	}

	/* Remove any additional whitespace, just in case */
	hp = remove_whitespace(hp);

	/* If we have '\0' the line does not contain a value */
	if (*hp == '\0') {
		rv = -2;
		goto out;
	}

	/* We must now have a '=' Otherwise something is wrong */
	if (*hp != '=') {
		rv = -2;
		goto out;
	}

	if (*key)
		free(*key);

	*key = (char *)malloc(len+1);
	memcpy((void *)(*key), lp, len);
	(*key)[len] = '\0';

	/* Now concentrate on the value */
	lp = hp;
	lp++;
	lp = remove_whitespace(lp);

	hp = lp;
	len = 0;
	while (!iswhite(*hp) && *hp != '\0') {
		hp++;
		len++;
	}

	/* We can safely ignore any whitespace after the value. */
	if (*value)
		free(*value);

	if (len > 0) {
		*value = (char *)malloc(len+1);
		memcpy((void *)(*value), lp, len);
		(*value)[len] = '\0';
	} else {
		*value = NULL;
	}

	rv = 3;

out:
	return rv;
}

/**
 * Identify the type of a key according to FUSE opts.
 * @param key The key that needs to be identified. It has the
 *            generic form '<key name>[=<type id>]'
 * @return 0 unidentified type
 *         1 string
 *         2 integer
 */
int xtreemfs_conf_key_type(const char *key)
{
	int rv = 0;
	char *d = NULL;
	char *type_id = NULL;

	d = rindex(key, '=');
	if (!d)
		goto out;
	type_id = strstr(d, "%");
	if (!type_id)
		goto out;
	if (!strcmp(type_id, "%s"))
		rv = 1;
	else if(!strcmp(type_id, "%d"))
		rv = 2;

out:
	return rv;
}

/** Parse a key
 *
 * Because we do not want to define a second set of options
 * we use the one from FUSE to also define the possible
 * arguments in a configuration file. This function parses
 * a given key and compares it to the fuse option list above.
 * from the option list it can also determine the type and
 * position of the value within the 'xtreemfs_conf' structure.
 */
struct fuse_opt *xtreemfs_conf_identify_key(char *key)
{
	struct fuse_opt *rv = NULL;
	int i;
	int t;

	for (i=0; xtreemfs_opts[i].templ; i++) {
		if (match_template(xtreemfs_opts[i].templ, key))
			break;
	}

	if (!xtreemfs_opts[i].templ)
		goto out;

	rv = &xtreemfs_opts[i];
out:	
	return rv;
}

/**
 * Add a key/value pair to an existing configuration
 *
 * @param xc XtreemFS configuration to add to.
 */
int xtreemfs_conf_add_key_value(struct xtreemfs_conf *xc,
				char *key, char *value,
				int overwrite)
{
	int err = 0;
	int t;
	struct fuse_opt *opt;
	char *ptr;
	char **cptr;
	int *iptr;

	if (!(key && value)) {
		goto out;
	}

	opt = xtreemfs_conf_identify_key(key);
	if (!opt) {
		dbg_msg("Cannot identify key '%s'\n", key);
		goto out;
	}
	t = xtreemfs_conf_key_type(opt->templ);
	if (!t)
		goto out;
	ptr = (char *)xc + opt->offset;

	switch(t) {
		case 1:	/* String */
			cptr = (char **)ptr;
			if (!(*cptr) || overwrite) {
				free(*cptr);
				dbg_msg("Adding %s=%s\n", key, value);
				*cptr = strdup(value);
			}
		break;
		case 2:	/* Integer, we assume that '0' means 'not set'! */
			iptr = (int *)ptr;
			if (!(*iptr) || overwrite)
				*iptr = atoi(value);
		break;
	}

out:
	return err;
}


/**
 * Read configuration of file on a line per line basis
 *
 * @return 0 on success, error code otherwise
 */
int xtreemfs_conf_read_conf(struct xtreemfs_conf *xc, FILE *file)
{
	int err = 0;
	char line_buf[1024];
	int len;
	int lt;
	char *section = NULL, *key = NULL, *value = NULL;

	while (!feof(file)) {
		if (fgets(line_buf, 1024, file)) {
			/* See if we have eol */
			len = strlen(line_buf);
			if (line_buf[len-1] == '\n') 	/* Regular line */
				line_buf[len-1] = '\0';
			lt = analyze_line(line_buf, &section, &key, &value);
			if (lt > 0) {
				if (lt == 3) {	/* Key/value */
					xtreemfs_conf_add_key_value(xc, key, value, 0);
				}
			} else {
				err_msg("Error while parsing config line.\n");
				err = lt;
				goto out;
			}
		} else {

		}
	}

out:
	free(key);
	free(value);

	return err;
}

/**
 * Read a configuration into an already existing 'xtreemfs_conf'
 */ 
int xtreemfs_conf_read_ip(struct xtreemfs_conf *xc, char *filename)
{
	int err = 0;
	FILE *file = NULL;

	/* See if we can open the file at all */
	file = fopen(filename, "r");
	if (!file) {
		err = 1;
		goto out;
	}

	err = xtreemfs_conf_read_conf(xc, file);

out:
	if (file)
		fclose(file);

	return err;
}

/**
 * Read a configuration into a newly created 'xtreemfs_conf'
 */
struct xtreemfs_conf *xtreemfs_conf_read(char *filename)
{
	struct xtreemfs_conf *rv = NULL;
	int err = 0;

	rv = xtreemfs_conf_new();
	if (!rv) {
		err = 1;
		goto out;
	}

	err = xtreemfs_conf_read_ip(rv, filename);

out:
	if (err) {
		if (rv)
			xtreemfs_conf_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int xtreemfs_conf_write(struct xtreemfs_conf *xc, char *filename)
{
	int err = 0;
	FILE *file = NULL;

	file = fopen(filename, "w");
	if (!file) {
		err = 1;
		goto out;
	}

	/* TODO: Print out complete set of options. */

	if (xc->uuid)
		fprintf(file, "uuid     = %s\n", xc->uuid);
	if (xc->ssl_cert)
		fprintf(file, "certfile = %s\n", xc->ssl_cert);
	if (xc->logfile)
		fprintf(file, "logfile  = %s\n", xc->logfile);

out:
	if (file)
		fclose(file);

	return err;
}

/**
 * Get xtreemfs configuration.
 *
 * This function parses command line arguments (assuming its FUSE like)
 * and evaluates them. Eventually additional configuration options are
 * read from configuration files, if given or found in default places.
 */
int xtreemfs_conf_get_ip(struct xtreemfs_conf *xc, int argc, char **argv)
{
	int err = 0;
	struct fuse_args args = FUSE_ARGS_INIT(argc, argv);

	/* The proceedings might appear a bit tricky... we first go through the
	   command line arguments. Potentially, there can be an antry for another
	   configuration file. We would then read that configuration, too. */

	/* Derived from 'FUSE_ARGS_INIT' */
	xc->fargs.argc      = argc;
	xc->fargs.argv      = argv;
	xc->fargs.allocated = 0;

	if (fuse_opt_parse(&xc->fargs, xc, xtreemfs_opts, NULL) == -1) {
		err = 1;
		dbg_msg("Error while parsing FUSE options.\n");
		goto out;
	}

	/* At this point we have parsed fuse options. Let's check if we have
	   a configuration file to read... */

	if (xc->conf_file) {
		/* This call does not overwrite already set options */
		if (xtreemfs_conf_read_ip(xc, xc->conf_file)) {
			dbg_msg("Cannot read configuration file '%s'\n",
				xc->conf_file);
			err = 1;
		}
	}
	/* xtreemfs_conf_print(xc); */

out:
	return err;
}

int xtreemfs_conf_print(struct xtreemfs_conf *xc)
{
	int err = 0;

	if (xc->uuid)
		fprintf(stdout, "UUID:        %s\n", xc->uuid);
	if (xc->dirservice)
		fprintf(stdout, "Dirservice:  %s\n", xc->dirservice);
	if (xc->ssl_cert)
		fprintf(stdout, "Cert file: %s\n", xc->ssl_cert);

	return err;
}
