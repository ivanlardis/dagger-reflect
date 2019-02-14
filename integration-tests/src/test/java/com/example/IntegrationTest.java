package com.example;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Provider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public final class IntegrationTest {
  @Parameters(name = "{0}")
  public static Object[] parameters() {
    return Backend.values();
  }

  @Parameter public Backend backend;

  @Test public void componentProvider() {
    ComponentProvider component = backend.create(ComponentProvider.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void componentProviderNull() {
    ComponentProviderNull component = backend.create(ComponentProviderNull.class);
    assertThat(component.string()).isNull();
  }

  @Test public void componentProviderQualified() {
    ComponentProviderQualified component = backend.create(ComponentProviderQualified.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void staticProvider() {
    StaticProvider component = backend.create(StaticProvider.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void bindsProvider() {
    BindsProvider component = backend.create(BindsProvider.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void bindsProviderNull() {
    BindsProviderNull component = backend.create(BindsProviderNull.class);
    assertThat(component.string()).isNull();
  }

  @Test public void bindIntoSet() {
    ignoreReflectionBackend();

    BindsIntoSet component = backend.create(BindsIntoSet.class);
    assertThat(component.strings()).containsExactly("foo");
  }

  @Test public void bindElementsIntoSet() {
    ignoreReflectionBackend();

    BindsElementsIntoSet component = backend.create(BindsElementsIntoSet.class);
    assertThat(component.strings()).containsExactly("foo");
  }

  @Test public void bindIntoMap() {
    ignoreReflectionBackend();

    BindsIntoMap component = backend.create(BindsIntoMap.class);
    assertThat(component.strings()).containsExactly("bar", "foo");
  }

  @Test public void optionalBinding() {
    OptionalBinding component = backend.create(OptionalBinding.class);
    assertThat(component.string()).isEqualTo(Optional.of("foo"));
  }

  @Test public void optionalBindingNullable() {
    ignoreCodegenBackend();

    OptionalBindingNullable component = backend.create(OptionalBindingNullable.class);
    try {
      component.string();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("@Provides[com.example.OptionalBindingNullable$Module1.foo(…)] "
              + "returned null which is not allowed for optional bindings");
    }
  }

  @Test public void optionalBindingAbsent() {
    OptionalBindingAbsent component = backend.create(OptionalBindingAbsent.class);
    assertThat(component.string()).isEqualTo(Optional.empty());
  }

  @Test public void optionalGuavaBinding() {
    ignoreReflectionBackend();

    OptionalGuavaBinding component = backend.create(OptionalGuavaBinding.class);
    assertThat(component.string()).isEqualTo(com.google.common.base.Optional.of("foo"));
  }

  @Test public void optionalGuavaBindingAbsent() {
    ignoreReflectionBackend();

    OptionalGuavaBindingAbsent component = backend.create(OptionalGuavaBindingAbsent.class);
    assertThat(component.string()).isEqualTo(com.google.common.base.Optional.absent());
  }

  @Test public void bindsInstance() {
    InstanceBinding component = backend.builder(InstanceBinding.Builder.class)
        .string("foo")
        .build();
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void bindsInstanceCalledTwice() {
    InstanceBinding component = backend.builder(InstanceBinding.Builder.class)
        .string("foo")
        .string("bar")
        .build();
    assertThat(component.string()).isEqualTo("bar");
  }

  @Test public void bindsInstanceNull() {
    InstanceBindingNull component = backend.builder(InstanceBindingNull.Builder.class)
        .string(null)
        .build();
    assertThat(component.string()).isNull();
  }

  @Test public void justInTimeConstructor() {
    JustInTimeConstructor component = backend.create(JustInTimeConstructor.class);
    assertThat(component.thing()).isNotNull();
  }

  @Test public void justInTimeGeneric() {
    JustInTimeGeneric component = backend.create(JustInTimeGeneric.class);
    assertThat(component.thing()).isNotNull();
  }

  @Test public void builderImplicitModules() {
    BuilderImplicitModules component = backend.builder(BuilderImplicitModules.Builder.class)
        .value(3L)
        .build();

    assertThat(component.string()).isEqualTo("3");
  }

  @Test public void builderExplicitModules() {
    BuilderExplicitModules component = backend.builder(BuilderExplicitModules.Builder.class)
        .module1(new BuilderExplicitModules.Module1("3"))
        .build();

    assertThat(component.string()).isEqualTo("3");
  }

  @Test public void builderExplicitModulesSetTwice() {
    BuilderExplicitModules component = backend.builder(BuilderExplicitModules.Builder.class)
        .module1(new BuilderExplicitModules.Module1("3"))
        .module1(new BuilderExplicitModules.Module1("4"))
        .build();

    assertThat(component.string()).isEqualTo("4");
  }

  @Test public void builderExplicitModulesOmitted() {
    try {
      backend.builder(BuilderExplicitModules.Builder.class).build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.BuilderExplicitModules.Module1 must be set");
    }
  }

  @Test public void builderDependency() {
    BuilderDependency component = backend.builder(BuilderDependency.Builder.class)
        .other(new BuilderDependency.Other("hey"))
        .build();

    assertThat(component.string()).isEqualTo("hey");
  }

  @Test public void builderDependencySetTwice() {
    BuilderDependency component = backend.builder(BuilderDependency.Builder.class)
        .other(new BuilderDependency.Other("hey"))
        .other(new BuilderDependency.Other("there"))
        .build();

    assertThat(component.string()).isEqualTo("there");
  }

  @Test public void builderDependencyOmitted() {
    try {
      backend.builder(BuilderDependency.Builder.class).build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("com.example.BuilderDependency.Other must be set");
    }
  }

  @Test public void memberInjectionEmptyClass() {
    MemberInjectionEmpty component = backend.create(MemberInjectionEmpty.class);
    MemberInjectionEmpty.Target target = new MemberInjectionEmpty.Target();
    component.inject(target);
    // No state, nothing to verify, except it didn't throw.
  }

  @Test public void memberInjectionEmptyAbstractClass() {
    MemberInjectionEmptyAbstract component = backend.create(MemberInjectionEmptyAbstract.class);
    MemberInjectionEmptyAbstract.Target target = new MemberInjectionEmptyAbstract.Target() {};
    component.inject(target);
    // No state, nothing to verify, except it didn't throw.
  }

  @Test public void memberInjectionEmptyInterface() {
    MemberInjectionEmptyInterface component = backend.create(MemberInjectionEmptyInterface.class);
    MemberInjectionEmptyInterface.Target target = new MemberInjectionEmptyInterface.Target() {};
    component.inject(target);
    // No state, nothing to verify, except it didn't throw.
  }

  @Test public void memberInjectionInterface() {
    MemberInjectionInterface component = backend.create(MemberInjectionInterface.class);
    class Target implements MemberInjectionInterface.Target {
      boolean called;
      @Override public void method(String foo) {
        called = true;
      }
    }
    Target target = new Target();
    component.inject(target);

    assertThat(target.called).isFalse();
  }

  @Test public void memberInjectionReturnInstance() {
    MemberInjectionReturnInstance component = backend.create(MemberInjectionReturnInstance.class);
    MemberInjectionReturnInstance.Target in = new MemberInjectionReturnInstance.Target();
    MemberInjectionReturnInstance.Target out = component.inject(in);
    assertThat(out.foo).isEqualTo("foo");
    assertThat(out).isSameAs(in);
  }

  @Test public void memberInjectionNoInjects() {
    MemberInjectionNoInjects component = backend.create(MemberInjectionNoInjects.class);
    MemberInjectionNoInjects.Target target = new MemberInjectionNoInjects.Target();
    component.inject(target);
    assertThat(target.one).isNull();
    assertThat(target.two).isNull();
    assertThat(target.three).isNull();
    assertThat(target.count).isEqualTo(0);
  }

  @Test public void memberInjectionFieldBeforeMethod() {
    MemberInjectionFieldBeforeMethod component =
        backend.create(MemberInjectionFieldBeforeMethod.class);
    MemberInjectionFieldBeforeMethod.Target target = new MemberInjectionFieldBeforeMethod.Target();
    component.inject(target);
    assertThat(target.fieldBeforeMethod).isTrue();
  }

  @Test public void memberInjectionFieldVisibility() {
    MemberInjectionFieldVisibility component = backend.create(MemberInjectionFieldVisibility.class);
    MemberInjectionFieldVisibility.Target target = new MemberInjectionFieldVisibility.Target();
    component.inject(target);
    assertThat(target.one).isEqualTo("one");
    assertThat(target.two).isEqualTo(2L);
    assertThat(target.three).isEqualTo(3);
  }

  @Test public void memberInjectionHierarchy() {
    MemberInjectionHierarchy component = backend.create(MemberInjectionHierarchy.class);
    MemberInjectionHierarchy.Subtype target = new MemberInjectionHierarchy.Subtype();
    component.inject(target);
    assertThat(target.baseOne).isEqualTo("foo");
    assertThat(target.baseCalled).isTrue();
    assertThat(target.subtypeOne).isEqualTo("foo");
    assertThat(target.subtypeCalled).isTrue();
  }

  @Test public void memberInjectionOrder() {
    MemberInjectionOrder component = backend.create(MemberInjectionOrder.class);
    MemberInjectionOrder.SubType target = new MemberInjectionOrder.SubType();
    component.inject(target);
    assertThat(target.calls)
        .containsExactly(
            // @Inject specification: Constructors are injected first
            "instantiation: baseField=null, subField=null",
            // followed by fields, and then methods.
            "baseMethod(foo): baseField=foo, subField=null",
            // Fields and methods in superclasses are injected before those in subclasses.
            "subMethod(foo): baseField=foo, subField=foo"
        )
        .inOrder();
  }

  @Test public void memberInjectionMethodVisibility() {
    MemberInjectionMethodVisibility component =
        backend.create(MemberInjectionMethodVisibility.class);
    MemberInjectionMethodVisibility.Target target = new MemberInjectionMethodVisibility.Target();
    component.inject(target);
    assertThat(target.count).isEqualTo(3);
    assertThat(target.one).isEqualTo("one");
    assertThat(target.two).isEqualTo(2L);
    assertThat(target.three).isEqualTo(3);
  }

  @Test public void memberInjectionMethodMultipleParams() {
    MemberInjectionMethodMultipleParams component =
        backend.create(MemberInjectionMethodMultipleParams.class);
    MemberInjectionMethodMultipleParams.Target target =
        new MemberInjectionMethodMultipleParams.Target();
    component.inject(target);
    assertThat(target.one).isEqualTo("one");
    assertThat(target.two).isEqualTo(2L);
    assertThat(target.two2).isEqualTo(2L);
    assertThat(target.three).isEqualTo(3);
  }

  @Test public void memberInjectionMethodReturnTypes() {
    MemberInjectionMethodReturnTypes component =
        backend.create(MemberInjectionMethodReturnTypes.class);
    MemberInjectionMethodReturnTypes.Target target = new MemberInjectionMethodReturnTypes.Target();
    component.inject(target);
    assertThat(target.count).isEqualTo(3);
  }

  @Test public void memberInjectionQualified() {
    MemberInjectionQualified component = backend.create(MemberInjectionQualified.class);
    MemberInjectionQualified.Target target = new MemberInjectionQualified.Target();
    component.inject(target);
    assertThat(target.fromField).isEqualTo("foo");
    assertThat(target.fromMethod).isEqualTo("foo");
  }

  @Test public void scoped() {
    ignoreReflectionBackend();

    Scoped component = backend.create(Scoped.class);
    assertThat(component.value()).isEqualTo(1);
    assertThat(component.value()).isEqualTo(1);
  }

  @Test public void multibindingSet() {
    ignoreReflectionBackend();

    MultibindingSet component = backend.create(MultibindingSet.class);
    assertThat(component.values()).containsExactly("one", "two");
  }

  @Test public void multibindingSetElements() {
    ignoreReflectionBackend();

    MultibindingSetElements component = backend.create(MultibindingSetElements.class);
    assertThat(component.values()).containsExactly("one", "two");
  }

  @Test public void multibindingProviderSet() {
    ignoreReflectionBackend();

    MultibindingProviderSet component = backend.create(MultibindingProviderSet.class);
    Provider<Set<String>> values = component.values();

    // Ensure the Provider is lazy in invoking and aggregating its backing @Provides methods.
    MultibindingProviderSet.Module1.oneCount.set(1);
    MultibindingProviderSet.Module1.twoCount.set(1);

    assertThat(values.get()).containsExactly("one1", "two1");
    assertThat(values.get()).containsExactly("one2", "two2");
  }

  @Test public void multibindingMap() {
    ignoreReflectionBackend();

    MultibindingMap component = backend.create(MultibindingMap.class);
    assertThat(component.values()).containsExactly("1", "one", "2", "two");
  }

  @Test public void multibindingProviderMap() {
    ignoreReflectionBackend();

    MultibindingProviderMap component = backend.create(MultibindingProviderMap.class);
    Provider<Map<String, String>> values = component.values();

    // Ensure the Provider is lazy in invoking and aggregating its backing @Provides methods.
    MultibindingProviderMap.Module1.oneCount.set(1);
    MultibindingProviderMap.Module1.twoCount.set(1);

    assertThat(values.get()).containsExactly("1", "one1", "2", "two1");
    assertThat(values.get()).containsExactly("1", "one2", "2", "two2");
  }

  @Test public void multibindingMapProvider() {
    ignoreReflectionBackend();

    MultibindingMapProvider component = backend.create(MultibindingMapProvider.class);
    Map<String, Provider<String>> values = component.values();
    assertThat(values.keySet()).containsExactly("1", "2");

    // Ensure each Provider is lazy in invoking its backing @Provides method.
    MultibindingMapProvider.Module1.twoValue.set("two");
    assertThat(values.get("2").get()).isEqualTo("two");

    MultibindingMapProvider.Module1.oneValue.set("one");
    assertThat(values.get("1").get()).isEqualTo("one");
  }

  @Test public void moduleClass() {
    ModuleClass component = backend.create(ModuleClass.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void moduleClassAndInterfaceHierarchy() {
    ModuleClassAndInterfaceHierarchy component =
        backend.create(ModuleClassAndInterfaceHierarchy.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void moduleClassAndInterfaceDuplicatesHierarchy() {
    ModuleClassAndInterfaceDuplicatesHierarchy component =
        backend.create(ModuleClassAndInterfaceDuplicatesHierarchy.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void moduleClassHierarchy() {
    ModuleClassHierarchy component = backend.create(ModuleClassHierarchy.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void moduleClassHierarchyStatics() {
    ModuleClassHierarchyStatics component = backend.create(ModuleClassHierarchyStatics.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void moduleInterface() {
    ModuleInterface component = backend.create(ModuleInterface.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void moduleInterfaceHierarchy() {
    ModuleInterfaceHierarchy component = backend.create(ModuleInterfaceHierarchy.class);
    assertThat(component.number()).isEqualTo(42);
  }

  @Test public void nestedComponent() {
    NestedComponent.MoreNesting.AndMore.TheComponent component =
        backend.create(NestedComponent.MoreNesting.AndMore.TheComponent.class);
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void nestedComponentBuilder() {
    NestedComponent.MoreNesting.AndMore.TheComponent component =
        backend.builder(NestedComponent.MoreNesting.AndMore.TheComponent.Builder.class).build();
    assertThat(component.string()).isEqualTo("foo");
  }

  @Test public void primitiveAutoBoxing() {
    PrimitiveAutoBoxing component = backend.create(PrimitiveAutoBoxing.class);
    assertThat(component.getByte()).isEqualTo((byte) 8);
    assertThat(component.getShort()).isEqualTo((short) 16);
    assertThat(component.getInteger()).isEqualTo(32);
    assertThat(component.getLong()).isEqualTo(64L);
    assertThat(component.getFloat()).isEqualTo(-32.0f);
    assertThat(component.getDouble()).isEqualTo(-64.0);
    assertThat(component.getBoolean()).isEqualTo(true);
    assertThat(component.getCharacter()).isEqualTo('\u221E');
  }

  @Test public void primitiveAutoUnboxing() {
    PrimitiveAutoUnboxing component = backend.create(PrimitiveAutoUnboxing.class);
    assertThat(component.getByte()).isEqualTo((byte) 8);
    assertThat(component.getShort()).isEqualTo((short) 16);
    assertThat(component.getInt()).isEqualTo(32);
    assertThat(component.getLong()).isEqualTo(64L);
    assertThat(component.getFloat()).isEqualTo(-32.0f);
    assertThat(component.getDouble()).isEqualTo(-64.0);
    assertThat(component.getBoolean()).isEqualTo(true);
    assertThat(component.getChar()).isEqualTo('\u221E');
  }

  @Test public void providerCycle() {
    ignoreCodegenBackend();

    ProviderCycle component = backend.create(ProviderCycle.class);
    try {
      component.string();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("Dependency cycle for java.lang.String\n"
          + " * Requested: java.lang.String\n"
          + "     from @Provides[com.example.ProviderCycle$Module1.longToString(…)]\n"
          + " * Requested: java.lang.Long\n"
          + "     from @Provides[com.example.ProviderCycle$Module1.intToLong(…)]\n"
          + " * Requested: java.lang.Integer\n"
          + "     from @Provides[com.example.ProviderCycle$Module1.stringToInteger(…)]\n"
          + " * Requested: java.lang.String\n"
          + "     which forms a cycle.");
    }
  }

  @Test public void undeclaredModule() {
    ignoreCodegenBackend();

    UndeclaredModules.Builder builder = backend.builder(UndeclaredModules.Builder.class);
    try {
      builder.module(new UndeclaredModules.Module1());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("@Component.Builder has setters for modules that aren't required: "
              + "com.example.UndeclaredModules$Builder.module");
    }
  }
  @Test public void undeclaredDependencies() {
    ignoreCodegenBackend();

    UndeclaredDependencies.Builder builder = backend.builder(UndeclaredDependencies.Builder.class);
    try {
      builder.dep("hey");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("@Component.Builder has setters for dependencies that aren't required: "
              + "com.example.UndeclaredDependencies$Builder.dep");
    }
  }

  @Test public void membersInjectionWrongReturnType() {
    ignoreCodegenBackend();

    MembersInjectorWrongReturnType component = backend.create(MembersInjectorWrongReturnType.class);
    MembersInjectorWrongReturnType.Target instance = new MembersInjectorWrongReturnType.Target();
    try {
      component.inject(instance);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("Members injection methods may only return the injected type or void: "
              + "com.example.MembersInjectorWrongReturnType.inject");
    }
  }

  @SuppressWarnings("OverridesJavaxInjectableMethod")
  @Test public void membersInjectionAbstractMethod() {
    ignoreCodegenBackend();

    MembersInjectionAbstractMethod component = backend.create(MembersInjectionAbstractMethod.class);
    MembersInjectionAbstractMethod.Target instance = new MembersInjectionAbstractMethod.Target() {
      @Override public void abstractMethod(String one) {}
    };
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Methods with @Inject may not be abstract: "
              + "com.example.MembersInjectionAbstractMethod.Target.abstractMethod");
    }
  }

  @SuppressWarnings("OverridesJavaxInjectableMethod")
  @Test public void membersInjectionInterfaceMethod() {
    ignoreCodegenBackend();

    MembersInjectionInterfaceMethod component =
        backend.create(MembersInjectionInterfaceMethod.class);
    MembersInjectionInterfaceMethod.Target instance = new MembersInjectionInterfaceMethod.Target() {
      @Override public void interfaceMethod(String one) {}
    };
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Methods with @Inject may not be abstract: "
              + "com.example.MembersInjectionInterfaceMethod.Target.interfaceMethod");
    }
  }

  @Test public void membersInjectionPrivateField() {
    ignoreCodegenBackend();

    MembersInjectionPrivateField component = backend.create(MembersInjectionPrivateField.class);
    MembersInjectionPrivateField.Target instance = new MembersInjectionPrivateField.Target();
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Dagger does not support injection into private fields: "
              + "com.example.MembersInjectionPrivateField.Target.privateField");
    }
  }

  @Test public void membersInjectionStaticField() {
    ignoreCodegenBackend();

    MembersInjectionStaticField component = backend.create(MembersInjectionStaticField.class);
    MembersInjectionStaticField.Target instance = new MembersInjectionStaticField.Target();
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Dagger does not support injection into static fields: "
              + "com.example.MembersInjectionStaticField.Target.staticField");
    }
  }

  @Test public void membersInjectionPrivateMethod() {
    ignoreCodegenBackend();

    MembersInjectionPrivateMethod component = backend.create(MembersInjectionPrivateMethod.class);
    MembersInjectionPrivateMethod.Target instance = new MembersInjectionPrivateMethod.Target();
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Dagger does not support injection into private methods: "
              + "com.example.MembersInjectionPrivateMethod.Target.privateMethod()");
    }
  }

  @Test public void membersInjectionStaticMethod() {
    ignoreCodegenBackend();

    MembersInjectionStaticMethod component = backend.create(MembersInjectionStaticMethod.class);
    MembersInjectionStaticMethod.Target instance = new MembersInjectionStaticMethod.Target();
    try {
      component.inject(instance);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .startsWith("Dagger does not support injection into static methods: "
              + "com.example.MembersInjectionStaticMethod.Target.staticMethod()");
    }
  }

  @Test public void abstractClassCreateFails() {
    ignoreCodegenBackend();

    try {
      backend.create(AbstractComponent.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.AbstractComponent is not an interface. "
              + "Only interface components are supported.");
    }
  }

  @Test public void abstractClassBuilderFails() {
    ignoreCodegenBackend();

    try {
      backend.builder(AbstractComponent.Builder.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.AbstractComponent is not an interface. "
              + "Only interface components are supported.");
    }
  }

  @Test public void noComponentAnnotationCreateFails() {
    ignoreCodegenBackend();

    try {
      backend.create(NoAnnotation.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.NoAnnotation lacks @Component annotation");
    }
  }

  @Test public void noComponentAnnotationBuilderFails() {
    ignoreCodegenBackend();

    try {
      backend.builder(NoAnnotation.Builder.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.NoAnnotation lacks @Component annotation");
    }
  }

  @Test public void packagePrivateComponentFails() {
    ignoreCodegenBackend();

    try {
      backend.builder(PackagePrivateComponent.Builder.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("Component interface com.example.PackagePrivateComponent "
              + "must be public in order to be reflectively created");
    }
  }

  @Test public void abstractBuilderClassFails() {
    ignoreCodegenBackend();

    try {
      backend.builder(AbstractBuilderClass.Builder.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.AbstractBuilderClass.Builder is not an interface. "
              + "Only interface component builders are supported.");
    }
  }

  @Test public void noComponentBuilderAnnotationFails() {
    ignoreCodegenBackend();
    ignoreReflectionBackend(); // @Component.Builder does not have runtime retention

    try {
      backend.builder(NoBuilderAnnotation.Builder.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.NoBuilderAnnotation.Builder lacks "
              + "@Component.Builder annotation");
    }
  }

  @Test public void componentWithDependenciesCreateFails() {
    ignoreCodegenBackend();

    try {
      backend.create(ComponentWithDependencies.class);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageThat()
          .isEqualTo("com.example.ComponentWithDependencies "
              + "declares dependencies [java.lang.String, java.lang.Runnable] "
              + "and therefore must be created with a builder");
    }
  }

  private void ignoreReflectionBackend() {
    assumeTrue("Not yet implemented for reflection backend", backend != Backend.REFLECT);
  }

  private void ignoreCodegenBackend() {
    assumeTrue("Not supported for codegen backend", backend != Backend.CODEGEN);
  }
}
