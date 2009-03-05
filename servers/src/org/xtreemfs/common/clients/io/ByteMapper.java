///*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//    Grid Operating System, see <http://www.xtreemos.eu> for more details.
//    The XtreemOS project has been developed with the financial support of the
//    European Commission's IST program under contract #FP6-033576.
//
//    XtreemFS is free software: you can redistribute it and/or modify it under
//    the terms of the GNU General Public License as published by the Free
//    Software Foundation, either version 2 of the License, or (at your option)
//    any later version.
//
//    XtreemFS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
//*/
///*
// * AUTHORS: Nele Andersen (ZIB), Björn Kolbeck (ZIB)
// */
//
//
//package org.xtreemfs.common.clients.io;
//
//public interface ByteMapper {
//
//    /**
//     * reads data from file.
//     * @param data a buffer of length (length+offset) in which the data is stored
//     * @param offset offset within buffer to write to
//     * @param length number of bytes to read
//     * @param filePosition offset within file
//     * @return the number of bytes read
//     * @throws java.lang.Exception
//     */
//    public int read(byte[] data, int offset, int length, long filePosition) throws Exception;
//
//    /**
//     * writes data to a file.
//     * @param data the data to write (buffer must be length+offset bytes long).
//     * @param offset the position within the buffer to start at.
//     * @param length number of bytes to write
//     * @param filePosition the offset within the file
//     * @return the number of bytes written
//     * @throws java.lang.Exception
//     */
//    public int write(byte[] data, int offset, int length, long filePosition) throws Exception;
//}
