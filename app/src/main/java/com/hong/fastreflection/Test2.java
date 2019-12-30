package com.hong.fastreflection;

import com.hong.fastreflection.annotation.FastReflectionTag;

/**
 * @author Administrator
 * @version 1.0
 * @description
 * @date 2019/12/27
 */
public class Test2 {

    @FastReflectionTag
    public void do2Test() {
    }

    @FastReflectionTag
    public String do2Test2(String str) {
        return "do2Test2";
    }

    @FastReflectionTag
    public String do2Test2(String str, String str2) {
        return "do2Test2" + " : " + str + " , " + str2;
    }

}
