package com.hgd.data.rw.customized;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.Format;

/**
 * @author hgd
 * @date 2020/08/12
 * @description 主要是解决以下问题：当cell里是数字但没有设置类型时，默认会使用科学计数法。此处采用一般的数字表示
 */
public class PlainNumFormatterCglibProxy implements MethodInterceptor {
    public static final String INTERCEPT_METHOD_1 = "formatRawCellContents";
    public static final String CELL_DEFAULT_STYLE = "General";

    Format plainNumberFormat = new DecimalFormat("#.##########");
    private Object target;

    public PlainNumFormatterCglibProxy(Object target) {
        this.target = target;
    }

    public DataFormatter createProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());
        enhancer.setCallback(this);
        Object proxyObj = enhancer.create();
        return (DataFormatter) proxyObj;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (INTERCEPT_METHOD_1.equals(method.getName()) && args.length == 3) {
            if (CELL_DEFAULT_STYLE.equals(args[2])) {
                return plainNumberFormat.format(args[0]);
            }
        }
        return method.invoke(target, args);
    }
}
