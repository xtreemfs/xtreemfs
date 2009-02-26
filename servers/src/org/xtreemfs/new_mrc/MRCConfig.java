/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.new_mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.uuids.ServiceUUID;

/**
 * 
 * @author bjko
 */
public class MRCConfig extends ServiceConfig {
    
    private InetSocketAddress directoryService;
    
    private ServiceUUID       uuid;
    
    private int               localClockRenew;
    
    private int               remoteTimeSync;
    
    private String            dbDir;
    
    private int               osdCheckInterval;
    
    private String            appendLogFileName;
    
    private boolean           noatime;
    
    private boolean           noFsync;
    
    private int               dbCheckpointInterval;
    
    private int               idleIntervalForDBCheckpoint;
    
    private int               logFileSizeForDBCheckpoint;
    
    private String            policyDir;
    
    private String            authenticationProvider;
    
    private String            capabilitySecret;
    
    /** Creates a new instance of MRCConfig */
    public MRCConfig(String filename) throws IOException {
        super(filename);
        read();
    }
    
    public MRCConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public void read() throws IOException {
        super.read();
        
        this.osdCheckInterval = this.readRequiredInt("osd_check_interval");
        
        this.directoryService = this.readRequiredInetAddr("dir_service.host", "dir_service.port");
        
        this.appendLogFileName = this.readRequiredString("database.log");
        
        this.dbDir = this.readRequiredString("database.dir");
        
        this.noatime = this.readRequiredBoolean("no_atime");
        
        this.localClockRenew = this.readRequiredInt("local_clock_renewal");
        
        this.remoteTimeSync = this.readRequiredInt("remote_time_sync");
        
        this.noFsync = this.readOptionalBoolean("no_fsync", false);
        
        this.dbCheckpointInterval = this.readRequiredInt("database.checkpoint.interval");
        
        this.idleIntervalForDBCheckpoint = this
                .readRequiredInt("database.checkpoint.idle_interval");
        
        this.logFileSizeForDBCheckpoint = this.readRequiredInt("database.checkpoint.logfile_size");
        
        this.uuid = new ServiceUUID(this.readRequiredString("uuid"));
        
        this.policyDir = this.readOptionalString("policy_dir", null);
        
        this.authenticationProvider = readRequiredString("authentication_provider");
        
        this.capabilitySecret = readRequiredString("capability_secret");
    }
    
    public int getOsdCheckInterval() {
        return osdCheckInterval;
    }
    
    public InetSocketAddress getDirectoryService() {
        return directoryService;
    }
    
    public String getAppendLogFileName() {
        return appendLogFileName;
    }
    
    public String getDbDir() {
        return dbDir;
    }
    
    public boolean isNoAtime() {
        return noatime;
    }
    
    public int getLocalClockRenew() {
        return localClockRenew;
    }
    
    public int getRemoteTimeSync() {
        return remoteTimeSync;
    }
    
    public boolean isNoFsync() {
        return noFsync;
    }
    
    public int getDBCheckpointInterval() {
        return dbCheckpointInterval;
    }
    
    public int getIdleIntervalForDBCheckpoint() {
        return idleIntervalForDBCheckpoint;
    }
    
    public int getLogFileSizeForDBCheckpoint() {
        return logFileSizeForDBCheckpoint;
    }
    
    public ServiceUUID getUUID() {
        return uuid;
    }
    
    public String getPolicyDir() {
        return policyDir;
    }
    
    public String getAuthenticationProvider() {
        return authenticationProvider;
    }
    
    public String getCapabilitySecret() {
        return capabilitySecret;
    }
    
}
