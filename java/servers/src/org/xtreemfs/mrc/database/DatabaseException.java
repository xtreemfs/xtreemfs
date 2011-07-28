/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
