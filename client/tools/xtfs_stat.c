/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */
#include <stdio.h>
#include <stdlib.h>

#include <sys/types.h>
#ifndef _WIN32
#include <sys/xattr.h>
#endif

#include <json.h>
#include <json_object.h>

size_t getxattr_wrapper(char *path, char *name, char* value, size_t length) {
#ifdef __APPLE__
	return getxattr(path,name,value,length,0,0);
#else
	return getxattr(path,name,value,length);
#endif
}

/*
 *
 */
int main(int argc, char** argv) {

    char *path = argv[1];
    char value[10*1024];
    struct json_object *xloc, *rlist, *replica, *policy, *osds;

    if (argc != 2) {
        printf("usage: %s <filename>\n\n",argv[0]);
        return 1;
    }

    //fetch xtreemfs.url
    ssize_t attrlen = getxattr_wrapper(path,"xtreemfs.url",&value,sizeof(value));
    if (attrlen < 0) {
        printf("ERROR: Cannot retrieve XtreemFS specific file information.\n");
        printf("       The file is probably not part of an XtreemFS volume.\n\n");
        return 1;
    }

    printf("%-18s %s\n","filename",path);

    value[attrlen] = 0;
    printf("%-18s %s\n","XtreemFS URI",(char*)&value);


    //fetch xtreemfs.fileID
    attrlen = getxattr_wrapper(path,"xtreemfs.file_id",&value,sizeof(value));
    value[attrlen] = 0;
    printf("%-18s %s\n","XtreemFS fileID",(char*)&value);

    //fetch xtreemfs.objectType
    attrlen = getxattr_wrapper(path,"xtreemfs.object_type",&value,sizeof(value));
    char *typeName = "";
    char objectType = value[0];
    if (value[0] == '1')
        typeName = "regular file";
    else if (value[0] == '2')
        typeName = "directory";
    else
        typeName = "symlink";

    printf("%-18s %s\n","object type",typeName);

    //fetch xtreemfs.fileID
    attrlen = getxattr_wrapper(path,"xtreemfs.owner",&value,sizeof(value));
    value[attrlen] = 0;
    printf("%-18s %s\n","owner",(char*)&value);

    //fetch xtreemfs.fileID
    attrlen = getxattr_wrapper(path,"xtreemfs.group",&value,sizeof(value));
    value[attrlen] = 0;
    printf("%-18s %s\n","group",(char*)&value);

    //fetch xtreemfs.fileID
    if (objectType == '1') {
        attrlen = getxattr_wrapper(path,"xtreemfs.read_only",&value,sizeof(value));
        if (attrlen > 0) {
            value[attrlen] = 0;
            printf("%-18s %s\n","read-only",(char*)&value);
        } else {
            printf("%-18s %s\n","read-only","no");
        }
    }

    if (objectType == '2') {
        attrlen = getxattr_wrapper(path,"xtreemfs.default_sp",&value,sizeof(value));
        if (attrlen > 0) {
            value[attrlen] = 0;
            printf("%-18s %s\n","default SP",(char*)&value);
        } else {
            printf("%-18s %s\n","default SP","none");
        }
    }


    attrlen = getxattr_wrapper(path,"xtreemfs.ac_policy_id",&value,sizeof(value));
    if (attrlen > 0) {
        value[attrlen] = 0;
        printf("%-25s %s\n","access control policy ID",(char*)&value);
    }

    attrlen = getxattr_wrapper(path,"xtreemfs.osdsel_policy_id",&value,sizeof(value));
    if (attrlen > 0) {
        value[attrlen] = 0;
        printf("%-25s %s\n","OSD selection policy ID",(char*)&value);
    }

    attrlen = getxattr_wrapper(path,"xtreemfs.osdsel_policy_args",&value,sizeof(value));
	if (attrlen > 0) {
		value[attrlen] = 0;
		printf("%-25s %s\n","OSD selection policy arguments",(char*)&value);
	}

    attrlen = getxattr_wrapper(path,"xtreemfs.free_space",&value,sizeof(value));
    if (attrlen > 0) {
        value[attrlen] = 0;
        long long free = strtoll((char*)&value,NULL,10);
        if (free > 1024*1024*1024) {
            free = free/(1024*1024*1024);
            printf("%-25s %lld GB\n","free usable disk space",free);
        } else if (free > 1024*1024) {
            free = free/(1024*1024);
            printf("%-25s %lld MB\n","free usable disk space",free);
        } else if (free > 1024) {
            free = free/1024;
            printf("%-25s %lld kB\n","free usable disk space",free);
        } else {
            printf("%-25s %lld bytes\n","free usable disk space",free);
        }
    }

    //print Locations list
    attrlen = getxattr_wrapper(path,"xtreemfs.locations",&value,sizeof(value));
    if (attrlen > 0) {
        value[attrlen] = 0;

        //printf("attrvalue = %s",&value);

        xloc = json_tokener_parse(value);
        if (is_error(xloc)) {
            printf("ERROR: cannot parse server response.\n\n");
            return 1;
        }

        printf("%-18s %lld\n","replica list ver.",json_object_get_int(json_object_array_get_idx(xloc,1)));
        rlist = json_object_array_get_idx(xloc,0);

        int numReplicas = json_object_array_length(rlist);
        int i,repl,numOsds;

        for (i = 0; i < numReplicas; i++) {
            replica = json_object_array_get_idx(rlist,i);
            policy = json_object_array_get_idx(replica,0);
            char *policyName = json_object_get_string(json_object_object_get(policy,"policy"));
            int   stripeSize = json_object_get_int(json_object_object_get(policy,"stripe-size"));
            int   width = json_object_get_int(json_object_object_get(policy,"width"));
            osds = json_object_array_get_idx(replica,1);
            numOsds = json_object_array_length(osds);
            printf("%-18s %d","replica",i+1);
            printf(" ( policy: %s, width: %d, stripe-size: %dkB )\n",policyName,width,stripeSize);
            for (repl = 0; repl < numOsds; repl++) {
                printf("      OSD %-3d      %s\n",(repl+1),json_object_get_string(json_object_array_get_idx(osds,repl)));
            }
        }
    }
    return (EXIT_SUCCESS);
}

