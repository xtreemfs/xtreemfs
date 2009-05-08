package org.xtreemfs.sandbox;

import java.nio.ByteBuffer;

public class DirectIOReader {

    static {
        System.loadLibrary("readdirect");
    }

    public static native ByteBuffer loadFile(String name);

    public static void main(String[] args) {
        System.out.println("length: "
            + DirectIOReader.loadFile(args[0]).capacity());
    }

}
