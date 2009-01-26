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

package org.xtreemfs.new_mrc.operations;

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
    
    public Path(String path) {
        
        this.path = path.charAt(path.length() - 1) == SEPARATOR ? path.substring(0,
            path.length() - 1) : path;
        this.compIndices = new ArrayList<Integer>(15);
        compIndices.add(-1);
        
        byte[] bytes = this.path.getBytes();
        for (int i = 0; i < bytes.length; i++)
            if (bytes[i] == SEPARATOR)
                compIndices.add(i);
        
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
    
    public static void main(String[] args) {
        Path path = new Path("myVolume/test/blub/bla.txt");
        System.out.println(path);
        System.out.println(path.getComp(0));
        System.out.println(path.getLastComp(0));
        System.out.println(path.getComp(1));
        System.out.println(path.getLastComp(1));
        System.out.println(path.getComps(1, 2));
        System.out.println(path.getComp(path.getCompCount()));
        System.out.println(path.getLastComp(path.getCompCount()));
        System.out.println(path.getComps(0, 0));
        System.out.println(path.getComps(1, 1));
        System.out.println(path.getComps(2, 2));
        System.out.println(path.getComps(3, 3));
        System.out.println(path.getComps(5, -1));
        System.out.println(path.getCompCount());
    }
    
}
