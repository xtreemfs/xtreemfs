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

/**
 * Thrown by the JSON parser and writer.
 * 
 * @author bjko
 */
public class JSONException extends java.lang.Exception {

    /***/
    private static final long serialVersionUID = 2422241603599209392L;

    /**
     * Creates a new instance of <code>JSONException</code> without detail
     * message.
     */
    public JSONException() {
    }

    /**
     * Constructs an instance of <code>JSONException</code> with the specified
     * detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public JSONException(String msg) {
        super(msg);
    }
}
