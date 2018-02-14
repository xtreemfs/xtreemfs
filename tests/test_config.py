# Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

TestSets = {
    'short' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'full' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'short-ssl' : {
                'ssl': True,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    # contains only multi-threaded benchmarks. set option "-t" accordingly.
    'ssd' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
     # Contains dd tests for testing the SSL support of packages.
    'packages-ssl' : {
                'ssl': True,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    # Used for testing new test scripts, therefore usually contains no tests.
    'testing' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    # This configuration is used for manual test environment set-ups (option -e).
    'manual' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': True
    },
    # This configuration is used for manual test environment set-ups with SSL enabled (option -f).
    'manual-ssl' : {
                'ssl': True,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': True
    },
    # This configuration is used to run the tests on the Travis CI build environment.
    'travis' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-common' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-dir' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-foundation' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-integration' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-mrc' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit-osd' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-junit' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-cpp' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-valgrind' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    },
    'travis-contrib' : {
                'ssl': False,
                'mrc_repl': False,
                'dir_repl': False,
                'snmp': False
    }
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
                }
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
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl', 'packages-ssl' ]
    },
    {
        'name': 'Erichs data integrity test',
        'file': '03_erichs_data_integrity_test.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'Find Grep Tar',
        'file': '05_findgreptar.sh',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'fsx',
        'file': 'fsx.sh',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'bonnie',
        'file': '10_bonnie.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        # NOTE(mberlin): 2013/04: Disabled because it takes too long and was not helpful in finding problems so far.
        'TestSets': [ ]
    },
    {
        'name': 'IOZone diagnostic',
        'file': '11_iozone_diagnostic.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'IOZone throughput',
        'file': '12_iozone_throughput.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'DBench',
        'file': '13_dbench.py',
        'VolumeConfigs': [ 'regular', 'directio', 'striped2', 'nomdcache' ],
        'TestSets': [ ]
    },
    {
        'name': 'make xtreemfs',
        'file': '15_makextreemfs.py',
        'VolumeConfigs': [ 'regular', 'nomdcache' ],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'IOZone multithread',
        'file': '16_iozone_multithread.py',
        'VolumeConfigs': [ 'regular' ],
        'TestSets': [ 'ssd' ]
    },
    {
        'name': 'bonnie multithread',
        'file': '17_bonnie_multithread.py',
        'VolumeConfigs': [ 'regular' ],
        'TestSets': [ 'ssd' ]
    },
    {
        'name': 'view renewal',
        'file': '18_view_renewal.py',
        'VolumeConfigs': [ 'regular_two_osds' ],
        'TestSets': [ 'full', 'short', 'short-ssl' ]
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
        #'TestSets': [ 'full', 'short', 'short-ssl' ]
        'TestSets': []
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
        'TestSets': [ 'full', 'short', 'short-ssl' ]
    },
    {
        'name': 'hadoop test',
        'file': 'hadoop_test.sh',
        'VolumeConfigs': ['regular'],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'hadoop2 test',
        'file': 'hadoop2_test.sh',
        'VolumeConfigs': ['regular'],
        'TestSets': [ 'full' ]
    },
    {
        'name': 'hadoop with ssl test',
        'file': 'hadoop_ssl_test.sh',
        'VolumeConfigs': ['regular_two_osds'],
        'TestSets': [ 'short-ssl' ]
    },
    {
        'name': 'xtfs_benchmark',
        'file': '14_xtfs_benchmark.sh',
        'VolumeConfigs': [ 'regular', 'regular_two_osds', 'nomdcache', 'directio', 'striped2', 'replicated_wqrq', 'replicated_wqrq_asyncwrites'],
        'TestSets': [ 'full' ]
    },
    # SYSTEM TESTS
    {
        'name': 'JUnit tests',
        'file': 'junit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl', 'travis-junit' ]
    },
    {
        'name': 'JUnit server tests common',
        'file': 'junit_tests_common.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-common' ]
    },
    {
        'name': 'JUnit server tests dir',
        'file': 'junit_tests_dir.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-dir' ]
    },
    {
        'name': 'JUnit server tests foundation',
        'file': 'junit_tests_foundation.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-foundation' ]
    },
    {
        'name': 'JUnit server tests integration',
        'file': 'junit_tests_integration.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-integration' ]
    },
    {
        'name': 'JUnit server tests mrc',
        'file': 'junit_tests_mrc.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-mrc' ]
    },
    {
        'name': 'JUnit server tests osd',
        'file': 'junit_tests_osd.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'travis-junit-osd' ]
    },
    {
        'name': 'C++ Unit Tests',
        'file': 'cpp_unit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'short', 'short-ssl', 'travis-cpp' ]
    },
    {
        'name': 'Valgrind memory-leak check for C++ Unit Tests',
        'file': 'cpp_unit_tests_valgrind.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'travis-valgrind' ]
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
    },
    {
        'name': 'hadoop adapter junit tests',
        'file': 'hadoop_junit_tests.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full', 'travis-contrib' ]
    },
    {
        'name': 'Coverity Scan Test',
        'file': 'coverity_scan_test.sh',
        'VolumeConfigs': [],
        'TestSets': [ 'full' ]
    }
 ]
