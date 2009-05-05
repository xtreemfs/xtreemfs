/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: BjÃ¶rn Kolbeck (ZIB), Jan Stender (ZIB), JesÃºs Malo (BSC)
 */

package org.xtreemfs.foundation.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JSON Parser routines. This parser accepts any value as top level element, not
 * just an object or array.
 *
 * @author bjko
 */
public class JSONParser {
    
    /**
     * Creates a new instance of JSONParser
     */
    public JSONParser() {
    }
    
    private static String parseString(JSONInput input) throws JSONException {

        boolean nonEscaped = true;        
        StringBuilder str = new StringBuilder();
        
        while (input.hasMore()) {
            char ch = input.read();
            
            if (nonEscaped) {
                if (ch == '\\') {
                    nonEscaped = false;
                    continue;
                }
                else if (ch == '"') {
                    return str.toString();
                }
                else {
                   str.append(ch);
                }
            }
            else {
                if (ch == 'n') {
                    str.append('\n');
                } else if (ch == 'r') {
                    str.append('\r');
                } else if (ch == 't') {
                    str.append('\t');
                } else {
                    str.append(ch);
                }
            }

            nonEscaped = true;
        }
        
        throw new JSONException("[ E | JSONParser ] Unexpected end while parsing string");
        
    }
    
    private static Object parseNumber(JSONInput input) throws JSONException {
        
        StringBuilder str = new StringBuilder();
        input.skip(-1);
        
        boolean isFP = false;
        
        while (input.hasMore()) {
            char ch = input.read();
            
            if ((ch == '-') || (ch >= '0') && (ch <= '9')) {
                str.append(ch);
            } else if ((ch == '.') || (ch == 'E') || (ch == 'e')) {
                str.append(ch);
                isFP = true;
            } else {
                input.skip(-1);
                if (isFP)
                    return new BigDecimal(str.toString());
                else
                    return Long.valueOf(str.toString());
            }
        }
        
        if (isFP)
            return new BigDecimal(str.toString());
        else
            return Long.valueOf(str.toString());
    }
    
    private static Object parseArray(JSONInput input) throws JSONException {
        LinkedList<Object> arr = new LinkedList<Object>();
        
        while (input.hasMore()) {
            char ch = input.read();
            
            if (ch == ']') {
                return arr;
            } else if (ch == ',') {
                arr.add(parseJSON(input));
            } else if ((ch == ' ') || (ch == '\t')) {
                continue;
            } else {
                input.skip(-1);
                arr.add(parseJSON(input));
            }
        }
        
        throw new JSONException("[ E | JSONParser ] Unexpected end while parsing array");
    }
    
    private static Object parseObject(JSONInput input) throws JSONException {        
        
        HashMap<String, Object> map = new HashMap<String, Object>();
        while (input.hasMore()) {
            char ch = input.read();

            if (ch == '}') {
                return map;
            }
            // skip all ws
            if ((ch == ' ') || (ch == '\t')) {
                continue;
            }
            
            String name = parseString(input);
            ch = input.read();
            
            while ((ch == ' ') || (ch == '\t')) {
                ch = input.read();
            }
            
            if (ch != ':') {
                throw new JSONException("[ E | JSONParser ] Unexpected token '"
                        + ((char) ch) + "' or EOF. Expected : in Object.");
            }
            
            while ((ch == ' ') || (ch == '\t')) {
                ch = input.read();
            }
            
            Object value = parseJSON(input);
            map.put(name, value);
            ch = input.read();
            
            while ((ch == ' ') || (ch == '\t')) {
                ch = input.read();
            }
            
            if (ch == '}') {
                return map;
            }
            if (ch != ',') {
                throw new JSONException("[ E | JSONParser ] Unexpected token '"
                        + ((char) ch) + "' or EOF. Expected , or } in Object.");
            }
        }
        
        throw new JSONException("[ E | JSONParser ] Unexpected end while parsing object");
    }
    
