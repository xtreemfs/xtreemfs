#!/bin/sh

desc="Scenarios described by Andreas Grünbacher"

dir=`dirname $0`
. ${dir}/../misc.sh

d0=`namegen`
d1=`namegen`
n1=`namegen`
d2=`namegen`
n2=`namegen`

rm -rf ${d0}
#
# skip the test if ACLs are not implemented
#  
mkdir ${n1}
if setfacl -m 'm::5' ${n1} 2> /dev/null
then

rmdir ${n1}
echo "1..57"

# create the basic directory, make it owned by a plain user,
# clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0750
expect 0 setfacl ${d0} b
expect 0 chmod ${d0} 0750
expect 0 chown ${d0} 65533 65533
expect 0750 stat ${d0} mode
expect 'u::rwx,g::r-x,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Grant some access to another plain user, this implies setting the mask
# 8
expect 0 setfacl ${d0} m 'u::rwx,u:65531:rwx,g::r-x,m::rwx,o::---'
expect 'u::rwx,u:65531:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
expect 0770 stat ${d0} mode
#
# Revoke group rights, which implies setting the mask
# 12
expect 0 chmod ${d0} 0750
expect 0750 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,g::r-x,m::r-x,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Likewise grant group rights, which implies setting the mask
# 16
expect 0 chmod ${d0} 0770
expect 0770 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Add a default ACL to the directory
# 20
expect 0 setfacl ${d0} md 'u::rwx,g::r-x,m::r-x,o::---,g:65532:r-x'
expect 0770 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0} default
#
# Create a subdirectory (leaving system to apply appropriate permissions)
# 24
mkdir ${d0}/${d1}
expect 0750 stat ${d0}/${d1} mode
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0}/${d1} access
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0}/${d1} default
#
# Create a plain file (leaving system to apply appropriate permissions)
# 27
touch  ${d0}/${n1}
expect 0640 stat ${d0}/${n1} mode
expect 'u::rw-,g::r-x,g:65532:r-x,m::r--,o::---' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# Make the base directory owned by root and replay the scenario
# granting access to another user and group
# 30
expect 0 chown ${d0} 0 0
#expect 0 setfacl ${d0} x 'u:65533:,g:65533:'
expect 0 setfacl ${d0} k
expect 0 chmod ${d0} 0750
expect 0750 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,g::r-x,m::r-x,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Grant some access to another plain user, this implies setting the mask
# 36
expect 0 setfacl ${d0} m 'u::rwx,u:65531:rwx,g::r-x,m::rwx,o::---,u:65533:rwx'
expect 'u::rwx,u:65531:rwx,u:65533:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
expect 0770 stat ${d0} mode
#
# Revoke group rights, which implies setting the mask
# 40
expect 0 chmod ${d0} 0750
expect 0750 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,u:65533:rwx,g::r-x,m::r-x,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Likewise grant group rights, which implies setting the mask
# 44
expect 0 chmod ${d0} 0770
expect 0770 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,u:65533:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect void getfacl ${d0} default
#
# Add a default ACL to the directory
# 48
expect 0 setfacl ${d0} md 'u::rwx,g::r-x,m::r-x,o::---,g:65532:r-x'
expect 0770 stat ${d0} mode
expect 'u::rwx,u:65531:rwx,u:65533:rwx,g::r-x,m::rwx,o::---' getfacl ${d0} access
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0} default
#
# Create a subdirectory (leaving system to apply appropriate permissions)
# 51
mkdir ${d0}/${d2}
expect 0750 stat ${d0}/${d2} mode
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0}/${d2} access
expect 'u::rwx,g::r-x,g:65532:r-x,m::r-x,o::---' getfacl ${d0}/${d2} default
#
# Create a plain file (leaving system to apply appropriate permissions)
# 54
touch  ${d0}/${n2}
expect 0640 stat ${d0}/${n2} mode
expect 'u::rw-,g::r-x,g:65532:r-x,m::r--,o::---' getfacl ${d0}/${n2} access
expect EACCES getfacl ${d0}/${n2} default
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi
