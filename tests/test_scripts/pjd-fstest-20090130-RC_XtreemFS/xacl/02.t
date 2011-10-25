#!/bin/sh

desc="interactions between chown and setfacl for advanced rights"

# might want do add ACL applied to directories

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
echo "1..80"

# create a basic directory, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 setfacl ${d0} b
expect 0 create ${d0}/${n1} 0644
expect 0 chown ${d0}/${n1} 65533 65533
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# Grant some access to another user and group, and change ownership to them.
# The mode and the ACL are unchanged, but the new owner gets an entry as
# owner and another entry as a designated user, with different rights
# 6
expect 0 setfacl ${d0}/${n1} m 'u::r,g::r,o::r,m::rx,u:65531:rx,g:65532:rw'
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65531 65533
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65531 65532
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 65531,65532 stat ${d0}/${n1} uid,gid
#
# Change back to original owner and group
# 17
expect 0 chown ${d0}/${n1} 65533 65532
expect 0454 stat ${d0}/${n1} mode
expect 65533,65532 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65533 65533
expect 0454 stat ${d0}/${n1} mode
expect 65533,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
#
# Change ownership to root and back
# 26
expect 0 chown ${d0}/${n1} 0 65533
expect 0454 stat ${d0}/${n1} mode
expect 0,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access

expect 0 chown ${d0}/${n1} 65533 0
expect 0454 stat ${d0}/${n1} mode
expect 65533,0 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 0 0
expect 0454 stat ${d0}/${n1} mode
expect 0,0 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65533 65533
expect 0454 stat ${d0}/${n1} mode
expect 65533,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:65531:r-x,g::r--,g:65532:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access


#
#  Replay the same scenario, with root as the alternate user
#
#
# Grant some access to another user and group, and change ownership to them.
# The mode and the ACL are unchanged, but the new owner gets an entry as
# owner and another entry as a designated user, with different rights
# 41
expect 0 setfacl ${d0}/${n1} k
expect 0 setfacl ${d0}/${n1} m 'u::r,g::r,o::r,m::rx,u:0:rx,g:0:rw'
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 0 65533
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 0 0
expect 0454 stat ${d0}/${n1} mode
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0,0 stat ${d0}/${n1} uid,gid
#
# Change back to original owner and group
# 52
expect 0 chown ${d0}/${n1} 65533 65532
expect 0454 stat ${d0}/${n1} mode
expect 65533,65532 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65533 65533
expect 0454 stat ${d0}/${n1} mode
expect 65533,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
#
# Change ownership to root and back
# 60
expect 0 chown ${d0}/${n1} 0 65533
expect 0454 stat ${d0}/${n1} mode
expect 0,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access

expect 0 chown ${d0}/${n1} 65533 0
expect 0454 stat ${d0}/${n1} mode
expect 65533,0 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 0 0
expect 0454 stat ${d0}/${n1} mode
expect 0,0 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
expect 0 chown ${d0}/${n1} 65533 65533
expect 0454 stat ${d0}/${n1} mode
expect 65533,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,g::r--,g:root:rw-,m::r-x,o::r--' getfacl ${d0}/${n1} access
#
# A designated user with full access cannot change ownership
# 76
expect 0 setfacl ${d0}/${n1} m 'u::r--,u:root:r-x,u:65531:rwx,g::r--,g:root:rw-,g:65532:rwx,m::rwx,o::r--'
expect EPERM -u 65531 -g 65532 chown ${d0}/${n1} 65531 65532
expect 0474 stat ${d0}/${n1} mode
expect 65533,65533 stat ${d0}/${n1} uid,gid
expect 'u::r--,u:root:r-x,u:65531:rwx,g::r--,g:root:rw-,g:65532:rwx,m::rwx,o::r--' getfacl ${d0}/${n1} access
#
# Clean
#
rm -rf ${d0}

else quick_exit
fi
