package org.xtreemfs.interfaces.utils;


public class ONCRPCRecordFragmentHeader {


    public static int getFragmentHeaderSize() {
        return Integer.SIZE/8;
    }

    public static int getFragmentLength(int fragmentHeader) {
        return fragmentHeader ^ (1 << 31);
    }

    public static boolean isLastFragment(int fragmentHeader) {
        return (fragmentHeader >> 31) != 0;
    }

    public static int getFragmentHeader(int fragmentLength, boolean isLastFragment) {
        if (isLastFragment) {
            return fragmentLength | (1 << 31);
        } else {
            return fragmentLength;
        }
    }

};
