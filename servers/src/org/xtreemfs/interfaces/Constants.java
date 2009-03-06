package org.xtreemfs.interfaces;


public interface Constants
{
    public static final int STRIPING_POLICY_DEFAULT = 0;
    public static final int STRIPING_POLICY_RAID0 = 1;
    public static final int ACCESS_CONTROL_POLICY_NULL = 1;
    public static final int ACCESS_CONTROL_POLICY_POSIX = 2;
    public static final int ACCESS_CONTROL_POLICY_VOLUME = 3;
    public static final int OSD_SELECTION_POLICY_SIMPLE = 1;
    public static final int SERVICE_TYPE_MRC = 1;
    public static final int SERVICE_TYPE_OSD = 2;
    public static final int SERVICE_TYPE_VOLUME = 3;
    public static final String REPL_UPDATE_PC_NONE = "";
    public static final String REPL_UPDATE_PC_RONLY = "ronly";
};
