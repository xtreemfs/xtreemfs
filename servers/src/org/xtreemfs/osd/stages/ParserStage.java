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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.Request;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.LocationsCache;
import org.xtreemfs.osd.RPCTokens;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.ops.Operation;

/**
 * Parses HTTP requests and constructs requests plus operation
 */
public final class ParserStage extends Stage {

    /**
     * parse request method
     */
    public static final int      STAGEOP_PARSE              = 1;

    /**
     * remove cache entry method
     */
    public static final int      STAGEOP_REMOVE_CACHE_ENTRY = 2;

    /**
     * X-Location cache
     */
    private final LocationsCache xLocCache;

    /**
     * master
     */
    private RequestDispatcher    master;

    public ParserStage(RequestDispatcher controller) {
        super("OSD Parser Stage");
        xLocCache = new LocationsCache(10000);
        this.master = controller;
    }

    @Override
    protected void processMethod(final StageMethod op) {
        final OSDRequest rq = op.getRq();
        final int stageOp = op.getStageMethod();

        if (stageOp == STAGEOP_PARSE) {
            final ErrorRecord parseResult = parseMethod(rq);
            if (Logging.tracingEnabled()) {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "result is : " + parseResult);
                Logging.logMessage(Logging.LEVEL_DEBUG, this, rq.toString());
            }

            if (parseResult != null) {
                rq.setError(parseResult);
                master.requestFinished(rq);
            } else {
                assert (rq.getOperation() != null);
                calcRequestDuration(rq);
                rq.getOperation().startRequest(rq);
            }
        } else if (stageOp == STAGEOP_REMOVE_CACHE_ENTRY) {
            removeCacheEntry(op);
        }

    }

    protected ErrorRecord parseMethod(OSDRequest rq) {

        final PinkyRequest pr = rq.getPinkyRequest();

        // check method to handle different requests
        if (pr.requestMethod.equals(HTTPUtils.GET_TOKEN)) {

            if (pr.requestURI == null)
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_FILEID,
                    "missing file ID");

            if (pr.requestURI.equals("/")) {
                // status page request
                rq.setType(OSDRequest.Type.STATUS_PAGE);
                rq.setOperation(master.getOperation(RequestDispatcher.Operations.STATUS_PAGE));
            } else {
                // regular get request
                rq.setType(OSDRequest.Type.READ);
                final String fileId = parseFileIdFromURI(pr);
                if (fileId == null) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_FILEID,
                        "fileId contains invalid characters");
                }
                rq.getDetails().setFileId(fileId);
                rq.setOperation(master.getOperation(RequestDispatcher.Operations.READ));
            }

        } else if (pr.requestMethod.equals(HTTPUtils.PUT_TOKEN)) {
            // write request
            rq.setType(OSDRequest.Type.WRITE);
            final String fileId = parseFileIdFromURI(pr);
            if (fileId == null) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_FILEID,
                    "fileId contains invalid characters");
            }
            if (pr.requestBody == null) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_PARAMS,
                        "PUT requires data to write in the HTTP request body");
            }
            rq.getDetails().setFileId(fileId);
            rq.setOperation(master.getOperation(RequestDispatcher.Operations.WRITE));

        } else if (pr.requestMethod.equals(HTTPUtils.DELETE_TOKEN)) {
            rq.setType(OSDRequest.Type.DELETE);
            final String fileId = parseFileIdFromURI(pr);
            if (fileId == null) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_FILEID,
                    "fileId contains invalid characters");
            }
            rq.getDetails().setFileId(fileId);
            rq.setOperation(master.getOperation(RequestDispatcher.Operations.DELETE));

        } else if (pr.requestMethod.equals(HTTPUtils.POST_TOKEN)) {
            rq.setType(OSDRequest.Type.RPC);

            assert (pr.requestURI != null);

            if (pr.requestURI.length() == 0) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                    ErrorCodes.METHOD_NOT_IMPLEMENTED,
                    "must specify a method name to execute with POST");
            }

            ErrorRecord result = parseRPC(rq);
            if (result != null)
                return result;
        } else {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.METHOD_NOT_IMPLEMENTED,
                pr.requestMethod + " not implemented by this server or invalid");
        }

        // parse headers for all HTTP requests
        // this has to be done after the fileID was extracted which depends on
        // the request type
        final ErrorRecord hdrResult = parseHeaders(rq);
        if (hdrResult != null) {
            return hdrResult;
        }

        return null;

    }

    protected ErrorRecord parseRPC(OSDRequest rq) {
        final PinkyRequest pr = rq.getPinkyRequest();
        String methodName;
        if (pr.requestURI.charAt(0) == '/') {
            if (pr.requestURI.length() == 1) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                    ErrorCodes.METHOD_NOT_IMPLEMENTED,
                    "must specify a method name to execute with POST");
            }
            methodName = pr.requestURI.substring(1);
        } else {
            methodName = pr.requestURI;
        }

        List<Object> body = null;
        try {
            if (rq.getPinkyRequest().requestBody != null)
                body = parseJSONBody(rq);
        } catch (JSONException ex) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_RPC,
                "body contains invalid JSON", ex);
        }

        if (methodName.equals(RPCTokens.fetchGlobalMaxToken)) {
            final Operation fetchGmax = master
                    .getOperation(RequestDispatcher.Operations.FETCH_GMAX);
            rq.setOperation(fetchGmax);
            return null;
        }

        else if (methodName.equals(RPCTokens.truncateTOKEN)) {
            final Operation truncate = master.getOperation(RequestDispatcher.Operations.TRUNCATE);
            rq.setOperation(truncate);
            ErrorRecord result = truncate.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.truncateLocalTOKEN)) {
            final Operation truncate = master
                    .getOperation(RequestDispatcher.Operations.TRUNCATE_LOCAL);
            rq.setOperation(truncate);
            ErrorRecord result = truncate.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.deleteLocalTOKEN)) {
            final Operation deleteLocal = master
                    .getOperation(RequestDispatcher.Operations.DELETE_LOCAL);
            rq.setOperation(deleteLocal);
            return null;
        }

        else if (methodName.equals(RPCTokens.getProtocolVersionTOKEN)) {
            final Operation getProtocolVer = master
                    .getOperation(RequestDispatcher.Operations.GET_PROTOCOL_VERSION);
            rq.setOperation(getProtocolVer);
            ErrorRecord result = getProtocolVer.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.shutdownTOKEN)) {
            final Operation shutdown = master.getOperation(RequestDispatcher.Operations.SHUTDOWN);
            rq.setOperation(shutdown);
            return null;
        }

        else if (methodName.equals(RPCTokens.getstatsTOKEN)) {
            final Operation stats = master.getOperation(RequestDispatcher.Operations.GET_STATS);
            rq.setOperation(stats);
            return null;
        }

        else if (methodName.equals(RPCTokens.checkObjectTOKEN)) {
            final Operation checkObject = master.getOperation(RequestDispatcher.Operations.READ);
            rq.setOperation(checkObject);
            rq.getDetails().setCheckOnly(true);
            return null;
        }

        else if (methodName.equals(RPCTokens.recordRqDurationTOKEN)) {
            final Operation statConfig = master.getOperation(RequestDispatcher.Operations.STATS_CONFIG);
            rq.setOperation(statConfig);
            ErrorRecord result = statConfig.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.acquireLeaseTOKEN)) {
            final Operation acquireLease = master.getOperation(RequestDispatcher.Operations.ACQUIRE_LEASE);
            rq.setOperation(acquireLease);
            ErrorRecord result = acquireLease.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.returnLeaseTOKEN)) {
            final Operation returnLease = master.getOperation(RequestDispatcher.Operations.RETURN_LEASE);
            rq.setOperation(returnLease);
            ErrorRecord result = returnLease.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.cleanUpTOKEN)) {
            final Operation cleanup = master.getOperation(RequestDispatcher.Operations.CLEAN_UP);
            rq.setOperation(cleanup);
            ErrorRecord result = cleanup.parseRPCBody(rq, body);
            if (result != null)
                return result;
        }

        else if (methodName.equals(RPCTokens.readLocalTOKEN)) {
            final Operation readLocal = master.getOperation(RequestDispatcher.Operations.READ_LOCAL);
            rq.setOperation(readLocal);
            return null;
        }

        else {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.METHOD_NOT_IMPLEMENTED,
                methodName + " not implemented");
        }
        return null;
    }

    /**
     * Parses the all available headers
     *
     * @param rq
     *            the request to parse
     * @return null, if parsing was successful, errorMessage otherwise
     */
    protected ErrorRecord parseHeaders(OSDRequest rq) {

        final HTTPHeaders hdrs = rq.getPinkyRequest().requestHeaders;

        for (final HTTPHeaders.HeaderEntry hdr : hdrs) {

            if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XOBJECTNUMBER)) {

                try {
                    rq.getDetails().setObjectNumber(Long.parseLong(hdr.value));

                    if (rq.getDetails().getObjectNumber() < 0) {
                        return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                            ErrorCodes.INVALID_HEADER, HTTPHeaders.HDR_XOBJECTNUMBER
                                + " must contain a number >= 0");
                    }

                } catch (Exception ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XOBJECTNUMBER + " must contain a valid integer", ex);
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XCAPABILITY)) {

                try {
                    Capability cap = new Capability(hdr.value, master.getConfig().getCapabilitySecret());
                    rq.getDetails().setCapability(cap);

                } catch (JSONException ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XCAPABILITY + " is not valid JSON", ex);
                } catch (Exception ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XCAPABILITY + " is not valid and cannot be parsed", ex);
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_CONTENT_RANGE)) {

                try {
                    final String rangeHdr = hdr.value.trim();
                    // header is of format "bytes start - end /*

                    final String rangeOnly = rangeHdr.substring(rangeHdr.indexOf(' '),
                        rangeHdr.lastIndexOf('/')).trim();

                    final int indexOfMinus = rangeOnly.indexOf('-');
                    final String startRange = rangeOnly.substring(0, indexOfMinus).trim();
                    final String endRange = rangeOnly.substring(indexOfMinus + 1).trim();

                    rq.getDetails().setByteRangeStart(Long.parseLong(startRange));
                    rq.getDetails().setByteRangeEnd(Long.parseLong(endRange));
                    rq.getDetails().setRangeRequested(true);
                } catch (Exception ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_CONTENT_RANGE + " is not valid and cannot be parsed", ex);
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XLOCATIONS)) {

                final ErrorRecord result = parseLocations(rq, hdr.value);
                if (result != null) {
                    return result;
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XVERSIONNUMBER)) {

                try {
                    rq.getDetails().setObjectVersionNumber(Integer.parseInt(hdr.value));
                    rq.getDetails().setObjectVersionNumberRequested(true);

                    if (rq.getDetails().getObjectVersionNumber() < 0) {
                        return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                            ErrorCodes.INVALID_HEADER, HTTPHeaders.HDR_XVERSIONNUMBER
                                + " must contain a number >= 0");
                    }

                } catch (Exception ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XOBJECTNUMBER + " must contain a valid integer", ex);
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XFILEID)) {

                try {
                    if (validateFileId(hdr.value))
                        rq.getDetails().setFileId(hdr.value);

                } catch (Exception ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XOBJECTNUMBER + " must contain a valid integer", ex);
                }

            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XLEASETO)) {
                try {
                    Long to = Long.valueOf(hdr.value);
                    if (to < TimeSync.getGlobalTime()) {
                        return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.LEASE_TIMED_OUT,
                            "lease timed out");
                    }
                } catch (NumberFormatException ex) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                        HTTPHeaders.HDR_XLEASETO + " must contain a valid integer", ex);
                }
            } else if (hdr.name.equalsIgnoreCase(HTTPHeaders.HDR_XREQUESTID)) {
                rq.getDetails().setRequestId(hdr.value);
            }
        }

        return null;
    }

    /**
     * Parse the Locations header
     *
     * @param rq
     *            the request
     * @param loc
     *            the locations header string
     * @return an error, null if successfull
     */
    protected ErrorRecord parseLocations(OSDRequest rq, String loc) {
        int locVer = -1;
        try {
            int lastComma = loc.lastIndexOf(',');
            String lastArg = loc.substring(lastComma + 1, loc.length() - 1);
            if (lastArg.indexOf('"') >= 0) {
                // last arg is replication policy
                int sndLastComma = loc.substring(0, lastComma).lastIndexOf(',');
                String tmp = loc.substring(sndLastComma + 1, lastComma);
                tmp = tmp.trim();
                locVer = Integer.parseInt(tmp);
            } else {
                // last arg is version number
                lastArg = lastArg.trim();
                locVer = Integer.parseInt(lastArg);
            }

        } catch (Exception e) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, null, e);
            }
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                HTTPHeaders.HDR_XLOCATIONS + " cannot be parsed", e);
        }

        Locations cachedLoc = xLocCache.getLocations(rq.getDetails().getFileId());
        if ((cachedLoc != null) && (cachedLoc.getVersion() == locVer)) {
            rq.getDetails().setLocationList(cachedLoc);
        } else {
            try {
                Locations receivedLoc = new Locations(new JSONString(loc));
                if ((receivedLoc.getNumberOfReplicas() == 0) && (cachedLoc == null)) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.NEED_FULL_XLOC,
                        "X-Location not in cache, resend full X-Location list");
                }

                if ((cachedLoc != null) && (cachedLoc.getVersion() > locVer)) {
                    return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.XLOC_OUTDATED,
                        "outdated X-Location sent, version is " + cachedLoc.getVersion() + " sent "
                            + locVer);
                }

                xLocCache.update(rq.getDetails().getFileId(), receivedLoc);
                rq.getDetails().setLocationList(receivedLoc);
            } catch (JSONException ex) {
                return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                    "X-Location header is not valid JSON", ex);
            }
        }

        //resolve addresses
        try {
            rq.getDetails().getLocationList().resolveAll();
        } catch (UnknownUUIDException ex) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.INVALID_HEADER,
                    "Cannot resolve uuid in X-Locations list: "+ex);
        }

        // find the location for the currentReplica (i.e. the one this OSD is
        // in)
        // FIXME: get correct schema!
        final Location currentReplica = rq.getDetails().getLocationList().getLocation(
            master.getConfig().getUUID());
        if (currentReplica == null) {
            return new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrorCodes.NOT_IN_XLOC, "this OSD ("
                + master.getConfig().getUUID() + ") is not part of the current X-Location list");
        }
        rq.getDetails().setCurrentReplica(currentReplica);
        return null;
    }

    /**
     * Extracts the file ID from the URI.
     *
     * @param pr
     *            the Pinky Request
     * @return the file ID
     */
    private String parseFileIdFromURI(PinkyRequest pr) {
        if (pr.requestURI.length() == 0)
            return null;
        String rqURI = pr.requestURI;
        if (pr.requestURI.charAt(0) == '/') {
            rqURI = pr.requestURI.substring(1);
        }
        if (validateFileId(rqURI)) {
            return rqURI;
        } else {
            return null;
        }
    }

    /**
     * Parses the JSON content of the body.
     *
     * @param rq
     *            the OSD request
     * @return an object representation of the JSON body
     * @throws org.xtreemfs.foundation.json.JSONException
     */
    private List<Object> parseJSONBody(Request rq) throws JSONException {
        if (rq.getPinkyRequest().getBody() == null) {
            throw new JSONException("body is empty");
        }
        return (List<Object>) JSONParser.parseJSON(new JSONString(new String(rq.getPinkyRequest()
                .getBody())));
    }

    /**
     * remove an entry for a file from the X-Location cache
     *
     * @param op
     *            StageMethod to execute
     */
    private void removeCacheEntry(StageMethod op) {
        this.xLocCache.removeLocations(op.getRq().getDetails().getFileId());
        this.methodExecutionSuccess(op, StageResponseCode.OK);
    }

    public static boolean validateFileId(String requestURI) {
        for (int i = 0; i < requestURI.length(); i++) {
            char c = requestURI.charAt(i);

            if ((c < '0') || ((c > ':') && (c < 'A')) || (c > 'F')) {
                return false;
            }
        }
        return true;
    }
}
