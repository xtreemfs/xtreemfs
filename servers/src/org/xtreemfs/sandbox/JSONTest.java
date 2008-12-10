/*
 * JSONTest.java
 *
 * Created on December 15, 2006, 1:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xtreemfs.sandbox;

import java.io.FileNotFoundException;
import java.io.FileReader;
import org.xtreemfs.foundation.json.JSONException;

import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

/**
 * Simple (manual) test for the JSON parser.
 * 
 * @author bjko
 */
public class JSONTest {

    /** Creates a new instance of JSONTest */
    public JSONTest() {
    }

    public static void stupido() {
        stupido();
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        JSONString sr = null;
        try {
            // TODO code application logic here
            FileReader fr = new FileReader("json.txt");
            int ch = fr.read();
            StringBuilder in = new StringBuilder();
            while (ch != -1) {
                if ((ch != '\r') && (ch != '\n'))
                    in.append((char) ch);
                ch = fr.read();
            }
            fr.close();

            long tStart = System.currentTimeMillis();
            sr = new JSONString(in.toString());
            Object o = JSONParser.parseJSON(sr);
            long tEnd = System.currentTimeMillis();
            System.out.println("parsing took " + (tEnd - tStart) + "ms");
            System.out.println("");
            System.out.println(JSONParser.writeJSON(o));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            try {
                char ch = sr.read();
                System.out.print(ch);
                while (sr.hasMore()) {
                    ch = sr.read();
                    System.out.print(ch);                
                }
            } catch(JSONException jex) {                
            }           
            
            ex.printStackTrace();
        } catch (StackOverflowError e) {
            System.out.println("Stack Overflow");
            e.printStackTrace();
        }

    }

}
