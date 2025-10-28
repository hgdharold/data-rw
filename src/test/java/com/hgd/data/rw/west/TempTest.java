package com.hgd.data.rw.west;

import java.text.DecimalFormat;

public class TempTest {
    public static void main(String[] args) {

        // 下面的数字因为太长，所以会丢失精度，实际的值是：12345678901234.123046875
        double num = 12345678901234.12345;
        DecimalFormat df = new DecimalFormat("#.##########");
        System.out.println(df.format(num));
    }
}
