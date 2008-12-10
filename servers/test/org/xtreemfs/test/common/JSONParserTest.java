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

package org.xtreemfs.test.common;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.json.JSONCharBufferString;

import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.test.SetupUtils;

public class JSONParserTest extends TestCase {

    protected void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
    }

    protected void tearDown() {
    }

    public void testPrimitiveEncode() throws Exception {

        String json = JSONParser.writeJSON(1711);
        assertEquals(json, "1711");

        json = JSONParser.writeJSON(-12);
        assertEquals(json, "-12");

        json = JSONParser.writeJSON("this is a string");
        assertEquals(json, "\"this is a string\"");

        json = JSONParser.writeJSON(null);
        assertEquals(json, "null");

        json = JSONParser.writeJSON(true);
        assertEquals(json, "true");

        json = JSONParser.writeJSON(false);
        assertEquals(json, "false");
    }

    public void testPrimitiveDecode() throws Exception {

        int i = ((Long) JSONParser.parseJSON(new JSONString("17132")))
                .intValue();
        assertEquals(i, 17132);

        int i2 = ((Long) JSONParser.parseJSON(new JSONString("-7327")))
                .intValue();
        assertEquals(i2, -7327);

        String s = (String) JSONParser.parseJSON(new JSONString(
                "\"blubberbla\""));
        assertEquals(s, "blubberbla");

        s = (String) JSONParser.parseJSON(new JSONString(
                "\"\\\\\""));
        assertEquals(s, "\\");

        boolean b = (Boolean) JSONParser.parseJSON(new JSONString("true"));
        assertTrue(b);
    }

    public void testMapEncode() throws Exception {

        Map<String, Integer> someMap = new HashMap<String, Integer>();
        someMap.put("bla", 438);
        someMap.put("blub", -321);

        String s = JSONParser.writeJSON(someMap);
        assertEquals(s.charAt(0), '{');
        assertEquals(s.charAt(s.length() - 1), '}');
        assertTrue(s.indexOf("\"bla\":438") != -1);
        assertTrue(s.indexOf("\"blub\":-321") != -1);
    }

    public void testMapDecode() throws Exception {

        String json = "{\"foo\":\"bar\",\"bla\":482,\"4\":-3}";
        Map map = (Map) JSONParser.parseJSON(new JSONString(json));
        assertEquals(map.get("foo"), "bar");
        assertEquals(((Long) map.get("bla")).intValue(), 482);
        assertEquals(map.get("4"), new Long(-3));
        assertEquals(map.size(), 3);
    }

    public void testListEncode() throws Exception {

        List list = new ArrayList();
        list.add("bla");
        list.add(327);
        list.add(-5);
        list.add(false);
        list.add("blub");

        String s = JSONParser.writeJSON(list);
        assertEquals(s, "[\"bla\",327,-5,false,\"blub\"]");
    }

    public void testListDecode() throws Exception {

        String json = "[\"bla\",32,-3]";
        List l = (List) JSONParser.parseJSON(new JSONString(json));
        assertEquals(l.get(0), "bla");
        assertEquals(((Long) l.get(1)).intValue(), 32);
        assertEquals(((Long) l.get(2)).intValue(), -3);
        assertEquals(l.size(), 3);

    }

    public void testComplexEncode() throws Exception {

        ArrayList nestedList = new ArrayList();
        ArrayList nestedList2 = new ArrayList();
        nestedList2.add("bla");
        nestedList2.add(32);
        nestedList2.add(-12);
        nestedList2.add("blub");

        Map map = new HashMap();
        map.put(32, "blub");
        map.put("bla", nestedList);
        map.put("bar", nestedList2);
        map.put("test", false);

        // encode
        List list = new ArrayList();
        list.add(map);
        list.add(12);
        list.add("blub");
        list.add(new ArrayList());
        list.add(nestedList2);

        String json = JSONParser.writeJSON(list);

        // String l2Str = "[\"bla\",32,-12,\"blub\"]";
        // assertEquals(json, "[{32:\"blub\",\"bla\":[],\"bar\":" + l2Str +
        // ",\"test\":false},12,\"blub\",[]," + l2Str + "]");
    }

    public void testComplexDecode() throws Exception {

        String json = "[43,\"bla\",[],{},[43,{}],{\"43\":32,\"ertz\":{}}]";
        List l = (List) JSONParser.parseJSON(new JSONString(json));
        assertEquals(((Long) l.get(0)).intValue(), 43);
        assertTrue(((List) l.get(2)).isEmpty());
        assertTrue(((Map) l.get(3)).isEmpty());
        assertEquals(((Map) l.get(5)).size(), 2);

        json = "[[{\"atime\":1169209166,\"isDirectory\":false,\"userId\":1,\"name\":\"test.txt\",\"mtime\":1169209166,\"ctime\":1169209166,\"size\":0},[{\"userId\":1,\"value\":\"ertz\",\"type\":1,\"key\":\"ref\"}],[],[]],\"testVolume2\\/newTest.txt\"]";
        l = (List) JSONParser.parseJSON(new JSONString(json));
        assertEquals(((Map) ((List) l.get(0)).get(0)).get("isDirectory"), false);
    }

    public void testCharBufferInput() throws Exception {

        String json = "[43,\"bla\",[],{},[43,{}],{\"43\":32,\"ertz\":{}}]";
        ByteBuffer bb = ByteBuffer.wrap(json.getBytes(HTTPUtils.ENC_UTF8));
        CharBuffer cb = HTTPUtils.ENC_UTF8.decode(bb);
        List l = (List) JSONParser.parseJSON(new JSONCharBufferString(cb));
        assertEquals(((Long) l.get(0)).intValue(), 43);
        assertEquals(((String) l.get(1)), "bla");
        assertTrue(((List) l.get(2)).isEmpty());
        assertTrue(((Map) l.get(3)).isEmpty());
        assertEquals(((Map) l.get(5)).size(), 2);
    }

    public static void main(String[] args) {
        TestRunner.run(JSONParserTest.class);
    }
}
