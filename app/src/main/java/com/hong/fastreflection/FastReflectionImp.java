package com.hong.fastreflection;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author huagzhihong
 * @version 1.0
 * @description
 * @date 2019/12/27
 */
public class FastReflectionImp {

    public Object newObj(String clazz, List<Object> args) {
        return null;
    }

    public Object invoke(Object obj, String method, List<Object> args) {
        return null;
    }

    public Object getField(Object obj, String fieldName) {
        String name = obj.getClass().getName();
        if ("sdfsd".equals(obj.getClass().getName())) {
            switch (fieldName) {
                case "a":
                    return obj.a;
                case "b":
                    return obj.b;
            }
            return null;
        }

        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.get()
            field.getName()
        }
        return null;
    }

}
