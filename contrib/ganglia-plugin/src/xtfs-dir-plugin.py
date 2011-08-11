'''
Created on May 25, 2011

@author: bzcseife

This is a python ganglia plugin which monitors the status of an DIR service of the XtreemFS 
filesystem. It is intend to run on the same host as the DIR and gathers information of the DIR per
SNMP. Therefore you have to configure your DIR to provide a SNMP Agent on this host.

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
       
        return int(varBinds[0][1]/1024/1024)
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
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 8, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0


#Get the number of pending requests
def PendingRequests(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 9, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0

#Get the number of address mappings registered
def AddressMappingCount(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 2, 1, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0

#Get the number of services  registered
def ServiceCount(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 2, 2, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return int(varBinds[0][1])
    else:
        return 0

#get the status of the DIR
#OID: 1.3.6.1.4.1.38350.1.11.0
def Status(name):
    errorIndication, errorStatus, errorIndex, varBinds = cmdgen.CommandGenerator().getCmd(authData,
                                                                                        transportTarget,
                                                                                        (1, 3, 6, 1, 4, 1, 38350, 1, 11, 0))
            
    if (errorStatus == False and errorIndication == None):       
        return str(varBinds[0][1])
    else:
        return "OFFLINE"
    
#get the UUID of the DIR
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
    transportTarget = cmdgen.UdpTransportTarget((Snmp_Host, Snmp_Port), 1, 0)
    
    d0 = {'name': 'dir_jvm_used_mem',
        'call_back': JvmUsedMem,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'both',
        'format': '%u',
        'description': 'The amount of memory the JVM uses currently.',
        'groups': 'dir'}
        
    d1 = {'name': 'dir_jvm_free_mem',
        'call_back': JvmFreeMem,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'Megabytes',
        'slope': 'both',
        'format': '%u',
        'description': 'The amount of free memory the JVM can still use.',
        'groups': 'dir'}
    
    d2 = {'name': 'dir_client_connections',
        'call_back': ClientConnections,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'clients',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of active client connection this DIR has currently to handle.',
        'groups': 'dir'}

    d3 = {'name': 'dir_pending_requests',
        'call_back': PendingRequests,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'pending requests',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of pending requests this DIR has enqueued.',
        'groups': 'dir'}    
 
    d4 = {'name': 'addr_mapping_count',
        'call_back': AddressMappingCount,
        'time_max': 90,
        #value_type: string | uint | float | double
        'value_type': 'uint',
        #units: unit of your metric
        'units': 'mappings',
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
        'description': 'The number of address mapping registered at the DIR.',
        #groups (optional): groups your metric belongs to
        'groups': 'dir'}
    
    d5 = {'name': 'service_count',
        'call_back': ServiceCount,
        'time_max': 90,
        'value_type': 'uint',
        'units': 'services',
        'slope': 'both',
        'format': '%u',
        'description': 'The number of services registered at the DIR.',
        'groups': 'dir'}   
    
    d6 = {'name': 'dir_status',
        'call_back': Status,
        'time_max': 90,
        'value_type': 'string',
        'units': '',
        'slope': 'zero',
        'format': '%s',
        'description': 'ONLINE if this DIR is running correctly, OFFLINE otherwise',
        'groups': 'dir'}
    
    d7 = {'name': 'dir_uuid',
        'call_back': Uuid,
        'time_max': 90,
        'value_type': 'string',
        'units': '',
        'slope': 'zero',
        'format': '%s',
        'description': 'UUID of the DIR running on this host',
        'groups': 'dir'}

    
    descriptors = [d0, d1, d2, d3, d4, d5, d6, d7]
    
    return descriptors
    
def metric_cleanup():
    '''Clean up the metric module.'''
    pass


#for debugging purpose     
if __name__ == '__main__':
    params = {'CommunityString': 'public', 'Host': 'localhost', 'Port': 9001}
    metric_init(params)
    for d in descriptors:
        v = d['call_back'](d['name'])
        print 'value for %s is' % (d['name'])
        print v
 