    /**
     * Parses a JSON message.
     *
     * @return the objects encoded in input.
     * @attention This routine may cause a StackOverflow exception when parsing
     *            incorrect, very deep or maliciously malformed JSON messages.
     * @param input
     *            the JSON string
     * @throws org.xtreemos.wp34.mrc.utils.JSONException
     *             if input is not valid JSON
     */
    public static Object parseJSON(JSONInput input) throws JSONException {

        while (input.hasMore()) {
            char ch = input.read();
            
            if (ch == '[') {
                return parseArray(input);
            } else if (ch == '{') {
                return parseObject(input);
            } else if (ch == '"') {
                return parseString(input);
            } else if ((ch == '-') || ((ch >= '0') && (ch <= '9'))) {
                return parseNumber(input);
            } else if (ch == 't') {
                input.skip(3);
                return Boolean.valueOf(true);
            } else if (ch == 'f') {
                input.skip(4);
                return Boolean.valueOf(false);
            } else if (ch == 'n') {
                input.skip(3);
                return null;
            } else if ((ch == ' ') || (ch == '\t')) {
                continue;
            } else {
                throw new JSONException("[ E | JSONParser ] Unexpected token '"
                        + ((char) ch) + "' expected Object, Array or Value.");
            }
        }
        
        throw new JSONException("[ E | JSONParser ] Unexpected end while parsing root element");
    }
    
    /**
     * Creates a JSON encoded message from an object. Can handle Boolean,
     * Integer, Long, BigDecimal, List and Map.
     *
     * @param input
     *            object to encode, objects can be nested.
     * @return a JSON encoded message
     * @throws org.xtreemos.wp34.mrc.utils.JSONException
     *             if there are one or more objects it cannot encode
     */
    public static String writeJSON(Object input) throws JSONException {
        return writeJSON(input,new StringBuilder()).toString();
    }
    
    /**
     * Creates a JSON encoded message from an object. Can handle Boolean,
     * Integer, Long, BigDecimal, List and Map.
     *
     * @param input
     *            object to encode, objects can be nested.
     * @return a JSON encoded message
     * @throws org.xtreemos.wp34.mrc.utils.JSONException
     *             if there are one or more objects it cannot encode
     */
    public static StringBuilder writeJSON(Object input, StringBuilder result) throws JSONException {
        
        if (input == null) {
            return result.append("null");
        } else if (input instanceof Boolean) {
            return result.append(((Boolean) input).booleanValue() ? "true" : "false");
        } else if (input instanceof BigDecimal) {
            return result.append(((BigDecimal) input).toString());
        } else if (input instanceof Integer) {
            return result.append((Integer) input);
        } else if (input instanceof Long) {
            return result.append((Long) input);
        } else if (input instanceof String) {
            return writeJSONString(input,result);
        } else if (input instanceof Map) {
            return writeJSONObject(input,result);
        } else if (input instanceof Collection) {
            return writeJSONArray(input,result);
        } else {
            throw new JSONException(
                    "[ E | JSONParser ] Unexpected Object type: "
                    + input.getClass().getName());
        }
    }
    
    @SuppressWarnings("unchecked")
    private static StringBuilder writeJSONObject(Object input, StringBuilder result) throws JSONException {
        Map<Object, Object> map = (Map) input;
        result.append("{");
        int i = 1;
        for (Object key : map.keySet()) {
            writeJSONString(key.toString(),result);
            result.append(":");
            writeJSON(map.get(key),result);
            if (i < map.size())
                result.append(",");
            i++;
        }
        return result.append("}");
    }
    
    @SuppressWarnings("unchecked")
    private static StringBuilder writeJSONArray(Object input, StringBuilder result) throws JSONException {
        Collection<Object> arr = (Collection<Object>) input;
        result.append("[");
        int i = 1;
        for (Object obj : arr) {
            writeJSON(obj,result);
            if (i < arr.size())
                result.append(",");
            i++;
        }
        return result.append("]");
    }
    
    private static StringBuilder writeJSONString(Object input, StringBuilder result) {
        /*
         * This is 10 times faster than using str.replace
         */
        final String str = input.toString();
        result.append("\"");
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            
            switch (ch) {
                case '\n' : result.append("\\n"); break;
                case '\r' : result.append("\\r"); break;
                case '\t' : result.append("\\t"); break;
                case '"' : result.append("\\\""); break;
                case '\\' : result.append("\\\\"); break;
                case '/' : result.append("\\/"); break;
                default: result.append(ch);
            }
        }
        result.append("\"");
        return result;
    }
    
    
    public static String toJSON(Object... args) throws JSONException {
        List<Object> argList = new ArrayList<Object>(args.length);
        for (Object arg : args)
            argList.add(arg);

	return writeJSON(argList);
    }

}
