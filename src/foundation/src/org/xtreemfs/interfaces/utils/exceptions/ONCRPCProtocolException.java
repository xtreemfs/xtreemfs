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

package org.xtreemfs.interfaces.utils.exceptions;

import java.io.IOException;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

/**
 *
 * @author bjko
 */
public abstract class ONCRPCProtocolException extends IOException {
    private static final long serialVersionUID = -4843962183415415948L;

    public ONCRPCProtocolException() {
        super();
    }

    protected ONCRPCProtocolException(String message) {
        super(message);
    }

    public abstract int getAcceptStat();

    public static ONCRPCProtocolException getException(int accept_stat) {
        switch (accept_stat) {
            case ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS : return new GarbageArgumentsException();
            case ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL : return new ProcedureUnavailableException();
            case ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH : return new ProgramMismatchException();
            case ONCRPCResponseHeader.ACCEPT_STAT_PROG_UNAVAIL : return new ProgramUnavailableException();
            case ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR : return new SystemErrorException();
        }
        throw new RuntimeException("invalid accept_stat code "+accept_stat);
    }

}
