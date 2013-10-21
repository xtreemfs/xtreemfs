General Information
===================

Quote from http://buildroot.uclibc.org/:

"Buildroot is a set of Makefiles and patches that makes it easy to generate
 a complete embedded Linux system."

In this directory, you find information how to run XtreemFS
in an buildroot environment. The main purpose of this effort is to
be able to run and test XtreemFS in a L4 Linux virtual machine.

Please see http://l4linux.org/ for more information about L4 Linux.

Nonetheless, you could also use these buildroot packages to compile
XtreemFS for your favorite embedded device.

The following how-to describes how to:

a) compile XtreemFS client and server packages for the buildroot system
b) install the generated packages


Install xtreemfs packages into buildroot
========================================

> mkdir package/xtreemfs/
> cp Config.in xtreemfs.mk package/xtreemfs/

add xtreemfs/Config.in to Filesystem section in package/Config.in

 source "package/unionfs/Config.in"
 source "package/xfsprogs/Config.in"
+source "package/xtreemfs/Config.in"
 endmenu


Build Linux with xtreemfs
=========================

> make qemu_x86_defconfig
> make menuconfig

Toolchain ->
  Enable large file
  Enable IPv6
  Enable WCHAR support
  Enable C++ support

Target packages ->
  BusyBox ->
    Show packages that are also provided by busybox

  Interpreter languages and scripting ->
    jamvm

  Shell and utilities ->
    bash

  System tools ->
    coreutils
    util-linux ->
      libuuid
      install utilities

  Filesystem and flash utilities ->
    xtreemfs

Start VM
========

> qemu-system-i386 -kernel bzImage -hda rootfs.ext2 -boot c -m 512M -append "root=/dev/sda"

Start XtreemFS in VM
====================

> hostname localhost
> /etc/xos/xtreemfs/postinstall_setup.sh
> /etc/init.d/xtreemfs-dir start
> /etc/init.d/xtreemfs-mrc start
> /etc/init.d/xtreemfs-osd start
> mkfs.xtreemfs localhost/v1
