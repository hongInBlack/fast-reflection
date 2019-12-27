package com.hong.fastreflection.processor;

/**
 * @author Administrator
 * @version 1.0
 * @description
 * @date 2019/12/27
 */

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.zhangke.annotations.BindView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;
    private Types typeUtils;

    /**
     * `
     * 初始化操作
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        typeUtils = processingEnvironment.getTypeUtils();
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    /**
     * 返回此Porcessor可以处理的注解操作
     *
     * @return
     */
    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    /**
     * 返回此注释 Processor 支持的最新的源版本
     * <p>
     * 该方法可以通过注解@SupportedSourceVersion指定
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 返回此 Processor 支持的注释类型的名称。结果元素可能是某一受支持注释类型的规范（完全限定）名称。它也可能是 " name.*" 形式的名称，表示所有以 " name." 开头的规范名称的注释类型集合。最后， "*" 自身表示所有注释类型的集合，包括空集。注意，Processor 不应声明 "*"，除非它实际处理了所有文件；声明不必要的注释可能导致在某些环境中的性能下降。
     * <p>
     * 该方法可以通过注解@SupportedSourceVersion指定
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.add(BindView.class.getCanonicalName());
        return set;
    }


    /**
     * 注解处理器的核心方法，处理具体的注解
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 通过roundEnvironment扫描所有的类文件，获取所有存在指定注解的字段
        Map<TypeElement, List<FieldViewBinding>> targetMap = getTargetMap(roundEnvironment);


        createJavaFile(targetMap.entrySet());
        return false;
    }

    /**
     * 获取所有存在注解的类
     *
     * @param roundEnvironment
     * @return
     */
    private Map<TypeElement, List<FieldViewBinding>> getTargetMap(RoundEnvironment roundEnvironment) {
        /**
         * 键：TypeElement，指定Activity；
         * 值：List<FieldViewBinding>，activiyt中所有的注解修饰的字段
         */
        Map<TypeElement, List<FieldViewBinding>> targetMap = new HashMap<>();

        // 1、获取代码中所有使用@BindView注解修饰的字段
        Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        for (Element element : annotatedElements) {
            // 获取字段名称 (textView)
            String fieldName = element.getSimpleName().toString();
            // 获取字段类型 (android.widget.TextView)
            TypeMirror fieldType = element.asType();
            // 获取注解元素的值 (R.id.textView)
            int viewId = element.getAnnotation(BindView.class).value();

            // 获取声明element的全限定类名 (com.zhangke.simplifybutterknife.MainActivity)
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            List<FieldViewBinding> list = targetMap.get(typeElement);
            if (list == null) {
                list = new ArrayList<>();
                targetMap.put(typeElement, list);
            }

            list.add(new FieldViewBinding(fieldName, fieldType, viewId));

        }

        return targetMap;
    }

    /**
     * 创建Java文件
     * @param entries
     */
    private void createJavaFile(Set<Map.Entry<TypeElement, List<FieldViewBinding>>> entries) {
        for (Map.Entry<TypeElement, List<FieldViewBinding>> entry : entries) {
            TypeElement typeElement = entry.getKey();
            List<FieldViewBinding> list = entry.getValue();
            if (list == null || list.size() == 0) {
                continue;
            }


            // 获取包名
            String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            // 根据旧Java类名创建新的Java文件
            String className = typeElement.getQualifiedName().toString().substring(packageName.length() + 1);
            String newClassName = className + "_ViewBinding";


            MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.bestGuess(className), "target");
            for (FieldViewBinding fieldViewBinding : list) {
                String packageNameString = fieldViewBinding.getFieldType().toString();
                ClassName viewClass = ClassName.bestGuess(packageNameString);
                methodBuilder.addStatement
                        ("target.$L=($T)target.findViewById($L)", fieldViewBinding.getFieldName()
                                , viewClass, fieldViewBinding.getViewId());
            }


            TypeSpec typeBuilder = TypeSpec.classBuilder(newClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(methodBuilder.build())
                    .build();


            JavaFile javaFile = JavaFile.builder(packageName, typeBuilder)
                    .addFileComment("Generated code from Butter Knife. Do not modify!")
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}