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
import org.xtreemfs.interfaces.Context;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.FileCredentialsSet;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.StripingPolicy;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.stat_;
import org.xtreemfs.interfaces.statfs_;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.admin_checkpointRequest;
import org.xtreemfs.interfaces.DIRInterface.admin_checkpointResponse;
import org.xtreemfs.interfaces.DIRInterface.admin_shutdownRequest;
import org.xtreemfs.interfaces.DIRInterface.admin_shutdownResponse;
import org.xtreemfs.interfaces.MRCInterface.accessRequest;
import org.xtreemfs.interfaces.MRCInterface.accessResponse;
import org.xtreemfs.interfaces.MRCInterface.admin_dump_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.admin_dump_databaseResponse;
import org.xtreemfs.interfaces.MRCInterface.admin_restore_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.admin_restore_databaseResponse;
import org.xtreemfs.interfaces.MRCInterface.chmodRequest;
import org.xtreemfs.interfaces.MRCInterface.chmodResponse;
import org.xtreemfs.interfaces.MRCInterface.chownRequest;
import org.xtreemfs.interfaces.MRCInterface.chownResponse;
import org.xtreemfs.interfaces.MRCInterface.createRequest;
import org.xtreemfs.interfaces.MRCInterface.createResponse;
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
import org.xtreemfs.interfaces.MRCInterface.mkvolRequest;
import org.xtreemfs.interfaces.MRCInterface.mkvolResponse;
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
import org.xtreemfs.interfaces.MRCInterface.rmvolRequest;
import org.xtreemfs.interfaces.MRCInterface.rmvolResponse;
import org.xtreemfs.interfaces.MRCInterface.setattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setattrResponse;
import org.xtreemfs.interfaces.MRCInterface.setxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setxattrResponse;
import org.xtreemfs.interfaces.MRCInterface.statfsRequest;
import org.xtreemfs.interfaces.MRCInterface.statfsResponse;
import org.xtreemfs.interfaces.MRCInterface.symlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.symlinkResponse;
import org.xtreemfs.interfaces.MRCInterface.unlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.unlinkResponse;
import org.xtreemfs.interfaces.MRCInterface.utimeRequest;
import org.xtreemfs.interfaces.MRCInterface.utimeResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_fileRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_fileResponse;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_update_file_sizeResponse;

/**
 * 
 * @author bjko
 */
public class MRCClient extends ONCRPCClient {
    
    public MRCClient(RPCNIOSocketClient client, InetSocketAddress defaultServer) {
        super(client, defaultServer, 1, DIRInterface.getVersion());
    }
    
    /* admin calls */

