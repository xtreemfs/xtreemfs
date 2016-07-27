/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.InternalServerErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidViewException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.PBRPCException;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SERVICES;

import com.google.protobuf.Message;

/**
 * Helper class provides static methods for all kinds of RPC calls to the servers. Abstracts error
 * handling and retrying of failure.
 */
public class RPCCaller {

    /**
     * Interface for syncCall which generates the calls. Will be called for each retry.
     */
    protected interface CallGenerator<C, R extends Message> {
        public RPCResponse<R> executeCall(InetSocketAddress server, Auth authHeader,
                UserCredentials userCreds, C input) throws IOException, PosixErrorException;
    }

    protected static <C, R extends Message> R syncCall(SERVICES service, UserCredentials userCreds,
            Auth auth, Options options, UUIDResolver uuidResolver, UUIDIterator it,
            boolean uuidIteratorHasAddresses, C callRequest, CallGenerator<C, R> callGen) throws IOException,
            PosixErrorException, InternalServerErrorException, AddressToUUIDNotFoundException {
        return syncCall(service, userCreds, auth, options, uuidResolver, it, uuidIteratorHasAddresses, false,
                options.getMaxTries(), callRequest, null, callGen);
    }

    protected static <C, R extends Message> R
            syncCall(SERVICES service, UserCredentials userCreds, Auth auth, Options options,
                    UUIDResolver uuidResolver, UUIDIterator it, boolean uuidIteratorHasAddresses,
                    boolean delayNextTry, int maxRetries, C callRequest, CallGenerator<C, R> callGen) throws IOException,
                    PosixErrorException, InternalServerErrorException, AddressToUUIDNotFoundException {
        return syncCall(service, userCreds, auth, options, uuidResolver, it, uuidIteratorHasAddresses,
                delayNextTry, maxRetries, callRequest, null, callGen);
    }

    protected static <C, R extends Message> R syncCall(SERVICES service, UserCredentials userCreds,
            Auth auth, Options options, UUIDResolver uuidResolver, UUIDIterator it,
            boolean uuidIteratorHasAddresses, C callRequest, ReusableBuffer buf, CallGenerator<C, R> callGen)
            throws IOException, PosixErrorException, InternalServerErrorException,
            AddressToUUIDNotFoundException {
        return syncCall(service, userCreds, auth, options, uuidResolver, it, uuidIteratorHasAddresses, false,
                options.getMaxTries(), callRequest, buf, callGen);
    }

    protected static <C, R extends Message> R syncCall(SERVICES service, UserCredentials userCreds,
            Auth auth, Options options, UUIDResolver uuidResolver, UUIDIterator it,
            boolean uuidIteratorHasAddresses, boolean delayNextTry, int maxRetries, C callRequest,
            ReusableBuffer buffer, CallGenerator<C, R> callGen) throws PosixErrorException, IOException,
            InternalServerErrorException, AddressToUUIDNotFoundException {
        int maxTries = maxRetries;
        int attempt = 0;

        R response = null;
        try {
            while (++attempt <= maxTries || maxTries == 0) {
                // Retry only if it is a recoverable error (REDIRECT, IO_ERROR, INTERNAL_SERVER_ERROR).
                boolean retry = false;
                IOException responseError = null;

                RPCResponse<R> r = null;
                try {
                    // create an InetSocketAddresse depending on the uuidIterator and
                    // the kind of service
                    InetSocketAddress server;
                    if (uuidIteratorHasAddresses) {
                        server = getInetSocketAddressFromAddress(it.getUUID(), service);
                    } else { // UUIDIterator has really1 UUID, not just address Strings.
                        String address = uuidResolver.uuidToAddress(it.getUUID());
                        server = getInetSocketAddressFromAddress(address, service);
                    }

                    r = callGen.executeCall(server, auth, userCreds, callRequest);
                    response = r.get();

                    // If the buffer is not null it should be filled with data
                    // piggybacked in the RPCResponse.
                    // This is used by the read request.
                    if (r.getData() != null) {
                        if (buffer != null) {
                            buffer.put(r.getData());
                        }
                        BufferPool.free(r.getData());
                    }
                } catch (PBRPCException pbe) {
                    responseError = pbe;
                    // handle special redirect
                    if (pbe.getErrorType().equals(ErrorType.REDIRECT)) {
                        assert (pbe.getRedirectToServerUUID() != null);
                        // Log redirect.
                        if (Logging.isInfo()) {
                            String error;
                            if (uuidIteratorHasAddresses) {
                                error =
                                        "The server " + it.getUUID() + " redirected to the current master: "
                                                + pbe.getRedirectToServerUUID() + " at attempt: " + attempt;
                            } else {
                                error =
                                        "The server with UUID " + it.getUUID()
                                                + " redirected to the current master: "
                                                + pbe.getRedirectToServerUUID() + " at attempt: " + attempt;
                            }
                            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, pbe, error);
                        }

                        if (maxTries != 0 && attempt == maxTries) {
                            // This was the last retry, but we give it another chance.
                            maxTries++;
                        }
                        // Do a fast retry and do not delay until next attempt.
                        it.markUUIDAsFailed(it.getUUID());
                        continue;
                    }

                    if (pbe.getErrorType().equals(ErrorType.IO_ERROR)
                            || pbe.getErrorType().equals(ErrorType.INTERNAL_SERVER_ERROR)) {
                        // Mark the current UUID as failed and get the next one.
                        it.markUUIDAsFailed(it.getUUID());
                        retry = true;
                    }
                } catch (IOException ioe) {
                    responseError = ioe;
                    // Mark the current UUID as failed and get the next one.
                    it.markUUIDAsFailed(it.getUUID());
                    retry = true;
                } catch (InterruptedException ie) {
                    // TODO: Ask what that is.
                    if (options.getInterruptSignal() == 0) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, ie,
                                    "Caught interrupt, aborting sync request");
                        }
                        break;
                    }
                    throw new IOException();
                } finally {
                    if (r != null) {
                        r.freeBuffers();
                    }
                }

                if (responseError != null) {
                    // Log only the first retry.
                    if (attempt == 1 && maxTries != 1) {
                        String retriesLeft = (maxTries == 0) ? ("infinite") : (String.valueOf(maxTries - attempt));
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, responseError,
                                "Got no response from %s, " + "retrying (%s attemps left, waiting at least %s seconds"
                                        + " between two attemps) Error was: %s", it.getUUID(), retriesLeft,
                                options.getRetryDelay_s(), responseError.getMessage());
                        if (Logging.isDebug()) {
                            Logging.logError(Logging.LEVEL_DEBUG, null, responseError);
                        }
                    }
                    
                    // Retry (and delay)?
                    if (retry && 
                            // Retry (and delay) only if at least one retry is left
                            (attempt < maxTries || maxTries == 0)
                            // or this last retry should be delayed
                            || (attempt == maxTries && delayNextTry)) {
                        waitDelay(options.getRetryDelay_s());
                        continue;
                    } else {
                        throw responseError;
                    }
                }

                return response;
            }
        } catch (PBRPCException e) {
            // Max attempts reached or non-IO error seen. Throw an exception.
            handleErrorAfterMaxTriesExceeded(e, it);
        }
        return null;
    }

    /**
     * Blocks the thread for delay_s seconds and throws an exception if interrupted.
     * 
     * @param delay_s
     * @throws IOException
     */
    static void waitDelay(long delay_s) throws IOException {
        try {
            Thread.sleep(delay_s * 1000);
        } catch (InterruptedException e) {
            String msg = "Caught interrupt while waiting for the next attempt, aborting sync request";
            if (Logging.isInfo()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, e, msg);
            }
            throw new IOException(msg);
        }
    }

    /**
     * Determines what to throw when the maximum number of retries is reached and there is still no valid
     * answer.
     * 
     * @param e
     * @param it
     */
    private static void handleErrorAfterMaxTriesExceeded(PBRPCException e, UUIDIterator it) throws PosixErrorException,
            IOException, InternalServerErrorException, InvalidViewException, XtreemFSException {
        // By default all errors are logged as errors.
        int logLevel = Logging.LEVEL_INFO;

        String errorMsg = "";
        switch (e.getErrorType()) {
        case ERRNO:
            // Posix error are usally not logged as errors.
            if (e.getPOSIXErrno().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                logLevel = Logging.LEVEL_DEBUG;
            }
            errorMsg =
                    "The server " + it.getUUID() + " denied the requested operation. " + "Error value: "
                            + e.getErrorType().name() + " Error message: " + e.getErrorMessage();

            Logging.logMessage(logLevel, Category.misc, e, errorMsg);
            throw new PosixErrorException(e.getPOSIXErrno(), errorMsg);
        case IO_ERROR:
            Logging.logMessage(logLevel, Category.misc, e, "The client encountered a communication "
                    + "error sending a request to the server %s Error: %s", it.getUUID(), e.getErrorMessage());
            throw new IOException(e.getErrorMessage());
        case INTERNAL_SERVER_ERROR:
            Logging.logMessage(logLevel, Category.misc, e, "The server %s returned an internal server"
                    + "error: %s", it.getUUID(), e.getErrorMessage());
            throw new InternalServerErrorException(errorMsg);
        case REDIRECT:
            throw new XtreemFSException("This error (A REDIRECT error was not handled "
                    + "and retried but thrown instead) should never happen. Report this");
        case INVALID_VIEW:
            Logging.logMessage(logLevel, Category.replication, e,
                    "The server %s denied the requested operation because the clients view is outdated. Error: %s",
                    it.getUUID(), e.getErrorMessage());
            throw new InvalidViewException(e.getErrorMessage());
        default:
            errorMsg =
                    "The server " + it.getUUID() + "returned an error: " + e.getErrorType().name()
                            + " Error: " + e.getErrorMessage();
            Logging.logMessage(logLevel, Category.misc, e, errorMsg);
            throw new XtreemFSException(errorMsg);
        } // end of switch
    }

    // private static void handlePBException(PBRPCException e) throws IOException, PosixErrorException {
    // int loglevel = Logging.LEVEL_INFO;
    //
    // switch (e.getErrorType()) {
    //
    // case ErrorType.ERRNO:
    // // Posix errors are usually not logged as errors.
    // if (e.getPOSIXErrno().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
    // loglevel = Logging.LEVEL_DEBUG;
    // }
    // Logging.logMessage(loglevel, Category.misc, e, "The server %s (" + , args)
    //
    // default:
    // return;
    // }
    // }

    /**
     * Create an InetSocketAddress depending on the address and the type of service object is. If address does
     * not contain a port a default port depending on the client object is used.
     * 
     * @param address
     *            The address.
     * @param service
     *            The service used to determine which default port should used when address does not contain a
     *            port.
     * @return
     */
    protected static InetSocketAddress getInetSocketAddressFromAddress(String address, SERVICES service) {
        if (SERVICES.DIR.equals(service)) {
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());
        }
        if (SERVICES.MRC.equals(service)) {
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.MRC_PBRPC_PORT_DEFAULT.getNumber());
        }
        if (SERVICES.OSD.equals(service)) {
            return Helper.stringToInetSocketAddress(address,
                    GlobalTypes.PORTS.OSD_PBRPC_PORT_DEFAULT.getNumber());
        }
        return null;
    }
}
