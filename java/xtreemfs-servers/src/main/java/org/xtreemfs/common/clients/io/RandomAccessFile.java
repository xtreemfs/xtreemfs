///*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//Grid Operating System, see <http://www.xtreemos.eu> for more details.
//The XtreemOS project has been developed with the financial support of the
//European Commission's IST program under contract #FP6-033576.
//
//XtreemFS is free software: you can redistribute it and/or modify it under
//the terms of the GNU General Public License as published by the Free
//Software Foundation, either version 2 of the License, or (at your option)
//any later version.
//
//XtreemFS is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Nele Andersen (ZIB), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
// */
//package org.xtreemfs.common.clients.io;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicLong;
//
//import org.xtreemfs.common.uuids.ServiceUUID;
//import org.xtreemfs.common.xloc.InvalidXLocationsException;
//import org.xtreemfs.common.xloc.Replica;
//import org.xtreemfs.common.xloc.StripingPolicyImpl;
//import org.xtreemfs.common.xloc.XLocations;
//import org.xtreemfs.foundation.buffer.BufferPool;
//import org.xtreemfs.foundation.buffer.ReusableBuffer;
//import org.xtreemfs.foundation.logging.Logging;
//import org.xtreemfs.foundation.logging.Logging.Category;
//import org.xtreemfs.foundation.monitoring.Monitoring;
//import org.xtreemfs.foundation.monitoring.NumberMonitoring;
//import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
//import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
//import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;
//import org.xtreemfs.interfaces.Constants;
//import org.xtreemfs.interfaces.FileCredentials;
//import org.xtreemfs.interfaces.FileCredentialsSet;
//import org.xtreemfs.interfaces.NewFileSize;
//import org.xtreemfs.interfaces.OSDWriteResponse;
//import org.xtreemfs.interfaces.ObjectData;
//import org.xtreemfs.interfaces.Stat;
//import org.xtreemfs.interfaces.StatSet;
//import org.xtreemfs.interfaces.StringSet;
//import org.xtreemfs.interfaces.StripingPolicy;
//import org.xtreemfs.interfaces.UserCredentials;
//import org.xtreemfs.interfaces.VivaldiCoordinates;
//import org.xtreemfs.interfaces.XCap;
//import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
//import org.xtreemfs.interfaces.MRCInterface.setxattrResponse;
//import org.xtreemfs.interfaces.OSDInterface.OSDException;
//import org.xtreemfs.mrc.ac.FileAccessManager;
//import org.xtreemfs.mrc.client.MRCClient;
//import org.xtreemfs.osd.ErrorCodes;
//import org.xtreemfs.osd.client.OSDClient;
//
///**
// * @deprecated
// */
//public class RandomAccessFile implements ObjectStore {
//    /**
//     * resorts the replicas <br>
//     * 12.05.2009
//     */
//    public abstract static class ReplicaSelectionPolicy {
//        public abstract List<Replica> getReplicaOrder(List<Replica> replicas);
//    }
//
//    /**
//     * policy randomizes the entries in list <br>
//     * DEFAULT POLICY
//     */
//    public final ReplicaSelectionPolicy RANDOM_REPLICA_SELECTION_POLICY             = new RandomAccessFile.ReplicaSelectionPolicy() {
//                                                                                        @Override
//                                                                                        public List<Replica> getReplicaOrder(
//                                                                                            List<Replica> replicas) {
//                                                                                            List<Replica> list = new ArrayList<Replica>(
//                                                                                                replicas);
//                                                                                            Collections
//                                                                                                    .shuffle(list);
//                                                                                            return list;
//                                                                                        }
//                                                                                    };
//
//    /**
//     * policy rotates the entries in list (like round-robin)
//     */
//    public final ReplicaSelectionPolicy SEQUENTIAL_REPLICA_SELECTION_POLICY         = new RandomAccessFile.ReplicaSelectionPolicy() {
//                                                                                        private int rotateValue = 0;
//
//                                                                                        @Override
//                                                                                        public List<Replica> getReplicaOrder(
//                                                                                            List<Replica> replicas) {
//                                                                                            List<Replica> list = new ArrayList<Replica>(
//                                                                                                replicas);
//                                                                                            Collections
//                                                                                                    .rotate(
//                                                                                                        list,
//                                                                                                        rotateValue);
//                                                                                            rotateValue = 0 - (((0 - rotateValue) + 1) % list
//                                                                                                    .size());
//                                                                                            return list;
//                                                                                        }
//                                                                                    };
//
//    private static final int            DEFAULT_CAP_VALIDITY                        = 60;
//
//    private MRCClient                   mrcClient;
//
//    private OSDClient                   osdClient;
//
//    private FileCredentials             fileCredentials;
//
//    // all replicas have the same stripesize at the moment
//    private StripingPolicyImpl          stripingPolicy;
//
//    private final String                fileId;
//
//    private final int                   mode;
//
//    private final String                volName;
//
//    private final String                pathName;
//
//    private final InetSocketAddress     mrcAddress;
//
//    private ByteMapper                  byteMapper;
//
//    private OSDWriteResponse            wresp;
//
//    private long                        filePos;
//
//    private XLocations                  xLoc;
//
//    private long                        capTime;
//
//    private List<Replica>               replicaOrder;
//
//    private boolean                     isReadOnly;
//
//    private final UserCredentials       credentials;
//
//    private ReplicaSelectionPolicy      replicaSelectionPolicy;
//
//    /*
//     * monitoring stuff
//     */
//    private AtomicLong                  monitoringReadDataSizeInLastXs;
//
//    private Thread                      monitoringThread                            = null;
//
//    private NumberMonitoring            monitoring;
//
//    /**
//     * Measures the throughput of the last 1 second.
//     */
//    public static final String          MONITORING_KEY_THROUGHPUT_OF_LAST_X_SECONDS = "RAF: throughput of last X seconds (KB/s)";
//
//    // /**
//    // * Measures the throughput of the read data so far. Just the time required
//    // * for the real network-transfer will be used.
//    // */
//    // public static final String MONITORING_KEY_THROUGHPUT =
//    // "RAF: throughput of all read data so far (KB/s)";
//
//    public static final int             MONITORING_INTERVAL                         = 1000;                                      // 10s
//
//    public RandomAccessFile(String mode, InetSocketAddress mrcAddress, String pathName,
//        RPCNIOSocketClient rpcClient, String userID, List<String> groupIDs) throws ONCRPCException,
//        InterruptedException, IOException {
//        this(mode, mrcAddress, pathName, rpcClient, MRCClient.getCredentials(userID, groupIDs));
//    }
//
//    public RandomAccessFile(String mode, InetSocketAddress mrcAddress, String pathName,
//        RPCNIOSocketClient rpcClient, UserCredentials credentials) throws ONCRPCException,
//        InterruptedException, IOException {
//
//        this.mrcAddress = mrcAddress;
//        this.mode = translateMode(mode);
//
//        int index = pathName.indexOf('/');
//        if (index == -1)
//            throw new IOException("invalid path: " + pathName);
//
//        this.volName = pathName.substring(0, index);
//        this.pathName = pathName.substring(index + 1);
//
//        assert (rpcClient != null);
//
//        // use the shared speedy to create an MRC and OSD client
//        mrcClient = new MRCClient(rpcClient, mrcAddress);
//        osdClient = new OSDClient(rpcClient);
//
//        this.credentials = credentials;
//
//        // OSD selection
//        this.replicaSelectionPolicy = RANDOM_REPLICA_SELECTION_POLICY;
//
//        // create a new file if necessary
//        RPCResponse<FileCredentials> r = mrcClient.open(mrcAddress, credentials, volName, this.pathName,
//            FileAccessManager.O_CREAT, this.mode, 0, new VivaldiCoordinates());
//        fileCredentials = r.get();
//        r.freeBuffers();
//
//        if (fileCredentials.getXlocs().getReplicas().size() == 0) {
//            throw new IOException("cannot assign OSDs to file");
//        }
//
//        // all replicas have the same striping policy (more precisely the same
//        // stripesize) at the moment
//        stripingPolicy = StripingPolicyImpl.getPolicy(fileCredentials.getXlocs().getReplicas().get(0), 0);
//        try {
//            this.xLoc = new XLocations(fileCredentials.getXlocs(), null);
//        } catch (InvalidXLocationsException ex) {
//            // ignore
//        }
//
//        // always use first replica at beginning (original order)
//        replicaOrder = this.replicaSelectionPolicy.getReplicaOrder(xLoc.getReplicas());
//
//        byteMapper = ByteMapperFactory.createByteMapper(stripingPolicy.getPolicyId(), stripingPolicy
//                .getStripeSizeForObject(0), this);
//
//        capTime = System.currentTimeMillis();
//
//        isReadOnly = fileCredentials.getXlocs().getReplica_update_policy().equals(
//            Constants.REPL_UPDATE_PC_RONLY);
//
//        fileId = fileCredentials.getXcap().getFile_id();
//        wresp = null;
//        filePos = 0;
//
//        monitoring = new NumberMonitoring();
//        monitoringReadDataSizeInLastXs = new AtomicLong(0);
//        if (Monitoring.isEnabled()) {
//            // enable statistics in client
//            RPCNIOSocketClient.ENABLE_STATISTICS = true;
//
//            monitoringThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        while (true) {
//                            if (Thread.interrupted())
//                                break;
//                            Thread.sleep(MONITORING_INTERVAL); // sleep
//
//                            long sizeInLastXs = monitoringReadDataSizeInLastXs.getAndSet(0);
//                            if (sizeInLastXs > 0) // log only interesting values
//                                monitoring.put(MONITORING_KEY_THROUGHPUT_OF_LAST_X_SECONDS,
//                                    (sizeInLastXs / 1024d) / (MONITORING_INTERVAL / 1000d));
//                        }
//                    } catch (InterruptedException e) {
//                        // shutdown
//                    }
//                }
//            });
//            monitoringThread.setDaemon(true);
//            monitoringThread.start();
//        }
//    }
//
//    private static int translateMode(String mode) {
//        if (mode.equals("r"))
//            return FileAccessManager.O_RDONLY;
//        if (mode.startsWith("rw"))
//            return FileAccessManager.O_RDWR;
//        throw new IllegalArgumentException("invalid mode");
//    }
//
//    private void updateWriteResponse(OSDWriteResponse r) {
//        if (r.getNew_file_size().size() > 0) {
//            final NewFileSize nfs = r.getNew_file_size().get(0);
//            if (wresp == null) {
//                wresp = r;
//            } else {
//                final NewFileSize ofs = wresp.getNew_file_size().get(0);
//                if ((nfs.getSize_in_bytes() > ofs.getSize_in_bytes())
//                    && (nfs.getTruncate_epoch() == ofs.getTruncate_epoch())
//                    || (nfs.getTruncate_epoch() > ofs.getTruncate_epoch())) {
//                    wresp = r;
//                }
//            }
//        }
//    }
//
//    /**
//     *
//     * @param resultBuffer
//     *            - the buffer into which the data is read.
//     * @param offset
//     *            - the start offset of the data.
//     * @param bytesToRead
//     *            - the maximum number of bytes read.
//     * @return - the total number of bytes read into the buffer, or -1 if there
//     *         is no more data because the end of the file has been reached.
//     * @throws Exception
//     * @throws IOException
//     */
//    public int read(byte[] resultBuffer, int offset, int bytesToRead) throws Exception {
//
//        int tmp = byteMapper.read(resultBuffer, offset, bytesToRead, filePos);
//        filePos += tmp;
//        return tmp;
//    }
//
//    public ReusableBuffer readObject(long objectNo) throws IOException {
//        return readObject(objectNo, 0, stripingPolicy.getStripeSizeForObject(objectNo));
//    }
//
//    /**
//     *
//     * @param objectNo
//     *            - relative object number.
//     * @param firstByteInObject
//     *            - the first byte to be read.
//     * @param bytesInObject
//     *            - the maximal number of bytes to be read.
//     * @return a ReusableBuffer containing the data which was read.
//     */
//    @Override
//    public ReusableBuffer readObject(long objectNo, int offset, int length) throws IOException {
//
//        RPCResponse<ObjectData> response = null;
//
//        int size = 0;
//        ObjectData data = null;
//        ReusableBuffer buffer = null;
//        Iterator<Replica> iterator = this.replicaOrder.iterator();
//        while (iterator.hasNext()) { // will be aborted, if object could be read
//            Replica replica = iterator.next();
//            // check whether capability needs to be renewed
//            checkCap();
//
//            // get OSD
//            ServiceUUID osd = replica.getOSDForObject(objectNo);
//            try {
//                if (Logging.isDebug())
//                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.tool, this,
//                        "%s:%d - read object from OSD %s", fileId, objectNo, osd);
//
//                response = osdClient.read(osd.getAddress(), fileId, fileCredentials, objectNo, 0, offset,
//                    length);
//                data = response.get();
//
//                if (data.getInvalid_checksum_on_osd()) {
//                    // try next replica
//                    if (!iterator.hasNext()) { // all replicas had been tried
//                        throw new IOException("object " + objectNo + " has an invalid checksum");
//                    }
//                }
//
//                // fill up with padding zeros
//                if (data.getZero_padding() == 0) {
//                    buffer = data.getData();
//                } else {
//                    final int dataSize = data.getData().capacity();
//                    if (data.getData().enlarge(dataSize + data.getZero_padding())) {
//                        data.getData().position(dataSize);
//                        while (data.getData().hasRemaining())
//                            data.getData().put((byte) 0);
//                        buffer = data.getData();
//                        buffer.position(0);
//                    } else {
//                        buffer = BufferPool.allocate(dataSize + data.getZero_padding());
//                        buffer.put(data.getData());
//                        while (buffer.hasRemaining())
//                            buffer.put((byte) 0);
//                        buffer.position(0);
//                        BufferPool.free(data.getData());
//                    }
//                }
//
//                // // monitor data for throughput
//                // if (Monitoring.isEnabled()) {
//                // monitoring.putAverage(MONITORING_KEY_THROUGHPUT,
//                // (buffer.limit() / 1024d)
//                // / (response.getDuration() / 1000000000d));
//                // monitoringReadDataSizeInLastXs.addAndGet(buffer.limit());
//                // }
//
//                break;
//
//            } catch (OSDException ex) {
//                if (buffer != null)
//                    BufferPool.free(buffer);
//                // all replicas had been tried or replication has been failed
//                if (ex instanceof OSDException)
//                    if (iterator.hasNext() || ((OSDException) ex).getError_code() != ErrorCodes.IO_ERROR)
//                        continue;
//                throw new IOException("cannot read object", ex);
//            } catch (ONCRPCException ex) {
//                if (buffer != null)
//                    BufferPool.free(buffer);
//                // all replicas had been tried or replication has been failed
//                throw new IOException("cannot read object: " + ex.getMessage(), ex);
//            } catch (IOException ex) {
//                if (buffer != null)
//                    BufferPool.free(buffer);
//                // all replicas had been tried
//                if (!iterator.hasNext()) {
//                    throw new IOException("cannot read object", ex);
//                }
//            } catch (InterruptedException ex) {
//                // ignore
//            } finally {
//                if (response != null) {
//                    response.freeBuffers();
//                }
//            }
//        }
//        return buffer;
//    }
//
//    /**
//     *
//     * @param objectNo
//     * @return
//     * @throws IOException
//     */
//    public int checkObject(long objectNo) throws IOException {
//        checkCap();
//
//        RPCResponse<ObjectData> response = null;
//
//        int size = 0;
//        ObjectData data = null;
//        ReusableBuffer buffer = null;
//        Iterator<Replica> iterator = this.replicaOrder.iterator();
//        while (iterator.hasNext()) { // will be aborted, if object could be read
//            Replica replica = iterator.next();
//            try {
//                // get OSD
//                ServiceUUID osd = replica.getOSDForObject(objectNo);
//
//                response = osdClient.check_object(osd.getAddress(), fileId, fileCredentials, objectNo, 0);
//                data = response.get();
//
//                if (data.getInvalid_checksum_on_osd()) {
//                    // try next replica
//                    if (!iterator.hasNext()) { // all replicas had been tried
//                        throw new IOException("object " + objectNo + " has an invalid checksum");
//                    }
//                }
//
//                size = data.getZero_padding();
//
//                break;
//            } catch (ONCRPCException ex) {
//                if (buffer != null)
//                    BufferPool.free(buffer);
//                // all replicas had been tried or replication has been failed
//                if (!iterator.hasNext() || ((OSDException) ex).getError_code() == ErrorCodes.IO_ERROR) {
//                    throw new IOException("cannot read object", ex);
//                }
//            } catch (IOException ex) {
//                if (buffer != null)
//                    BufferPool.free(buffer);
//                // all replicas had been tried
//                if (!iterator.hasNext()) {
//                    throw new IOException("cannot read object", ex);
//                }
//            } catch (InterruptedException ex) {
//                // ignore
//            } finally {
//                if (response != null) {
//                    response.freeBuffers();
//                }
//                if ((data != null) && (data.getData() != null)) {
//                    BufferPool.free(data.getData());
//                    data.setData(null);
//                }
//            }
//        }
//        return size;
//    }
//
//    /**
//     * Writes bytesToWrite bytes from the writeFromBuffer byte array starting at
//     * offset to this file.
//     *
//     * @param writeFromBuffer
//     * @param offset
//     * @param bytesToWrite
//     * @return the number of bytes written
//     * @throws Exception
//     */
//    public int write(byte[] writeFromBuffer, int offset, int bytesToWrite) throws Exception {
//
//        int tmp = byteMapper.write(writeFromBuffer, offset, bytesToWrite, filePos);
//        filePos += bytesToWrite;
//        return tmp;
//    }
//
//    /**
//     * Writes...
//     *
//     * @param firstByteInObject
//     *            - the start offset in the file
//     * @param objectNo
//     *            - the relative object number
//     * @param data
//     *            - the data to be written.....
//     */
//    public void writeObject(long firstByteInObject, long objectNo, ReusableBuffer data) throws IOException {
//
//        // check whether capability needs to be renewed
//        checkCap();
//
//        if (isReadOnly)
//            throw new IOException("File is marked as read-only. You cannot write anymore.");
//
//        RPCResponse<OSDWriteResponse> response = null;
//        try {
//            // uses always first replica
//            ServiceUUID osd = replicaOrder.get(0).getOSDs().get(stripingPolicy.getOSDforObject(objectNo));
//            ObjectData odata = new ObjectData(0, false, 0, data);
//            response = osdClient.write(osd.getAddress(), fileId, fileCredentials, objectNo, 0,
//                (int) firstByteInObject, 0, odata);
//            OSDWriteResponse owr = response.get();
//            this.updateWriteResponse(owr);
//        } catch (ONCRPCException ex) {
//            throw new IOException("cannot write object: " + ex.getMessage(), ex);
//        } catch (InterruptedException ex) {
//            throw new IOException("cannot write object", ex);
//        } finally {
//            if (response != null)
//                response.freeBuffers();
//        }
//
//    }
//
//    public void flush() throws IOException {
//        if (wresp != null) {
//            RPCResponse r = null;
//            try {
//                long fs = wresp.getNew_file_size().get(0).getSize_in_bytes();
//                int ep = wresp.getNew_file_size().get(0).getTruncate_epoch();
//                r = mrcClient.fsetattr(mrcAddress, fileCredentials.getXcap(), new Stat(0, 0, 0, 0, "", "",
//                    fs, 0, 0, 0, 0, 0, ep, 0), MRCInterface.SETATTR_SIZE);
//                r.get();
//                wresp = null;
//            } catch (ONCRPCException ex) {
//                throw new IOException("cannot write object", ex);
//            } catch (InterruptedException ex) {
//                throw new IOException("cannot write object", ex);
//            } finally {
//                if (r != null)
//                    r.freeBuffers();
//            }
//        }
//    }
//
//    public void delete() throws Exception {
//        checkCap();
//
//        if (fileCredentials.getXlocs().getReplicas().size() == 1) {
//            RPCResponse<FileCredentialsSet> r = null;
//            RPCResponse delR = null;
//            try {
//                r = mrcClient.unlink(mrcAddress, credentials, volName, pathName);
//                FileCredentialsSet fcreds = r.get();
//                if (fcreds.size() > 0) {
//                    // must delete on OSDs too!
//                    final FileCredentials delCred = fcreds.get(0);
//                    // uses always first replica
//                    delR = osdClient.unlink(replicaOrder.get(0).getHeadOsd().getAddress(), fileId, delCred);
//                    delR.get();
//                    delR.freeBuffers();
//                }
//            } catch (ONCRPCException ex) {
//                throw new IOException("cannot write object", ex);
//            } catch (InterruptedException ex) {
//                throw new IOException("cannot write object", ex);
//            } finally {
//                if (r != null)
//                    r.freeBuffers();
//            }
//        } else {
//            throw new IOException("There is more than 1 replica existing. Delete all replicas first.");
//        }
//    }
//
//    public long length() throws IOException {
//        RPCResponse<StatSet> r = null;
//        try {
//            r = mrcClient.getattr(mrcAddress, credentials, volName, pathName);
//            Stat statInfo = r.get().get(0);
//
//            // decide what to use...
//            if (wresp != null) {
//                final NewFileSize localFS = wresp.getNew_file_size().get(0);
//
//                // check if we know a larger file size locally
//                if (localFS.getTruncate_epoch() < statInfo.getTruncate_epoch())
//                    return statInfo.getSize();
//                if (localFS.getSize_in_bytes() > statInfo.getSize())
//                    return localFS.getSize_in_bytes();
//            }
//            return statInfo.getSize();
//        } catch (ONCRPCException ex) {
//            throw new IOException("cannot write object", ex);
//        } catch (InterruptedException ex) {
//            throw new IOException("cannot write object", ex);
//        } finally {
//            if (r != null)
//                r.freeBuffers();
//        }
//
//    }
//
//    public void close() throws IOException {
//        flush();
//
//        // shutdown
//        if (monitoringThread != null)
//            monitoringThread.interrupt();
//    }
//
//    /**
//     * Sets the file read-only and changes the access mode to "r", if mode is
//     * "true". Sets the file writable and changes the access mode to the
//     * original mode, if mode is "false" and no replicas exist.
//     *
//     * @param mode
//     * @throws Exception
//     */
//    public void setReadOnly(boolean mode) throws Exception {
//        if (isReadOnly == mode)
//            return;
//
//        try {
//            if (mode) {
//                flush();
//
//                // set read only
//                RPCResponse<setxattrResponse> r = mrcClient.setxattr(mrcAddress, credentials, volName,
//                    pathName, "xtreemfs.read_only", "true", 0);
//                r.get();
//                r.freeBuffers();
//
//                forceXCapUpdate();
//
//                // get correct filesize
//                RPCResponse<Long> r2 = osdClient.internal_get_file_size(replicaOrder.get(0).getHeadOsd()
//                        .getAddress(), fileId, fileCredentials);
//                long filesize = r2.get();
//                r2.freeBuffers();
//
//                // set filesize on mrc
//                forceFileSize(filesize);
//
//                forceFileCredentialsUpdate(translateMode("r"));
//            } else {
//                if (fileCredentials.getXlocs().getReplicas().size() > 1)
//                    throw new IOException("File has still replicas.");
//                else {
//                    // set read only
//                    RPCResponse<setxattrResponse> r = mrcClient.setxattr(mrcAddress, credentials, volName,
//                        pathName, "xtreemfs.read_only", "false", 0);
//                    r.get();
//                    r.freeBuffers();
//
//                    forceFileCredentialsUpdate(this.mode);
//                }
//            }
//        } catch (ONCRPCException ex) {
//            throw new IOException("Cannot change objects read-only-state.", ex);
//        } catch (InterruptedException ex) {
//            throw new IOException("Cannot change objects read-only-state.", ex);
//        }
//    }
//
//    /**
//     * adds a replica for this file
//     *
//     * @param osds
//     * @param spPolicy
//     * @param replicationFlags
//     * @throws Exception
//     */
//    public void addReplica(List<ServiceUUID> osds, StripingPolicy spPolicy, int replicationFlags)
//        throws Exception {
//        // check correct parameters
//        if (spPolicy.getStripe_size() != stripingPolicy.getPolicy().getStripe_size())
//            throw new IllegalArgumentException("New replica must have a stripe size of "
//                + stripingPolicy.getPolicy().getStripe_size() + " (given value is "
//                + spPolicy.getStripe_size() + ").");
//        if (osds.size() != spPolicy.getWidth())
//            throw new IllegalArgumentException("Too many or less OSDs in list.");
//        for (ServiceUUID osd : osds) {
//            if (xLoc.containsOSD(osd)) // OSD is used for any replica so far
//                throw new IllegalArgumentException(
//                    "At least one OSD from list is already used for this file.");
//        }
//
//        if (isReadOnly) {
//            StringSet osdSet = new StringSet();
//            for (ServiceUUID osd : osds) {
//                osdSet.add(osd.toString());
//            }
//
//            org.xtreemfs.interfaces.Replica newReplica = new org.xtreemfs.interfaces.Replica(osdSet,
//                replicationFlags, spPolicy);
//            RPCResponse r = mrcClient.xtreemfs_replica_add(mrcAddress, credentials, fileId, newReplica);
//            r.get();
//            r.freeBuffers();
//
//            forceFileCredentialsUpdate(translateMode("r"));
//
//            replicaOrder = replicaSelectionPolicy.getReplicaOrder(xLoc.getReplicas());
//        } else
//            throw new IOException("File is not marked as read-only.");
//    }
//
//    /**
//     * removes a replica for this file
//     *
//     * @param replica
//     * @throws Exception
//     */
//    public void removeReplica(Replica replica) throws Exception {
//        removeReplica(replica.getHeadOsd());
//    }
//
//    /**
//     * removes a replica for this file
//     *
//     * @param osd
//     * @throws Exception
//     */
//    public void removeReplica(ServiceUUID osd) throws Exception {
//        if (isReadOnly) {
//            if (fileCredentials.getXlocs().getReplicas().size() < 2)
//                throw new IOException("Cannot remove last replica.");
//
//            boolean otherCompleteReplicaExists = false;
//            if (xLoc.getReplica(osd).isComplete()) { // complete replica
//                // check if another replica is also complete
//                for (Replica r : xLoc.getReplicas())
//                    if (r.isComplete() && !r.equals(xLoc.getReplica(osd))) {
//                        otherCompleteReplicaExists = true;
//                        break;
//                    }
//                if (!otherCompleteReplicaExists)
//                    throw new IOException(
//                        "This is the last remaining COMPLETE replica. It cannot be removed,"
//                            + " otherwise it can happen that the file will be destroyed.");
//            }
//
//            RPCResponse<XCap> r = mrcClient.xtreemfs_replica_remove(mrcAddress, credentials, fileId, osd
//                    .toString());
//            XCap deleteCap = r.get();
//            r.freeBuffers();
//
//            RPCResponse r2 = osdClient.unlink(osd.getAddress(), fileId, new FileCredentials(deleteCap,
//                fileCredentials.getXlocs()));
//            r2.get();
//            r2.freeBuffers();
//
//            forceFileCredentialsUpdate(translateMode("r"));
//
//            replicaOrder = replicaSelectionPolicy.getReplicaOrder(xLoc.getReplicas());
//        } else
//            throw new IOException("File is not marked as read-only.");
//    }
//
//    /**
//     * removes "all" replicas, so that only one (the first) replica exists
//     */
//    public void removeAllReplicas() throws Exception {
//        List<Replica> replicas = xLoc.getReplicas();
//        for (int i = 1; i < replicas.size(); i++) {
//            removeReplica(replicas.get(i));
//        }
//    }
//
//    /**
//     * returns suitable OSDs which can be used for a replica of this file
//     *
//     * @return
//     * @throws Exception
//     */
//    public List<ServiceUUID> getSuitableOSDsForAReplica() throws Exception {
//
//        assert (xLoc.getNumReplicas() > 0);
//
//        RPCResponse<StringSet> r = mrcClient.xtreemfs_get_suitable_osds(mrcAddress, fileId, xLoc
//                .getReplica(0).getOSDs().size());
//        StringSet osds = r.get();
//        r.freeBuffers();
//
//        ArrayList<ServiceUUID> osdList = new ArrayList<ServiceUUID>();
//        for (String osd : osds) {
//            ServiceUUID uuid = new ServiceUUID(osd);
//            osdList.add(uuid);
//        }
//        return osdList;
//    }
//
//    public void setReplicaSelectionPolicy(ReplicaSelectionPolicy policy) {
//        replicaSelectionPolicy = policy;
//        replicaOrder = replicaSelectionPolicy.getReplicaOrder(xLoc.getReplicas());
//        stripingPolicy = replicaOrder.get(0).getStripingPolicy();
//    }
//
//    // useful for tests
//    public void changeReplicaOrder() {
//        replicaOrder = replicaSelectionPolicy.getReplicaOrder(xLoc.getReplicas());
//        stripingPolicy = replicaOrder.get(0).getStripingPolicy();
//    }
//
//    /**
//     * returns the StripingPolicy of the first replica (but at the moment it is
//     * the same for all replicas)
//     *
//     * @return
//     */
//    public StripingPolicy getStripingPolicy() {
//        return stripingPolicy.getPolicy();
//    }
//
//    /**
//     * returns the stripe size of used replica in bytes
//     *
//     * @return
//     */
//    public long getStripeSize() {
//        // the stripe size of a file is constant.
//        return stripingPolicy.getPolicy().getStripe_size();
//    }
//
//    public XLocations getXLoc() {
//        return xLoc;
//    }
//
//    public boolean isReadOnly() {
//        return isReadOnly;
//    }
//
//    public Replica getCurrentlyUsedReplica() {
//        return replicaOrder.get(0);
//    }
//
//    public long noOfObjects() throws Exception {
//        // all replicas have the same striping policy (more precisely the same
//        // stripesize) at the moment
//        return stripingPolicy.getObjectNoForOffset(length() - 1);
//    }
//
//    public String getFileId() {
//        return fileId;
//    }
//
//    public String getPath() {
//        return volName + "/" + pathName;
//    }
//
//    public void seek(long pos) {
//        filePos = pos;
//    }
//
//    public long getFilePointer() {
//        return filePos;
//    }
//
//    public String getStripingPolicyAsString() {
//        return fileCredentials.getXlocs().getReplicas().get(0).toString();
//    }
//
//    public FileCredentials getCredentials() {
//        return this.fileCredentials;
//    }
//
//    private void checkCap() throws IOException {
//
//        long time = System.currentTimeMillis();
//
//        if (time - capTime > (DEFAULT_CAP_VALIDITY - 60) * 1000) {
//            try {
//                forceXCapUpdate();
//            } catch (Exception e) {
//                throw new IOException(e);
//            }
//        }
//    }
//
//    private void forceXCapUpdate() throws IOException {
//        // update Xcap
//        try {
//            RPCResponse<XCap> r = mrcClient.xtreemfs_renew_capability(mrcAddress, fileCredentials.getXcap());
//            XCap cap = r.get();
//            r.freeBuffers();
//
//            fileCredentials.setXcap(cap);
//
//            capTime = System.currentTimeMillis();
//        } catch (Exception e) {
//            throw new IOException(e);
//        }
//    }
//
//    /**
//     * @throws ONCRPCException
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    private void forceFileCredentialsUpdate(int mode) throws ONCRPCException, IOException,
//        InterruptedException {
//        try {
//            RPCResponse<FileCredentials> r = mrcClient.open(mrcAddress, credentials, volName, pathName,
//                FileAccessManager.O_CREAT, mode, 0, new VivaldiCoordinates());
//            fileCredentials = r.get();
//            r.freeBuffers();
//            xLoc = new XLocations(fileCredentials.getXlocs(), null);
//            isReadOnly = fileCredentials.getXlocs().getReplica_update_policy().equals(
//                Constants.REPL_UPDATE_PC_RONLY);
//        } catch (InvalidXLocationsException ex) {
//            throw new IOException(ex);
//        }
//    }
//
//    public void forceFileSize(long newFileSize) throws IOException {
//        RPCResponse r = null;
//        try {
//            r = mrcClient.fsetattr(mrcAddress, fileCredentials.getXcap(), new Stat(0, 0, 0, 0, "", "",
//                newFileSize, 0, 0, 0, 0, 0, fileCredentials.getXcap().getTruncate_epoch(), 0),
//                MRCInterface.SETATTR_SIZE);
//            r.get();
//        } catch (ONCRPCException ex) {
//            throw new IOException("cannot update file size", ex);
//        } catch (InterruptedException ex) {
//            throw new IOException("cannot update file size", ex);
//        } finally {
//            if (r != null)
//                r.freeBuffers();
//        }
//    }
//
//    public Stat stat() throws IOException {
//        RPCResponse<StatSet> r = null;
//        try {
//            r = mrcClient.getattr(mrcAddress, credentials, volName, pathName);
//            return r.get().get(0);
//        } catch (ONCRPCException ex) {
//            throw new IOException("cannot update file size", ex);
//        } catch (InterruptedException ex) {
//            throw new IOException("cannot update file size", ex);
//        } finally {
//            if (r != null)
//                r.freeBuffers();
//        }
//    }
//
//    public NumberMonitoring getMonitoringInfo() {
//        return monitoring;
//    }
//}
