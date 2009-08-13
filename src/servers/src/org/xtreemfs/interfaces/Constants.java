package org.xtreemfs.interfaces;


public interface Constants
{
    public static final String ONCRPC_SCHEME = "oncrpc";
    public static final String ONCRPCS_SCHEME = "oncrpcs";
    public static final String ONCRPCU_SCHEME = "oncrpcu";
    public static final int ONCRPC_AUTH_FLAVOR = 1326;
    public static final String REPL_UPDATE_PC_NONE = "";
    public static final String REPL_UPDATE_PC_RONLY = "ronly";
    public static final int REPL_FLAG_IS_COMPLETE = 0x0001;
    public static final int REPL_FLAG_FULL_REPLICA = 0x0002;
    public static final int REPL_FLAG_STRATEGY_SEQUENTIAL = 0x0004;
    public static final int REPL_FLAG_STRATEGY_RANDOM = 0x0008;
    public static final int REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING = 0x000C;
    public static final int REPL_FLAG_STRATEGY_RAREST_FIRST = 0x0010;
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
};
