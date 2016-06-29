/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a path separated by '/' into multiple components.
 * 
 * @author stender
 * 
 */
public class Path {
    
    private static final char SEPARATOR = '/';
    
    private String            path;
    
    private List<Integer>     compIndices;


    public Path(String volumeName, String path) {
        if ((path.length() > 0) && (path.charAt(0) == '/')) {
            parsePath(volumeName+path);
        } else {
            parsePath(volumeName+"/"+path);
        }
    }

    public Path(String path) {
        parsePath(path);
    }

    private void parsePath(String path) {
        if (path.length() == 0) {
            this.path = "";
            this.compIndices = new ArrayList<Integer>(15);
            compIndices.add(-1);
        } else {

            while(path.contains("//")) {
                path = path.replace("//", "/");
            }

            this.path = path.charAt(path.length() - 1) == SEPARATOR ? path.substring(0,
                path.length() - 1) : path;
            this.compIndices = new ArrayList<Integer>(15);
            compIndices.add(-1);

            char[] chars = this.path.toCharArray();
            for (int i = 0; i < chars.length; i++)
                if (chars[i] == SEPARATOR)
                    compIndices.add(i);
        }
    }
    
    public Path(String[] comps) {
        
        this.compIndices = new ArrayList<Integer>(15);
        
        StringBuilder sb = new StringBuilder();
        int index = 0;
        
        for(String comp: comps) {
            compIndices.add(index);
            index += comp.length() + 1;
            sb.append("/"+ comp);
        }
        
        path = sb.toString();
    }
    
    public String getComp(int index) {
        
        if (index >= compIndices.size())
            return null;
        
        return path.substring(compIndices.get(index) + 1, index == compIndices.size() - 1 ? path
                .length() : compIndices.get(index + 1));
        
    }
    
    public String getLastComp(int index) {
        
        if (index >= compIndices.size())
            return null;
        
        return path.substring(compIndices.get(compIndices.size() - 1 - index) + 1,
            index == 0 ? path.length() : compIndices.get(compIndices.size() - index));
    }
    
    public String getComps(int startIndex, int endIndex) {
        
        if (endIndex < startIndex)
            return "";
        
        if (startIndex >= compIndices.size())
            startIndex = compIndices.size() - 1;
        
        if (endIndex < 0)
            endIndex = 0;
        
        return path.substring(compIndices.get(startIndex) + 1,
            endIndex == compIndices.size() - 1 ? path.length() : compIndices.get(endIndex + 1));
        
    }
    
    public boolean equals(Path p) {
        return path.equals(p.path);
    }
    
    public int getCompCount() {
        return compIndices.size();
    }
    
    public boolean isSubDirOf(Path p) {
        return path.startsWith(p.path + "/");
    }
    
    public String toString() {
        return path;
    }
    
//    public static void main(String[] args) {
//        Path path = new Path("myVolume/test/blub/bla.txt");
//        System.out.println(path);
//        System.out.println(path.getComp(0));
//        System.out.println(path.getLastComp(0));
//        System.out.println(path.getComp(1));
//        System.out.println(path.getLastComp(1));
//        System.out.println(path.getComps(1, 2));
//        System.out.println(path.getComp(path.getCompCount()));
//        System.out.println(path.getLastComp(path.getCompCount()));
//        System.out.println(path.getComps(0, 0));
//        System.out.println(path.getComps(1, 1));
//        System.out.println(path.getComps(2, 2));
//        System.out.println(path.getComps(3, 3));
//        System.out.println(path.getComps(5, -1));
//        System.out.println(path.getCompCount());
//    }
    
}
