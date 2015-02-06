/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.common.libxtreemfs.*;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.tracing.TraceInfo;
import org.xtreemfs.osd.tracing.TracingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class TracingStage extends Stage {
    private OSDRequestDispatcher master;
    private Map<String, Volume> volumes;
    private Client client;
    private SSLOptions sslOpts;

    public TracingStage(OSDRequestDispatcher master, int queueCapacity) {
        super("OSD Tracing Stage", queueCapacity);
        this.master = master;
        this.volumes = new HashMap<String, Volume>();
        RPC.UserCredentials uc = RPC.UserCredentials.newBuilder().setUsername("service")
                .addGroups("service").build();

        OSDConfig config = master.getConfig();
        SSLOptions.TrustManager tm = null;

        try {
            if (config.isUsingSSL()) {

                PolicyContainer policyContainer = new PolicyContainer(config);
                try {
                    tm = policyContainer.getTrustManager();
                } catch (Exception e) {
                    throw new IOException(e);
                }

                if (Logging.isInfo() && tm != null)
                    Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.misc, this,
                            "using custom trust manager '%s'", tm.getClass().getName());
            }

            sslOpts = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                    .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                    .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                    .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config
                    .isGRIDSSLmode(), config.getSSLProtocolString(), tm) : null;

            client = ClientFactory.createClient(master.getConfig().getDirectoryService().getHostName() + ":" +
                            master.getConfig().getDirectoryService().getPort(), uc, sslOpts, new Options());
            client.start();
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            notifyCrashed(ex);
        }
    }

    @Override
    public void run() {

        notifyStarted();

        do {
            try {
                while (!q.isEmpty()) {
                    final StageRequest op = q.take();
                    processMethod(op);
                }

            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }

            waitForWritebackSignal();
        } while (!quit);


        for (Volume v : this.volumes.values()) {
            v.close();
        }

        notifyStopped();
    }

    @Override
    protected void processMethod(StageRequest method) {
        try {
            TraceInfo traceInfo = (TraceInfo) method.getArgs()[0];
            FileHandle traceTargetFile = getFileHandle(traceInfo);
        } catch(IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void traceRequest(OSDRequest req) {
        TraceInfo traceInfo = createTraceInfo(req);
        this.enqueueOperation(req.getOperation().getProcedureId(), new Object[]{traceInfo}, req, null);
    }

    private void waitForWritebackSignal() {
        try {
            while (!writeTraceLog()) {
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private boolean writeTraceLog() {
        //TODO(ckleineweber): Find condition that considers current OSD load
        return (q.size() > 100);
    }

    private TraceInfo createTraceInfo(OSDRequest req) {
        TraceInfo result = new TraceInfo();

        result.setClient(req.getCapability().getClientIdentity());
        result.setFileId(req.getFileId());
        result.setTargetVolume(req.getCapability().getTraceConfig().getTargetVolume());

        return result;
    }

    private FileHandle getFileHandle(TraceInfo traceInfo) throws IOException {
        Volume volume = null;
        FileHandle result = null;
        TracingPolicy policy = getPolicy(traceInfo);
        RPC.UserCredentials uc = RPC.UserCredentials.newBuilder().setUsername("service")
                .addGroups("service").build();

        if (!this.volumes.containsKey(traceInfo.getTargetVolume())) {
            String[] volumeNames = client.listVolumeNames();
            boolean existing = false;

            if(volumeNames != null) {
                for (String volumeName : volumeNames) {
                    if (volumeName.equals(traceInfo.getTargetVolume()))
                        existing = true;
                }
            }

            if(!existing) {
                try {
                    DIR.ServiceSet mrcs = master.getDIRClient().xtreemfs_service_get_by_type(
                             master.getConfig().getDirectoryService(), RPCAuthentication.authNone, uc,
                             DIR.ServiceType.SERVICE_TYPE_MRC);
                    List<String> mrcAddresses = new ArrayList<String>();
                    for(DIR.Service mrc: mrcs.getServicesList()) {
                        mrcAddresses.add(mrc.getUuid());
                    }
                    client.createVolume(mrcAddresses, RPCAuthentication.authNone, uc, traceInfo.getTargetVolume());
               } catch (InterruptedException ex) {
                    throw new IOException(ex);
                }
           }

            volume = client.openVolume(traceInfo.getTargetVolume(), sslOpts, new Options());
            this.volumes.put(traceInfo.getTargetVolume(), volume);
        } else {
            volume = this.volumes.get(traceInfo.getTargetVolume());
        }

        result = volume.openFile(uc, policy.getTargetPath(traceInfo),
                GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber() | GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber());

        return result;
    }

    private TracingPolicy getPolicy(TraceInfo traceInfo) {
        return new TracingPolicy() {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public TraceInfo extractTraceInfo(OSDRequest req) {
                return null;
            }

            @Override
            public String logTraceInfo(TraceInfo info) {
                return null;
            }

            @Override
            public String getTargetPath(TraceInfo info) {
                return "/trace.txt";
            }
        };
    }
}
