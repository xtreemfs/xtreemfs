/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Minor Gordon (NEC), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;


public interface Serializable {
    /**
     * Gets the IDL module[::interface]::type_name for this Serializable type
     * @return
     */
    public String getTypeName();
    
    /**
     * Serializes the message into one or more buffers and appends
     * them to the responseBuffers list. The buffers must be ready
     * for being written (i.e. position = 0 and limit set correctly).
     * @param responseBuffers list
     */
    public void serialize(ONCRPCBufferWriter writer);

    /**
     * Deserialize the message from the buffer. The buffer's
     * position should be set to the start of the message.
     * @param buf buffer containing the message
     */
    public void deserialize(ReusableBuffer buf);

    /**
     * Returns the size of this message in bytes.
     * @return
     */
    public int calculateSize();
};   
