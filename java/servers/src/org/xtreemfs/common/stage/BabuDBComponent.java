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
import org.xtreemfs.common.olp.OverloadProtectedComponent;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.common.olp.RequestMetadata;
import org.xtreemfs.common.olp.RequestMonitoring;
import org.xtreemfs.common.stage.BabuDBPostprocessing.NullPostprocessing;
import org.xtreemfs.common.stage.Callback.NullCallback;

import com.google.protobuf.Message;

import static org.xtreemfs.common.stage.BabuDBComponent.Operation.*;

/**
 * <p>Wrapper for BabuDB requests enabling overload-protection measurements for the component BabuDB.</p>
 *
 * @author fx.langner
 * @version 1.00, 09/15/11
 */
@SuppressWarnings("rawtypes")
public class BabuDBComponent extends OverloadProtectedComponent<BabuDBComponent.BabuDBRequest> 
        implements DatabaseRequestListener {

    enum Operation {
        
        LOOKUP,
        PREFIX_LOOKUP,
        REVERSE_PREFIX_LOOKUP,
        RANGE_LOOKUP,
        REVERSE_RANGE_LOOKUP,
        USER_DEFINED_LOOKUP,
        SINGLE_INSERT,
        INSERT
    }
    
    private final PerformanceInformationReceiver  piggybackReceiver;
    
    /**
     * @param stageId
     * @param numRqTypes
     * @param master
     */
    public BabuDBComponent(int stageId, int numRqTypes, PerformanceInformationReceiver master) {
        
        super(stageId, numRqTypes, 0, new boolean[numRqTypes], new PerformanceInformationReceiver[] { master });
        
        this.piggybackReceiver = master;
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#lookup(int, byte[], java.lang.Object)
     */
    public void lookup(Database database, RPCRequestCallback callback, int indexId, byte[] key, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, LOOKUP, indexId, key), 
                    metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#prefixLookup(int, byte[], java.lang.Object)
     */
    public void prefixLookup(Database database, RPCRequestCallback callback, int indexId, byte[] key, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, PREFIX_LOOKUP, indexId, 
                    key), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reversePrefixLookup(int, byte[], java.lang.Object)
     */
    public void reversePrefixLookup(Database database, RPCRequestCallback callback, int indexId, byte[] key, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {

        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, REVERSE_PREFIX_LOOKUP, 
                    indexId, key), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#rangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void rangeLookup(Database database, RPCRequestCallback callback, int indexId, byte[] from, byte[] to, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, RANGE_LOOKUP, indexId, 
                    from, to), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reverseRangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void reverseRangeLookup(Database database, RPCRequestCallback callback, int indexId, byte[] from, byte[] to, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
                
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, REVERSE_RANGE_LOOKUP, 
                    indexId, from, to), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#
     *          userDefinedLookup(org.xtreemfs.babudb.api.database.UserDefinedLookup, java.lang.Object)
     */
    public void userDefinedLookup(Database database, RPCRequestCallback callback, UserDefinedLookup udl, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, udl), 
                    metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(Database database, RPCRequestCallback callback, int indexId, byte[] key, byte[] value, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, 
                    indexId, key, value), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
    /**
     * <p>Method to support re-queue mechanics.</p>
     * 
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(int indexId, byte[] key, byte[] value, BabuDBRequest request, 
            BabuDBPostprocessing postprocessing) {
        
        request.postprocessing = postprocessing;
        resumeRequestProcessing(request.monitoring);
        enter(request);
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#
     *          insert(org.xtreemfs.babudb.api.database.DatabaseInsertGroup, java.lang.Object)
     */
    public void insert(Database database, Callback callback, DatabaseInsertGroup irg, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBRequest(database, callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, irg), 
                    metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#enter(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void enter(BabuDBRequest request) {
                
        switch (request.operation) {
        case LOOKUP:
            request.database.lookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case INSERT:
            request.database.insert((DatabaseInsertGroup) request.arguments[0], request).registerListener(this);
            break;
        case PREFIX_LOOKUP:
            request.database.prefixLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case RANGE_LOOKUP:
            request.database.rangeLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case REVERSE_PREFIX_LOOKUP:
            request.database.reversePrefixLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case REVERSE_RANGE_LOOKUP:
            request.database.reverseRangeLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case SINGLE_INSERT:
            request.database.singleInsert((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case USER_DEFINED_LOOKUP:
            request.database.userDefinedLookup((UserDefinedLookup) request.arguments[0], request)
                    .registerListener(this);
            break;
        default:
            assert(false);
        }
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#exit(java.lang.Object)
     */
    @Override
    public void exit(BabuDBRequest request) {
        
        if (request.error != null) {
            request.callback.failed(request.error);
        } else {
            request.callback.success(request.result);
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.babudb.api.database.DatabaseRequestListener#failed(org.xtreemfs.babudb.api.exception.BabuDBException, java.lang.Object)
     */
    @Override
    public void failed(BabuDBException error, Object context) {
        
        BabuDBRequest request = (BabuDBRequest) context;
        request.error = error;
        request.monitoring.voidMeasurments();
        exit(request, request.metadata, request.monitoring);
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.babudb.api.database.DatabaseRequestListener#finished(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void finished(Object result, Object context) {
        
        BabuDBRequest request = (BabuDBRequest) context;
        try {
            request.result = request.postprocessing.execute(result, request);
            
            // rerun
            if (request.result == null) {
                suspendRequestProcessing(request.monitoring);
                request.postprocessing.requeue(request);
                return;
            }
        } catch (Exception e) {
            request.error = e;
        }
        
        exit(request, request.metadata, request.monitoring);
    }

    public final class BabuDBRequest {
        
        final Database             database;
        final Object[]             arguments;
        final Operation            operation;
        final RequestMonitoring    monitoring;
        final RequestMetadata      metadata;
        BabuDBPostprocessing       postprocessing;
        final Callback             callback;
        Exception                  error = null;
        Message                    result = null;
        
        public BabuDBRequest(Database database, Callback callback, RequestMetadata metadata, 
                RequestMonitoring monitoring, BabuDBPostprocessing postprocessing, 
                Operation operation, Object ... args) {
            
            this.database = database;
            this.callback = (callback == null) ? NullCallback.INSTANCE : callback;
            this.metadata = metadata;
            this.monitoring = monitoring;
            this.arguments = args;
            this.operation = operation;
            this.postprocessing = (postprocessing == null) ? NullPostprocessing.INSTANCE : postprocessing;
        }
    }
}