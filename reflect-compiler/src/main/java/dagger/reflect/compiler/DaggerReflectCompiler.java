/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger.reflect.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import dagger.Component;
import dagger.reflect.DaggerReflect;

import static dagger.reflect.compiler.DaggerReflectUtils.getAnnotationMirror;
import static dagger.reflect.compiler.DaggerReflectUtils.getAnnotationValue;
import static dagger.reflect.compiler.DaggerReflectUtils.toLowerCaseFirstLetter;
import static java.util.Collections.singleton;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING;

@IncrementalAnnotationProcessor(ISOLATING)
@AutoService(Processor.class)
public final class DaggerReflectCompiler extends AbstractProcessor {
  private Filer filer;
  private Messager messager;
  private @Nullable AnnotationSpec generatedAnnotation;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();

    String generatedAnnotationName =
        processingEnv.getSourceVersion().compareTo(RELEASE_8) <= 0
            ? "javax.annotation.Generated"
            : "javax.annotation.processing.Generated";
    TypeElement generatedAnnotationType =
        processingEnv.getElementUtils().getTypeElement(generatedAnnotationName);
    if (generatedAnnotationType != null) {
      generatedAnnotation =
          AnnotationSpec.builder(ClassName.get(generatedAnnotationType))
              .addMember("value", "$S", getClass().getName())
              .addMember("comments", "$S", "https://github.com/JakeWharton/dagger-reflect")
              .build();
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return singleton(Component.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<? extends Element> candidates = roundEnv.getElementsAnnotatedWith(Component.class);
    for (Element candidate : candidates) {
      TypeElement component = (TypeElement) candidate;
      ClassName componentName = ClassName.get(component);
      TypeElement builder = findBuilder(component);
      ClassName builderName = builder != null ? ClassName.get(builder) : null;
      TypeElement factory = findFactory(component);
      ClassName factoryName = factory != null ? ClassName.get(factory) : null;

      List<AnnotationValue> dependencies = findDependencies(component);

      TypeSpec.Builder typeBuilder =
              createComponent(componentName, builderName, factoryName, dependencies)
                      .toBuilder()
                      .addOriginatingElement(component);
      if (generatedAnnotation != null) {
        typeBuilder.addAnnotation(generatedAnnotation);
      }
      JavaFile file =
          JavaFile.builder(componentName.packageName(), typeBuilder.build())
              .addFileComment("Generated by Dagger's reflect-compiler. Do not modify!")
              .build();
      try {
        file.writeTo(filer);
      } catch (Exception e) {
        messager.printMessage(ERROR, "Unable to write component implementation: " + e, component);
      }
    }
    return false;
  }

  private static @Nullable TypeElement findBuilder(TypeElement component) {
    for (Element enclosed : component.getEnclosedElements()) {
      if (enclosed.getAnnotation(Component.Builder.class) != null) {
        return (TypeElement) enclosed;
      }
    }
    return null;
  }

  private @Nullable List<AnnotationValue> findDependencies(TypeElement component) {
    AnnotationMirror am = getAnnotationMirror(component, Component.class);
    if (am == null) {
      return null;
    }
    AnnotationValue av = getAnnotationValue(am, "dependencies");
    if (av != null) {
      return (List<AnnotationValue>) av.getValue();
    }

    return null;
  }

  private static @Nullable TypeElement findFactory(TypeElement component) {
    for (Element enclosed : component.getEnclosedElements()) {
      if (enclosed.getAnnotation(Component.Factory.class) != null) {
        return (TypeElement) enclosed;
      }
    }
    return null;
  }

  private static TypeSpec createComponent(
          ClassName component,
          @Nullable ClassName builder,
          @Nullable ClassName factory,
          @Nullable List<AnnotationValue> dependencies) {
    String componentName = "Dagger" + String.join("_", component.simpleNames());
    TypeSpec.Builder type =
        TypeSpec.classBuilder(componentName)
            .addModifiers(PUBLIC, FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PRIVATE)
                    .addStatement("throw new $T()", AssertionError.class)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("create")
                    .addModifiers(PUBLIC, STATIC)
                    .returns(component)
                    .addStatement("return $T.create($T.class)", DaggerReflect.class, component)
                    .build());
    if (builder != null) {
      type.addMethod(
          MethodSpec.methodBuilder("builder")
              .addModifiers(PUBLIC, STATIC)
              .returns(builder)
              .addStatement("return $T.builder($T.class)", DaggerReflect.class, builder)
              .build());
    }
    if (factory != null) {
      type.addMethod(
          MethodSpec.methodBuilder("factory")
              .addModifiers(PUBLIC, STATIC)
              .returns(factory)
              .addStatement("return $T.factory($T.class)", DaggerReflect.class, factory)
              .build());
    }

    if (dependencies != null && builder == null) {
      ClassName builderClassName = ClassName.bestGuess("Builder");
      TypeSpec.Builder componentBuilder = TypeSpec.interfaceBuilder(builderClassName)
              .addModifiers(PUBLIC, STATIC)
              .addAnnotation(Component.Builder.class);

      componentBuilder.addMethod(MethodSpec.methodBuilder("build")
              .addModifiers(PUBLIC, ABSTRACT)
              .returns(component)
              .build());

      for (AnnotationValue dependency : dependencies) {

        ClassName dependencyClassName = ClassName.bestGuess(dependency.getValue().toString());

        String methodBuilderName = toLowerCaseFirstLetter(dependencyClassName.simpleName());
        componentBuilder.addMethod(MethodSpec.methodBuilder(methodBuilderName)
                .addModifiers(PUBLIC, ABSTRACT)
                .addParameter(dependencyClassName, methodBuilderName)
                .returns(builderClassName)
                .build());
      }


      type.addType(componentBuilder.build());


      type.addMethod(
              MethodSpec.methodBuilder("builder")
                      .addModifiers(PUBLIC, STATIC)
                      .returns(builderClassName)
                      .addStatement("return $T.builder($T.class, $T.class)",
                              DaggerReflect.class,
                              builderClassName,
                              component)
                      .build());
    }
    return type.build();
  }
}
