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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.util.Properties;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.config.ServiceConfig;

/**
 * 
 * @author bjko
 */
public class DIRConfig extends ServiceConfig {
    
    private boolean autodiscoverEnabled;

    private boolean monitoringEnabled;

    private String  adminEmail;

    private String  senderAddress;

    private int     maxWarnings;

    private int     timeoutSeconds;

    private String  sendmailBin;
    
    /** Creates a new instance of OSDConfig */
    public DIRConfig(String filename) throws IOException {
        super(filename);
        read();
    }
    
    public DIRConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public void read() throws IOException {
        super.read();

        this.autodiscoverEnabled = this.readOptionalBoolean("discover", true);

        this.monitoringEnabled = this.readOptionalBoolean("monitoring", false);

        this.adminEmail = this.readOptionalString("monitoring.email.receiver", "");

        this.senderAddress = this.readOptionalString("monitoring.email.sender", "XtreemFS DIR monitoring <dir@localhost>");

        this.maxWarnings = this.readOptionalInt("monitoring.max_warnings", 1);

        this.timeoutSeconds = this.readOptionalInt("service_timeout_s", 5*60);

        this.sendmailBin = this.readOptionalString("monitoring.email.programm", "/usr/sbin/sendmail");
    }

    /**
     * @return the autodiscoverEnabled
     */
    public boolean isAutodiscoverEnabled() {
        return autodiscoverEnabled;
    }

    /**
     * @return the monitoringEnabled
     */
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    /**
     * @return the adminEmail
     */
    public String getAdminEmail() {
        return adminEmail;
    }

    /**
     * @return the senderAddress
     */
    public String getSenderAddress() {
        return senderAddress;
    }

    /**
     * @return the maxWarnings
     */
    public int getMaxWarnings() {
        return maxWarnings;
    }

    /**
     * @return the timeoutSeconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @return the sendmailBin
     */
    public String getSendmailBin() {
        return sendmailBin;
    }
    
}
