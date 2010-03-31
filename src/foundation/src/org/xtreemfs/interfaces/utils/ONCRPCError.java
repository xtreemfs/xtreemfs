/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils;

import yidl.runtime.Marshaller;
import yidl.runtime.Unmarshaller;

/**
 *
 * @author bjko
 */
public class ONCRPCError extends ONCRPCException {
    private static final long serialVersionUID = -2480538525621061770L;
    
    final int accept_stat;
    
    public ONCRPCError(int accept_stat) {
        this.accept_stat = accept_stat;
    }

    public int getAcceptStat() {
        return accept_stat;
    }

    @Override
    public int getTag() {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public String getTypeName() {
        return "ONCRPCError";
    }

    @Override
    public void marshal(Marshaller writer) {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public void unmarshal(Unmarshaller buf) {
        throw new RuntimeException("this exception must not be serialized");
    }

    @Override
    public int getXDRSize() {
        throw new RuntimeException("this exception must not be serialized");
    }



}
