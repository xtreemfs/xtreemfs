/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.DatabaseInsertGroup;
import org.xtreemfs.babudb.api.database.DatabaseRequestListener;
import org.xtreemfs.babudb.api.database.UserDefinedLookup;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.olp.AugmentedRequest;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.olp.OverloadProtectedComponent;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;

/**
 * <p>Wrapper for BabuDB requests enabling overload-protection measurements for the component BabuDB.</p>
 *
 * @author fx.langner
 * @version 1.00, 09/15/11
 */
@SuppressWarnings("rawtypes")
public class BabuDBComponent<R extends AugmentedRequest> extends OverloadProtectedComponent<R> 
        implements DatabaseRequestListener {
     
    private final static int LOOKUP = 1;
    private final static int PREFIX_LOOKUP = 2;
    private final static int REVERSE_PREFIX_LOOKUP = 3;
    private final static int RANGE_LOOKUP = 4;
    private final static int REVERSE_RANGE_LOOKUP = 5;
    private final static int USER_DEFINED_LOOKUP = 6;
    private final static int SINGLE_INSERT = 7;
    private final static int INSERT = 8;
    
    private final PerformanceInformationReceiver[]  piggybackReceiver;
    
    /**
     * @param stageId
     * @param numRqTypes
     * @param master
     */
    public BabuDBComponent(int stageId, int numRqTypes, PerformanceInformationReceiver master) {
        super(stageId, numRqTypes, 0, 0, new boolean[numRqTypes]);
        
        registerPerformanceInformationReceiver(new PerformanceInformationReceiver[] { master });
        this.piggybackReceiver = new PerformanceInformationReceiver[] { master };
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#lookup(int, byte[], java.lang.Object)
     */
    public void lookup(Database database, AbstractRPCRequestCallback callback, int indexId, byte[] key, R request) {
        
        try {
            enter(LOOKUP, new Object [] { database, indexId, key }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#prefixLookup(int, byte[], java.lang.Object)
     */
    public void prefixLookup(Database database, AbstractRPCRequestCallback callback, int indexId, byte[] key, 
            R request) {
        
        try {
            enter(PREFIX_LOOKUP, new Object [] { database, indexId, key }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reversePrefixLookup(int, byte[], java.lang.Object)
     */
    public void reversePrefixLookup(Database database, RPCRequestCallback callback, int indexId, byte[] key, 
            R request) {

        try {
            enter(REVERSE_PREFIX_LOOKUP, new Object [] { database, indexId, key }, request, callback, 
                    piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#rangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void rangeLookup(Database database, RPCRequestCallback callback, int indexId, byte[] from, byte[] to, 
            R request) {
        
        try {
            enter(RANGE_LOOKUP, new Object [] { database, indexId, from, to }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reverseRangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void reverseRangeLookup(Database database, RPCRequestCallback callback, int indexId, byte[] from, 
            byte[] to, R request) {
                
        try {
            enter(REVERSE_RANGE_LOOKUP, new Object [] { database, indexId, from, to }, request, callback, 
                    piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#
     *          userDefinedLookup(org.xtreemfs.babudb.api.database.UserDefinedLookup, java.lang.Object)
     */
    public void userDefinedLookup(Database database, RPCRequestCallback callback, UserDefinedLookup udl, 
            R request) {
        
        try {
            enter(USER_DEFINED_LOOKUP, new Object [] { database, udl }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(Database database, RPCRequestCallback callback, int indexId, byte[] key, byte[] value, 
            R request) {
        
        try {
            enter(SINGLE_INSERT, new Object [] {database, indexId, key, value }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
    /**
     * <p>Method to support re-queue mechanics.</p>
     * 
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(int indexId, byte[] key, byte[] value, OLPStageRequest<R> stageRequest, 
            AbstractRPCRequestCallback callback) {
        
        // interrupt measurement to save the current value to request
        suspendRequestProcessing(stageRequest);
        resumeRequestProcessing(stageRequest);
        
        // update request
        Object[] newArgs = { stageRequest.getArgs()[0], indexId, key, value };
        stageRequest.update(SINGLE_INSERT, newArgs, callback);
        
        enter(stageRequest);
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#
     *          insert(org.xtreemfs.babudb.api.database.DatabaseInsertGroup, java.lang.Object)
     */
    public void insert(Database database, Callback callback, DatabaseInsertGroup irg, R request) {
        
        try {
            enter(INSERT, new Object [] { database, irg }, request, callback, piggybackReceiver);
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#enter(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void enter(OLPStageRequest<R> stageRequest) {
                
        final Object[] arguments = stageRequest.getArgs();
        final int requestedMethod = stageRequest.getStageMethod();
        
        switch (requestedMethod) {
        case LOOKUP:
            ((Database) arguments[0]).lookup((Integer) arguments[1], (byte[]) arguments[2], stageRequest)
                                     .registerListener(this);
            break;
        case INSERT:
            ((Database) arguments[0]).insert((DatabaseInsertGroup) arguments[1], stageRequest).registerListener(this);
            break;
        case PREFIX_LOOKUP:
            ((Database) arguments[0]).prefixLookup((Integer) arguments[1], (byte[]) arguments[2], stageRequest)
                                     .registerListener(this);
            break;
        case RANGE_LOOKUP:
            ((Database) arguments[0]).rangeLookup((Integer) arguments[1], (byte[]) arguments[2], (byte[]) arguments[3], 
                    stageRequest).registerListener(this);
            break;
        case REVERSE_PREFIX_LOOKUP:
            ((Database) arguments[0]).reversePrefixLookup((Integer) arguments[1], (byte[]) arguments[2], stageRequest)
                    .registerListener(this);
            break;
        case REVERSE_RANGE_LOOKUP:
            ((Database) arguments[0]).reverseRangeLookup((Integer) arguments[1], (byte[]) arguments[2], 
                    (byte[]) arguments[3], stageRequest).registerListener(this);
            break;
        case SINGLE_INSERT:
            ((Database) arguments[0]).singleInsert((Integer) arguments[1], (byte[]) arguments[2], 
                    (byte[]) arguments[3], stageRequest).registerListener(this);
            break;
        case USER_DEFINED_LOOKUP:
            ((Database) arguments[0]).userDefinedLookup((UserDefinedLookup) arguments[1], stageRequest)
                    .registerListener(this);
            break;
        default:
            Logging.logMessage(Logging.LEVEL_ERROR, this, "unknown method called: %d", requestedMethod);
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.babudb.api.database.DatabaseRequestListener#failed(org.xtreemfs.babudb.api.exception.BabuDBException, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void failed(BabuDBException error, Object context) {
        
        OLPStageRequest<R> request = (OLPStageRequest<R>) context;
        request.getCallback().failed(error);
        request.voidMeasurments();
        exit(request);
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.babudb.api.database.DatabaseRequestListener#finished(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void finished(Object result, Object context) {
        
        final OLPStageRequest<R> request = (OLPStageRequest<R>) context;
        final AbstractRPCRequestCallback callback = (AbstractRPCRequestCallback) request.getCallback();
        
        try {           
            
            if (!callback.success(result, request)) {
                return;
            }
        } catch (ErrorResponseException e) {
            
            request.voidMeasurments();
            callback.failed(e.getRPCError());
        }
        
        exit(request);
    }
}