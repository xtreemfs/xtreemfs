'''
Created on May 25, 2011

@author: bzcseife

This is a python ganglia plugin which monitors the status of an OSD service of the XtreemFS 
filesystem. It is intend to run on the same host as the OSD and gathers information of the OSD per
SNMP. Therefore you have to configure your OSD to provide a SNMP Agent on this host.

'''
#TODO: If ganglia supports 64bit values uses 64bit integers instead of converting all 64 bit integers
#reported from the SNMP Agent to 32bit integers.


import random
from pysnmp.entity.rfc3413.oneliner import cmdgen
from pysnmp.entity.rfc3413.oneliner.cmdgen import UdpTransportTarget



    
descriptors = list()
Random_Max = 50
Constant_Value = 50



#Get the used memory of the JVM
def JvmUsedMem(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 1, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       



#Get the free memory of the JVM
def JvmFreeMem(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 2, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       


#Get the number of client connections
def ClientConnections(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 7, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       


#Get the number of pending requests
def PendingRequests(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 8, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       

#Get the number of objects received
def ObjectsReceived(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 1, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       

#Get the number of replicated objects received
def ReplObjectsReceived(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 2, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       

#Get the number of replicated objects transmitted
def ObjectsTransmitted(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 3, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       

#Get the number of replicated bytes received
def ReplBytesReceived(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 4, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       

#Get the number of bytes received
def BytesReceived(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 5, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       

#Get the number of bytes transmitted
def BytesTransmitted(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 6, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       

#Get the length of the preprocessing stage queue 
def PreprocQueueLength(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 7, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       

#Get the length of the storage stage queue 
def StorageQueueLength(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 8, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       

#Get the length of the deletion stage queue 
def DeletionQueueLength(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 9, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       


#Get the number of open files from the OSD per snmp
def OsdOpenFiles(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 10, 0))
       
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       



#Get the number of deleted files from the OSD per snmp
def OsdDeletedFiles(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 11, 0))
       
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0       


#Get the free space from the OSD per snmp
def OsdFreeSpace(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 4, 12, 0))
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1] / 1024 / 1024)
    else:
        return 0       

#get the status of the OSD
def Status(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 11, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return str(varBinds[0][1])
    else:
        return "OFFLINE"

#get the UUID of the OSD
#OID: 1.3.6.1.4.1.38350.1.13.0
def Uuid(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 13, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return str(varBinds[0][1])
    else:
        return "Service not available"
    
def metric_init(params):

    global descriptors        
    global Commmunity_String 
    global Snmp_Port
    global authData
    global transportTarget
    
        
    if 'ComummunityString' in params:
        Community_String = params['CommunityString']
    else:
        Community_String = 'public'
        
    if 'Port' in params:
        Snmp_Port = int(params['Port'])
    if 'Host' in params:
        Snmp_Host = params['Host']
    
    authData = cmdgen.CommunityData('xtreemfs-agent', 'public')
    transportTarget = cmdgen.UdpTransportTarget((Snmp_Host, Snmp_Port),1,0)
    
    d0 = {'name': 'osd_jvm_used_mem',
        'call_back': JvmUsedMem,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'both',
        'format': '%u',
        'description': 'The amount of memory the JVM uses currently.',
        'groups': 'osd'}
    
    d1 = {'name': 'osd_jvm_free_mem',
        'call_back': JvmFreeMem,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'both',
        'format': '%u',
        'description': 'The amount of free memory the JVM can still use.',
        'groups': 'osd'}    
    
    d2 = {'name': 'osd_client_connections',
        'call_back': ClientConnections,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'clients',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of active client connection this OSD has currently to handle.',
        'groups': 'osd'}

    d3 = {'name': 'osd_pending_requests',
        'call_back': PendingRequests,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'pending requests',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of pending requests this OSD has enqueued.',
        'groups': 'osd'}
    
    d4 = {'name': 'objects_received',
        'call_back': ObjectsReceived,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'objects',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of objects this OSD has received.',
        'groups': 'osd'}

    d5 = {'name': 'repl_objects_received',
        'call_back': ReplObjectsReceived,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'objects',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of replicated objects this OSD has received.',
        'groups': 'osd'}
        
    d6 = {'name': 'objects_transmitted',
        'call_back': ObjectsTransmitted,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'objects',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of objects this OSD has transmitted.',
        'groups': 'osd'}

    d7 = {'name': 'repl_bytes_received',
        'call_back': ReplBytesReceived,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of replicated bytes this OSD has received.',
        'groups': 'osd'}
    
    d8 = {'name': 'bytes_received',
        'call_back': BytesReceived,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of bytes this OSD has received.',
        'groups': 'osd'}

    d9 = {'name': 'bytes_transmitted',
        'call_back': BytesTransmitted,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of bytes this OSD has transmitted.',
        'groups': 'osd'}

    d10 = {'name': 'preproc_queue_length',
        'call_back': PreprocQueueLength,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'requests',
        'slope': 'both',
        'format': '%u',
        'description': 'The length of the preprocessing stage queue of this OSD.',
        'groups': 'osd'}

    d11 = {'name': 'storage_queue_length',
        'call_back': StorageQueueLength,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'requests',
        'slope': 'positive',
        'format': '%u',
        'description': 'The length of the storage stage queue of this OSD.',
        'groups': 'osd'}

    d12 = {'name': 'deletion_queue_length',
        'call_back': DeletionQueueLength,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'requests',
        'slope': 'both',
        'format': '%u',
        'description': 'The length of the deletion stage queue of this OSD.',
        'groups': 'osd'}

    d13 = {'name': 'storage_queue_length',
        'call_back': StorageQueueLength,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'requests',
        'slope': 'both',
        'format': '%u',
        'description': 'The length of the storage stage queue of this OSD.',
        'groups': 'osd'}

    d14 = {'name': 'open_files',
        'call_back': OsdOpenFiles,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'files',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of file this OSD has currently opened.',
        'groups': 'osd'}

    d15 = {'name': 'deleted_files',
        'call_back': OsdDeletedFiles,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'files',
        'slope': 'positive',
        'format': '%u',
        'description': 'The number of deleted files on this OSD',
        'groups': 'osd'}


    d16 = {'name': 'free_space',
        'call_back': OsdFreeSpace,
        'time_max': 90,
        #value_type: string | uint | float | double
        'value_type': 'uint',
        #units: unit of your metric
        'units': 'Megabytes',
        #slope: zero | positive | negative | both
            #This value maps to the data source types defined for RRDTool
            #If 'positive', RRD file generated will be of COUNTER type (calculating the rate of change)
            #If 'negative', ????
            #'both' will be of GAUGE type (no calculations are performed, graphing only the value reported)
            #If 'zero', the metric will appear in the "Time and String Metrics" or the "Constant Metrics" depending on the value_type of the m
        'slope': 'both',
        #format: format string of your metric
            #Must correspond to value_type otherwise value of your metric will be unpredictable (reference: http://docs.python.org/library/stdtypes.html#string-formatting)
        'format': '%u',
        #description: description of your metric
        'description': 'The free disc space on the partition this OSD stores the object files.',
        #groups (optional): groups your metric belongs to
        'groups': 'osd'}
    
    d17 = {'name': 'osd_status',
        'call_back': Status,
        'time_max': 90,
        'value_type': 'string',
        'units': '',
        'slope': 'zero',
        'format': '%s',
        'description': 'ONLINE if this OSD is running correctly, OFFLINE otherwise',
        'groups': 'osd'}
    
    d18 = {'name': 'osd_uuid',
        'call_back': Uuid,
        'time_max': 90,
        'value_type': 'string',
        'units': '',
        'slope': 'zero',
        'format': '%s',
        'description': 'UUID of the OSD running on this host',
        'groups': 'osd'}
    
    descriptors = [d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, d17, d18]
    
    return descriptors
    
def metric_cleanup():
    '''Clean up the metric module.'''
    pass


#for debugging purpose     
if __name__ == '__main__':
    params = {'CommunityString': 'public', 'Host': 'localhost', 'Port': 9003}
    metric_init(params)
    for d in descriptors:
        v = d['call_back'](d['name'])
        print 'value for %s is' % (d['name'])
        print v


