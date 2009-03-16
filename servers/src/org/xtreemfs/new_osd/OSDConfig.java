/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.new_osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.uuids.ServiceUUID;

/**
 * 
 * @author bjko
 */
public class OSDConfig extends ServiceConfig {
    
    public static final int   CHECKSUM_NONE    = 0;
    
    public static final int   CHECKSUM_ADLER32 = 1;
    
    public static final int   CHECKSUM_CRC32   = 2;
    
    private InetSocketAddress directoryService;
    
    private ServiceUUID       uuid;
    
    private int               localClockRenew;
    
    private int               remoteTimeSync;
    
    private String            objDir;
    
    private boolean           reportFreeSpace;
    
    private boolean           basicStatsEnabled;
    
    private boolean           measureRqsEnabled;
    
    private boolean           useChecksums;
    
    private String            checksumProvider;
    
    private String            capabilitySecret;

    private boolean           ignoreCaps;
    
    /** Creates a new instance of OSDConfig */
    public OSDConfig(String filename) throws IOException {
        super(filename);
        read();
    }
    
    public OSDConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public void read() throws IOException {
        super.read();
        
        this.directoryService = this.readRequiredInetAddr("dir_service.host", "dir_service.port");
        
        this.objDir = this.readRequiredString("object_dir");
        
        this.localClockRenew = this.readRequiredInt("local_clock_renewal");
        
        this.remoteTimeSync = this.readRequiredInt("remote_time_sync");
        
        this.uuid = new ServiceUUID(this.readRequiredString("uuid"));
        
        this.reportFreeSpace = this.readRequiredBoolean("report_free_space");
        
        this.setMeasureRqsEnabled(this.readOptionalBoolean("measure_requests", false));
        
        this.setBasicStatsEnabled(this.readOptionalBoolean("basic_statistics", false));
        
        this.useChecksums = this.readRequiredBoolean("checksums.enabled");
        
        this.checksumProvider = useChecksums ? this.readOptionalString("checksums.algorithm", null)
            : null;
        
        this.capabilitySecret = this.readRequiredString("capability_secret");

        this.ignoreCaps = this.readOptionalBoolean("ignore_capabilities", false);
    }
    
    public InetSocketAddress getDirectoryService() {
        return directoryService;
    }
    
    public String getObjDir() {
        return objDir;
    }
    
    public int getLocalClockRenew() {
        return localClockRenew;
    }
    
    public int getRemoteTimeSync() {
        return remoteTimeSync;
    }
    
    public ServiceUUID getUUID() {
        return uuid;
    }
    
    public boolean isReportFreeSpace() {
        return reportFreeSpace;
    }
    
    public void setReportFreeSpace(boolean reportFreeSpace) {
        this.reportFreeSpace = reportFreeSpace;
    }
    
    public boolean isBasicStatsEnabled() {
        return basicStatsEnabled;
    }
    
    public void setBasicStatsEnabled(boolean basicStatsEnabled) {
        this.basicStatsEnabled = basicStatsEnabled;
    }
    
    public boolean isMeasureRqsEnabled() {
        return measureRqsEnabled;
    }
    
    public void setMeasureRqsEnabled(boolean measureRqsEnabled) {
        this.measureRqsEnabled = measureRqsEnabled;
    }
    
    public String getChecksumProvider() {
        return checksumProvider;
    }
    
    public boolean isUseChecksums() {
        return useChecksums;
    }
    
    public String getCapabilitySecret() {
        return capabilitySecret;
    }

    /**
     * @return the ignoreCaps
     */
    public boolean isIgnoreCaps() {
        return ignoreCaps;
    }
    
}