    public RPCResponse admin_shutdown(InetSocketAddress server, String password) {
        
        admin_shutdownRequest rq = new admin_shutdownRequest(password);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final admin_shutdownResponse resp = new admin_shutdownResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse admin_checkpoint(InetSocketAddress server, String password) {
        
        admin_checkpointRequest rq = new admin_checkpointRequest(password);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final admin_checkpointResponse resp = new admin_checkpointResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse admin_dump_database(InetSocketAddress server, String password, String dumpFile) {
        
        admin_dump_databaseRequest rq = new admin_dump_databaseRequest(password, dumpFile);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final admin_dump_databaseResponse resp = new admin_dump_databaseResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse admin_restore_database(InetSocketAddress server, String password, String dumpFile) {
        
        admin_restore_databaseRequest rq = new admin_restore_databaseRequest(password, dumpFile);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final admin_restore_databaseResponse resp = new admin_restore_databaseResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    /* POSIX metadata calls */

    public RPCResponse<Boolean> access(InetSocketAddress server, String uid, List<String> gids, String path,
        int mode) {
        
        accessRequest rq = new accessRequest(toContext(uid, gids), path, mode);
        RPCResponse<Boolean> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<Boolean>() {
                
                @Override
                public Boolean getResult(ReusableBuffer data) {
                    final accessResponse resp = new accessResponse();
                    resp.deserialize(data);
                    return resp.getReturnValue();
                }
            });
        return r;
    }
    
    public RPCResponse chmod(InetSocketAddress server, String uid, List<String> gids, String path, int mode) {
        
        chmodRequest rq = new chmodRequest(toContext(uid, gids), path, mode);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final chmodResponse resp = new chmodResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse chown(InetSocketAddress server, String uid, List<String> gids, String path,
        String newUID, String newGID) {
        
        chownRequest rq = new chownRequest(toContext(uid, gids), path, newUID, newGID);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final chownResponse resp = new chownResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse create(InetSocketAddress server, String uid, List<String> gids, String path, int mode) {
        
        createRequest rq = new createRequest(toContext(uid, gids), path, mode);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final createResponse resp = new createResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<stat_> getattr(InetSocketAddress server, String uid, List<String> gids, String path) {
        
        getattrRequest rq = new getattrRequest(toContext(uid, gids), path);
        RPCResponse<stat_> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<stat_>() {
                
                @Override
                public stat_ getResult(ReusableBuffer data) {
                    final getattrResponse resp = new getattrResponse();
                    resp.deserialize(data);
                    return resp.getStbuf();
                }
            });
        return r;
    }
    
    public RPCResponse<String> getxattr(InetSocketAddress server, String uid, List<String> gids, String path,
        String name) {
        
        getxattrRequest rq = new getxattrRequest(toContext(uid, gids), path, name);
        RPCResponse<String> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<String>() {
                
                @Override
                public String getResult(ReusableBuffer data) {
                    final getxattrResponse resp = new getxattrResponse();
                    resp.deserialize(data);
                    return resp.getReturnValue();
                }
            });
        return r;
    }
    
    public RPCResponse link(InetSocketAddress server, String uid, List<String> gids, String targetPath,
        String linkPath) {
        
        linkRequest rq = new linkRequest(toContext(uid, gids), targetPath, linkPath);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final linkResponse resp = new linkResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<StringSet> listxattr(InetSocketAddress server, String uid, List<String> gids,
        String path) {
        
        listxattrRequest rq = new listxattrRequest(toContext(uid, gids), path);
        RPCResponse<StringSet> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<StringSet>() {
                
                @Override
                public StringSet getResult(ReusableBuffer data) {
                    final listxattrResponse resp = new listxattrResponse();
                    resp.deserialize(data);
                    return resp.getNames();
                }
            });
        return r;
    }
    
    public RPCResponse mkdir(InetSocketAddress server, String uid, List<String> gids, String path, int mode) {
        
        mkdirRequest rq = new mkdirRequest(toContext(uid, gids), path, mode);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final mkdirResponse resp = new mkdirResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse mkvol(InetSocketAddress server, String uid, List<String> gids, String password,
        String volumeName, int osdSelectionPolicy, StripingPolicy defaultStripingPolicy,
        int accessControlPolicy) {
        
        mkvolRequest rq = new mkvolRequest(toContext(uid, gids), password, volumeName, osdSelectionPolicy,
            defaultStripingPolicy, accessControlPolicy);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final mkvolResponse resp = new mkvolResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<FileCredentials> open(InetSocketAddress server, String uid, List<String> gids,
        String path, int flags, int mode) {
        
        openRequest rq = new openRequest(toContext(uid, gids), path, flags, mode);
        RPCResponse<FileCredentials> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<FileCredentials>() {
                
                @Override
                public FileCredentials getResult(ReusableBuffer data) {
                    final openResponse resp = new openResponse();
                    resp.deserialize(data);
                    return resp.getCredentials();
                }
            });
        return r;
    }
    
    public RPCResponse<DirectoryEntrySet> readdir(InetSocketAddress server, String uid, List<String> gids,
        String path) {
        
        readdirRequest rq = new readdirRequest(toContext(uid, gids), path);
        RPCResponse<DirectoryEntrySet> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<DirectoryEntrySet>() {
                
                @Override
                public DirectoryEntrySet getResult(ReusableBuffer data) {
                    final readdirResponse resp = new readdirResponse();
                    resp.deserialize(data);
                    return resp.getDirectory_entries();
                }
            });
        return r;
    }
    
    public RPCResponse removexattr(InetSocketAddress server, String uid, List<String> gids, String path,
        String name) {
        
        removexattrRequest rq = new removexattrRequest(toContext(uid, gids), path, name);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final removexattrResponse resp = new removexattrResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<FileCredentialsSet> rename(InetSocketAddress server, String uid, List<String> gids,
        String sourcePath, String targetPath) {
        
        renameRequest rq = new renameRequest(toContext(uid, gids), sourcePath, targetPath);
        RPCResponse<FileCredentialsSet> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<FileCredentialsSet>() {
                
                @Override
                public FileCredentialsSet getResult(ReusableBuffer data) {
                    final renameResponse resp = new renameResponse();
                    resp.deserialize(data);
                    return resp.getCredentials();
                }
            });
        return r;
    }
    
    public RPCResponse rmdir(InetSocketAddress server, String uid, List<String> gids, String path) {
        
        rmdirRequest rq = new rmdirRequest(toContext(uid, gids), path);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final rmdirResponse resp = new rmdirResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse rmvol(InetSocketAddress server, String uid, List<String> gids, String password,
        String volumeName) {
        
        rmvolRequest rq = new rmvolRequest(toContext(uid, gids), password, volumeName);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final rmvolResponse resp = new rmvolResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse setattr(InetSocketAddress server, String uid, List<String> gids, String path,
        stat_ statInfo) {
        
        setattrRequest rq = new setattrRequest(toContext(uid, gids), path, statInfo);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final setattrResponse resp = new setattrResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse setxattr(InetSocketAddress server, String uid, List<String> gids, String path,
        String name, String value, int flags) {
        
        setxattrRequest rq = new setxattrRequest(toContext(uid, gids), path, name, value, flags);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final setxattrResponse resp = new setxattrResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<statfs_> statfs(InetSocketAddress server, String uid, List<String> gids,
        String volumeName) {
        
        statfsRequest rq = new statfsRequest(toContext(uid, gids), volumeName);
        RPCResponse<statfs_> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<statfs_>() {
                
                @Override
                public statfs_ getResult(ReusableBuffer data) {
                    final statfsResponse resp = new statfsResponse();
                    resp.deserialize(data);
                    return resp.getStatfsbuf();
                }
            });
        return r;
    }
    
    public RPCResponse symlink(InetSocketAddress server, String uid, List<String> gids, String targetPath,
        String linkPath) {
        
        symlinkRequest rq = new symlinkRequest(toContext(uid, gids), targetPath, linkPath);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final symlinkResponse resp = new symlinkResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse<FileCredentialsSet> unlink(InetSocketAddress server, String uid, List<String> gids,
        String path) {
        
        unlinkRequest rq = new unlinkRequest(toContext(uid, gids), path);
        RPCResponse<FileCredentialsSet> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<FileCredentialsSet>() {
                
                @Override
                public FileCredentialsSet getResult(ReusableBuffer data) {
                    final unlinkResponse resp = new unlinkResponse();
                    resp.deserialize(data);
                    return resp.getCredentials();
                }
            });
        return r;
    }
    
    public RPCResponse utime(InetSocketAddress server, String uid, List<String> gids, String path,
        long atime, long ctime, long mtime) {
        
        utimeRequest rq = new utimeRequest(toContext(uid, gids), path, atime, ctime, mtime);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final utimeResponse resp = new utimeResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    /* xtreemfs-specific calls */

    public RPCResponse<String> xtreemfs_checkFileExists(InetSocketAddress server, String volumeId,
        StringSet fileIds) {
        
        xtreemfs_check_file_existsRequest rq = new xtreemfs_check_file_existsRequest(volumeId, fileIds);
        RPCResponse<String> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<String>() {
                
                @Override
                public String getResult(ReusableBuffer data) {
                    final xtreemfs_check_file_existsResponse resp = new xtreemfs_check_file_existsResponse();
                    resp.deserialize(data);
                    return resp.getBitmap();
                }
            });
        return r;
    }
    
    public RPCResponse<StringSet> xtreemfs_get_suitable_osds(InetSocketAddress server, String fileId) {
        
        xtreemfs_get_suitable_osdsRequest rq = new xtreemfs_get_suitable_osdsRequest(fileId);
        RPCResponse<StringSet> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<StringSet>() {
                
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
        RPCResponse<XCap> r = sendRequest(server, rq.getOperationNumber(), rq,
            new RPCResponseDecoder<XCap>() {
                
                @Override
                public XCap getResult(ReusableBuffer data) {
                    final xtreemfs_renew_capabilityResponse resp = new xtreemfs_renew_capabilityResponse();
                    resp.deserialize(data);
                    return resp.getXcap();
                }
            });
        return r;
    }
    
    public RPCResponse xtreemfs_replica_add(InetSocketAddress server, String uid, List<String> gids,
        String fileId, Replica newReplica) {
        
        xtreemfs_replica_addRequest rq = new xtreemfs_replica_addRequest(toContext(uid, gids), fileId,
            newReplica);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_replica_addResponse resp = new xtreemfs_replica_addResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse xtreemfs_replica_remove(InetSocketAddress server, String uid, List<String> gids,
        String fileId, String osdUUID) {
        
        xtreemfs_replica_removeRequest rq = new xtreemfs_replica_removeRequest(toContext(uid, gids), fileId,
            osdUUID);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_replica_addResponse resp = new xtreemfs_replica_addResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse xtreemfs_restore_file(InetSocketAddress server, String filePath, String fileId,
        int fileSize, String osdUUID, int stripeSize) {
        
        xtreemfs_restore_fileRequest rq = new xtreemfs_restore_fileRequest(filePath, fileId, fileSize,
            osdUUID, stripeSize);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_restore_fileResponse resp = new xtreemfs_restore_fileResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    public RPCResponse xtreemfs_update_file_size(InetSocketAddress server, XCap xcap,
        OSDWriteResponse newFileSize) {
        
        xtreemfs_update_file_sizeRequest rq = new xtreemfs_update_file_sizeRequest(xcap, newFileSize);
        RPCResponse r = sendRequest(server, rq.getOperationNumber(), rq, new RPCResponseDecoder() {
            
            @Override
            public Object getResult(ReusableBuffer data) {
                final xtreemfs_update_file_sizeResponse resp = new xtreemfs_update_file_sizeResponse();
                resp.deserialize(data);
                return null;
            }
        });
        return r;
    }
    
    private static Context toContext(String uid, List<String> gids) {
        
        StringSet gidsAsSet = new StringSet();
        for (String gid : gids)
            gidsAsSet.add(gid);
        
        return new Context(uid, gidsAsSet);
    }
    
}
