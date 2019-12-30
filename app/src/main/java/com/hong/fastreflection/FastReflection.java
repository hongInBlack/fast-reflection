package com.hong.fastreflection;

import java.util.List;

/**
 * @author huagzhihong
 * @version 1.0
 * @description
 * @date 2019/12/27
 */
public class FastReflection {

    private static FastReflection instance;
    private FastReflectionImp reflectionImp;

    private FastReflection() {
        reflectionImp = new FastReflectionImp();
    }

    public static FastReflection getInstance() {
        if (instance == null) {
            instance = new FastReflection();
        }
        return instance;
    }
//
//    public Object newObj(String clazz, List<Object> args) {
//        return reflectionImp.newObj(clazz, args);
//    }


    public Object invoke(Object obj, String method, List<Object> args) {
        return reflectionImp.invoke(obj, method, args);
    }

//    public Object getField(Object obj, String fieldName) {
//        return reflectionImp.getField(obj, fieldName);
//    }

}
