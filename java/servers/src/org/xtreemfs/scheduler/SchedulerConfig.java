package org.xtreemfs.scheduler;

import java.io.IOException;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;

public class SchedulerConfig extends ServiceConfig{
	private final Parameter[] schedulerParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.HTTP_PORT,
            Parameter.USE_SSL,
            Parameter.DIRECTORY_SERVICE,
            Parameter.FAILOVER_MAX_RETRIES,
            Parameter.FAILOVER_WAIT,
            Parameter.LOCAL_CLOCK_RENEW,
            Parameter.REMOTE_TIME_SYNC,
            Parameter.SCHEDULER_SERVICE
	};
	
	public SchedulerConfig(String filename) throws IOException {
        super(filename);
        read();
	}
	
	public SchedulerConfig(Properties props) throws IOException {
		super(props);
		read();
	}
	
	public void read() {
		for(Parameter param : schedulerParameter) {
			parameter.put(param, readParameter(param));
		}
	}
	
    public void setDefaults() {
        super.setDefaults(schedulerParameter);
    }
    
    /**
     * Check if the configuration contain all necessary values to start the Service
     */
    public void checkConfig() {
        super.checkConfig(schedulerParameter);
    }
    
    public int getLocalClockRenew() {
        return (Integer) parameter.get(Parameter.LOCAL_CLOCK_RENEW);
    }

    public int getRemoteTimeSync() {
        return (Integer) parameter.get(Parameter.REMOTE_TIME_SYNC);
    }
}
