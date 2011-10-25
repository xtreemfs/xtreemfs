#!/bin/sh

desc="check rights granted to designated users and groups are effective"

dir=`dirname $0`
. ${dir}/../misc.sh

d0=`namegen`
d1=`namegen`
n1=`namegen`
n2=`namegen`

rm -rf ${d0}

#
# skip the test if ACLs are not implemented
#  
mkdir ${n1}
if setfacl -m 'm::5' ${n1} 2> /dev/null
then

rmdir ${n1}
echo "1..53"

# create a basic directory, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 setfacl ${d0} b
expect 0 create ${d0}/${n1} 0644
expect 0 chown ${d0}/${n1} 65533 65533
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# Grant read or write access to another user or group, check access
# 6
expect 0 setfacl ${d0}/${n1} m 'u::-,g::-,o::-,m::rw,u:65531:r,g:65532:w'
expect 0 -u 65531 -g 65531 open ${d0}/${n1} O_RDONLY
expect EACCES -u 65531 -g 65531 open ${d0}/${n1} O_RDWR
expect 0 -u 65532 -g 65532 open ${d0}/${n1} O_WRONLY
expect EACCES -u 65532 -g 65532 open ${d0}/${n1} O_RDWR
# make sure the mask applies
expect 0 setfacl ${d0}/${n1} m 'u::-,g::-,o::-,m::-,u:65531:r,g:65532:w'
expect EACCES -u 65531 -g 65531 open ${d0}/${n1} O_RDONLY
expect EACCES -u 65532 -g 65532 open ${d0}/${n1} O_WRONLY
expect 0 setfacl ${d0}/${n1} m 'u::-,g::-,o::-,m::rw,u:65531:w,g:65532:r'
expect 0 -u 65531 -g 65531 open ${d0}/${n1} O_WRONLY
expect EACCES -u 65531 -g 65531 open ${d0}/${n1} O_RDONLY
expect 0 -u 65532 -g 65532 open ${d0}/${n1} O_RDONLY
expect EACCES -u 65532 -g 65532 open ${d0}/${n1} O_WRONLY
#
# create a directory, clean its inherited ACLs, and check initial ACL
# 20
expect 0 mkdir ${d0}/${d1} 0777
expect 0 chown ${d0}/${d1} 65533 65533
expect 'u::rwx,g::rwx,o::rwx' getfacl ${d0}/${d1} access
expect void getfacl ${d0}/${d1} default
#
# Grant read or write access to another user or group, check access
# then add execute access
# 24
expect 0 setfacl ${d0}/${d1} m 'u::-,g::-,o::-,m::rw,u:65531:r,g:65532:w'
expect 0 -u 65531 -g 65531 open ${d0}/${d1} O_RDONLY
expect EACCES -u 65531 -g 65531 create ${d0}/${d1}/${n1} 0644
expect EACCES -u 65532 -g 65532 create ${d0}/${d1}/${n1} 0644
expect 0 setfacl ${d0}/${d1} m 'u::-,g::-,o::-,m::rwx,u:65531:rx,g:65532:wx'
expect 0 -u 65531 -g 65531 open ${d0}/${d1} O_RDONLY
expect EACCES -u 65531 -g 65531 create ${d0}/${d1}/${n1} 0644
expect 0 -u 65532 -g 65532 create ${d0}/${d1}/${n1} 0644
expect EACCES -u 65532 -g 65532 open ${d0}/${d1} O_RDONLY
# make sure the execute mask applies
# 33
expect 0 setfacl ${d0}/${d1} m 'u::-,g::-,o::-,m::rw,u:65531:rx,g:65532:wx'
expect EACCES -u 65532 -g 65532 unlink ${d0}/${d1}/${n1}
# 35
expect 0 setfacl ${d0}/${d1} m 'u::-,g::-,o::-,m::rw,u:65531:w,g:65532:r'
expect EACCES -u 65531 -g 65531 create ${d0}/${d1}/${n2} 0644
expect EACCES -u 65531 -g 65531 open ${d0}/${d1} O_RDONLY
expect 0 -u 65532 -g 65532 open ${d0}/${d1} O_RDONLY
expect EACCES -u 65532 -g 65532 create ${d0}/${d1}/${n2} 0644
# 40
expect 0 setfacl ${d0}/${d1} m 'u::-,g::-,o::-,m::rwx,u:65531:wx,g:65532:rx'
expect 0 -u 65531 -g 65531 create ${d0}/${d1}/${n2} 0644
expect EACCES -u 65531 -g 65531 open ${d0}/${d1} O_RDONLY
expect 0 -u 65532 -g 65532 open ${d0}/${d1} O_RDONLY
expect EACCES -u 65532 -g 65532 unlink ${d0}/${d1}/${n2}
#
# check rights granted to owner as a designated user are not effective
# 45
expect 0 setfacl ${d0}/${d1} m 'u::---,u:65531:-wx,g::---,g:65532:r-x,m::rwx,o::---,u:65533:rwx,g:65533:rwx'
expect EACCES -u 65533 -g 65533 stat ${d0}/${d1}/${n2} mode
#
# check rights denied to root as a designated user are still effective
# 47
expect 0 setfacl ${d0}/${n1} m 'u::---,u:0:-,u:65531:-wx,u:65533:rwx,g::---,g:0:-,g:65532:r-x,g:65533:rwx,m::rwx,o::---'
expect 0 open ${d0}/${n1} O_RDONLY
#
# when a couple of rights is needed they must be granted through the same ACE
# (user belonging to two groups and not owner or a designated user)
# 49
expect 0 setfacl ${d0}/${n1} m 'u::---,u:0:-,u:65531:wx,u:65533:rwx,g::---,g:0:-,g:65531:w,g:65532:r-x,g:65533:rwx,m::rwx,o::---'
expect EACCES -u 65532 -g 65531,65532 open ${d0}/${n1} O_RDWR
#
# Check only the owner (apart from root) can modify an ACL
# 51
expect 0 setfacl ${d0}/${n1} m 'u::---,u:0:-,u:65531:rwx,u:65533:rwx,g::---,g:0:-,g:65531:w,g:65532:r-x,g:65533:rwx,m::rwx,o::---'
expect EPERM -u 65531 setfacl ${d0}/${n1} m 'u::rw,g::r,o::r,m::rx'
expect 0 -u 65533 setfacl ${d0}/${n1} m 'u::rw,g::r,o::r,m::rx'
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi
