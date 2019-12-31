package com.hong.fastreflection.processor;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.hong.fastreflection.annotation.FastReflectionTag;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
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
import javax.tools.JavaFileObject;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FastReflectionProcessor extends AbstractProcessor {

    private final static String className = "FastReflectionImp";
    private final static String packageName = "com.hong.fastreflection";

    private Messager mMessager;
    final private String tag = "----fast-reflection----";
    private ListMap<TypeElement, ExecutableElement> methodsByClass;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnv.getMessager();
//        try {
//            JavaFileObject javaFileObject = processingEnv.getFiler()
//                    .createSourceFile(packageName + "." + className);
//            Writer writer = javaFileObject.openWriter();
//            String formattedSource = "package com.hong.fastreflection;\n" +
//                    "\n" +
//                    "/** create Java File by build */\n" +
//                    "public class FastReflectionImp {\n" +
//                    "  /** auto created */\n" +
//                    "  public Object invoke(Object obj, String method, Object... args) {\n" +
//                    "    return null;\n" +
//                    "  }\n" +
//                    "}\n";
//            writer.write(formattedSource);
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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


    private void createJavaFile() {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("invoke")
                .addJavadoc("auto created")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "obj")
                .addParameter(String.class, "method")
                .addParameter(ArrayTypeName.of(Object.class), "args")
                .varargs()
                .returns(Object.class);

        builder.addCode("if(args == null){\n" +
                "            args = new Object[0];\n" +
                "        }");

        for (TypeElement element : methodsByClass.keySet()) {
            printMessage("class:  " + element.toString());
            builder.beginControlFlow("if (obj instanceof $T)", ClassName.get(element));

            List<ExecutableElement> elements = methodsByClass.get(element);
            for (ExecutableElement method : elements) {
                String methodName = getMethodName(method.getSimpleName().toString());

//                CodeBlock.Builder headCode = CodeBlock.builder();
                StringBuilder headSb = new StringBuilder();

                headSb.append(" if (method.equals($S)");
//                headCode.add(" if (method.equals($S)", methodName);
                List<? extends VariableElement> parameters = method.getParameters();
                // 入参
                final ArrayList<String> argList = new ArrayList<>();
                for (VariableElement parameter : parameters) {
                    argList.add(parameter.asType().toString());
                }
                headSb.append(" && args.length == ").append(parameters.size());
//                headCode.add(" && args.length == " + parameters.size());
                for (int i = 0; i < argList.size(); i++) {
                    headSb.append(MessageFormat.format(" && args[{0}] instanceof {1}",
                            i, getTypeString(argList.get(i))));
//                    headCode.add(MessageFormat.format(" && args[{0}] instanceof $T", i),
//                            ClassName.bestGuess(argList.get(i)));
                }
                headSb.append(")");
//                headCode.add(");\n");

                builder.beginControlFlow(headSb.toString(), methodName);
//                builder.addCode(headCode.build());
                String returnType = method.getReturnType().toString();
                StringBuilder sb2 = new StringBuilder();

                for (int i = 0; i < argList.size(); i++) {
                    if (i > 0) {
                        sb2.append(",");
                    }
                    sb2.append(MessageFormat.format("({0}) (args[{1}])",
                            argList.get(i), i));
                }
                sb2.append(")");

                sb2.insert(0, "(($T) obj).$N(");
                if (!"void".equals(returnType)) {
                    sb2.insert(0, "return ");
                }
                builder.addStatement(sb2.toString(),
                        ClassName.bestGuess(element.asType().toString()),
                        methodName);
                if ("void".equals(returnType)) {
                    builder.addStatement("return null");
                }
                builder.endControlFlow();
            }
            builder.addStatement("return null")
                    .endControlFlow();
        }

        builder.addStatement("return null");
        MethodSpec invoke = builder.build();

        TypeSpec FastReflectionImp = TypeSpec.classBuilder(className)
                .addJavadoc(" create Java File by build  ")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(invoke)
                .build();

        //create Java file
        JavaFile javaFile = JavaFile.builder(packageName, FastReflectionImp)
                .build();
        try {
            JavaFileObject javaFileObject = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + className);
            javaFileObject.delete();
            Writer writer = javaFileObject.openWriter();
            String formattedSource;
            try {
                formattedSource = new Formatter().formatSource(javaFile.toString());
            } catch (FormatterException e) {
                formattedSource = javaFile.toString();
            }
            writer.write(formattedSource);
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

    /**
     * getSuperclass
     */
    private TypeElement getSuperclass(TypeElement type) {
        if (type.getSuperclass().getKind() == TypeKind.DECLARED) {
            TypeElement superclass = (TypeElement) processingEnv.getTypeUtils().asElement(type.getSuperclass());
            String name = superclass.getQualifiedName().toString();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
                return null;
            } else {
                return superclass;
            }
        } else {
            return null;
        }
    }


    private void printMessage(String s) {
//        mMessager.printMessage(Diagnostic.Kind.WARNING, tag + " " + s);
    }

    private String getTypeString(String s) {
        if (s.equals("int")) {
            return "Integer";
        }
        if (s.equals("long")) {
            return "Long";
        }
        if (s.equals("double")) {
            return "Double";
        }
        if (s.equals("float")) {
            return "Float";
        }
        if (s.equals("boolean")) {
            return "Boolean";
        }
        if (s.equals("byte")) {
            return "Byte";
        }
        if (s.equals("short")) {
            return "Short";
        }
        if (s.equals("char")) {
            return "Character";
        }
        if (s.equals("void")) {
            return "Void";
        }
        return s;
    }
}