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
*  C Implementation: xtreemfs_utils
*
* Description: 
*
*
* Author: Matthias Hess <matthiash@acm.org>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <openssl/sha.h>

#include <ne_uri.h>

#include <dirservice.h>
#include <mrc.h>

#include "xtreemfs_utils.h"


enum error_causes {
	ERR_NO_ERROR=0,
	ERR_ONLY_POLICY,
	ERR_NO_STRIPE_WIDTH,
	ERR_STRIPE_SIZE_NOT_A_NUMBER,
	ERR_STRIPE_WIDTH_NOT_A_NUMBER,
	ERR_TOO_MANY_ARGUMENTS,
	ERR_NO_VOLUME_URL,
	ERR_NO_MEM,
	ERR_UNKNOWN_POLICY,
	ERR_UNSUPPORTED_SCHEME,
	ERR_PARSE_ERROR
};

char *supported_schemes[] = {
	"http",
	"https",
	NULL
};

/**
 * Extract the host id part of a volume specifier.
 *
 * This can be either a uuid, a pseudo uuid or an URL.
 * The host id and the volume name are seperated by '/'
 */
char *extract_hostid(char *path)
{
	char *rv = NULL;
	char *beg = path;
	char *loc = NULL;
	char *scheme = NULL;
	int len;
	char *d;

	/* Check if we have a scheme at the beginning */
	if ((scheme = strstr(path, "://"))) {
		beg = scheme + strlen("://");
	}

	loc = strrchr(beg, '/');
	if (!loc) {
		rv = strdup(path);
		goto out;
	}

	for(d=path, len=0; d != loc; d++, len++);

	rv = (char *)malloc(len+1);
	if (!rv)
		goto out;

	memcpy((void *)rv, (void *)path, len);
	rv[len] = '\0';
	
out:
	return rv;
}

char *extract_volumeid(char *path)
{
	char *rv = NULL;
	char *beg = path;
	char *loc = NULL;
	char *scheme = NULL;

	/* Check if we have a scheme at the beginning */
	if ((scheme = strstr(path, "://"))) {
		beg = scheme + strlen("://");
	}
	
	loc = strrchr(beg, '/');
	if (!loc)
		goto out;

	rv = strdup(loc+1);
out:
	return rv;
}

/**
 * Take a given string a try to construct a URI out of it.
 *
 * This function uses a directory service to resolve UUIDs eventually.
 */
int
str_to_uri_ds(char *str, ne_uri *uri, int def_port, struct dirservice *ds)
{
	int rv = 0;
	char *real_url = NULL;
	int real_url_len;
	char *real_scheme = NULL;
	char *host_id = NULL;
	char *host_url = NULL;
	char *vol_id = NULL;
	int i;

	memset((void *)uri, 0, sizeof(*uri));

	if(str == NULL || uri == NULL) {
		rv = -1;
		goto finish;
	}

	dbg_msg("Analysing string: %s\n", str);

	/* Before we start we separate the host_id part from the vol_id */
	host_id = extract_hostid(str);
	vol_id  = extract_volumeid(str);

	dbg_msg("Host id: '%s'\n", host_id);
	dbg_msg("Vol id: '%s'\n", vol_id);

	if (host_id == NULL) {
		rv = -2;
		goto finish;
	}

	/* If the string starts with one of the supported schemes, it cannot
	   be a UUID. */
		
	if(strstr(host_id, "://") == NULL) {
		/* No sign of scheme in string... */
		if (ds) {
			host_url = dirservice_get_hostaddress(ds, host_id);
			/* replace the initial host id with the newly found
			   host address */
			if (host_url) {
				free(host_id);
				host_id = host_url;
			}
		}
		if (!host_url) {	/* No resolving possible. */
			host_url =
				(char *)malloc(  strlen(host_id)
					       + strlen("http://")
					       + 1);
			host_url = strcpy(host_url, "http://");
			host_url = strcat(host_url, host_id);
			dbg_msg("New host id '%s'\n", host_url);
			free(host_id);
			host_id = host_url;
		}
#if 0
		real_url = (char *)malloc(strlen(str) + strlen("http://") + 1);
		real_url = strcpy(real_url, "http://");
		real_url = strcat(real_url, str);
#endif
	}

	dbg_msg("host id = '%s'\n", host_id);

	/* Now we can put together our real url. */
	real_url_len = strlen(host_id);
	if (vol_id) {
		real_url_len += strlen(vol_id);
		real_url_len++;
	}
	real_url_len++;	/* For concluding '\0' */

	real_url = (char *)malloc(real_url_len);
	real_url = strcpy(real_url, host_id);
	if (vol_id) {
		real_url = strcat(real_url, "/");
		real_url = strcat(real_url, vol_id);
	}

	dbg_msg("Real URL: '%s'\n", real_url);

	if(ne_uri_parse((const char *)real_url, uri) != 0) {
		err_msg("Cannot parse the given url '%s'\n",
			str);
		rv = ERR_PARSE_ERROR;
		goto finish;
	}
	
	/* Scheme supported? */
	for(i=0; supported_schemes[i] != NULL; i++) {
		if(!strcmp(uri->scheme, supported_schemes[i])) break;
	}
	if(supported_schemes[i] == NULL) {
		err_msg("Scheme '%s' is not supported.\n", uri->scheme);
		rv = ERR_UNSUPPORTED_SCHEME;
		goto finish;
	}
		
	/* Port given? */
	if(uri->port == 0)
		uri->port = def_port;
	
finish:
	free(host_id);
	free(vol_id);
	free(real_url);
	return rv;
}

