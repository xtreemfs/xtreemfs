# Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
# Licensed under the BSD License, see LICENSE file for details.

import types

MANUAL_TEST_SET_NAME = 'manual'

class TestConfig:
    def __init__(self, filename, testSetName, volume):
        self.__tests = list()
        self.__system_tests = list()
        self.__volumeConfigs = dict()
        self.__testSets = dict()
        self.__filename = filename
        self.__testSetName = testSetName
        self.__selectedVolume = volume
        self.__curren_test_set = dict()
        self.parseConfig()
        self.applyTestSet()

    def getTestSet(self):
        return self.__testSets[self.__testSetName]

    def getVolumeConfigs(self):
        return self.__volumeConfigs

    def getVolumeTests(self):
        return self.__tests

    def getSystemTests(self):
        return self.__system_tests

    def parseConfig(self):
        cfgVars = dict()
        execfile(self.__filename, cfgVars)
        if not cfgVars.has_key('TestSets'):
            raise Exception('TestSets is not defined!')
        if not isinstance(cfgVars['TestSets'], types.DictType):
            raise Exception('TestSets must be a dictionary!')
        for k,v in cfgVars['TestSets'].items():
            if not isinstance(v, types.DictType):
                raise Exception('Item '+str(k)+' in TestSet is not a dictionary!')
            if not v.has_key('ssl'):
                raise Exception('TestSet '+str(k)+' is missing a field: ssl')
            if not v.has_key('mrc_repl'):
                raise Exception('TestSet '+str(k)+' is missing a field: mrc_repl')
            if not v.has_key('dir_repl'):
                raise Exception('TestSet '+str(k)+' is missing a field: dir_repl')

        if not cfgVars.has_key('VolumeConfigs'):
            raise Exception('VolumeConfigs is not defined!')

        if not isinstance(cfgVars['VolumeConfigs'], types.DictType):
            raise Exception('VolumeConfigs must be a dictionary!')

        for k, v in cfgVars['VolumeConfigs'].items():
            if not isinstance(v, types.DictType):
                raise Exception('Item '+str(k)+' in VolumeConfig is not a dictionary!')
            if not v.has_key('stripe_size'):
                raise Exception('VolumeConfig '+str(k)+' is missing a field: stripe_size')
            if not v.has_key('stripe_width'):
                raise Exception('VolumeConfig '+str(k)+' is missing a field: stripe_width')
            if not v.has_key('mount_options'):
                raise Exception('VolumeConfig '+str(k)+' is missing a field: mount_options')
            if not v.has_key('rwr_factor'):
                raise Exception('VolumeConfig '+str(k)+' is missing a field: rwr_factor')
            if not v.has_key('ronly_factor'):
                raise Exception('VolumeConfig '+str(k)+' is missing a field: ronly_factor')
            v.setdefault("stripe_parity_width", 0)

        if self.__selectedVolume is not None:
            if self.__selectedVolume not in cfgVars['VolumeConfigs']:
                raise Exception('There exists no volume config for the selected volume: ' + self.__selectedVolume)

        if not cfgVars.has_key('Tests'):
            raise Exception('Tests is not defined!')

        if not isinstance(cfgVars['Tests'], types.ListType):
            raise Exception('Tests must be a list!')

        for test in cfgVars['Tests']:
            if not isinstance(test, types.DictType):
                raise Exception('Item '+str(test)+' in Tests is not a dictionary!')
            if not test.has_key('file'):
                raise Exception('Test '+str(test)+' is missing a field: file')
            if not test.has_key('VolumeConfigs'):
                raise Exception('Test '+str(test)+' is missing a field: VolumeConfigs')
            if not test.has_key('TestSets'):
                raise Exception('Test '+str(test)+' is missing a field: TestSets')

        self.__testSets = cfgVars['TestSets']
        self.__tests = cfgVars['Tests']
        if self.__selectedVolume is not None:
          self.__volumeConfigs[self.__selectedVolume] = cfgVars['VolumeConfigs'][self.__selectedVolume]
        else:
          self.__volumeConfigs = cfgVars['VolumeConfigs']

    def getTestSetConfig(self):
        return self.__curren_test_set

    # Removes tests and volume configs which aren't necessary for the selected
    # test set.
    def applyTestSet(self):
        activeVolumeConfigs = dict()
        activeTests = list()

        try:
            self.__curren_test_set = self.__testSets[self.__testSetName]
        except KeyError:
            raise Exception('Invalid TestSet: "' + self.__testSetName + '"')

        if self.__testSetName.startswith(MANUAL_TEST_SET_NAME):
            #skip this for manual set-ups.
            return

        for test in self.__tests:
            validTestSets = test['TestSets']
            if self.__testSetName in validTestSets:
                if not test['VolumeConfigs']:
                    self.__system_tests.append(test)
                else:
                    anyVolumeConfigFound = False
                    for volConf in list(test['VolumeConfigs']):
                        if volConf in self.__volumeConfigs:
                            activeVolumeConfigs[volConf] = self.__volumeConfigs[volConf]
                            anyVolumeConfigFound = True
                        else:
                            if self.__selectedVolume is None:
                                raise Exception("Unknown VolumeConfig '+str(volConf)+' in test '+test['name']")
                            else:
                                test['VolumeConfigs'].remove(volConf)
                    if anyVolumeConfigFound:
                        activeTests.append(test)

        self.__tests = activeTests
        self.__volumeConfigs = activeVolumeConfigs

    def printConfig(self):
        if not self.__testSetName.startswith(MANUAL_TEST_SET_NAME):
            print 'Active System Tests:'
            for test in self.__system_tests:
                print '  "'+test['name']+'"'

            print 'Active Volume Tests:'
            for test in self.__tests:
                print '  "'+test['name']+'" running on:'
                for volconf in test['VolumeConfigs']:
                    print '    "'+volconf+'"'

            print ''
        
        print 'Active VolumeConfig:'
        for k,v in self.__volumeConfigs.items():
            print '  "'+k+'"'


if __name__ == "__main__":
    config = TestConfig('test_config.py','short')
    config.printConfig()
