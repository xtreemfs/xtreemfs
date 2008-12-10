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
package org.xtreemfs.mrc;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.storage.SliceID;

/**
 *
 * 29.09.2008
 *
 * @author clorenz
 */
public final class RequestDetails {
	public SliceID sliceId;

	public String userId;

	public boolean superUser;

	public List<String> groupIds;

	public boolean authenticated;

	public boolean authorized;

	public boolean persistentOperation;

	public Map<String, Object> context;

	/**
	 *
	 */
	public RequestDetails() {
		sliceId = null;
		userId = null;
		authenticated = false;
		authorized = false;
	}
}
