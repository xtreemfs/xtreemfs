/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.xtreemfs.foundation.IntervalVersionAVLTree;
import org.xtreemfs.foundation.IntervalVersionTree.Interval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.stages.StorageStage;


/**
 * Class to wrap an IntervalVersionTree that is backed by a log file. <br>
 * Each insert is appended to a log which can be used to recreate the tree. <br>
 * For storage optimization it is advised to make snapshots of the trees after some time.
 */
public class IntervalVersionTreeLog {

    StorageStage           storageStage;
    IntervalVersionAVLTree tree;

    File                   vtLogFile;

    /** Size in bytes of the input buffer used for reading and writing */
    private static int     IO_BUFSIZE = 8192;


    public IntervalVersionTreeLog(File vtLogFile) throws IOException {
        this.vtLogFile = vtLogFile;
        tree = new IntervalVersionAVLTree();

        if (!vtLogFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "IntervalVersionTreeLog file created.");
            vtLogFile.createNewFile();
        }
    }

    public void insert(long begin, long end, long version) throws IOException {
        // TODO (jdillmann): Investigate if it would be faster to keep the handle open. What about mem?

        if (vtLogFile == null) {
            throw new FileNotFoundException();
        }

        if (!vtLogFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "IntervalVersionTreeLog file created.");
            vtLogFile.createNewFile();
        }

        // TODO (jdillmann): Trigger save() if a overwritten threshold is reached

        OutputStream out = null;
        try {
            out = new FileOutputStream(vtLogFile, true);
            out = new BufferedOutputStream(out, IO_BUFSIZE);
            append(begin, end, version, out);
        } finally {
            if (out != null)
                out.close();
        }
    }

    // @Override
    public List<Interval> getVersions(long begin, long end) {
        return tree.getVersions(begin, end);
    }

    void load() throws IOException {
        if (vtLogFile == null) {
            throw new FileNotFoundException();
        }

        if (!vtLogFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "IntervalVersionTreeLog file does not exist.");
            return;
        }

        InputStream in = null;
        try {
            // Create a new empty tree.
            tree = new IntervalVersionAVLTree();
            
            // Load data into it
            in = new BufferedInputStream(new FileInputStream(vtLogFile), IO_BUFSIZE);
            load(tree, in);
        } finally {
            if (in != null)
                in.close();
        }
    }

    void save() throws IOException {
        if (vtLogFile == null) {
            throw new FileNotFoundException();
        }

        if (!vtLogFile.exists()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "IntervalVersionTreeLog file created.");
            vtLogFile.createNewFile();
        }
        
        OutputStream out = null;
        try {
            out = new FileOutputStream(vtLogFile, false);
            ((FileOutputStream) out).getChannel().truncate(0);
            out = new BufferedOutputStream(out, IO_BUFSIZE);
            save(tree, out);
        } finally {
            if (out != null)
                out.close();
        }

        // Since the tree has been written as a whole, the overwrite counter can be reset.
        tree.resetOverwrites();
    }

    /**
     * Reads long triples from the input stream and builds the tree. <br>
     * Ensures the input stream is containing only complete triples.
     * 
     * @param tree
     * @param inputStream
     * @throws IOException
     */
    void load(IntervalVersionAVLTree tree, InputStream inputStream) throws IOException {
        ObjectInputStream objectStream = new ObjectInputStream(inputStream);

        boolean complete = true;
        try {
            long begin, end, version;
            while(true) {
                begin = objectStream.readLong();

                complete = false;
                end = objectStream.readLong();
                version = objectStream.readLong();
                complete = true;

                tree.insert(begin, end, version);
            }
        } catch (EOFException e) {
            if (!complete) {
                throw new IOException("IntervalVersionTree is corrupt. Does not only contain valid long triples.");
            }
        }
    }


    /**
     * Writes long triples from the tree nodes to the input stream.
     * 
     * @param tree
     * @param outputStream
     * @throws IOException
     */
    void save(IntervalVersionAVLTree tree, OutputStream outputStream) throws IOException {
        ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);

        for (Interval i : tree.serialize()) {
            objectStream.writeLong(i.begin);
            objectStream.writeLong(i.end);
            objectStream.writeLong(i.version);
        }
        objectStream.flush();
    }

    /**
     * Append the interval to the log
     * 
     * @param begin
     * @param end
     * @param version
     * @param out
     * @throws IOException
     */
    void append(long begin, long end, long version, OutputStream out) throws IOException {
        ObjectOutputStream objectStream = new ObjectOutputStream(out);
        objectStream.writeLong(begin);
        objectStream.writeLong(end);
        objectStream.writeLong(version);
        objectStream.flush();
    }
}
