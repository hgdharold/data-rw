package com.hgd.data.rw.west.test_byte_buddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class TestByteBuddy {

    public static void main(String[] ags) throws InstantiationException, IllegalAccessException {
        TestTarget target = new TestTarget();
        Class<? extends TestTarget> proxyClass = new ByteBuddy()
                .subclass(target.getClass())
                .method(any()).intercept(
                        InvocationHandlerAdapter.of(
                                (proxy, method, args) -> {
                                    // put your code here
                                    if (method.getName().equals("say123") && args.length == 3) {
                                        return args[0] + " # " + args[1] + " # " + args[2];
                                    }
                                    return method.invoke(target, args);
                                }
                        )
                )
                .make()
                .load(TestByteBuddy.class.getClassLoader())
                .getLoaded();
        TestTarget proxyIns = proxyClass.newInstance();

        System.out.println(proxyIns.say123("1", 2, "3")); // 1 # 2 # 3
        System.out.println(proxyIns.say123("1", 2)); // 1 * 2
        System.out.println(proxyIns.say456("1", 2, "3")); // 1 ! 2 ! 3

    }
}

