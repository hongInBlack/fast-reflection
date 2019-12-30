package com.hong.fastreflection.processor;

import com.hong.fastreflection.annotation.FastReflectionTag;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FastReflectionProcessor extends AbstractProcessor {

    private Filer mFilerUtils;       // 文件管理工具类，可以用于生成java源文件
    private Types mTypesUtils;        // 类型处理工具类，
    private Elements mElementsUtils;  // Element处理工具类，获取Element的信息
    private Messager mMessager;       // 用于打印信息
    final private String tag = "----fast-reflection----";

    private ListMap<TypeElement, ExecutableElement> methodsByClass;

    private final String packageName = "com.hong.fastreflection";
    private final String className = "FastReflectionImp2";


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFilerUtils = processingEnv.getFiler();
        mTypesUtils = processingEnv.getTypeUtils();
        mElementsUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.add(FastReflectionTag.class.getCanonicalName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        methodsByClass = new ListMap<>();
        Collection<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(FastReflectionTag.class);
        List<ExecutableElement> elements = ElementFilter.methodsIn(annotatedElements);
        collectSubscribers(elements);
        try {
            createJavaFile();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void collectSubscribers(List<ExecutableElement> elements) {
        for (ExecutableElement method : elements) {
            if (checkHasNoErrors(method)) {
                TypeElement classElement = (TypeElement) method.getEnclosingElement();
                methodsByClass.putElement(classElement, method);
            }
        }
        for (TypeElement typeElement : methodsByClass.keySet()) {
            TypeElement superclass;
            TypeElement element = typeElement;
            while (true) {
                superclass = getSuperclass(element);
                if (superclass == null) {
                    break;
                }
                if (methodsByClass.containsKey(superclass)) {
                    methodsByClass.get(typeElement).addAll(methodsByClass.get(superclass));
                }
                element = superclass;
            }
        }
    }


    private boolean checkHasNoErrors(ExecutableElement element) {
        if (element.getModifiers().contains(Modifier.STATIC)) {
            printMessage(element.getSimpleName() + " method must not be static");
            return false;
        }

        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            printMessage(element.getSimpleName() + " method must be public");
            return false;
        }
        return true;
    }

    private void createJavaFile() throws ClassNotFoundException {

//        public Object invoke(Object obj, String method, List<Object> args) {
//            if (obj.getClass().getTypeName().equals("clazz")) {
//                if (method.equals("doTest")
//                        && args.size() == 2
//                        && args.get(0).getClass().getTypeName().equals("string")
//                        && args.get(1).getClass().getTypeName().equals("string")
//                ) {
//                    return ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
//                }
//                if (method.equals("do3Test")
//                        && args.size() == 0
//                ) {
//                    ((Test3) obj).do3Test();
//                    return null;
//                }
//                return null;
//            }
//            return null;
//        }
        // 构造一个方法
        MethodSpec.Builder builder = MethodSpec.methodBuilder("invoke")      // 名称
                .addModifiers(Modifier.PUBLIC)                            // 修饰
                .returns(Object.class)                                    // 返回
                .addParameter(Object.class, "obj")                // 参数
                .addParameter(String.class, "method")              // 参数
                .addParameter(Object[].class, "args");              // 参数


        for (TypeElement element : methodsByClass.keySet()) {
            printMessage("class:  " + element.toString());

            builder.beginControlFlow("if (obj.getClass().getTypeName().equals($S))", element.toString());

            List<ExecutableElement> elements = methodsByClass.get(element);
            for (ExecutableElement method : elements) {
                printMessage("method:  " + method.toString());

//                if (method.equals("doTest")
//                        && args.size() == 2
//                        && args.get(0).getClass().getTypeName().equals("string")
//                        && args.get(1).getClass().getTypeName().equals("string")
//                ) {
//                    return ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
//                }
                StringBuilder sb = new StringBuilder();
                sb.append(" if (method.equals($S)");
                List<? extends VariableElement> parameters = method.getParameters();
                if (parameters != null && !parameters.isEmpty()) {
                    sb.append("\n && args.length == " + parameters.size());
                    for (int i = 0; i < parameters.size(); i++) {

                        sb.append(MessageFormat.format("\n && args[{0}] instanceof {1}",
                                i,
                                parameters.get(i).asType().toString()));
                    }
                }
                sb.append(")");

                String methodName = getMethodName(method.getSimpleName().toString());
                builder.beginControlFlow(sb.toString(), methodName);
//                if ("void".equals(method.getReturnType().toString())) {
////                    builder.addCode("$[");
////                    builder.addCode("(($T) obj).$S",
////                            Class.forName(element.asType().toString()), methodName);
////                    builder.addCode(code.build());
////                    builder.addCode(";\n$]");
////                    builder.addStatement("return null");
//                } else {
//                    builder.addCode("(($T) obj).$S",
//                            Class.forName(element.asType().toString()), methodName);
////                    builder.addCode(code.build());
//                    builder.addCode(";\n");
//                    builder.addStatement("return null");
//                }
//                return ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
                String returnType = method.getReturnType().toString();
                //
                StringBuilder sb2 = new StringBuilder();
                sb2.append(MessageFormat.format("(({0}) obj).{1}(",
                        element.asType().toString(),
                        methodName));

                if (parameters != null && !parameters.isEmpty()) {
                    for (int i = 0; i < parameters.size(); i++) {
                        if (i > 0) {
                            sb2.append(",");
                        }
                        sb2.append(MessageFormat.format("({0}) (args[{1}])",
                                parameters.get(i).asType().toString(),
                                i));
                    }
                }
                sb2.append(")");

                if ("void".equals(returnType)) {
                    //    ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
                    builder.addStatement(sb2.toString());
                    builder.addStatement("return null");
                } else {
                    builder.addStatement("return " + sb2.toString());
                    //   return ((Test3) obj).do2Test2((String) (args.get(0)), (String) (args.get(1)));
                }
                builder.endControlFlow();
            }

            builder.addStatement("return null")
                    .endControlFlow();
        }

        builder.addStatement("return null");
        MethodSpec invoke = builder.build();

        // 构造一个类
        // 名称
        TypeSpec FastReflectionImp = TypeSpec.classBuilder(className)
                .addJavadoc(" create Java File by build  ")
                .addModifiers(Modifier.PUBLIC)
                // 方法
                .addMethod(invoke)
                .build();

        //生成一个Java文件
        JavaFile javaFile = JavaFile.builder(packageName, FastReflectionImp)
                .build();

        try {
            JavaFileObject javaFileObject = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + className);
            Writer writer = javaFileObject.openWriter();
            writer.write(javaFile.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getMethodName(String s) {
        if (s.contains("(")) {
            return s.substring(0, 1 + s.indexOf("("));
        }
        return s;
    }


    private TypeElement getSuperclass(TypeElement type) {
        if (type.getSuperclass().getKind() == TypeKind.DECLARED) {
            TypeElement superclass = (TypeElement) processingEnv.getTypeUtils().asElement(type.getSuperclass());
            String name = superclass.getQualifiedName().toString();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
                // Skip system classes, this just degrades performance
                return null;
            } else {
                return superclass;
            }
        } else {
            return null;
        }
    }


    private void printMessage(String s) {
        mMessager.printMessage(Diagnostic.Kind.WARNING, tag + " " + s);
    }
}