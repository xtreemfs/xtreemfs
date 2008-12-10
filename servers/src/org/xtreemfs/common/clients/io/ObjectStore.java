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
 * AUTHORS: Nele Andersen (ZIB)
 */

package org.xtreemfs.common.clients.io;

import java.io.IOException;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.foundation.json.JSONException;

public interface ObjectStore {

    /**
     * read an object from an OSD.
     * @param offset offset within the object
     * @param objectNo object number (0 is the first object in a file)
     * @param length number of bytes to read
     * @return the data read. In case of an EOF the buffer's length will be smaller than requested!
     * @throws java.io.IOException
     * @throws org.xtreemfs.foundation.json.JSONException
     * @throws java.lang.InterruptedException
     * @throws org.xtreemfs.common.clients.HttpErrorException
     */
    ReusableBuffer readObject(long offset, long objectNo, long length) throws IOException,
    JSONException, InterruptedException, HttpErrorException;
    
    void writeObject(long offset, long objectNo, ReusableBuffer buffer) throws IOException,
    JSONException, InterruptedException, HttpErrorException;
}
