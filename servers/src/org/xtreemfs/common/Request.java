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
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */
package org.xtreemfs.common;

import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.osd.ErrorRecord;

/**
 *
 * 29.09.2008
 *
 * @author clorenz
 */
public abstract class Request {

	/**
	 * The HTTP request object.
	 */
	private PinkyRequest pinkyRequest;

	/**
	 * request id used for tracking.
	 */
	protected long requestId;

	/**
	 * error record, if an error occurred
	 */
	protected ErrorRecord error;

	private Object attachment;

	private long enqueueNanos, finishNanos;

	protected Request(PinkyRequest pr) {
		this.setPinkyRequest(pr);
	}

	public long getRequestId() {
		return requestId;
	}

	public ErrorRecord getError() {
		return error;
	}

	public void setError(ErrorRecord error) {
		this.error = error;
	}

	public PinkyRequest getPinkyRequest() {
		return pinkyRequest;
	}

	public void setPinkyRequest(PinkyRequest pinkyRequest) {
		this.pinkyRequest = pinkyRequest;
	}

	public Object getAttachment() {
		return attachment;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	public long getEnqueueNanos() {
		return enqueueNanos;
	}

	public void setEnqueueNanos(long enqueueNanos) {
		this.enqueueNanos = enqueueNanos;
	}

	public long getFinishNanos() {
		return finishNanos;
	}

	public void setFinishNanos(long finishNanos) {
		this.finishNanos = finishNanos;
	}
}