int str_to_uri(char *str, ne_uri *uri, int def_port)
{
	return str_to_uri_ds(str, uri, def_port, NULL);
}


char *
extract_mrc(ne_uri *uri)
{
	char *rv = NULL;
	char *oldPath = NULL;
	
	/* Exploit a trick to get MRC url. */
	
	oldPath = uri->path;
	uri->path = strdup("/");
	rv = ne_uri_unparse(uri);
	free(uri->path);
	uri->path = oldPath;
	
	return rv;
}

char *
extract_volpath(ne_uri *uri)
{
	char *rv = NULL;
	
	if (!uri->path || !strcmp(uri->path, "/"))
		goto finish;
		
	if (uri->path[0] == '/') {
		rv = strdup(&uri->path[1]);
	} else {
		rv = strdup(uri->path);
	}

finish:
	return rv;	
}

char *
extract_volume(ne_uri *uri)
{
	char *rv = NULL;
	char *volpath_str;
	size_t len;
	
	volpath_str = extract_volpath(uri);
	if(volpath_str == NULL)
		goto finish;
	
	for(len=0; len<strlen(volpath_str) && volpath_str[len] != '/'; len++) ;

	rv = strndup(volpath_str, len);
	free(volpath_str);
	
finish:
	return rv;
}

char *
extract_path(ne_uri *uri)
{
	char *rv = NULL;
	char *volpath_str = NULL;
	size_t len, vplen;
	
	volpath_str = extract_volpath(uri);
	if(volpath_str == NULL)
		goto finish;
	vplen = strlen(volpath_str);
	
	/* Sort out volume part */
	for(len=0; len<vplen && volpath_str[len] != '/'; len++) ;
	
	if(vplen - len > 0) {
		rv = strndup(&volpath_str[len], vplen - len);
	}
	free(volpath_str);
		
finish:
	return rv;
}

char *extract_pseudo_uuid(char *name)
{
	char *rv = NULL;
	char *s = NULL;
	char *e = NULL;
	int len;
	int i;

	s = name;
	e = &name[strlen(name)-1];
	len = strlen(name);

	/* Check if we have a scheme in 'name' */
	for (i=0; supported_schemes[i] != NULL; i++) {
		if (strstr(name, supported_schemes[i]))
			break;
	}

	/* Eventually remove the scheme */
	if (supported_schemes[i]) {
		s = &name[strlen(supported_schemes[i]) + 3];
		len -= (strlen(supported_schemes[i]) + 3);
	}

	/* And check for a trailing '/' */
	if (name[strlen(name)-1] == '/')
		len--;

	rv = (char *)malloc(sizeof(char) * (len + 1));
	memcpy((void *)rv, (void *)s, len);
	rv[len] = '\0';

	return rv;
}

/**
 * Create a full XtreemFS name out of a path
 *
 * This functions simply appends the volume name to 'path'.
 */
