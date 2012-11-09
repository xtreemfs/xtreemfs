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
    # Used for testing new test scripts, therefore usually contains no tests.
    'testing' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False,
              },
    # This configuration is used for manual test environment set-ups.
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
                    'mount_options': [ '-ouser_xattr', '-oallow_other' ],
                    'mkfs_options':  [ '--chown-non-root' ]
                },
    'regular_two_osds' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'min_osds': 2,
                    'mount_options': [ '-ouser_xattr' ]
                },
    'nomdcache' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '--metadata-cache-size=0', '-ouser_xattr', '-oallow_other' ],
                    'mkfs_options':  [ '--chown-non-root' ]
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
                    'mount_options': [ ]
                },
    'replicated_wqrq' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 3,
                    'ronly_factor': 0,
                    'mount_options': [ ]
                },
    'replicated_wqrq_asyncwrites' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 3,
                    'ronly_factor': 0,
                    'mount_options': [ '--enable-async-writes' ]
                },
    'replicated_war1' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_policy': 'all',
                    'rwr_factor': 2,
                    'ronly_factor': 0,
                    'mount_options': [ ]
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
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'Erichs data integrity test',
        'file': '03_erichs_data_integrity_test.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'Find Grep Tar',
        'file': '05_findgreptar.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short' ]
    },
    {
        'name': 'fsx',
        'file': 'fsx.sh',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'bonnie',
        'file': '10_bonnie.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'IOZone diagnostic',
        'file': '11_iozone_diagnostic.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [  ]
    },
    {
        'name': 'IOZone throughput',
        'file': '12_iozone_throughput.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_war1', 'replicated_wqrq_asyncwrites' ],
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
        'name': 'snapshot test',
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
    {
        'name': 'Add and delete replica manually (read-only replication)',
        'file': 'ronly_replication_add_delete_replica.sh',
        'VolumeConfigs': ['regular_two_osds'],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'POSIX Test Suite (root required)',
        'file': 'posix_test_suite.sh',
        'VolumeConfigs': ['regular', 'nomdcache'],
        'TestSets': [ 'short', 'short-ssl' ]
    },
    # SYSTEM TESTS
    {
        'name': 'JUnit tests',
        'file': 'junit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
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
