/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox;

import java.io.IOException;
import java.util.Properties;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.RealSingleFileStorageLayout;
import org.xtreemfs.osd.storage.SingleFileStorageLayout;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

/**
 *
 * @author bjko
 */
public class BenchmarkStorageLayouts {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            int objs = (args.length > 0) ? Integer.valueOf(args[0]) : 1024;
            String path = (args.length > 1) ? args[1] : "/tmp";
            int objSize = (args.length > 2) ? Integer.valueOf(args[2]) : 128;
            Logging.start(Logging.LEVEL_ERROR, Category.all);

            System.out.println("press enter after flushing caches: echo 3 > /proc/sys/vm/drop_caches");
            System.in.read();

            SingleFileStorageLayout sfl = new SingleFileStorageLayout(new OSDConfig(createOSDProperties(path+"/sleval_single/")), new MetadataCache());
            HashStorageLayout hsl = new HashStorageLayout(new OSDConfig(createOSDProperties(path+"/sleval_hash/")), new MetadataCache());
            RealSingleFileStorageLayout rsl = new RealSingleFileStorageLayout(new OSDConfig(createOSDProperties(path+"/sleval_real/")), new MetadataCache());

            /*write(hsl,objSize,objs);

            System.out.println("press enter after flushing caches: echo 3 > /proc/sys/vm/drop_caches");
            System.in.read();

            write(sfl,objSize,objs);*/

            System.out.println("press enter after flushing caches: echo 3 > /proc/sys/vm/drop_caches");
            System.in.read();

            write(rsl,objSize,objs);
            

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void write(StorageLayout layout, int objSize, int numObjs) throws IOException {

        System.out.println("testing: "+layout.getClass().getCanonicalName());

        String fileId = "ABCDEF:123";
        Replica r = Replica.newBuilder().setReplicationFlags(0).setStripingPolicy(StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setWidth(1).setStripeSize(objSize)).build();
        StripingPolicyImpl sp = StripingPolicyImpl.getPolicy(r, 0);

        FileMetadata md = layout.getFileMetadata(sp, fileId);

        ReusableBuffer buf = BufferPool.allocate(objSize*1024);
        while (buf.hasRemaining()) {
            buf.put((byte) 'A');
        }
        buf.flip();

        long tStart = System.currentTimeMillis();

        for (int i = 0; i < numObjs; i++) {
            layout.writeObject(fileId, md, buf.createViewBuffer(), i, 0, 1, false, false);
            buf.position(0);
        }

        long tEnd = System.currentTimeMillis();

        layout.closeFile(md);

        System.out.println("write: " + (tEnd - tStart) + " ms");

        md = layout.getFileMetadata(sp, fileId);

        tStart = System.currentTimeMillis();

        for (int i = 0; i < numObjs; i++) {
            ObjectInformation oinfo = layout.readObject(fileId, md, i, 0, StorageLayout.FULL_OBJECT_LENGTH, md.getLatestObjectVersion(i));
            if (oinfo.getData() != null)
                BufferPool.free(oinfo.getData());
        }

        tEnd = System.currentTimeMillis();

        System.out.println("read : " + (tEnd - tStart) + " ms");

        layout.closeFile(md);

    }

    private static Properties createOSDProperties(String dir) {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", dir);
        props.setProperty("debug.level", "" + 5);
        props.setProperty("debug.categories", "all");
        props.setProperty("listen.port", "3333");
        props.setProperty("http_port", "3334");
        props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "0");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "false");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", "false");
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", "aygga");
        return props;
    }
}
