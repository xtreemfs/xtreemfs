/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion
 and Consiglio Nazionale delle Ricerche.

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
 * AUTHOR: Felix Langner (ZIB)
 */
package org.xtreemfs.common.clients.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class models a {@link Thread} safe FileMap for listing all available files of an OSD for
 * service purpose like cleaning deleted files up.</p>
 * 
 * <p>The first key is the volume as a {@link List} of volumeID, mrcAddress and mrcPort, the second
 * key is the fileID and the third key some file attributes like fileSize and preview.</p> 
 * 
 * @author langner
 */
public final class ConcurrentFileMap {
    private static final long serialVersionUID = -7736474666790682726L;
    
    Map<Volume,Map<String,Map<String,String>>> map = new ConcurrentHashMap<Volume, Map<String, Map<String,String>>>();
    
    /**
     * Default constructor.
     */
    public ConcurrentFileMap() {
        super();
    }
    
    /**
     * Parses a JSON response into a {@link ConcurrentFileMap}.
     * @param map
     */
    public ConcurrentFileMap(Map<String,Map<String,Map<String,String>>> map) {
        super();       
        for (String key : map.keySet()){
            this.map.put(Volume.parse(key) , map.get(key));
        }    
    }
    
    /**
     * <p>Inserts a volumeID,fileID pair given by a directory's name into the fileMap.
     * And adds the attributes size and preview to the entry.</p>
     * 
     * @param directory hex name (volumeID:fileID)
     * @param size
     * @param preview
     */
    public synchronized void insert(String directory, long size, String preview, long maxObjectSize) throws IOException{
        String[] fileDesc = directory.split(":");
        if (fileDesc.length==2){
            try{
                Integer.parseInt(fileDesc[1]);
                
                Volume newVol = new Volume (fileDesc[0]);
        
                if (fileDesc.length!=2) throw new IOException("Directory: '"+directory+"' has an illegal format!");
        
                if(containsKey(newVol))
                    addFile(directory,size,maxObjectSize,preview,(Map<String, Map<String, String>>) get(newVol));
                else
                    map.put(newVol, newVolFile(directory,size,maxObjectSize,preview)); 
            }catch(NumberFormatException ne){
                // ignore
            }
        }
    }
    
    /**
     * 
     * @return set of unresolved volumeIDs.
     */
    public Set<String> unresolvedVolumeIDSet() {
        Set<String> result = new HashSet<String>();
        
        Set<Volume> keys = map.keySet();
        for(Volume key : keys){
            if (key.size()==1) result.add(key.get(0));
        }
        return result;
    }
    
    /**
     * @return set of resolved volumeIDs.
     */
    public Set<String> resolvedVolumeIDSet() {
        Set<String> result = new HashSet<String>();
        
        Set<Volume> keys = map.keySet();
        for(Volume key : keys){
            if (key.size()>1 || key.equals(Volume.NOT_AVAILABLE)) result.add(key.get(0));
        }
        return result;
    }
    
    /**
     * @return set of resolved volumeIDs without not available volume.
     */
    public Set<String> volumeIDSetForRequest() {
        Set<String> result = new HashSet<String>();
        
        Set<Volume> keys = map.keySet();
        for(Volume key : keys){
            if (key.size()>1 && !key.equals(Volume.NOT_AVAILABLE)) result.add(key.get(0));
        }
        return result;
    }
    
    /**
     * <p>Replaces the entry with the given volumeID with a new one with address.</p>
     * 
     * <p>If address is <code>null</code> volume is marked as 'unknown'.
     * 
     * @param volumeID
     * @param address
     */
    public synchronized void saveAddress(String volumeID, InetSocketAddress address) {
        if (address!=null)
            map.put(new Volume(volumeID,address.getHostName(),((Integer) address.getPort()).toString()), remove(new Volume(volumeID)));
        else{
            if (containsKey(Volume.NOT_AVAILABLE))
                ((Map<String, Map<String, String>>) get(Volume.NOT_AVAILABLE)).putAll((Map<String, Map<String, String>>) remove(new Volume(volumeID)));
            else
                map.put(Volume.NOT_AVAILABLE, remove(new Volume(volumeID)));
        }
    }

