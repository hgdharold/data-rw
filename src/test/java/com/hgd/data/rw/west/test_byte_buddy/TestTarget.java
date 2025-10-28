package com.hgd.data.rw.west.test_byte_buddy;

public class TestTarget {
    public String say123(String str1, int int2, String str2) {
        return str1 + " " + int2 + " " + str2;
    }

    public String say123(String str1, int int2) {
        return str1 + " * " + int2;
    }

    public String say456(String str1, int int2, String str2) {
        return str1 + " ! " + int2 + " ! " + str2;
    }
}