/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common.clients.mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;

/**
 * A client for the MRC. Can be used as a generic client for all JSON-WP34-RPC
 * calls. Supports sync and async RPCs.
 *
 * @author bjko
 */
public class MRCClient extends RPCClient {

    /**
     * Creates a new instance of MRCClient
     *
     * @param debug
     *            if true speedy will generate debug messages
     * @throws java.io.IOException
     */
    public MRCClient(MultiSpeedy sharedSpeedy) throws IOException {
        super(sharedSpeedy);
    }

    public MRCClient(MultiSpeedy sharedSpeedy, int timeout) throws IOException {
        super(sharedSpeedy, timeout);
    }

    public MRCClient() throws IOException {
        this(null);
    }

    public MRCClient(int timeout, SSLOptions sslOptions) throws IOException {
        super(timeout, sslOptions);
    }

    public void setACLEntries(InetSocketAddress server, String path,
        Map<String, Object> entries, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "setACLEntries", RPCClient.generateList(path,
                entries), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void setXAttrs(InetSocketAddress server, String path,
        Map<String, Object> attrs, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "setXAttrs", RPCClient
                    .generateList(path, attrs), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void addReplica(InetSocketAddress server, String fileId,
        Map<String, Object> stripingPolicy, List<String> osdList,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "addReplica", RPCClient.generateList(fileId,
                stripingPolicy, osdList), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void removeReplica(InetSocketAddress server, String fileId,
        Map<String, Object> stripingPolicy, List<String> osdList,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "removeReplica", RPCClient.generateList(fileId,
                stripingPolicy, osdList), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void changeAccessMode(InetSocketAddress server, String path,
        long mode, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "changeAccessMode", RPCClient.generateList(
                path, mode), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void changeOwner(InetSocketAddress server, String path,
        String userId, String groupId, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "changeOwner", RPCClient.generateList(path,
                userId, groupId), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public boolean checkAccess(InetSocketAddress server, String path,
        String mode, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "checkAccess", RPCClient.generateList(path,
                mode), authString, null);
            return (Boolean) r.get();
        } finally {
            r.freeBuffers();
        }
    }
    
    /**
     * <p>The MRC makes an analyze stringPattern for the given list of fileIDs.</br>
     * example result: {001001111...}</br>
     * 1 means that file does exist, 0 if not and one single 2 will be returned, </br>
     * if the whole volume does not exist.</p>
     * 
     * @param server
     * @param volumeID
     * @param data
     * 
     * @return MRC Response
     * 
     * @throws JSONException 
     * @throws IOException 
     */
    public RPCResponse<String> checkFileList(InetSocketAddress server, String volumeID, 
        List<String> fileList, String authString) throws IOException, JSONException {
        
        RPCResponse<String> r = sendRPC(server, "checkFileList", 
                                        RPCClient.generateList(volumeID, fileList), 
                                        authString, null);
        return r;
    }

    public void createDir(InetSocketAddress server, String dirPath,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createDir", RPCClient.generateList(dirPath),
                authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void createDir(InetSocketAddress server, String dirPath,
        Map<String, String> attrs, long accessMode, String authString)
        throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createDir", RPCClient.generateList(dirPath,
                attrs, accessMode), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void createFile(InetSocketAddress server, String filePath,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createFile", RPCClient.generateList(filePath),
                authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public RPCResponse async_createFile(InetSocketAddress server, String filePath,
        String authString) throws Exception {

        RPCResponse r = null;
        return sendRPC(server, "createFile", RPCClient.generateList(filePath),
                authString, null);
    }

    public void createFile(InetSocketAddress server, String filePath,
        Map<String, Object> attrs, Map<String, Object> stripingPolicy,
        long accessMode, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createFile", RPCClient.generateList(filePath,
                attrs, stripingPolicy, accessMode), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, String> createFile(InetSocketAddress server,
        String filePath, Map<String, Object> attrs,
        Map<String, Object> stripingPolicy, long accessMode, boolean open,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createFile", RPCClient.generateList(filePath,
                attrs, stripingPolicy, accessMode, open), authString, null);

            return toXCapMap(r.getHeaders());
        } finally {
            r.freeBuffers();
        }
    }
    
    /**
     * Restore the MetaData for the given fileID.
     * 
     * @param server
     * @param filePath
     * @param fileID
     * @param fileSize
     * @param xAttrs
     * @param authString
     * @param osd
     * @param objectSize
     * @throws Exception
     */
    public void restoreFile(InetSocketAddress server, String filePath, long fileID, long fileSize, Map<String, Object> xAttrs, 
            String authString,String osd, long objectSize, String volumeID) 
    throws Exception {

            RPCResponse r = null;
            try {
                r = sendRPC(server, "restoreFile", RPCClient.generateList(filePath,
                    fileID, fileSize, xAttrs, osd, objectSize, volumeID), authString, null);
                r.waitForResponse();
            } finally {
                r.freeBuffers();
            }
        }

    public void createLink(InetSocketAddress server, String linkPath,
        String targetPath, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createLink", RPCClient.generateList(linkPath,
                targetPath), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void createSymbolicLink(InetSocketAddress server, String linkPath,
        String targetPath, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createSymbolicLink", RPCClient.generateList(
                linkPath, targetPath), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void createVolume(InetSocketAddress server, String volumeName,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createVolume", RPCClient
                    .generateList(volumeName), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void createVolume(InetSocketAddress server, String volumeName,
        long osdSelectionPolicyId, Map<String, Object> stripingPolicy,
        long acPolicyId, long partitioningPolicyId, Map<String, Object> acl,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "createVolume", RPCClient.generateList(
                volumeName, osdSelectionPolicyId, stripingPolicy, acPolicyId,
                partitioningPolicyId, acl), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void delete(InetSocketAddress server, String path, String authString)
        throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "delete", RPCClient.generateList(path),
                authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public RPCResponse async_delete(InetSocketAddress server, String path, String authString)
        throws Exception {

        return sendRPC(server, "delete", RPCClient.generateList(path),
                authString, null);
    }

    public void deleteVolume(InetSocketAddress server, String name,
        String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "deleteVolume", RPCClient.generateList(name),
                authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, String> getLocalVolumes(InetSocketAddress server,
        String authString) throws Exception {

        RPCResponse<Map<String, String>> r = null;
        try {
            r = sendRPC(server, "getLocalVolumes", RPCClient.generateList(),
                authString, null);
            return r.get();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, Object> getServerConfiguration(InetSocketAddress server,
        String authString) throws Exception {

        RPCResponse<Map<String, Object>> r = null;
        try {
            r = sendRPC(server, "getServerConfiguration", RPCClient.generateList(),
                authString, null);
            return r.get();
        } finally {
            r.freeBuffers();
        }
    }

    public void initFileSystem(InetSocketAddress server, String authString)
        throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "initFileSystem", RPCClient.generateList(),
                authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, String> move(InetSocketAddress server,
        String sourcePath, String targetPath, String authString)
        throws Exception {

        RPCResponse<Map<String, Object>> r = null;
        try {
            r = sendRPC(server, "move", RPCClient.generateList(sourcePath,
                targetPath), authString, null);
            return toXCapMap(r.getHeaders());
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, String> open(InetSocketAddress server, String path,
        String accessMode, String authString) throws Exception {

        RPCResponse<Map<String, String>> r = null;
        try {
            r = sendRPC(server, "open", RPCClient
                    .generateList(path, accessMode), authString, null);
            return toXCapMap(r.getHeaders());
        } finally {
            r.freeBuffers();
        }
    }

    public List<String> query(InetSocketAddress server, String path,
        String queryString, String authString) throws Exception {

        RPCResponse<List<String>> r = null;
        try {
            r = sendRPC(server, "query", RPCClient.generateList(path,
                queryString), authString, null);
            return r.get();
        } finally {
            r.freeBuffers();
        }
    }

    public List<String> readDir(InetSocketAddress server, String path,
        String authString) throws Exception {

        RPCResponse<List<String>> r = null;
        try {
            r = sendRPC(server, "readDir", RPCClient.generateList(path),
                authString, null);
            return r.get();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, Map<String, Object>> readDirAndStat(
        InetSocketAddress server, String path, String authString)
        throws Exception {

        RPCResponse<Map<String, Map<String, Object>>> r = null;
        try {
            r = sendRPC(server, "readDirAndStat", RPCClient.generateList(path),
                authString, null);
            return r.get();
        } finally {
            r.freeBuffers();
        }
    }

    public void removeACLEntries(InetSocketAddress server, String path,
        List<Object> entities, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "removeACLEntries", RPCClient.generateList(
                path, entities), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public void removeXAttrs(InetSocketAddress server, String path,
        List<String> attrKeys, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "removeXAttrs", RPCClient.generateList(path,
                attrKeys), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, String> renew(InetSocketAddress server,
        Map<String, String> capability, String authString) throws Exception {

        RPCResponse<Map<String, String>> r = null;
        try {
            r = sendRPC(server, "renew", RPCClient.generateList(), authString,
                toHTTPHeaders(capability));
            return toXCapMap(r.getHeaders());
        } finally {
            r.freeBuffers();
        }
    }

    public void updateFileSize(InetSocketAddress server, String capability,
        String newFileSizeHeader, String authString) throws Exception {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HTTPHeaders.HDR_XCAPABILITY, capability);
        headers.put(HTTPHeaders.HDR_XNEWFILESIZE, newFileSizeHeader);

        RPCResponse r = null;
        try {
            r = sendRPC(server, "updateFileSize", RPCClient.generateList(),
                authString, toHTTPHeaders(headers));
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, Object> stat(InetSocketAddress server, String path,
        boolean inclReplicas, boolean inclXAttrs, boolean inclACLs,
        String authString) throws Exception {

        RPCResponse<Map<String, Object>> r = null;
        try {
            r = sendRPC(server, "stat", RPCClient.generateList(path,
                inclReplicas, inclXAttrs, inclACLs), authString, null);
            return r.get();
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public String getXAttr(InetSocketAddress server, String path, String key,
        String authString) throws Exception {

        RPCResponse<String> r = null;
        try {
            r = sendRPC(server, "getXAttr", RPCClient.generateList(path, key),
                authString, null);
            return r.get();
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public void setDefaultStripingPolicy(InetSocketAddress server, String path,
        Map<String, Object> stripingPolicy, String authString) throws Exception {

        RPCResponse r = null;
        try {
            r = sendRPC(server, "setDefaultStripingPolicy", RPCClient
                    .generateList(path, stripingPolicy), authString, null);
            r.waitForResponse();
        } finally {
            r.freeBuffers();
        }
    }

    public Map<String, Object> getDefaultStripingPolicy(
        InetSocketAddress server, String path, String authString)
        throws Exception {

        RPCResponse<Map<String, Object>> r = null;
        try {
            r = sendRPC(server, "getDefaultStripingPolicy", RPCClient
                    .generateList(path), authString, null);
            return r.get();
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    public long getProtocolVersion(InetSocketAddress server, List versions,
        String authString) throws Exception {

        RPCResponse<Long> r = null;
        try {
            r = sendRPC(server, "getDefaultStripingPolicy", RPCClient
                    .generateList(versions), authString, null);
            return r.get();
        } finally {
            if (r != null)
                r.freeBuffers();
        }
    }

    private static HTTPHeaders toHTTPHeaders(Map<String, String> hdrs) {

        HTTPHeaders headers = new HTTPHeaders();
        for (String key : hdrs.keySet())
            headers.addHeader(key, hdrs.get(key));

        return headers;
    }

    private static Map<String, String> toXCapMap(HTTPHeaders hdrs) {

        Map<String, String> map = new HashMap<String, String>();

        if (hdrs.getHeader(HTTPHeaders.HDR_XCAPABILITY) != null)
            map.put(HTTPHeaders.HDR_XCAPABILITY, hdrs
                    .getHeader(HTTPHeaders.HDR_XCAPABILITY));
        if (hdrs.getHeader(HTTPHeaders.HDR_XLOCATIONS) != null)
            map.put(HTTPHeaders.HDR_XLOCATIONS, hdrs
                    .getHeader(HTTPHeaders.HDR_XLOCATIONS));

        return map;
    }

}
