SConscript( "proj/xtfs_lsvol/xtfs_lsvol.SConscript" )
SConscript( "proj/xtfs_mkvol/xtfs_mkvol.SConscript" )
SConscript( "proj/xtfs_mount/xtfs_mount.SConscript" )
SConscript( "proj/xtfs_rmvol/xtfs_rmvol.SConscript" )
SConscript( "proj/xtfs_vivaldi/xtfs_vivaldi.SConscript" )

Import( "build_env", "build_conf" )

build_env.SharedLibrary( "lib/gridmap_flog", "src/policies/gridmap_flog.c" )

if build_conf.CheckCHeader( "xos_ams.h" ):
    build_env.SharedLibrary( "lib/xos_ams_flog", "src/policies/xos_ams_flog.c" )
   

