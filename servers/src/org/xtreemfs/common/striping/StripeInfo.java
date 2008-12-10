/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jesús Malo (BSC), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.striping;

/**
 * It encapsulates the information related to a stripe
 *
 * @author Jesús Malo (jmalo)
 */
public class StripeInfo {
	public final Long objectNumber; // Relative object number
	public final Long OSD; // Relative osd number
	public final Long firstByte; // Relative first byte offset
	public final Long lastByte; // Relative last byte offset

	/**
	 * Creates a new instance of StripeInfo
	 *
	 * @param r
	 *            Relative object number
	 * @param o
	 *            Relative OSD position (it begins at 0)
	 * @param f
	 *            Relative offset of the first byte of the stripe
	 * @param l
	 *            Relative offset of the last byte of the stripe
	 * @pre (r >= 0) && (o >= 0) && (f >= 0) && (l >= 0)
	 */
	public StripeInfo(long r, long o, long f, long l) {
		assert ((r >= 0) && (o >= 0) && (f >= 0) && (l >= 0)) : "r = " + r
				+ ", o = " + o + ", f = " + f + ", l = " + l;

		objectNumber = Long.valueOf(r);
		OSD = Long.valueOf(o);
		firstByte = Long.valueOf(f);
		lastByte = Long.valueOf(l);
	}

	public boolean equals(Object obj) {

		if (this == obj)
			return true;

		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		final StripeInfo toCompare = (StripeInfo) obj;
		return objectNumber.equals(toCompare.objectNumber)
				&& OSD.equals(toCompare.OSD)
				&& firstByte.equals(toCompare.firstByte)
				&& lastByte.equals(toCompare.lastByte);
	}

	public int hashCode() {
		return objectNumber.hashCode() + OSD.hashCode() + firstByte.hashCode()
				+ lastByte.hashCode();
	}

	@Override
	public String toString() {
		return "StripeInfo: object "+objectNumber+" on osd "+OSD+" with bytes from "+firstByte+" to "+lastByte;
	}
}
