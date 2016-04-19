# Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

TestSets = {
    # Used for testing new test scripts, therefore usually contains no tests.
    'testing' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False,
    },
    # This configuration is used for manual test environment set-ups (option -e).
    'manual' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': True,
    },
    # This configuration is used for manual test environment set-ups with SSL enabled (option -f).
    'manual-ssl' : {
                'ssl': True,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': True,
    },
    'single' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False,
    },
    'null' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False,
    },
    'junit' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False,
    },

}

VolumeConfigs = {
    'regular' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '-ouser_xattr', '-oallow_other' ],
                    'mkfs_options':  [ '--max-tries=10', '--chown-non-root' ]
                },
    'regular_two_osds' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'min_osds': 2,
                    'mount_options': [ '-ouser_xattr' ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'nomdcache' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '--metadata-cache-size=0', '-ouser_xattr', '-oallow_other' ],
                    'mkfs_options':  [ '--max-tries=10', '--chown-non-root' ]
                },
    'directio' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '-odirect_io' ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'striped2' : {
                    'stripe_size': 128,
                    'stripe_width': 2,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'replicated_wqrq' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 3,
                    'ronly_factor': 0,
                    'mount_options': [ ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'replicated_wqrq_5' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 5,
                    'ronly_factor': 0,
                    'mount_options': [ ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'replicated_wqrq_asyncwrites' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 3,
                    'ronly_factor': 0,
                    'mount_options': [ '--enable-async-writes' ],
                    'mkfs_options':  [ '--max-tries=10']
                },
    'replicated_war1' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_policy': 'all',
                    'rwr_factor': 2,
                    'ronly_factor': 0,
                    'mount_options': [ '--max-tries=240', '--max-read-tries=240', '--max-write-tries=240' ],
                    'mkfs_options':  [ '--max-tries=10']
                },
}

Tests = [

    # SYSTEM TESTS
    {
        'name': 'JUnit tests',
        'file': 'junit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl', 'testing', 'junit' ]
    },
    {
        'name': 'C++ Unit Tests',
        'file': 'cpp_unit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl', 'testing' ]
    },
    {
        'name': 'snapshot test',
        'file': 'system_snap_test.sh',
        'VolumeConfigs': ['regular'],
        'TestSets': [ 'full', 'short', 'short-ssl', 'single' ]
    },
    {
        'name': 'null test',
        'file': '00_null_test.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'null' ]
    },
 ]
