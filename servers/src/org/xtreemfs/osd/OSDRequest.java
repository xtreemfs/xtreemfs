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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd;

import org.xtreemfs.common.Request;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.stages.StageCallbackInterface;

/**
 * Request object.
 * 
 * @author bjko
 */
public final class OSDRequest extends Request {

	/**
	 * Request type
	 */
	public enum Type {
		READ, WRITE, DELETE, RPC, INTERNAL_EVENT, STATUS_PAGE
	};

	/**
	 * Request operation which contains state machine.
	 */
	private Operation operation;

	/**
	 * The callback, which is registered for the actual state of the operation.
	 */
	private StageCallbackInterface currentCallback;

	/**
	 * request data (e.g. object data)
	 */
	private ReusableBuffer data;

	/**
	 * Data content type (text/binary)
	 */
	private HTTPUtils.DATA_TYPE dataType;

	/**
	 * current state of the request (see operation)
	 */
	private int state;

	/**
	 * request type
	 */
	private Type type;

	/**
	 * request details (e.g. for an OSD)
	 */
	private final RequestDetails details;

	/**
	 * original osdRequest (used for "suboperations")
	 */
	private OSDRequest originalOsdRequest;

	/**
	 * list of sub-requests sent via Speedy.
	 */
	private SpeedyRequest[] httpRequests;

	public OSDRequest(long requestId) {
		super(null);
		this.requestId = requestId;
		details = new RequestDetails();
	}

	public RequestDetails getDetails() {
		return details;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public ReusableBuffer getData() {
		return data;
	}

	public HTTPUtils.DATA_TYPE getDataType() {
		return this.dataType;
	}

	public void setData(ReusableBuffer data, HTTPUtils.DATA_TYPE dataType) {
		this.data = data;
		this.dataType = dataType;
	}

	public SpeedyRequest[] getHttpRequests() {
		return httpRequests;
	}

	public void setHttpRequests(SpeedyRequest[] httpRequests) {
		this.httpRequests = httpRequests;
	}

	public String toString() {

		return getClass().getCanonicalName()
				+ " #"
				+ this.requestId
				+ "\n"
				+ "Operation       "
				+ ((this.operation == null) ? "not yet parsed" : this.operation
						.getClass().getCanonicalName())
				+ "\n"
				+ "state           "
				+ this.state
				+ "\n"
				+ "error           "
				+ this.error
				+ "\n"
				+ "data            "
				+ ((data == null) ? "null" : data.limit() + " bytes / "
						+ dataType);

	}

	public StageCallbackInterface getCurrentCallback() {
		return this.currentCallback;
	}

	public void setCurrentCallback(StageCallbackInterface actualCallback) {
		this.currentCallback = actualCallback;
	}

	public OSDRequest getOriginalOsdRequest() {
		return this.originalOsdRequest;
	}

	public void setOriginalOsdRequest(OSDRequest osdRequest) {
		this.originalOsdRequest = osdRequest;
	}

}