    /**
     * 
     * @param volumeID
     * @return the address for the given volumeID, or null if not available.
     */
    public InetSocketAddress getAddress(String volumeID) {
        Volume predicate = new Volume(volumeID);
        
        Set<Volume> keys = map.keySet();
        for (Volume key : keys){
            if (predicate.equals(Volume.NOT_AVAILABLE))
                return null;
            else if (key.equals(predicate))
                return new InetSocketAddress(key.get(1),Integer.parseInt(key.get(2)));
        }
            
        return null;
    }
    
    /**
     * 
     * @param volume
     * @return a {@link Set} of fileIDs for the given volume.
     */
    public Set<String> getFileNumberSet(List<String> volume) {
        Set<String> result = new HashSet<String>();
        for (String fID : getFileIDSet(volume)){
            result.add(fID.substring(fID.indexOf(":")+1, fID.length()));
        }
        return result;
    }
    
    /**
     * 
     * @param volume
     * @return a {@link Set} of fileIDs for the given volumeID.
     */
    public Set<String> getFileNumberSet(String volumeID) {
        Set<String> result = new HashSet<String>();
        for (String fID : getFileIDSet(volumeID)){
            result.add(fID.substring(fID.indexOf(":")+1, fID.length()));
        }
        return result;
    }
    
    /**
     * 
     * @param volumeID
     * @return a {@link List} of fileNumbers for the given volumeID.
     */
    public List<String> getFileNumbers(String volumeID) {
        List<String> result = new LinkedList<String>();
        for (String fID : getFileIDs(volumeID)){
            result.add(fID.substring(fID.indexOf(":")+1, fID.length()));
        }
        return result;
    }
    
    /**
     * 
     * @param volume
     * @return a {@link Set} of fileIDs for the given volume.
     */
    public Set<String> getFileIDSet(List<String> volume) {
        return ((Map<String, Map<String, String>>) get(volume)).keySet();
    }
    
    /**
     * 
     * @param volumeID
     * @return a {@link Set} of fileIDs for the given volumeID.
     */
    public Set<String> getFileIDSet(String volumeID) {
        return ((Map<String, Map<String, String>>) get(volumeID)).keySet();
    }
    
    /**
     * 
     * @return the fileMap JSON compatible.
     */
    public Map<String, Map<String, Map<String,String>>> getJSONCompatible (){
        Map<String, Map<String, Map<String,String>>> result = new ConcurrentHashMap<String, Map<String,Map<String,String>>>();
        for (Volume key : map.keySet()){
            result.put(key.toString(), get(key));
        }
        return result;
    }  

    /**
     * Removes a file given by volumeID and fileID from the fileMap.
     * @param volumeID
     * @param fileID
     */
    public void remove(String volumeID, String fileID) {
        ((Map<String,Map<String,String>>) get(new Volume(volumeID))).remove(fileID);
        
    }
    
    /**
     * 
     * @return the number of fileIDs in the fileMap.
     */
    public synchronized int size(){
       int result = 0;
       
       for (Volume key : ((Set<Volume>) map.keySet())){
           result += ((Map<String,Map<String,String>>) get(key)).size();
       }
       
       return result;
    }
 
/*
 * getter    
 */
    public Long getFileSize(String volumeID, String file) { 
        return Long.valueOf(get(volumeID).get(file).get("size"));
    }
    
    public Long getFileSize(List<String> volume, String file) { 
        return Long.valueOf(get(volume).get(file).get("size"));
    } 
    
    public String getFilePreview(List<String> volume, String file) { 
        return get(volume).get(file).get("preview");
    } 
    
    public Long getObjectSize(List<String> volume, String file) {
        return Long.valueOf(get(volume).get(file).get("objectSize"));
    }

/*
 * override    
 */
    
    public Set<List<String>> keySetList() {
        Set<List<String>> result = new HashSet<List<String>>();
        for (Volume v: map.keySet()){
            result.add(v);
        }
        
        return result;
    }
    
    public boolean containsKey(Object key) {
        for (Volume thisKey : map.keySet()){
            if(thisKey.equals(key)){
                return true;
            }
        }
        return false;
    }

