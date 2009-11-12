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
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.mrc.database;

public class DatabaseException extends Exception {
    private static final long serialVersionUID = -8079959463034197259L;

    public enum ExceptionType {
        INTERNAL_DB_ERROR, FILE_EXISTS, WRONG_DB_VERSION, NOT_ALLOWED, REDIRECT, REPLICATION
    }
    
    private ExceptionType type;
    private Object attachment = null;
    
    public DatabaseException(ExceptionType type) {
        this.type = type;
    }
    
    public DatabaseException(ExceptionType type, Object attachment) {
        this.type = type;
        this.attachment = attachment;
    }
    
    public DatabaseException(String message, ExceptionType type) {
        super(message);
        this.type = type;
    }
    
    public DatabaseException(Throwable cause) {
        super(cause);
        this.type = ExceptionType.INTERNAL_DB_ERROR;
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExceptionType getType() {
        return type;
    }
    
    public Object getAttachment() {
        return this.attachment;
    }
    
}
