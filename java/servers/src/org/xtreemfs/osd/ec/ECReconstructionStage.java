/*
 * Copyright (c) 2016 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ListIntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.StripeReconstructor.StripeReconstructorCallback;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StorageStage.ECReconstructStripeCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class ECReconstructionStage extends Stage {

    private final OSDRequestDispatcher   master;

    private OSDServiceClient             osdClient;

    private final ServiceUUID            localUUID;
    private final ASCIIString            localID;

    private final Map<String, FileState> fileStates;

    public ECReconstructionStage(OSDRequestDispatcher master, SSLOptions sslOpts, int maxReconstructionLength) {
        super("ECReconstructionStage", maxReconstructionLength);

        this.master = master;
        // FIXME (jdillmann): Maybe use a new client
        osdClient = master.getOSDClient();

        // TODO (jdillmann): make local id a parameter?
        localUUID = master.getConfig().getUUID();
        localID = new ASCIIString(localUUID.toString());

        fileStates = new ConcurrentHashMap<String, FileState>();
    }

    @Override
    public synchronized void start() {
        osdClient = master.getOSDClient();
        super.start();
    }

    public boolean isInReconstruction(String fileId) {
        return fileStates.containsKey(fileId);
    }

    private static enum STAGE_OP {
        START_RECONSTRUCTION, REQUEST_RECONSTRUCTION, EVENT_STRIPE_STORED, EVENT_STRIPE_RECONSTRUCTED;

        private static STAGE_OP[] values_ = values();

        public static STAGE_OP valueOf(int n) {
            return values_[n];
        }
    };

    void enqueueOperation(final STAGE_OP stageOp, final Object[] args, final OSDRequest request,
            final ReusableBuffer createdViewBuffer, final Object callback) {
        enqueueOperation(stageOp.ordinal(), args, request, createdViewBuffer, callback);
    }

    @Override
    protected void processMethod(StageRequest rq) {
        switch (STAGE_OP.valueOf(rq.getStageMethod())) {
        // case REQUEST_RECONSTRUCTION:
        // processRequestReconstruction(rq);
        // break;
        case START_RECONSTRUCTION:
            processStartReconstruction(rq);
            break;
        case EVENT_STRIPE_RECONSTRUCTED:
            processEventStripeReconstructed(rq);
            break;
        case EVENT_STRIPE_STORED:
            processEventStripeStored(rq);
            break;
        // default : throw new IllegalArgumentException("No such stageop");
        }
    }

    public void requestReconstruction(String fileId, FileCredentials fileCreds, Capability capability,
            XLocations xloc) {
        // Relay the request to the master
        int osdNumber = xloc.getLocalReplica().getStripingPolicy().getRelativeOSDPosition();
        master.getECMasterStage().triggerReconstruction(fileId, fileCreds, xloc, osdNumber);

        // this.enqueueOperation(STAGE_OP.REQUEST_RECONSTRUCTION, new Object[] { fileId, fileCreds, capability, xloc },
        // null, null, null);
    }


    // public void processRequestReconstruction(StageRequest rq) {
    // final String fileId = (String) rq.getArgs()[0];
    // final FileCredentials fileCredentials = (FileCredentials) rq.getArgs()[1];
    // final Capability capability = (Capability) rq.getArgs()[2];
    // final XLocations xloc = (XLocations) rq.getArgs()[3];
    //
    // int osdNumber = xloc.getLocalReplica().getStripingPolicy().getRelativeOSDPosition();
    // master.getECMasterStage().triggerReconstruction(fileId, fileCredentials, xloc, osdNumber);
    // }

    public void startReconstruction(String fileId, FileCredentials fileCreds, Capability capability, XLocations xloc,
            List<Interval> commitIntervals, List<Interval> missingIntervals) {
        this.enqueueOperation(STAGE_OP.START_RECONSTRUCTION,
                new Object[] { fileId, fileCreds, capability, xloc, commitIntervals, missingIntervals }, null, null,
                null);
    }

    void processStartReconstruction(StageRequest rq) {
        final String fileId = (String) rq.getArgs()[0];
        final FileCredentials fileCredentials = (FileCredentials) rq.getArgs()[1];
        final Capability capability = (Capability) rq.getArgs()[2];
        final XLocations xloc = (XLocations) rq.getArgs()[3];
        final List<Interval> commitIntervals = (List<Interval>) rq.getArgs()[4];
        final List<Interval> missingIntervals = (List<Interval>) rq.getArgs()[5];

        if (isInReconstruction(fileId)) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this, "File %s is already in reconstruction", fileId);
            return;
        }

        FileState file = new FileState(fileId, fileCredentials, capability, xloc);
        file.setCommitIntervals(commitIntervals);

        fileStates.put(fileId, file);

        startReconstruction(file, missingIntervals);
    }

    void startReconstruction(FileState file, List<Interval> missingIntervals) {
        if (missingIntervals.isEmpty()) {
            // We are done!
            return;
        }

        StripingPolicyImpl sp = file.getPolicy();
        int localOsdNo = sp.getRelativeOSDPosition();

        // Find all the Stripes from the missing intervals
        long missingStart = missingIntervals.get(0).getStart();
        long missingEnd = missingIntervals.get(missingIntervals.size() - 1).getEnd();

        Interval rangeInterval = ObjectInterval.empty(missingStart, missingEnd);
        
        file.setState(ReconstructionState.START);

        ListIterator<Interval> missingIt = missingIntervals.listIterator();
        Interval missing = missingIt.next();

        for (Stripe stripe : Stripe.getIterable(rangeInterval, file.getPolicy())) {
            if (missing == null) {
                break;
            }

            Interval stripeInterval = stripe.getStripeInterval();
            
            // Advance the missing intervals to the next possible match.
            while (stripeInterval.getStart() > missing.getEnd()) {
                missing = missingIt.hasNext() ? missingIt.next() : null;
            }
                        
            // The current stripe does not overlap with any missing interval: Ignore stripe
            if (!stripeInterval.overlaps(missing)) {
                continue;
            }

            boolean needsData = false; // FIXME: true if to restore osd isparity

            // Add all missing intervals overlapping with this stripe
            List<Interval> missingStripeIntervals = new LinkedList<Interval>();
            while (stripeInterval.overlaps(missing)) {

                if (sp.getOSDforOffset(missing.getStart()) <= localOsdNo
                        && sp.getOSDforOffset(missing.getEnd() - 1) >= localOsdNo) {
                    // There is data to be fetched for this interval
                    needsData = true;
                }

                missingStripeIntervals.add(missing);
                missing = missingIt.hasNext() ? missingIt.next() : null;
            }


            List<Interval> commitStripeIntervals = file.getCommitStripeIntervals(stripeInterval);
            file.addStripe(stripe, commitStripeIntervals, needsData);
        }


        reconstructNextStripe(file);
    }


    void reconstructNextStripe(FileState file) {
        if (!file.activateNextStripe()) {
            finished(file);
            return;
        }
        
        file.setState(ReconstructionState.RECONSTRUCTION);
        StripeState stripe = file.getActiveStripe();
        final String fileId = file.getFileId();

        if (!stripe.needsData()) {
            // Only the version has to be reconstructed, no data
            master.getStorageStage().ecReconstructStripe(file.getFileId(), file.getPolicy(),
                    stripe.getStripe().getStripeNo(), stripe.getCommitStripeIntervals(), null, new ECReconstructStripeCallback() {
                        
                        @Override
                        public void ecReconstructStripeComplete(String fileId, long stripeNo, ErrorResponse error) {
                            eventStripeStored(fileId, stripeNo, error);
                        }
                    });
        } else {
            // Start the Stripe Reconstructor
            try {
                checkCap(file);
            } catch (IOException ex) {
                failed(file, ErrorUtils.getInternalServerError(ex));
                return;
            }

            StripeReconstructor reconstructor = new StripeReconstructor(master, file.getFileCredentials(),
                    file.getXLoc(), file.getFileId(), stripe.getStripe().getStripeNo(), file.getPolicy(),
                    stripe.getCommitStripeIntervals(), osdClient, new StripeReconstructorCallback() {

                        @Override
                        public void success(long stripeNo) {
                            eventStripeReconstructed(fileId, stripeNo);
                        }

                        @Override
                        public void failed(long stripeNo) {
                            // TODO Auto-generated method stub
                            Logging.logMessage(Logging.LEVEL_CRIT, this, "Could not reconstruct");
                        }
                    });

            stripe.setReconstructor(reconstructor);
            reconstructor.start();
        }
    }

    void eventStripeReconstructed(String fileId, long stripeNo) {
        this.enqueueOperation(STAGE_OP.EVENT_STRIPE_RECONSTRUCTED, new Object[] { fileId, stripeNo }, null, null, null);
    }

    void processEventStripeReconstructed(final StageRequest rq) {
        final String fileId = (String) rq.getArgs()[0];
        final long stripeNo = (Long) rq.getArgs()[1];

        FileState file = fileStates.get(fileId);
        if (file == null) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                    "Received Event StripeReconstructed for file '%s' which is not in reconstruction", fileId);
            return;
        }

        StripeState stripe = file.getActiveStripe();
        assert (stripeNo == stripe.getStripe().getStripeNo());

        StripeReconstructor reconstructor = stripe.getReconstructor();

        StripingPolicyImpl sp = file.getPolicy();
        
        int osdNum = sp.getRelativeOSDPosition();
        boolean isParity = osdNum >= sp.getWidth();

        assert (reconstructor.hasFinished());
        reconstructor.decode(isParity);

        ReusableBuffer data = reconstructor.getObject(osdNum);
        master.getStorageStage().ecReconstructStripe(fileId, sp, stripe.getStripe().getStripeNo(),
                stripe.getCommitStripeIntervals(), data, new ECReconstructStripeCallback() {

                    @Override
                    public void ecReconstructStripeComplete(String fileId, long stripeNo, ErrorResponse error) {
                        eventStripeStored(fileId, stripeNo, error);
                    }
                });

        reconstructor.freeBuffers();
        stripe.setReconstructor(null);
    }

    void eventStripeStored(String fileId, long stripeNo, ErrorResponse error) {
        this.enqueueOperation(STAGE_OP.EVENT_STRIPE_STORED, new Object[] { fileId, stripeNo, error }, null, null, null);
    }

    void processEventStripeStored(final StageRequest rq) {
        final String fileId = (String) rq.getArgs()[0];
        final long stripeNo = (Long) rq.getArgs()[1];
        final ErrorResponse error = (ErrorResponse) rq.getArgs()[2];

        FileState file = fileStates.get(fileId);
        if (file == null) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.ec, this,
                    "Received Event StripeStored for file '%s' which is not in reconstruction", fileId);
            return;
        }

        if (error != null) {
            failed(file, error);
            return;
        }

        // Continue reconstruction
        reconstructNextStripe(file);
    }

    void finished(FileState file) {
        fileStates.remove(file.getFileId());
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this, "Reconstruction finished for file '%s'",
                file.getFileId());
    }

    void failed(FileState file, ErrorResponse error) {
        fileStates.remove(file.getFileId());

        StripeState stripe = file.getActiveStripe();
        if (stripe.getReconstructor() != null) {
            stripe.getReconstructor().abort();
        }

        Logging.logMessage(Logging.LEVEL_WARN, Category.ec, this, "Reconstruction failed for file '%s': %s",
                file.getFileId(), ErrorUtils.formatError(error));
    }

    private enum ReconstructionState {
        INIT,
        REQUEST_VECTOR,
        COMMIT_VECTOR,
        START,
        RECONSTRUCTION
    }

    private final static class StripeState {
        private final Stripe         stripe;
        private final List<Interval> commitStripeIntervals;
        private final boolean        needsData;
        private StripeReconstructor  reconstructor;

        public StripeState(Stripe stripe, List<Interval> commitStripeIntervals, boolean needsData) {
            this.stripe = stripe;
            this.commitStripeIntervals = commitStripeIntervals;
            this.needsData = needsData;
        }

        Stripe getStripe() {
            return stripe;
        }

        List<Interval> getCommitStripeIntervals() {
            return commitStripeIntervals;
        }

        boolean needsData() {
            return needsData;
        }

        StripeReconstructor getReconstructor() {
            return reconstructor;
        }

        void setReconstructor(StripeReconstructor reconstructor) {
            this.reconstructor = reconstructor;
        }

    }

    private final static class FileState {
        private final String             fileId;
        private final XLocations         xloc;
        private final StripingPolicyImpl sp;

        private final Queue<StripeState> pendingStripes;
        private StripeState              activeStripe;

        private FileCredentials          fileCredentials;
        private Capability               capability;
        private InetSocketAddress        mrcAddress;

        private ReconstructionState      state;
        private List<Interval>           commitIntervals;
        private IntervalVector           commitVector;


        public FileState(String fileId, FileCredentials fileCredentials, Capability capability, XLocations xloc) {
            this.fileId = fileId;
            this.fileCredentials = fileCredentials;
            this.capability = capability;
            this.xloc = xloc;
            this.sp = xloc.getLocalReplica().getStripingPolicy();
            
            pendingStripes = new LinkedList<StripeState>();
            activeStripe = null;

            state = ReconstructionState.INIT;
        }

        ReconstructionState getState() {
            return state;
        }

        void setState(ReconstructionState state) {
            this.state = state;
        }

        void assertState(ReconstructionState... states) {
            boolean found = false;
            for (ReconstructionState state: states) {
                if (this.state == state) {
                    found = true;
                    break;
                }
            }
            
            assert (found) : "Current state " + this.state + " not in " + Arrays.deepToString(states);
        }

        FileCredentials getFileCredentials() {
            return fileCredentials;
        }

        Capability getCapability() {
            return capability;
        }

        void setCapability(Capability capability) {
            this.capability = capability;
            this.fileCredentials = fileCredentials.toBuilder().setXcap(capability.getXCap()).build();
        }

        XLocations getXLoc() {
            return xloc;
        }

        InetSocketAddress getMrcAddress() {
            return mrcAddress;
        }

        void setMrcAddress(InetSocketAddress mrcAddress) {
            this.mrcAddress = mrcAddress;
        }

        String getFileId() {
            return fileId;
        }

        StripingPolicyImpl getPolicy() {
            return sp;
        }

        List<Interval> getCommitIntervals() {
            return commitIntervals;
        }

        void setCommitIntervals(List<Interval> commitIntervals) {
            this.commitIntervals = commitIntervals;

            // FIXME (jdillmann): ListIntervalsVectors are slow on slive/overlapping
            commitVector = new ListIntervalVector(commitIntervals);
        }

        List<Interval> getCommitStripeIntervals(Interval stripeInterval) {
            List<Interval> commitStripeInterval = commitVector.getSlice(stripeInterval.getStart(),
                    stripeInterval.getEnd());
            // FIXME (jdilmann): Find a better way to handle truncated or non stripe aligned files
            while (commitStripeInterval.get(commitStripeInterval.size() - 1).isEmpty()) {
                commitStripeInterval.remove(commitStripeInterval.size() - 1);
            }
            return commitStripeInterval;
        }

        void addStripe(Stripe stripe, List<Interval> commitStripeInterval, boolean needsData) {
            pendingStripes.add(new StripeState(stripe, commitStripeInterval, needsData));
        }

        StripeState getActiveStripe() {
            return activeStripe;
        }

        boolean activateNextStripe() {
            if (activeStripe != null) {
                if (activeStripe.getReconstructor() != null) {
                    activeStripe.getReconstructor().freeBuffers();
                }
            }

            activeStripe = pendingStripes.poll();
            return (activeStripe != null);
        }

    }


    /**
     * checks if the capability is still valid; renews the capability if necessary
     */
    private void checkCap(FileState file) throws IOException {
        try {
            long curTime = TimeSync.getGlobalTime() / 1000; // s

            // get the correct MRC only once and only if the capability must be updated
            if (file.getCapability().getExpires() - curTime < 60 * 1000) {
                // capability expires in less than 60s
                if (file.getMrcAddress() == null) {
                    String volume = null;
                    try {
                        // get volume of file
                        volume = new MRCHelper.GlobalFileIdResolver(file.getFileId()).getVolumeId();

                        // get MRC appropriate for this file
                        ServiceSet sSet = master.getDIRClient().xtreemfs_service_get_by_uuid(null,
                                RPCAuthentication.authNone, RPCAuthentication.userService, volume);

                        if (sSet.getServicesCount() != 0) {
                            for (KeyValuePair kvp : sSet.getServices(0).getData().getDataList()) {
                                if (kvp.getKey().equals("mrc")) {
                                    file.setMrcAddress(new ServiceUUID(kvp.getValue()).getAddress());
                                    break;
                                }
                            }

                        } else
                            throw new IOException("Cannot find a MRC.");
                    } catch (UserException e) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.ec, this,
                                e.getLocalizedMessage() + "; for file %s", file.getFileId());
                    }

                }

                // update Xcap
                RPCResponse<XCap> r = master.getMRCClient().xtreemfs_renew_capability(file.getMrcAddress(),
                        RPCAuthentication.authNone, RPCAuthentication.userService, file.getCapability().getXCap());
                XCap xCap = r.get();
                r.freeBuffers();

                file.setCapability(new Capability(xCap, master.getConfig().getCapabilitySecret()));
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
