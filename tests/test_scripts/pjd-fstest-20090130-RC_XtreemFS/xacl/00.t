#!/bin/sh -x

desc="interactions between chmod and setfacl for basic rights"

# might want do add ACL applied to directories
# prefer execution with a plain user rights

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
echo "1..45"

# create a basic directory, clean its inherited ACLs, and check initial ACL
# 1
expect 0 mkdir ${d0} 0777
expect 0 setfacl ${d0} b
expect 0 create ${d0}/${n1} 0644
expect 'u::rw-,g::r--,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# do a few chmod and check access ACL
# 6
expect 0 chmod ${d0}/${n1} 0421
expect 'u::r--,g::-w-,o::--x' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0142
expect 'u::--x,g::r--,o::-w-' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0214
expect 'u::-w-,g::--x,o::r--' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0635
expect 'u::rw-,g::-wx,o::r-x' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0563
expect 'u::r-x,g::rw-,o::-wx' getfacl ${d0}/${n1} access
expect 0 chmod ${d0}/${n1} 0356
expect 'u::-wx,g::r-x,o::rw-' getfacl ${d0}/${n1} access
#
# do a few setfacl and check mode
# 18
expect 0 setfacl ${d0}/${n1} m 'u::r,g::w,o::x'
expect 0421 stat ${d0}/${n1} mode
expect 0 setfacl ${d0}/${n1} m 'u::w,g::x,o::r'
expect 0214 stat ${d0}/${n1} mode
expect 0 setfacl ${d0}/${n1} m 'u::x,g::r,o::w'
expect 0142 stat ${d0}/${n1} mode
expect 0 setfacl ${d0}/${n1} m 'u::rw,g::wx,o::rx'
expect 0635 stat ${d0}/${n1} mode
expect 0 setfacl ${d0}/${n1} m 'u::wx,g::rx,o::rw'
expect 0356 stat ${d0}/${n1} mode
expect 0 setfacl ${d0}/${n1} m 'u::rx,g::rw,o::wx'
expect 0563 stat ${d0}/${n1} mode
#
#  Create a file and make sure the initial mode is 0664 (with umask 002)
#  for a directory, the initial mode is 0776
# 30
umask 0002
touch ${d0}/${n2}
expect 0664 stat ${d0}/${n2} mode
expect 'u::rw-,g::rw-,o::r--' getfacl ${d0}/${n2} access
mkdir ${d0}/${d1}
expect 0775 stat ${d0}/${d1} mode
expect 'u::rwx,g::rwx,o::r-x' getfacl ${d0}/${d1} access
expect void getfacl ${d0}/${d1} default
#
#  The special mode bits should not interfere with the ACL settings
# 35
expect 0 chmod ${d0}/${n1} 07754
expect 07754 stat ${d0}/${n1} mode
expect 'u::rwx,g::r-x,o::r--' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
expect 0 setfacl ${d0}/${n1} m 'u::rx,g::rw,o::wx'
expect 07563 stat ${d0}/${n1} mode
expect 'u::r-x,g::rw-,o::-wx' getfacl ${d0}/${n1} access
expect EACCES getfacl ${d0}/${n1} default
#
# updating an ACL has no impact on ctime
#
ctime1=`${fstest} stat ${d0} ctime`
ctime2=`${fstest} stat ${d0}/${n2} ctime`
sleep 1
expect 0 setfacl ${d0}/${n2} m 'u::x,g::x,o::x'
ctime3=`${fstest} stat ${d0} ctime`
ctime4=`${fstest} stat ${d0}/${n2} ctime`
test_check $ctime1 -eq $ctime3
test_check $ctime2 -eq $ctime4

#
# Clean
#
rm -rf ${d0}

else quick_exit
fi
