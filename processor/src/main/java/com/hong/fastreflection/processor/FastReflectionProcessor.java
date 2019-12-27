package com.hong.fastreflection.processor;

import com.hong.fastreflection.annotation.FastReflectionTag;
import com.hong.fastreflection.annotation.Greet;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FastReflectionProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.add(FastReflectionTag.class.getCanonicalName());
        return set;
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {


        Collection<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(FastReflectionTag.class);
        List<TypeElement> types = ElementFilter.typesIn(annotatedElements);
        String packageName = null;
        String[] names = null;

        for (TypeElement type : types) {
            PackageElement packageElement = (PackageElement) type.getEnclosingElement();
            packageName = packageElement.getQualifiedName().toString();
            names = type.getAnnotation(Greet.class).value();
        }

        System.out.println("GreetProcessor process");

        if (packageName == null) {
            return false;
        }

        StringBuilder builder = new StringBuilder()
                .append("package " + packageName + ";\n\n")
                .append("public class Greeter {\n\n")
                .append("   public static String hello() {\n")
                .append("       return \"Hello ");

        for (int i = 0; i < names.length; i++) {
            if (i == 0) {
                builder.append(names[i]);
            } else {
                builder.append(", ").append(names[i]);
            }
        }

        builder.append("!\";\n")
                .append("   }\n")
                .append("}\n");

        try {
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(packageName + ".Greeter");
            Writer writer = javaFileObject.openWriter();
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}