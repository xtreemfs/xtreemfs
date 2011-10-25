#!/bin/sh

desc="check errors which can occur when getting an ACL by acl_get_file()"

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
echo "1..21"

# create a basic directory, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 chown ${d0} 65533 65533
expect 0 setfacl ${d0} km 'u::rwx,g::rwx,o::rwx'
expect 0 create ${d0}/${n1} 0644
expect 0 chown ${d0}/${n1} 65533 65533
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# EACCES : search permission of parent directory
#       or wrong ACL type
# 8
expect 0 setfacl ${d0} m 'u::rw,g::rwx,o::rwx,m::rwx'
expect 'u::rw-,g::rwx,m::rwx,o::rwx' getfacl ${d0} access
expect EACCES -u 65533 -g 65533 getfacl ${d0}/${n1} access
expect 0 setfacl ${d0} m 'u::rwx,g::rwx,o::rwx,m::rwx'
expect 'u::rwx,g::rwx,m::rwx,o::rwx' getfacl ${d0} access
expect 'u::rw-,g::r--,o::r--' -u 65533 -g 65533 getfacl ${d0}/${n1} access
expect EACCES -u 65533 -g 65533 getfacl ${d0}/${n1} default
#
# EINVAL argument is not ACL_TYPE_ACCESS or ACL_TYPE_DEFAULT
#       not done, meaningless here
#
#
# ENAMETOOLONG path to file name is too long
# 15
expect 0 create ${d0}/${name255} 0644
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${name255} access
expect 0 unlink ${d0}/${name255}
expect ENAMETOOLONG getfacl ${d0}/${name256} access
#
# ENOENT file does not exist
#       or no file name
# 19
expect ENOENT getfacl ${d0}/none access
expect ENOENT getfacl '\0' access
#
# ENOMEM not enough memory
#       not done, meaningless here
#
# ENOTDIR path contains a non-directory segment
# 21
expect ENOTDIR getfacl ${d0}/${n1}/none access
#
# ENOTSUP file system does not support ACL
#       not done, meaningless here
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi

