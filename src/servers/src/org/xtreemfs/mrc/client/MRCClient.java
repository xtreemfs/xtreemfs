/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.client;

import java.net.InetSocketAddress;
import java.util.List;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.client.ONCRPCClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseDecoder;
import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.FileCredentialsSet;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StatVFS;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.Volume;
import org.xtreemfs.interfaces.VolumeSet;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.MRCInterface.accessRequest;
import org.xtreemfs.interfaces.MRCInterface.accessResponse;
import org.xtreemfs.interfaces.MRCInterface.chmodRequest;
import org.xtreemfs.interfaces.MRCInterface.chmodResponse;
import org.xtreemfs.interfaces.MRCInterface.chownRequest;
import org.xtreemfs.interfaces.MRCInterface.chownResponse;
import org.xtreemfs.interfaces.MRCInterface.creatRequest;
import org.xtreemfs.interfaces.MRCInterface.creatResponse;
import org.xtreemfs.interfaces.MRCInterface.ftruncateRequest;
import org.xtreemfs.interfaces.MRCInterface.ftruncateResponse;
import org.xtreemfs.interfaces.MRCInterface.getattrRequest;
import org.xtreemfs.interfaces.MRCInterface.getattrResponse;
import org.xtreemfs.interfaces.MRCInterface.getxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.getxattrResponse;
import org.xtreemfs.interfaces.MRCInterface.linkRequest;
import org.xtreemfs.interfaces.MRCInterface.linkResponse;
import org.xtreemfs.interfaces.MRCInterface.listxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.listxattrResponse;
import org.xtreemfs.interfaces.MRCInterface.mkdirRequest;
import org.xtreemfs.interfaces.MRCInterface.mkdirResponse;
import org.xtreemfs.interfaces.MRCInterface.openRequest;
import org.xtreemfs.interfaces.MRCInterface.openResponse;
import org.xtreemfs.interfaces.MRCInterface.readdirRequest;
import org.xtreemfs.interfaces.MRCInterface.readdirResponse;
import org.xtreemfs.interfaces.MRCInterface.removexattrRequest;
import org.xtreemfs.interfaces.MRCInterface.removexattrResponse;
import org.xtreemfs.interfaces.MRCInterface.renameRequest;
import org.xtreemfs.interfaces.MRCInterface.renameResponse;
import org.xtreemfs.interfaces.MRCInterface.rmdirRequest;
import org.xtreemfs.interfaces.MRCInterface.rmdirResponse;
import org.xtreemfs.interfaces.MRCInterface.setattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setattrResponse;
import org.xtreemfs.interfaces.MRCInterface.setxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setxattrResponse;
import org.xtreemfs.interfaces.MRCInterface.statvfsRequest;
import org.xtreemfs.interfaces.MRCInterface.statvfsResponse;
import org.xtreemfs.interfaces.MRCInterface.symlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.symlinkResponse;
import org.xtreemfs.interfaces.MRCInterface.unlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.unlinkResponse;
import org.xtreemfs.interfaces.MRCInterface.utimensRequest;
import org.xtreemfs.interfaces.MRCInterface.utimensResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_checkpointRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_checkpointResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_lsvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_lsvolResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_mkvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_mkvolResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_databaseResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_fileRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_fileResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_rmvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_rmvolResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_shutdownRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_shutdownResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeResponse;

/**
 * 
 * @author bjko
 */
public class MRCClient extends ONCRPCClient {
    
    public MRCClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer, 1, MRCInterface.getVersion());
    }
    
    /* admin calls */

    public RPCResponse xtreemfs_shutdown(InetSocketAddress server, UserCredentials credentials) {
        
        xtreemfs_shutdownRequest rq = new xtreemfs_shutdownRequest();
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_shutdownResponse resp = new xtreemfs_shutdownResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse xtreemfs_checkpoint(InetSocketAddress server, UserCredentials credentials) {
        
        xtreemfs_checkpointRequest rq = new xtreemfs_checkpointRequest();
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_checkpointResponse resp = new xtreemfs_checkpointResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse xtreemfs_dump_database(InetSocketAddress server, UserCredentials credentials,
        String dumpFile) {
        
        xtreemfs_dump_databaseRequest rq = new xtreemfs_dump_databaseRequest(dumpFile);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_dump_databaseResponse resp = new xtreemfs_dump_databaseResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse xtreemfs_restore_database(InetSocketAddress server, UserCredentials credentials,
        String dumpFile) {
        
        xtreemfs_restore_databaseRequest rq = new xtreemfs_restore_databaseRequest(dumpFile);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_restore_databaseResponse resp = new xtreemfs_restore_databaseResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    /* POSIX metadata calls */

    public RPCResponse<Boolean> access(InetSocketAddress server, UserCredentials credentials, String path,
        int mode) {
        
        accessRequest rq = new accessRequest(path, mode);
        RPCResponse<Boolean> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<Boolean>() {
            
            @Override
            public Boolean getResult(ReusableBuffer data) {
                final accessResponse resp = new accessResponse();
                resp.deserialize(data);
                return resp.getReturnValue();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse chmod(InetSocketAddress server, UserCredentials credentials, String path, int mode) {
        
        chmodRequest rq = new chmodRequest(path, mode);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final chmodResponse resp = new chmodResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse chown(InetSocketAddress server, UserCredentials credentials, String path,
        String newUID, String newGID) {
        
        chownRequest rq = new chownRequest(path, newUID, newGID);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final chownResponse resp = new chownResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse create(InetSocketAddress server, UserCredentials credentials, String path, int mode) {
        
        creatRequest rq = new creatRequest(path, mode);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final creatResponse resp = new creatResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<XCap> ftruncate(InetSocketAddress server, XCap writeCap) {
        
        ftruncateRequest rq = new ftruncateRequest(writeCap);
        RPCResponse<XCap> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<XCap>() {
            
            @Override
            public XCap getResult(ReusableBuffer data) {
                final ftruncateResponse resp = new ftruncateResponse();
                resp.deserialize(data);
                return resp.getTruncate_xcap();
            }
        });
        return r;
    }
    
    public RPCResponse<Stat> getattr(InetSocketAddress server, UserCredentials credentials, String path) {
        
        getattrRequest rq = new getattrRequest(path);
        RPCResponse<Stat> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<Stat>() {
            
            @Override
            public Stat getResult(ReusableBuffer data) {
                final getattrResponse resp = new getattrResponse();
                resp.deserialize(data);
                return resp.getStbuf();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<String> getxattr(InetSocketAddress server, UserCredentials credentials, String path,
        String name) {
        
        getxattrRequest rq = new getxattrRequest(path, name);
        RPCResponse<String> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<String>() {
            
            @Override
            public String getResult(ReusableBuffer data) {
                final getxattrResponse resp = new getxattrResponse();
                resp.deserialize(data);
                return resp.getValue();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse link(InetSocketAddress server, UserCredentials credentials, String targetPath,
        String linkPath) {
        
        linkRequest rq = new linkRequest(targetPath, linkPath);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final linkResponse resp = new linkResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<StringSet> listxattr(InetSocketAddress server, UserCredentials credentials, String path) {
        
        listxattrRequest rq = new listxattrRequest(path);
        RPCResponse<StringSet> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<StringSet>() {
            
            @Override
            public StringSet getResult(ReusableBuffer data) {
                final listxattrResponse resp = new listxattrResponse();
                resp.deserialize(data);
                return resp.getNames();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<VolumeSet> lsvol(InetSocketAddress server, UserCredentials credentials) {
        
        xtreemfs_lsvolRequest rq = new xtreemfs_lsvolRequest();
        RPCResponse<VolumeSet> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<VolumeSet>() {
            
            @Override
            public VolumeSet getResult(ReusableBuffer data) {
                final xtreemfs_lsvolResponse resp = new xtreemfs_lsvolResponse();
                resp.deserialize(data);
                return resp.getVolumes();
            }
        }, credentials);
        return r;
        
    }
    
    public RPCResponse mkdir(InetSocketAddress server, UserCredentials credentials, String path, int mode) {
        
        mkdirRequest rq = new mkdirRequest(path, mode);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final mkdirResponse resp = new mkdirResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse mkvol(InetSocketAddress server, UserCredentials credentials, String volumeName,
        int osdSelectionPolicy, StripingPolicy defaultStripingPolicy, int accessControlPolicy, int accessMode) {
        
        xtreemfs_mkvolRequest rq = new xtreemfs_mkvolRequest(new Volume(volumeName, accessMode,
            OSDSelectionPolicyType.parseInt(osdSelectionPolicy), defaultStripingPolicy,
            AccessControlPolicyType.parseInt(accessControlPolicy), "", "", ""));
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_mkvolResponse resp = new xtreemfs_mkvolResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<FileCredentials> open(InetSocketAddress server, UserCredentials credentials,
        String path, int flags, int mode, int w32attrs) {
        
        openRequest rq = new openRequest(path, flags, mode, w32attrs);
        RPCResponse<FileCredentials> r = sendRequest(server, rq.getTag(), rq,
            new RPCResponseDecoder<FileCredentials>() {
                
                @Override
                public FileCredentials getResult(ReusableBuffer data) {
                    final openResponse resp = new openResponse();
                    resp.deserialize(data);
                    return resp.getFile_credentials();
                }
            }, credentials);
        return r;
    }
    
    public RPCResponse<DirectoryEntrySet> readdir(InetSocketAddress server, UserCredentials credentials,
        String path) {
        
        readdirRequest rq = new readdirRequest(path);
        RPCResponse<DirectoryEntrySet> r = sendRequest(server, rq.getTag(), rq,
            new RPCResponseDecoder<DirectoryEntrySet>() {
                
                @Override
                public DirectoryEntrySet getResult(ReusableBuffer data) {
                    final readdirResponse resp = new readdirResponse();
                    resp.deserialize(data);
                    return resp.getDirectory_entries();
                }
            }, credentials);
        return r;
    }
    
    public RPCResponse removexattr(InetSocketAddress server, UserCredentials credentials, String path,
        String name) {
        
        removexattrRequest rq = new removexattrRequest(path, name);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final removexattrResponse resp = new removexattrResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<FileCredentialsSet> rename(InetSocketAddress server, UserCredentials credentials,
        String sourcePath, String targetPath) {
        
        renameRequest rq = new renameRequest(sourcePath, targetPath);
        RPCResponse<FileCredentialsSet> r = sendRequest(server, rq.getTag(), rq,
            new RPCResponseDecoder<FileCredentialsSet>() {
                
                @Override
                public FileCredentialsSet getResult(ReusableBuffer data) {
                    final renameResponse resp = new renameResponse();
                    resp.deserialize(data);
                    return resp.getFile_credentials();
                }
            }, credentials);
        return r;
    }
    
    public RPCResponse rmdir(InetSocketAddress server, UserCredentials credentials, String path) {
        
        rmdirRequest rq = new rmdirRequest(path);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final rmdirResponse resp = new rmdirResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse rmvol(InetSocketAddress server, UserCredentials credentials, String volumeName) {
        
        xtreemfs_rmvolRequest rq = new xtreemfs_rmvolRequest(volumeName);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_rmvolResponse resp = new xtreemfs_rmvolResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse setattr(InetSocketAddress server, UserCredentials credentials, String path,
        Stat statInfo) {
        
        setattrRequest rq = new setattrRequest(path, statInfo);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final setattrResponse resp = new setattrResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse setxattr(InetSocketAddress server, UserCredentials credentials, String path,
        String name, String value, int flags) {
        
        setxattrRequest rq = new setxattrRequest(path, name, value, flags);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final setxattrResponse resp = new setxattrResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<StatVFS> statfs(InetSocketAddress server, UserCredentials credentials,
        String volumeName) {
        
        statvfsRequest rq = new statvfsRequest(volumeName);
        RPCResponse<StatVFS> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<StatVFS>() {
            
            @Override
            public StatVFS getResult(ReusableBuffer data) {
                final statvfsResponse resp = new statvfsResponse();
                resp.deserialize(data);
                return resp.getStbuf();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse symlink(InetSocketAddress server, UserCredentials credentials, String targetPath,
        String linkPath) {
        
        symlinkRequest rq = new symlinkRequest(targetPath, linkPath);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final symlinkResponse resp = new symlinkResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<FileCredentialsSet> unlink(InetSocketAddress server, UserCredentials credentials,
        String path) {
        
        unlinkRequest rq = new unlinkRequest(path);
        RPCResponse<FileCredentialsSet> r = sendRequest(server, rq.getTag(), rq,
            new RPCResponseDecoder<FileCredentialsSet>() {
                
                @Override
                public FileCredentialsSet getResult(ReusableBuffer data) {
                    final unlinkResponse resp = new unlinkResponse();
                    resp.deserialize(data);
                    return resp.getFile_credentials();
                }
            }, credentials);
        return r;
    }
    
    public RPCResponse utime(InetSocketAddress server, UserCredentials credentials, String path, long atime,
        long ctime, long mtime) {
        
        utimensRequest rq = new utimensRequest(path, atime, ctime, mtime);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final utimensResponse resp = new utimensResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    /* xtreemfs-specific calls */

    public RPCResponse<String> xtreemfs_checkFileExists(InetSocketAddress server, String volumeId,
        StringSet fileIds, String osdUUID) {
        
        xtreemfs_check_file_existsRequest rq = new xtreemfs_check_file_existsRequest(volumeId, fileIds, osdUUID);
        RPCResponse<String> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<String>() {
            
            @Override
            public String getResult(ReusableBuffer data) {
                final xtreemfs_check_file_existsResponse resp = new xtreemfs_check_file_existsResponse();
                resp.deserialize(data);
                return resp.getBitmap();
            }
        });
        return r;
    }
    
    public RPCResponse<String> xtreemfs_internal_debug(InetSocketAddress server, UserCredentials creds, String cmd) {
        
        xtreemfs_internal_debugRequest rq = new xtreemfs_internal_debugRequest(cmd);
        RPCResponse<String> r = sendRequest(server, rq.getTag(), rq,
            new RPCResponseDecoder<String>() {
                
                @Override
                public String getResult(ReusableBuffer data) {
                    final xtreemfs_internal_debugResponse resp = new xtreemfs_internal_debugResponse();
                    resp.deserialize(data);
                    return resp.getResult();
                }
            },creds);
        return r;
    }
    
    public RPCResponse<StringSet> xtreemfs_get_suitable_osds(InetSocketAddress server, String fileId) {
        
        xtreemfs_get_suitable_osdsRequest rq = new xtreemfs_get_suitable_osdsRequest(fileId);
        RPCResponse<StringSet> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<StringSet>() {
            
            @Override
            public StringSet getResult(ReusableBuffer data) {
                final xtreemfs_get_suitable_osdsResponse resp = new xtreemfs_get_suitable_osdsResponse();
                resp.deserialize(data);
                return resp.getOsd_uuids();
            }
        });
        return r;
    }
    
    public RPCResponse<XCap> xtreemfs_renew_capability(InetSocketAddress server, XCap capability) {
        
        xtreemfs_renew_capabilityRequest rq = new xtreemfs_renew_capabilityRequest(capability);
        RPCResponse<XCap> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder<XCap>() {
            
            @Override
            public XCap getResult(ReusableBuffer data) {
                final xtreemfs_renew_capabilityResponse resp = new xtreemfs_renew_capabilityResponse();
                resp.deserialize(data);
                return resp.getRenewed_xcap();
            }
        });
        return r;
    }
    
    public RPCResponse xtreemfs_replica_add(InetSocketAddress server, UserCredentials credentials,
        String fileId, Replica newReplica) {
        
        xtreemfs_replica_addRequest rq = new xtreemfs_replica_addRequest(fileId, newReplica);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_replica_addResponse resp = new xtreemfs_replica_addResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse<XCap> xtreemfs_replica_remove(InetSocketAddress server, UserCredentials credentials,
        String fileId, String osdUUID) {
        
        xtreemfs_replica_removeRequest rq = new xtreemfs_replica_removeRequest(fileId, osdUUID);
        RPCResponse<XCap> r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_replica_removeResponse resp = new xtreemfs_replica_removeResponse();
                resp.deserialize(data);
                return resp.getDelete_xcap();
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse xtreemfs_restore_file(InetSocketAddress server, String filePath, String fileId,
        long fileSizeInBytes, String osdUUID, int stripeSizeInKB, UserCredentials credentials) {
        
        xtreemfs_restore_fileRequest rq = new xtreemfs_restore_fileRequest(filePath, fileId, fileSizeInBytes,
            osdUUID, stripeSizeInKB);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_restore_fileResponse resp = new xtreemfs_restore_fileResponse();
                resp.deserialize(data);
                return null;
            }
        }, credentials);
        return r;
    }
    
    public RPCResponse xtreemfs_update_file_size(InetSocketAddress server, XCap xcap,
        OSDWriteResponse newFileSize) {
        
        xtreemfs_update_file_sizeRequest rq = new xtreemfs_update_file_sizeRequest(xcap, newFileSize);
        RPCResponse r = sendRequest(server, rq.getTag(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_update_file_sizeResponse resp = new xtreemfs_update_file_sizeResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public static UserCredentials getCredentials(String uid, List<String> gids) {
        
        StringSet gidsAsSet = new StringSet();
        for (String gid : gids)
            gidsAsSet.add(gid);
        
        return new UserCredentials(uid, gidsAsSet, "");
    }
    
}
