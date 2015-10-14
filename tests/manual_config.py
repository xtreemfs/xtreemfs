# Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

TestSets = {
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
}

VolumeConfigs = {
    'regular' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'min_osds': 3,
                    'mount_options': [ '-ouser_xattr' ],
                    'mkfs_options':  [ '--max-tries=10' ]
                },
    'nomdcache' : {
                    'stripe_size': 128,
                    'stripe_width': 1,
                    'rwr_factor': 0,
                    'ronly_factor': 0,
                    'mount_options': [ '--metadata-cache-size=0', '-ouser_xattr' ],
                    'mkfs_options':  [ '--max-tries=10' ]
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

Tests = [ ]
