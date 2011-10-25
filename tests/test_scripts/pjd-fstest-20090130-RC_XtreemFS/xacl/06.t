#!/bin/sh

desc="check errors which can occur when setting an ACL by acl_set_file() or acl_delete_def_file()"

dir=`dirname $0`
. ${dir}/../misc.sh

d0=`namegen`
n1=`namegen`

rm -rf ${d0}

#
# skip the test if ACLs are not implemented
#  
mkdir ${n1}
if setfacl -m 'm::5' ${n1} 2> /dev/null
then

rmdir ${n1}
echo "1..42"

# create a basic file, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 chown ${d0} 65533 65533
expect 0 setfacl ${d0} km 'u::rwx,g::rwx,o::rwx'
expect 0 create ${d0}/${n1} 0644
expect 0 chown ${d0}/${n1} 65533 65533
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# EACCES : no search permission on parent directory
#       or wrong ACL type
#  only for acl_set_file()
# 8
expect 0 setfacl ${d0} m 'u::rw,g::rwx,o::rwx,m::rwx'
expect 'u::rw-,g::rwx,m::rwx,o::rwx' getfacl ${d0} access
expect EACCES -u 65533 -g 65533 setfacl ${d0}/${n1} m 'u::rw-,g::r--,o::r--'
expect 0 setfacl ${d0} m 'u::rwx,g::rwx,o::rwx,m::rwx'
expect 'u::rwx,g::rwx,m::rwx,o::rwx' getfacl ${d0} access
expect 0 -u 65533 -g 65533 setfacl ${d0}/${n1} m 'u::rw-,g::r--,o::r--'
expect EACCES -u 65533 -g 65533 setfacl ${d0}/${n1} md 'u::rw-,g::r--,o::r--'
#
#
# EINVAL argument does not point to a valid ACL
#     or argument has too many entries
#              not done (no known limit)
#     or argument is not ACL_TYPE_ACCESS or ACL_TYPE_DEFAULT
#              not done, meaningless here
#     or setting a default acl to non-directory
#              not done : conflicting error code, EACCES found on ext3
#		this should also happen for acl_delete_def_file(), but
#		on ext3 it is accepted.
# 15
expect EINVAL setfacl ${d0}/${n1} m 'u::r,u::w,g::r--,o::r--'
expect 0 -u 65533 -g 65533 setfacl ${d0}/${n1} k
#
# ENAMETOOLONG path to file name is too long
#               not mentioned for acl_delete_def_file(), but it obviously
#		happens nevertheless
# 17
expect 0 create ${d0}/${name255} 0644
expect 0 setfacl ${d0}/${name255} m 'u::rw,g::rwx,o::-,u:65532:x,m::w'
expect 0620 stat ${d0}/${name255} mode
expect 'u::rw-,u:65532:--x,g::rwx,m::-w-,o::---' getfacl ${d0}/${name255} access
expect 0 unlink ${d0}/${name255}
expect ENAMETOOLONG setfacl ${d0}/${name256} m 'u::w,g::r--,o::r--'
#
expect 0 mkdir ${d0}/${name255} 0755
expect 0 setfacl ${d0}/${name255} md 'u::rw,g::rwx,o::-,u:65532:x,m::w'
expect 'u::rw-,u:65532:--x,g::rwx,m::-w-,o::---' getfacl ${d0}/${name255} default
expect 0 setfacl ${d0}/${name255} k
expect void getfacl ${d0}/${name255} default
expect 'u::rwx,g::r-x,o::r-x' getfacl ${d0}/${name255} access
expect 0 rmdir ${d0}/${name255}
expect ENAMETOOLONG setfacl ${d0}/${name256} k
#
# ENOENT file does not exist
#       or no file name
#               not mentioned for acl_delete_def_file(), but it obviously
#		happens nevertheless
# 31
expect ENOENT setfacl ${d0}/none m 'u::w,g::r--,o::r--'
expect ENOENT setfacl '\0' m 'u::rw-,g::r--,o::r--'
expect ENOENT setfacl ${d0}/none k
expect ENOENT setfacl '\0' k
#
# ENOSPC not enough space in directory
#       not done, no known limit
#
# ENOTDIR path contains a non-directory segment
#               not mentioned for acl_delete_def_file(), but it obviously
#		happens nevertheless
# 35
expect ENOTDIR setfacl ${d0}/${n1}/none m 'u::rw-,g::r--,o::r--'
expect ENOTDIR setfacl ${d0}/${n1}/none k
#
# ENOTSUP file system does not support ACL
#       not done, meaningless here
#
# EPERM process is not allowed to set the ACL
# 37
expect EPERM -u 65531 -g 65531 setfacl ${d0}/${n1} m 'u::rw-,g::r--,o::r--'
expect EPERM -u 65531 -g 65533 setfacl ${d0}/${n1} m 'u::rw-,g::r--,o::r--'
expect 0 -u 65533 -g 65533 setfacl ${d0}/${n1} m 'u::rw-,g::r--,o::r--'
#
expect EPERM -u 65531 -g 65531 setfacl ${d0} k
expect EPERM -u 65531 -g 65533 setfacl ${d0} k
expect 0 -u 65533 -g 65533 setfacl ${d0} k
#
# EROFS file system is read-only
#       not done, meaningless here
#
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi

#/home/linux/rpmbuild/ntfs/fstest/pjd-fstest/fstest -u 65533 getfacl ${d0} access

