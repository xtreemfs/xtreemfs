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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 
 * @author bjko
 */
public class utils {
    
    public static Map<String, String> getxattrs(String filename) throws IOException, InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "getfattr", "-m", "xtreemfs.*", "-d", f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0)
            return null;
        
        Map<String, String> result = new HashMap<String, String>();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        br.readLine(); // skip first line
        
        for (;;) {
            String nextLine = br.readLine();
            if (nextLine == null)
                break;
            StringTokenizer st = new StringTokenizer(nextLine, "=");
            if (!st.hasMoreElements())
                continue;
            
            String key = st.nextToken();
            String value = st.nextToken();
            
            // remove leading and trailing quotes
            value = value.substring(1, value.length() - 1);
            value = value.replace("\\\"", "\"");
            
            result.put(key, value);
        }
        
        return result;
    }
    
    public static String getxattr(String filename, String attrname) throws IOException, InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "getfattr", "--only-values", "-n", attrname, f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0)
            return null;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String target = br.readLine();
        
        return target;
    }
    
    public static void setxattr(String filename, String attrname, String attrvalue) throws IOException,
        InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "setfattr", "-n", attrname, "-v", attrvalue, f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0)
            throw new IOException("a problem occurred when setting '" + attrname + "': " + p.exitValue());
    }
    
    public static String expandPath(String path) {
        File f = new File(path);
        return f.getAbsolutePath();
    }
    
    public static boolean isXtreemFSDir(String path) throws IOException, InterruptedException {
        String url = getxattr(path, "xtreemfs.url");
        return url != null;
    }
    
    public static String findXtreemFSRootDir(String path) throws IOException, InterruptedException {
        
        path = expandPath(path);
        
        String url = getxattr(path, "xtreemfs.url");
        if (url == null)
            return null;
        
        String parentDir = path.substring(0, path.lastIndexOf(File.separator));
        String xtfsParent = findXtreemFSRootDir(parentDir);
        if (xtfsParent == null)
            return path;
        else
            return xtfsParent;
    }
    
}
