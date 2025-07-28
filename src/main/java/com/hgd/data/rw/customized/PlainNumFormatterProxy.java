package com.hgd.data.rw.customized;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.text.DecimalFormat;
import java.text.Format;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class PlainNumFormatterProxy {
    public static final String INTERCEPT_METHOD_1 = "formatRawCellContents";
    public static final String CELL_DEFAULT_STYLE = "General";
    private static final ThreadLocal<Format> plainNumberFormat =
            ThreadLocal.withInitial(() -> new DecimalFormat("#.##########"));

    public static DataFormatter createProxy(DataFormatter target) throws InstantiationException, IllegalAccessException {
        Class<? extends DataFormatter> proxyClass = new ByteBuddy()
                .subclass(target.getClass())
                .method(any()).intercept(
                        InvocationHandlerAdapter.of(
                                (proxy, method, args) -> {
                                    if (INTERCEPT_METHOD_1.equals(method.getName()) && args.length == 3) {
                                        if (CELL_DEFAULT_STYLE.equals(args[2])) {
                                            return plainNumberFormat.get().format(args[0]);
                                        }
                                    }
                                    return method.invoke(target, args);
                                }
                        )
                )
                .make()
                .load(PlainNumFormatterProxy.class.getClassLoader())
                .getLoaded();
        return proxyClass.newInstance();
    }

}
