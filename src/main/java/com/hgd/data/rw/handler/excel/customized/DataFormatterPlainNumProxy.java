package com.hgd.data.rw.handler.excel.customized;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.text.DecimalFormat;
import java.text.Format;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * 创建一个DataFormatter的代理类，用于对默认格式“常规”的数字进行格式化
 * <p>
 * Excel中cell的值基本分为两类，一类是数字，一类是字符串，
 * 字符串原样输出即可，而数字需要进行格式化，
 * 如果单元格没有设置格式，那么默认的数字格式为常规：General，对于大数字会进行科学计数法，丢失精度，不符合实际需求
 */
public class DataFormatterPlainNumProxy {
    /**
     * 需要代理的方法
     * {@link DataFormatter#formatRawCellContents(double, int, String)}
     */
    public static final String NUM_FORMAT_METHOD = "formatRawCellContents";
    /**
     * 默认的数字格式：“常规”
     */
    public static final String CELL_DEFAULT_STYLE = "General";
    /**
     * 线程安全的 DecimalFormat
     */
    private static final ThreadLocal<Format> plainNumberFormat =
            ThreadLocal.withInitial(() -> new DecimalFormat("#.##########"));

    public static DataFormatter createProxy(DataFormatter target) throws InstantiationException, IllegalAccessException {
        Class<? extends DataFormatter> proxyClass = new ByteBuddy()
                .subclass(target.getClass())
                .method(any()).intercept(
                        InvocationHandlerAdapter.of(
                                (proxy, method, args) -> {
                                    if (NUM_FORMAT_METHOD.equals(method.getName()) && args.length == 3) {
                                        if (CELL_DEFAULT_STYLE.equals(args[2])) {
                                            return plainNumberFormat.get().format(args[0]);
                                        }
                                    }
                                    return method.invoke(target, args);
                                }
                        )
                )
                .make()
                .load(DataFormatterPlainNumProxy.class.getClassLoader())
                .getLoaded();
        return proxyClass.newInstance();
    }

}
