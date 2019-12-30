package com.hong.fastreflection;

import java.util.List;

/**
 * @author huagzhihong
 * @version 1.0
 * @description
 * @date 2019/12/27
 */
public class FastReflectionImp {


    public Object invoke(Object obj, String method, List<Object> args) {
        if (obj.getClass().getTypeName().equals("clazz")) {
            if (method.equals("doTest")
                    && args.size() == 2
                    && args.get(0) instanceof java.lang.String
                    && args.get(1).getClass().getTypeName().equals("string")
            ) {
                return ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
            }
            if (method.equals("do3Test")
                    && args.size() == 0
            ) {
                ((Test3) obj).do3Test();
                return null;
            }
            return null;
        }
        return null;
    }

}
