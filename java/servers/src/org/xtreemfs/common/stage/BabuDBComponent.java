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
import org.xtreemfs.babudb.api.index.ByteRangeComparator;
import org.xtreemfs.common.olp.OverloadProtectedComponent;
import org.xtreemfs.common.olp.PerformanceInformationReceiver;
import org.xtreemfs.common.olp.RequestMetadata;
import org.xtreemfs.common.olp.RequestMonitoring;

import com.google.protobuf.Message;

import static org.xtreemfs.common.stage.BabuDBComponent.Operation.*;

/**
 * <p>Wrapper for BabuDB requests enabling overload-protection measurements for the component BabuDB.</p>
 *
 * @author fx.langner
 * @version 1.00, 09/15/2011
 */
@SuppressWarnings("rawtypes")
public class BabuDBComponent extends OverloadProtectedComponent<BabuDBComponent.BabuDBDatabaseRequest> 
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
    
    private final Database                        database;
    private final PerformanceInformationReceiver  piggybackReceiver;
    
    /**
     * @param stageId
     * @param numRqTypes
     * @param master
     * @param database
     */
    public BabuDBComponent(int stageId, int numRqTypes, PerformanceInformationReceiver master, Database database) {
        
        super(stageId, numRqTypes, 0, new boolean[numRqTypes], new PerformanceInformationReceiver[] { master });
        
        this.piggybackReceiver = master;
        this.database = database;
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#lookup(int, byte[], java.lang.Object)
     */
    public void lookup(RPCRequestCallback callback, int indexId, byte[] key, RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, LOOKUP, indexId, key), 
                    metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#prefixLookup(int, byte[], java.lang.Object)
     */
    public void prefixLookup(RPCRequestCallback callback, int indexId, byte[] key, RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, PREFIX_LOOKUP, indexId, 
                    key), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reversePrefixLookup(int, byte[], java.lang.Object)
     */
    public void reversePrefixLookup(RPCRequestCallback callback, int indexId, byte[] key, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {

        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, REVERSE_PREFIX_LOOKUP, 
                    indexId, key), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#rangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void rangeLookup(RPCRequestCallback callback, int indexId, byte[] from, byte[] to, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, RANGE_LOOKUP, indexId, from, 
                    to), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#reverseRangeLookup(int, byte[], byte[], java.lang.Object)
     */
    public void reverseRangeLookup(RPCRequestCallback callback, int indexId, byte[] from, byte[] to, 
            RequestMetadata metadata, BabuDBPostprocessing postprocessing) {
                
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, REVERSE_RANGE_LOOKUP, 
                    indexId, from, to), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.DatabaseRO#
     *          userDefinedLookup(org.xtreemfs.babudb.api.database.UserDefinedLookup, java.lang.Object)
     */
    public void userDefinedLookup(RPCRequestCallback callback, UserDefinedLookup udl, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, udl), 
                    metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#getName()
     */
    public String getName() {
        
        return database.getName();
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#createInsertGroup()
     */
    public DatabaseInsertGroup createInsertGroup() {
        
        return database.createInsertGroup();
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#getComparators()
     */
    public ByteRangeComparator[] getComparators() {
        
        return database.getComparators();
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(RPCRequestCallback callback, int indexId, byte[] key, byte[] value, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, 
                    indexId, key, value), metadata, monitoring);
        } catch (Exception e) {
            callback.failed(e);
        }
    }
    
    /**
     * @see org.xtreemfs.babudb.api.database.Database#singleInsert(int, byte[], byte[], java.lang.Object)
     */
    public void singleInsert(int indexId, byte[] key, byte[] value, BabuDBDatabaseRequest request, 
            BabuDBPostprocessing postprocessing) {
        
        request.postprocessing = postprocessing;
        resumeRequestProcessing(request.monitoring);
        enter(request);
    }

    /**
     * @see org.xtreemfs.babudb.api.database.Database#
     *          insert(org.xtreemfs.babudb.api.database.DatabaseInsertGroup, java.lang.Object)
     */
    public void insert(RPCRequestCallback callback, DatabaseInsertGroup irg, RequestMetadata metadata, 
            BabuDBPostprocessing postprocessing) {
        
        try {
            RequestMonitoring monitoring = new RequestMonitoring(piggybackReceiver);
            enter(new BabuDBDatabaseRequest(callback, metadata, monitoring, postprocessing, USER_DEFINED_LOOKUP, irg), 
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
    public void enter(BabuDBDatabaseRequest request) {
                
        switch (request.operation) {
        case LOOKUP:
            database.lookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case INSERT:
            database.insert((DatabaseInsertGroup) request.arguments[0], request).registerListener(this);
            break;
        case PREFIX_LOOKUP:
            database.prefixLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case RANGE_LOOKUP:
            database.rangeLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case REVERSE_PREFIX_LOOKUP:
            database.reversePrefixLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], request)
                    .registerListener(this);
            break;
        case REVERSE_RANGE_LOOKUP:
            database.reverseRangeLookup((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case SINGLE_INSERT:
            database.singleInsert((Integer) request.arguments[0], (byte[]) request.arguments[1], 
                    (byte[]) request.arguments[2], request).registerListener(this);
            break;
        case USER_DEFINED_LOOKUP:
            database.userDefinedLookup((UserDefinedLookup) request.arguments[0], request).registerListener(this);
            break;
        default:
            assert(false);
        }
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.stage.AutonomousComponent#exit(java.lang.Object)
     */
    @Override
    public void exit(BabuDBDatabaseRequest request) {
        
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
        
        BabuDBDatabaseRequest request = (BabuDBDatabaseRequest) context;
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
        
        BabuDBDatabaseRequest request = (BabuDBDatabaseRequest) context;
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

    public final class BabuDBDatabaseRequest {
        
        final Object[]             arguments;
        final Operation            operation;
        final RequestMonitoring    monitoring;
        final RequestMetadata      metadata;
        BabuDBPostprocessing       postprocessing;
        final RPCRequestCallback   callback;
        Exception                  error = null;
        Message                    result = null;
        
        public BabuDBDatabaseRequest(RPCRequestCallback callback, RequestMetadata metadata, 
                RequestMonitoring monitoring, BabuDBPostprocessing postprocessing, 
                Operation operation, Object ... args) {
           
            this.callback = callback;
            this.metadata = metadata;
            this.monitoring = monitoring;
            this.arguments = args;
            this.operation = operation;
            this.postprocessing = postprocessing;
        }
    }
    
    public abstract class BabuDBPostprocessing<T> {
        
        public abstract Message execute(T result, BabuDBDatabaseRequest request) throws Exception;
        
        public void requeue(BabuDBDatabaseRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}