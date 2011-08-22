# Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

TestSets = {
    'short' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
		'snmp': False,
              },
    'full' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
		'snmp': False,
              },
    'short-ssl' : {
                'ssl': True,
                'mrc_repl': False,
                'dir_repl': False,
		'snmp': False,
              },
    # This configuration is used for manual test environment
    # set-ups.
    'manual' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
		'snmp': True,
    }
}

VolumeConfigs = {
    'regular' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '-ouser_xattr' ]
                },
    'nomdcache' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '--metadata-cache-size=0', '-ouser_xattr' ]
                },
    'directio' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '-odirect_io' ]
                },
    'striped2' : {
                    'stripe_size': 128,
                    'stripe_width': 2,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '--max-read-tries=9999' ]
                },
    'replicated' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 3,
                    'ronly_factor': 0,
                    'mount_options': [ '--max-read-tries=50', '--max-write-tries=50', '--max-tries=50' ]
                },
}

Tests = [
    # VOLUME TESTS
    {
        'name': 'Simple Metadata',
        'file': '01_simple_metadata.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache' ],
        'TestSets': [ 'short', 'full', 'short-ssl' ]
    },
    {
        'name': 'Erichs dd write',
        'file': '02_erichs_ddwrite.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'Erichs data integrity test',
        'file': '03_erichs_data_integrity_test.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'Find Grep Tar',
        'file': '05_findgreptar.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated' ],
        'TestSets': [ 'full', 'short' ]
    },
    {
        'name': 'fsx',
        'file': 'fsx.sh',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'bonnie',
        'file': '10_bonnie.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'IOZone diagnostic',
        'file': '11_iozone_diagnostic.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'IOZone throughput',
        'file': '12_iozone_throughput.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'DBench',
        'file': '13_dbench.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'make xtreemfs',
        'file': '15_makextreemfs.py',
        'VolumeConfigs': [ 'regular', 'nomdcache' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'xtfs_cleanup test',
        'file': 'system_cleanup_test.sh',
        'VolumeConfigs': ['nomdcache'],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'xtfs_snap test',
        'file': 'system_snap_test.sh',
        'VolumeConfigs': ['regular'],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'xattr test',
        'file': 'test_xattrs.sh',
        'VolumeConfigs': [ 'regular', 'nomdcache' ],
        'TestSets': [ 'full', 'short' ]
    },
    {
        'name': 'xtfs_scrub test',
        'file': 'system_scrub_test.sh',
        'VolumeConfigs': ['nomdcache'],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    # SYSTEM TESTS
    {
        'name': 'mkfs-lsfs-rmfs.xtreemfs test',
        'file': 'system_mkfs_lsfs_rmfs_test.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'xtfs_mrcdbtool test',
        'file': 'system_mrcdbtool_test.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'xtfs_chstatus test',
        'file': 'system_chstatus_test.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    }
]
