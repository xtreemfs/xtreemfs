/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils.exceptions;

import java.io.IOException;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

/**
 *
 * @author bjko
 */
public abstract class ONCRPCProtocolException extends IOException {

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
