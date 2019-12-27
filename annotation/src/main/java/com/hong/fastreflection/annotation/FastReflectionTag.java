package com.hong.fastreflection.annotation;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

/**
 * @author hong
 * @version 1.0
 * @description
 * @date 2019/12/27
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface FastReflectionTag {
}
