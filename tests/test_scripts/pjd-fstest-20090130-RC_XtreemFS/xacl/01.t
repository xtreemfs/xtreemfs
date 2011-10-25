#!/bin/sh

desc="interactions between chmod and setfacl for advanced rights"

dir=`dirname $0`
. ${dir}/../misc.sh

d0=`namegen`
n1=`namegen`
d1=`namegen`
n2=`namegen`

rm -rf ${d0}
#
# skip the test if ACLs are not implemented
#  
mkdir ${n1}
if setfacl -m 'm::5' ${n1} 2> /dev/null
then

rmdir ${n1}
echo "1..32"

# create a basic file, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 setfacl ${d0} b
expect 0 create ${d0}/${n1} 0644
expect 0 chown ${d0}/${n1} 65533 65533
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
expect 65533,65533 stat ${d0}/${n1} uid,gid
#
# mask should be seen as group rights in a mode
# 8
expect 0 setfacl ${d0}/${n1} m 'u::rw,g::r,o::r,m::rx'
expect 0654 stat ${d0}/${n1} mode
expect 'u::rw-,g::r--,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0611
expect 0611 stat ${d0}/${n1} mode
expect 'u::rw-,g::r--,m::--x,o::--x' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# add rights for specific users and groups
# 15
expect 0 setfacl ${d0}/${n1} m 'u::-,g::-,o::-,m::rx,u:65531:rw,u:65532:wx,g:65531:x,g:65532:r'
expect 050 stat ${d0}/${n1} mode
expect 'u::---,u:65531:rw-,u:65532:-wx,g::---,g:65531:--x,g:65532:r--,m::r-x,o::---' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# The owner may have different permissions as a designated user,
# same for group
# 19
expect 0 setfacl ${d0}/${n1} m 'u::-,g::-,o::-,m::rx,u:65531:rw,u:65533:wx'
expect 'u::---,u:65531:rw-,u:65533:-wx,g::---,m::r-x,o::---' getfacl ${d0}/${n1} access
expect 0 setfacl ${d0}/${n1} m 'u::-,u:65531:rw-,u:65533:-wx,g::-,o::-,m::rx,g:65531:x,g:65533:r'
expect 'u::---,u:65531:rw-,u:65533:-wx,g::---,g:65531:--x,g:65533:r--,m::r-x,o::---' getfacl ${d0}/${n1} access
#
# Set a default ACL and check initial setting on a file and a directory
#   umask should be ignored, but with fuse, it is not
#
expect 0 setfacl ${d0} md 'u::rx,u:65531:wx,u:0:x,g::wx,g:65532:x,g:0:rx,m::rwx,o::rx'
expect 0 create ${d0}/${n2} 077
expect 'u::---,u:root:--x,u:65531:-wx,g::-wx,g:root:r-x,g:65532:--x,m::rwx,o::r-x' getfacl ${d0}/${n2} access
expect 0 mkdir ${d0}/${d1} 077
expect 'u::---,u:root:--x,u:65531:-wx,g::-wx,g:root:r-x,g:65532:--x,m::rwx,o::r-x' getfacl ${d0}/${d1} access
expect 'u::r-x,u:root:--x,u:65531:-wx,g::-wx,g:root:r-x,g:65532:--x,m::rwx,o::r-x' getfacl ${d0}/${d1} default
#
# Do the same with a different umask, it should be ignored
#       (it is not with fuse)
#
rm -rf ${d0}/${n2} ${d0}/${d1}
expect 0 -U 077 create ${d0}/${n2} 077
expect 'u::---,u:root:--x,u:65531:-wx,g::-wx,g:root:r-x,g:65532:--x,m::rwx,o::r-x' getfacl ${d0}/${n2} access
expect 0 -U 077 mkdir ${d0}/${d1} 077
expect 'u::---,u:root:--x,u:65531:-wx,g::-wx,g:root:r-x,g:65532:--x,m::rwx,o::r-x' getfacl ${d0}/${d1} access
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi
