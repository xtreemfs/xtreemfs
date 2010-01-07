package org.xtreemfs.interfaces;


public interface Constants
{
    public static final int ONCRPC_AUTH_FLAVOR = 1326;
    public static final String ONCRPC_SCHEME = "oncrpc";
    public static final String ONCRPCG_SCHEME = "oncrpcg";
    public static final String ONCRPCS_SCHEME = "oncrpcs";
    public static final String ONCRPCU_SCHEME = "oncrpcu";
    public static final String REPL_UPDATE_PC_NONE = "";
    public static final String REPL_UPDATE_PC_RONLY = "ronly";
    public static final int REPL_FLAG_FULL_REPLICA = 1;
    public static final int REPL_FLAG_IS_COMPLETE = 2;
    public static final int REPL_FLAG_STRATEGY_RANDOM = 4;
    public static final int REPL_FLAG_STRATEGY_RAREST_FIRST = 8;
    public static final int REPL_FLAG_STRATEGY_SEQUENTIAL = 16;
    public static final int REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING = 32;
    public static final int SYSTEM_V_FCNTL_H_O_RDONLY = 0x0000;
    public static final int SYSTEM_V_FCNTL_H_O_WRONLY = 0x0001;
    public static final int SYSTEM_V_FCNTL_H_O_RDWR = 0x0002;
    public static final int SYSTEM_V_FCNTL_H_O_APPEND = 0x0008;
    public static final int SYSTEM_V_FCNTL_H_O_CREAT = 0x0100;
    public static final int SYSTEM_V_FCNTL_H_O_TRUNC = 0x0200;
    public static final int SYSTEM_V_FCNTL_H_O_EXCL = 0x0400;
    public static final int SYSTEM_V_FCNTL_H_O_SYNC = 0x0010;
    public static final int SYSTEM_V_FCNTL_H_S_IFREG = 0x8000;
    public static final int SYSTEM_V_FCNTL_H_S_IFDIR = 0x4000;
    public static final int SYSTEM_V_FCNTL_H_S_IFLNK = 0xA000;
    public static final int XCAP_EXPIRE_TIMEOUT_S_MIN = 30;
    public static final int SERVICE_STATUS_AVAIL = 0;
    public static final int SERVICE_STATUS_TO_BE_REMOVED = 1;
    public static final int SERVICE_STATUS_REMOVED = 2;
};