    public Map<String, Map<String, String>> remove(Object key) {
        Volume rq = null;
        for (Volume thisKey : map.keySet()){
            if(thisKey.get(0).equals(key) || thisKey.equals(key)){
                rq = thisKey;
                break;
            } 
        }
        return map.remove(rq);
    }
    
    public Map<String, Map<String, String>> get(Object key) {     
        for (Volume thisKey : map.keySet()){
            if(thisKey.get(0).equals(key) || thisKey.equals(key)){
                key = thisKey;
                break;
            }
        }
        return map.get(key);
    }
    
    /**
     * 
     * @return true, if there are any fileIDs saved in the map.false otherwise.
     */
    public boolean isEmpty() {
        if (!map.isEmpty()){
            boolean isEmpty = true;
            for (Map<String, Map<String, String>> value : map.values())
                isEmpty &= value.isEmpty();       
            return isEmpty;
        }
        return  true;
    }
/*
 * private methods
 */
    
    /**
     * 
     * @param volumeID
     * @return a {@link List} of fileIDs for the given volumeID.
     */
    private List<String> getFileIDs(String volumeID) {
        List<String> result = new LinkedList<String>();
        for (String fID : getFileIDSet(volumeID))
            result.add(fID);
            
        return result;
    }
    
    /**
     * 
     * @param size
     * @param objectSize
     * @param preview
     * @return a new Map with the given file details in it.
     */
    private Map<String,String> fileDetails (Long size,Long objectSize,String preview){
        ConcurrentHashMap<String, String> details = new ConcurrentHashMap<String, String>();
        details.put("size", size.toString());
        details.put("objectSize", objectSize.toString());
        details.put("preview", preview);
        
        return details;
    }
    
    /**
     * 
     * @param fileID
     * @param size
     * @param preview
     * @param objectSize
     * @return a new Map with the fileID with the given details in it.
     */
    private Map<String, Map<String,String>> newVolFile (String fileID,long size,long objectSize,String preview){
        Map<String, Map<String,String>> volFile = new ConcurrentHashMap<String,Map<String,String>>();
        
        volFile.put(fileID, fileDetails(size,objectSize,preview));
        
        return volFile;
    }
    
    /**
     * Put the fileID and the file details into the given map.
     * 
     * @param fileID
     * @param size
     * @param preview
     * @param objectSize
     * @param map
     */
    private void addFile(String fileID,long size,long objectSize,String preview, Map<String, Map<String,String>> map){    
        map.put(fileID, fileDetails (size,objectSize,preview));
    }
}

    /**
     * <p>Volume is a {@link List} of volumeID, mrcAddress and mrcPort.</p>
     * <p>It will just be compared by the first value in the List (the volumeID).</p>
     * 
     * @author langner
     *
     */
    class Volume extends LinkedList<String> implements List<String>{   
        private static final long serialVersionUID = 7408578018651016089L;
    
        public Volume(String volID) {
            super();
            add(volID);
        }

        public Volume(String volID, String mrcAddress, String mrcPort) {
            super();
            add(volID);
            add(mrcAddress);
            add(mrcPort);
        }
        
        private Volume(){
            super();
            add("unknown");
            add("unknown");
            add("unknown");
        }
      
        @Override
        public boolean equals(Object o) {
            if (o instanceof String){
                return get(0).equals(o);
            }else if (o instanceof Volume){
                return get(0).equals(((Volume) o).get(0));
            }
            return false;           
        }
           
        public static Volume NOT_AVAILABLE = new Volume();
        
        public static Volume parse(String key) {
            Volume result = null;
            String[] values = key.split(",");
            for (int i=0;i<values.length;i++){
                values[i] = removeBrackets(values[i]);
                if (i==0) result = new Volume(values[i]);
                else result.add(values[i]);
            }        
            return result;
        }
        
        private static String removeBrackets(String str){
            String result = "";
            for (int i= 0;i<str.length(); i++){
                if (str.charAt(i)!='[' && str.charAt(i)!=']' && str.charAt(i)!=' ')
                    result += str.charAt(i);
            }
            return result;
        }
    }