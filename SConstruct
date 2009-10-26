SConscript( "proj/lsfs.xtreemfs/lsfs.xtreemfs.SConscript" )
SConscript( "proj/mkfs.xtreemfs/mkfs.xtreemfs.SConscript" )
SConscript( "proj/mount.xtreemfs/mount.xtreemfs.SConscript" )
SConscript( "proj/rmfs.xtreemfs/rmfs.xtreemfs.SConscript" )
SConscript( "proj/xtfs_vivaldi/xtfs_vivaldi.SConscript" )

Import( "build_env", "build_conf" )

build_env.SharedLibrary( "lib/gridmap_flog", "src/policies/gridmap_flog.c" )

if build_conf.CheckCHeader( "xos_ams.h" ):
    build_env.SharedLibrary( "lib/xos_ams_flog", "src/policies/xos_ams_flog.c" )
   

