/**
 *
 */
package org.xtreemfs.common.clients.scrubber;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.foundation.pinky.SSLOptions;

public class VolumeWalker {
        
    private InetSocketAddress mrcAddress;
    private MRCClient mrcClient;
    private String authString;
    private LinkedList<String> files;
    private LinkedList<String> dirs;
    public HashMap<String, Integer> elementsInDir;
    private int noFilesToFetch;

    public VolumeWalker(String volumeName, InetSocketAddress mrcAddress, 
            int noFilesToFetch, String authString, SSLOptions ssl) throws Exception{
        this.mrcAddress = mrcAddress;
        if (ssl != null) {
            mrcClient = new MRCClient(MRCClient.DEFAULT_TIMEOUT, ssl);
        } else {
            mrcClient = new MRCClient();
        }
        this.noFilesToFetch = noFilesToFetch;
        this.authString = authString;
        dirs = new LinkedList<String>();
        dirs.add(volumeName);
        files = new LinkedList<String>();
        elementsInDir = new HashMap<String, Integer>();
    }
/**
 * Adds files and directories from the volume to the lists files and dirs.
 * The directories are traversed using depth first search.
 * @throws Exception thrown by readDirAndStat
 */
    private void getMoreFiles() throws Exception {
        while(!dirs.isEmpty() && files.size() < noFilesToFetch){
            String dir = dirs.removeFirst();
            Map<String, Map<String, Object>> dirsAndFiles = 
                mrcClient.readDirAndStat(mrcAddress, dir, authString);
            if(dirsAndFiles.isEmpty()){
                long latestScrub = System.currentTimeMillis();
                setLatestScrubOfDir(dir, latestScrub);
                fileOrDirScrubbed(dir, latestScrub);
            }
            else
                elementsInDir.put(dir, dirsAndFiles.size());
            for(String path : dirsAndFiles.keySet()){
                String type = dirsAndFiles.get(path).get("objType").toString();
                //if file
                if(type.equals("1")){
                    files.add(dir + "/" + path);
                }
                //if directory
                if(type.equals("2")){
                    dirs.add(dir + "/" + path);
                }
            }
        }
    }

    /**
     * 
     * @return 
     * @throws Exception thrown by getMoreFiles.
     */
    public boolean hasNext() throws Exception {

        if(!files.isEmpty())
            return true;
        else if(dirs.isEmpty())
            return false;
        else{
            getMoreFiles();
            return hasNext();
        }
    }

    public String removeNextFile(){
        return files.removeLast();
    }

    public void setLatestScrubOfDir(String path, long time) throws Exception {
        Map<String,Object> newXAttr = new HashMap<String,Object>();
        newXAttr.put(AsyncScrubber.latestScrubAttr, time);
        mrcClient.setXAttrs(mrcAddress, path, newXAttr, authString);
    }
    /**
     * @ TODO: currently sets a directories xattr to the largest time of its 
     *         entries. should use the minimum though.
     */
    public void fileOrDirScrubbed(String path, long time) throws Exception {
        String dir = getParentDir(path);
        if(dir != null){
            int noOfUnscrubbedElements = elementsInDir.get(dir)-1;
            elementsInDir.put(dir, noOfUnscrubbedElements);
            if(noOfUnscrubbedElements == 0){
                setLatestScrubOfDir(dir, time);
                fileOrDirScrubbed(dir, time);
            }
        }
        
    }
    
    public String getParentDir(String path) {
        int lastIndex = path.lastIndexOf('/');
        if(lastIndex != -1)
            return path.substring(0, lastIndex);
        else
            return null;
    }
    public void shutdown() {
        mrcClient.shutdown();
        mrcClient.waitForShutdown();
    }

}