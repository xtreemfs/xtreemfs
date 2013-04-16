/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.pbrpc.client.RPCAuthentication.authNone;

import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * @author jensvfischer
 */
public class DefaultParams {
    static final int  KiB_IN_BYTES          = 1024;
    static final long MiB_IN_BYTES          = 1024 * 1024;
    static final long GiB_IN_BYTES          = 1024 * 1024 * 1024;
    static int        numberOfThreads       = 1;
    static int        numberOfRepetitions   = 1;
    static long       sequentialSizeInBytes = 10L * MiB_IN_BYTES;
    static long       randomSizeInBytes     = 10L * MiB_IN_BYTES;
    static long       basefileSizeInBytes   = 3L * GiB_IN_BYTES;
    static int        randomIOFilesize      = 4 * KiB_IN_BYTES;
    static String     userName              = "root";
    static String     group                 = "root";
    static String     osdPassword           = "";
    static String     dirAddress            = "127.0.0.1:32638";
    static RPC.Auth   auth                  = authNone;
    static SSLOptions sslOptions            = null;
    static Options    options               = new Options();
    static int        stripeSizeInBytes     = 128 * KiB_IN_BYTES;
    static int        stripeWidth           = 1;
    static boolean    noCleanup             = false;
    static boolean    noCleanupOfVolumes    = false;
    static boolean    isNoCleanupOfBasefile = false;
}
