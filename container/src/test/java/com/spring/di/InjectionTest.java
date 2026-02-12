package com.spring.di;

import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Nested
class InjectionTest {
    private ContextConfig config = Mockito.mock(ContextConfig.class);
    private Context context = Mockito.mock(Context.class);
    private Dependency dependency = new Dependency() {
    };
    private Provider<Dependency> dependencyProvider = Mockito.mock(Provider.class);

    @BeforeEach
    public void setUp() throws NoSuchFieldException {
        when(config.getContext()).thenReturn(context);
        config.bind(Dependency.class, dependency);
        when(context.get(Dependency.class)).thenReturn(Optional.of(dependency));
        when(context.get((ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType())).thenReturn(Optional.of(dependencyProvider));
    }

    private <T, R extends T> T getComponent(Class<T> componentClass, Class<R> componentImplClass) {
        InjectionProvider<R> injectionProvider = new InjectionProvider<>(componentImplClass);
        return injectionProvider.get(context);
    }

    @Nested
    class ConstructorInjection {
        @Nested
        class InjectTest {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);
                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {

                Component instance = getComponent(Component.class, ComponentWithInjectionConstructor.class);
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectionConstructor) instance).dependency);
            }

            @Test
            public void should_bind_type_to_a_class_with_nested_injection_constructor() {
                when(context.get(Dependency.class)).thenReturn(Optional.of(new DependencyWithInjectionConstructor("Hello World!")));
                Component instance = getComponent(Component.class, ComponentWithInjectionConstructor.class);

                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertInstanceOf(DependencyWithInjectionConstructor.class, ((ComponentWithInjectionConstructor) instance).dependency);
                assertEquals("Hello World!", ((DependencyWithInjectionConstructor) ((ComponentWithInjectionConstructor) instance).dependency).value);
            }

            static class ConstructorProviderInjection {
                Provider<Dependency>  dependencyProvider;
                @Inject
                ConstructorProviderInjection(Provider<Dependency> dependencyProvider) {
                    this.dependencyProvider = dependencyProvider;
                }
            }
            @Test
            public void should_bind_provider_type_with_constructor_injection() {
                InjectionProvider<ConstructorProviderInjection> provider = new InjectionProvider<>(ConstructorProviderInjection.class);
                ConstructorProviderInjection component = provider.get(context);
                assertSame(dependencyProvider, component.dependencyProvider);
            }
        }

        @Nested
        class IllegalConstructor {
            abstract class AbstractComponentWithInjectConstructor {
                Dependency dependency;

                @Inject
                AbstractComponentWithInjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_throw_exception_if_abstract_class_with_inject_constructor() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractComponentWithInjectConstructor.class));
            }

            @Test
            public void should_throw_exception_if_bind_interface_class_as_component() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(Component.class));
            }

            @Test
            public void should_throw_illegal_exception_if_multi_inject_constructors_exist() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithMultiInjectConstructors.class));

            }

            @Test
            public void should_throw_illegal_exception_if_no_inject_constructor_nor_default_constructor_exists() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }
        }
    }

    @Nested
    class FieldInjection {

        @Nested
        class InjectTest {
            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            @Test
            public void should_inject_via_field() {
                Component component = getComponent(Component.class, ComponentWithFieldInjection.class);
                assertSame(dependency, ((ComponentWithFieldInjection) component).dependency);
            }

            @Test
            public void should_include_field_inject_dependencies_info() {
                InjectionProvider<ComponentWithFieldInjection> injectionProvider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, injectionProvider.getDependencies().toArray());
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_via_superclass() {
                Component component = getComponent(Component.class, SubclassWithFieldInjection.class);
                assertSame(dependency, ((SubclassWithFieldInjection) component).dependency);
            }

            static class ProviderInjectByField {
                @Inject
                Provider<Dependency> dependency;
            }
            @Test
            public void should_inject_provider_via_inject_field() {
                InjectionProvider<ProviderInjectByField> component = new InjectionProvider<>(ProviderInjectByField.class);
                assertSame(dependencyProvider, component.get(context).dependency);
            }
        }

        @Nested
        class IllegalFields {
            class ComponentWithFinalFieldInjection implements Component {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_final_field() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithFinalFieldInjection.class));
            }
        }
    }

    @Nested
    class MethodInjection {

        @Nested
        class InjectTest {

            static class ComponentWithMethodInjection implements Component {
                Dependency dependency;

                @Inject
                public void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            static class ComponentWithInjectMethodButNoDependencyDeclared implements Component {
                boolean called = false;

                @Inject
                public void install() {
                    called = true;
                }
            }

            @Test
            public void should_call_inject_method_if_no_dependency_declared() {
                Component component = getComponent(Component.class, ComponentWithInjectMethodButNoDependencyDeclared.class);

                assertTrue(((ComponentWithInjectMethodButNoDependencyDeclared) component).called);
            }

            @Test
            public void should_inject_via_inject_method() {
                Component component = getComponent(Component.class, ComponentWithMethodInjection.class);
                assertSame(dependency, ((ComponentWithMethodInjection) component).dependency);
            }

            @Test
            public void should_include_method_dependency_info() {
                InjectionProvider<ComponentWithMethodInjection> injectionProvider = new InjectionProvider<>(ComponentWithMethodInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, injectionProvider.getDependencies().toArray());
            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                public void install() {
                    superCalled++;
                }
            }

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                @Inject
                public void install() {
                    super.install();
                }
            }

            @Test
            public void should_call_inject_method_only_once_if_overridden() {
                SubClassWithInjectMethod component = getComponent(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);
                assertEquals(1, component.superCalled);
            }

            static class SubClassWithSuperClassInjectMethod extends SuperClassWithInjectMethod {
            }

            @Test
            public void should_call_inject_via_super_class() {
                SubClassWithSuperClassInjectMethod component = getComponent(SubClassWithSuperClassInjectMethod.class, SubClassWithSuperClassInjectMethod.class);
                assertEquals(1, component.superCalled);
            }

            static class SubClassWithOverrideMethodNoInjectAnnotation extends SuperClassWithInjectMethod {
                public void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_overridden_method_without_inject_annotation() {
                SubClassWithOverrideMethodNoInjectAnnotation component = getComponent(SubClassWithOverrideMethodNoInjectAnnotation.class, SubClassWithOverrideMethodNoInjectAnnotation.class);

                assertEquals(0, component.superCalled);
            }

            static class ProviderInjectionWithMethod {
                Provider<Dependency> dependency;

                @Inject
                public void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }
            @Test
            public void should_inject_provider_by_inject_method() {
                InjectionProvider<ProviderInjectionWithMethod> component = new InjectionProvider<>(ProviderInjectionWithMethod.class);
                assertSame(dependencyProvider, component.get(context).dependency);
            }
        }

        @Nested
        class IllegalMethods {
            static class ComponentWithMethodTypeParameter {
                @Inject
                <T> T install() {
                    return null;
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_have_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ComponentWithMethodTypeParameter.class));
            }
        }
    }
}
