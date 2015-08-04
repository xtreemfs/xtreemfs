/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.quota;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.UserException;

/**
 * This class manages all voucher requested affairs and if necessary, it delegates them to reference classes.
 * 
 * TODO: This class is currently thread-safe, but because the MRC has only one operational thread, there is no need for
 * this. --- really? Open & Auth vs. e.g. FileSizeUpdater or something like that
 */
public class MRCVoucherManager {

    private final MRCQuotaManager                 mrcQuotaManager;

    // TODO: split by volume UUID, if possible
    // maps the file id to a file voucher manager
    private final Map<String, FileVoucherManager> fileVoucherMap = new HashMap<String, FileVoucherManager>();

    /**
     * 
     */
    public MRCVoucherManager(MRCQuotaManager mrcQuotaManager) {
        this.mrcQuotaManager = mrcQuotaManager;
    }

    /**
     * 
     * @param volumeId
     * @param fileID
     * @param clientID
     * @param fileSize
     * @param expireTime
     * @return voucher size
     * @throws UserException
     *             iff an error occured at getting the voucher
     */
    public long getVoucher(String volumeId, String fileID, String clientID, long fileSize, long expireTime)
            throws UserException {

        System.out.println(getClass() + " getVoucher: " + fileID); // FIXME(remove)

        long newMaxFileSize = fileSize;

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(volumeId)) {

            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(volumeId);
            long voucherSize = volumeQuotaManager.getVoucher();

            System.out.println(getClass() + " VoucherSize: " + voucherSize); // FIXME(remove)


            synchronized (fileVoucherMap) {
                FileVoucherManager fileVoucherManager = fileVoucherMap.get(fileID);
                if (fileVoucherManager == null) {
                    fileVoucherManager = new FileVoucherManager(volumeQuotaManager, fileID, fileSize);
                    fileVoucherMap.put(fileID, fileVoucherManager);
                }
                newMaxFileSize = fileVoucherManager.addVoucher(clientID, expireTime, voucherSize);
            }

        }

        return newMaxFileSize;
    }

    public void checkVoucherAvailability(String volumeId) throws UserException {

        if (mrcQuotaManager.hasActiveVolumeQuotaManager(volumeId)) {

            VolumeQuotaManager volumeQuotaManager = mrcQuotaManager.getVolumeQuotaManagerById(volumeId);
            volumeQuotaManager.checkVoucherAvailability();
        }
    }

    public void clearVouchers(String fileID, String clientID, long fileSize, Set<Long> expireTimes) {

        System.out.println(getClass() + " clearVoucher: " + fileID); // FIXME(remove)

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(fileID);

            if (fileVoucherManager != null) {
                fileVoucherManager.clearVouchers(clientID, fileSize, expireTimes);

                if (fileVoucherManager.isCleared()) {
                    fileVoucherMap.remove(fileID);
                }
            } else {
                System.out.println("ERROR - FIXME given");
                // FIXME: throw exception? --> tell the user, that the voucher couldn't be cleared and that theres
                // blocked space regarding the quota (or just a WARNING?)
            }
        }
    }

    /**
     * Checks whether there is an entry for the fileID, clientID and oldExpireTime and iff so, it get's a new voucher
     * for the newExpireTime
     * 
     * @param fileID
     * @param clientID
     * @param oldExpireTime
     * @param newExpireTime
     * @return the new voucher size
     * @throws UserException
     *             if parameter couldn't be found or if no new voucher could be acquired
     */
    public long checkAndRenewVoucher(String fileID, String clientID, long oldExpireTime, long newExpireTime)
            throws UserException {

        long voucherSize = 0;

        synchronized (fileVoucherMap) {
            FileVoucherManager fileVoucherManager = fileVoucherMap.get(fileID);

            try{

                if (fileVoucherManager != null) {
                    voucherSize = fileVoucherManager.checkAndRenewVoucher(clientID, oldExpireTime, newExpireTime);
                } else {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "No open voucher for fileID " + fileID);
                }
            }catch(UserException userException){
                if(userException.getErrno() == POSIXErrno.POSIX_ERROR_EINVAL){
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "Cap with expire time " + oldExpireTime
                            + " has already been cleared!");                
                }else{
                    // unexpected Error
                    throw userException;
                }
            }
        }

        return voucherSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MRCVoucherManager [mrcQuotaManager=").append(mrcQuotaManager).append(", fileVoucherMap=")
                .append(fileVoucherMap).append("]");
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        // FIXME(baerhold): Export to test
        // VolumeQuotaManager volumeQuotaManager = new VolumeQuotaManager("blubb", 150 * 1024 * 1024);
        
        MRCQuotaManager mrcQuotaManager_ = new MRCQuotaManager();
        
        // mrcQuotaManager_.addVolumeQuotaManager(volumeQuotaManager);
        
        
        MRCVoucherManager mrcVoucherManager = new MRCVoucherManager(mrcQuotaManager_);
        
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 123456789));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client2", 0, 1234567890));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 1234567891));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file2", "client1", 0, 1234567892));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file2", "client1", 10, 1234567893));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file3", "client1", 0, 1234567894));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file4", "client1", 0, 1234567895));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client1", 0, 1234567896));
        System.out.println(mrcVoucherManager.getVoucher("blubb", "file1", "client2", 0, 1234567897));

        System.out.println(mrcVoucherManager.toString());
        System.out.println(mrcQuotaManager_.toString());

        // curBlockedSpace: 47158920 -> file blocked space 1-4: (26214400, 10485760, 5242880, 5242880)

        mrcVoucherManager.clearVouchers("file4", "client1", 3 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567895))));
        mrcVoucherManager.clearVouchers("file2", "client1", 5 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567893))));
        mrcVoucherManager.clearVouchers("file3", "client1", 1 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567894))));
        mrcVoucherManager.clearVouchers("file2", "client1", 7 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567892))));
        mrcVoucherManager.clearVouchers("file1", "client1", 4 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567891))));
        mrcVoucherManager.clearVouchers("file1", "client2", 8 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(1234567897), new Long(1234567890))));
        mrcVoucherManager.clearVouchers("file1", "client1", 20 * 1024 * 1024,
                new HashSet<Long>(Arrays.asList(new Long(123456789), new Long(1234567896))));

        System.out.println(mrcVoucherManager.toString());
        System.out.println(mrcQuotaManager_.toString());

        // blockSpace = 0
        // curVolSpace = 32505856
    }
}