char *
create_fullname(const char *volname, const char *path)
{
	char *rv = NULL;
	
	rv = (char *)malloc(sizeof(char) * (strlen(volname) + strlen(path) + 1));
	if(rv != NULL) {
		strcpy(rv, volname);
		rv = strcat(rv, path);
	}
	
	return rv;
}

/**
 * Create a URL out of <mrc uid>/<vol name>
 *
 * This function takes a uuid and a volume name and creates
 * a proper URL out of it. It uses the specified directory
 * service as a translation aid.
 */
char *create_vol_url(struct dirservice *ds, char *vol_path)
{
	char *rv = NULL;

	return rv;
}

/**
 * Find out the current base dir.
 *
 * This is used to determine for instance module directories
 * relative to this base dir.
 */
char *current_basedir()
{
	char *rv = NULL;
	pid_t pid;
	char exe_name[1024];
	char buf[1024] = "...uninitialized...";
	char *p = NULL;

	pid = getpid();
	snprintf(exe_name, 1024, "/proc/%d/exe", pid);
	if (readlink(exe_name, buf, 1024) == -1) {
		goto out;
	}
	if (p = strrchr(buf, '/')) {
		*p = '\0';
		rv = strdup(buf);
	}

out:	
	return rv;
}

/** Create or retrieve a UUID for this host
 */
char *
host_uuid()
{
	char *rv = NULL;
	char hostname[1024];
#if 0
	char buf[512];
	struct hostent hent, *hent_res=NULL;
	struct addrinfo *ainfo, ahint, *ai;
	int err;
#endif
	SHA_CTX ctx;
	unsigned char sha1sum[SHA_DIGEST_LENGTH];

	if (gethostname(hostname, 1024))
		goto out;
	
	SHA1_Init(&ctx);
	SHA1_Update(&ctx, (const void *)hostname, strlen(hostname));
	SHA1_Final(sha1sum, &ctx);

	rv = sha1_digest_to_str(sha1sum);

#if 0
	gethostent_r(&hent, buf, 512, &hent_res, &err);

	memset((void *)&ahint, 0, sizeof(ahint));
	ahint.ai_family = AF_UNSPEC;
	ahint.ai_socktype = SOCK_STREAM;

	if (getaddrinfo(hostname, NULL, &ahint, &ainfo)) {
		err_msg("Cannot get addrinfo.\n");
		goto out;
	}
	for (ai=ainfo;
	     ai != NULL && ai->ai_socktype != SOCK_STREAM;
	     ai = ai->ai_next);
	switch(ai->ai_family) {
		case AF_INET: {
			struct sockaddr_in *si =
				(struct sockaddr_in *)ai->ai_addr;
		}			
		break;
		case AF_INET6: {
			struct sockaddr_in6 *si =
				(struct sockaddr_in6 *)ai->ai_addr;
		}
		break;
		default:
		break;
	}
	freeaddrinfo(ainfo);
#endif

out:
	return rv;
}

/**
 * Convert SHA1 digest to hex string.
 */
char *sha1_digest_to_str(unsigned char *digest)
{
	char *rv = NULL;
	static char hex_array[] = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};
	int i, j, len;

	len = sizeof(char) * (2 * SHA_DIGEST_LENGTH + 3);
	rv = (char *)malloc(len);
	if (!rv)
		goto out;

	for(i=0, j=0; i<SHA_DIGEST_LENGTH; i++, j += 2) {
		rv[j]   = hex_array[(digest[i] & 0xf0) >> 4];
		rv[j+1] = hex_array[(digest[i] & 0x0f)];
	}
	rv[j] = '\0';
	
out:
	return rv;
}


/**
 * Print (ASCII) buffer
 */
int
print_neon_buffer(ne_buffer *buf)
{
	int err = 0;
	int len = buf->used;
	char *bytes = (char *)buf->data;
	char line_buf[1024];
	int lp, bp;

	bp = 0;
	lp = 0;
	while (bp < len) {
		if (bytes[bp] != '\n' && bytes[bp] != '\0' && lp < 1024) {
			line_buf[lp] = bytes[bp];
			lp++;
		} else {
			line_buf[lp] = '\0';
			dbg_msg("%s\n", line_buf);
			lp = 0;
		}
		bp++;
	}
	return err;
}
