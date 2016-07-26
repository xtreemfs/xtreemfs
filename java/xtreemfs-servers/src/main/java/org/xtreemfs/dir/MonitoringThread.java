/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.dir.data.ServiceRecords;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

/**
 *
 * @author bjko
 */
public class MonitoringThread extends LifeCycleThread {

    private final DIRConfig cfg;

    /**
     * check every 10 seconds
     */
    private final int sleep;

    private final Map<String,Integer> warnCount;

    private volatile boolean quit;

    private final DIRRequestDispatcher master;

    public MonitoringThread(DIRConfig config, DIRRequestDispatcher master) {
        super("MonThr");
        this.master = master;
        this.cfg = config;
        this.sleep = cfg.getTimeoutSeconds()/2*1000;
        warnCount = new HashMap();
    }

    public void run() {

        notifyStarted();

        Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "MonitoringThread started");

        do {

            try {
                sleep(sleep);
            } catch (InterruptedException ex) {
                break;
            }

            long now = System.currentTimeMillis()/1000l;

            try {
                ServiceRecords services = master.getServices();

                List<String> offlineServices = new LinkedList();
                boolean sendWarning = false;

                for (ServiceRecord srv : services.getList()) {

                    if (srv.getType() == ServiceType.SERVICE_TYPE_VOLUME)
                        continue;

                    if (srv.getLast_updated_s() < now+cfg.getTimeoutSeconds()) {

                        //we have a timeout

                        int wc = 0;
                        if (warnCount.get(srv.getUuid()) != null) {
                            wc = warnCount.get(srv.getUuid());
                        }
                        wc++;
                        final String str = srv.getUuid()+"/"+srv.getName()+" - "+(srv.getLast_updated_s() == 0 ? "service was shut down" : "last heartbeat "+new Date(srv.getLast_updated_s() * 1000));
                        offlineServices.add(str);
                        warnCount.put(srv.getUuid(), wc);
                        if (wc <= cfg.getMaxWarnings()) {
                            sendWarning = true;
                            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "service is offline: %s", str);
                        }

                    } else {
                        //service is online, remove warn cound
                        warnCount.remove(srv.getUuid());
                    }

                }

                if ( sendWarning && (cfg.getAdminEmail().length() > 0)) {
                    //send warning email
                    StringBuilder sendmailData = new StringBuilder();
                    sendmailData.append("To: "+cfg.getAdminEmail());
                    sendmailData.append("\n");
                    if (cfg.getSenderAddress().length() > 0) {
                        sendmailData.append("From: "+cfg.getSenderAddress());
                        sendmailData.append("\n");
                    }
                    sendmailData.append("Subject: XtreemFS service(s) offline\n");
                    sendmailData.append("The following service(s) did not send a heartbeat signal for at least "+cfg.getTimeoutSeconds()+" seconds: \n\n");
                    for (String srv : offlineServices) {
                        sendmailData.append(srv);
                        sendmailData.append("\n");
                    }
                    sendmailData.append("\n--END--\n.\n");

                    Process p = Runtime.getRuntime().exec(cfg.getSendmailBin()+" -t "+cfg.getAdminEmail());
                    OutputStream os = p.getOutputStream();
                    os.write(sendmailData.toString().getBytes());
                    os.close();
                    p.waitFor();
                    Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "sent email to %s", cfg.getAdminEmail());
                }
            } catch (Exception ex) {
                Logging.logError(Logging.LEVEL_INFO, Category.all, ex);
            }

        } while (!quit);

        notifyStopped();

    }

    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

}
