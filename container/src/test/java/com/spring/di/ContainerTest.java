package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Inject;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class InjectConstruction {
        @Test
        public void should_bind_type_to_an_instance() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);

            Component instance = config.getContext().get(Component.class).get();
            Assertions.assertSame(component, instance);
        }

        @Test
        public void should_return_null_is_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectionConstructor) instance).dependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_nested_injection_constructor() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                config.bind(String.class, "Hello World!");
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertInstanceOf(DependencyWithInjectionConstructor.class, ((ComponentWithInjectionConstructor) instance).dependency());
                assertEquals("Hello World!", ((DependencyWithInjectionConstructor) ((((ComponentWithInjectionConstructor) instance).dependency()))).value());
            }

            @Test
            public void should_throw_illegal_exception_if_multi_inject_constructors_exist() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });

            }

            @Test
            public void should_throw_illegal_exception_if_no_inject_constructor_nor_default_constructor_exists() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
                assertEquals(AnotherDependency.class, dependencyNotFoundException.getDependency());
                assertEquals(Dependency.class, dependencyNotFoundException.getComponent());
            }

            @Test
            public void should_throw_exception_if_cyclic_dependency_exist() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);
                CyclicDependencyFoundException cyclicDependencyFoundException = assertThrows(CyclicDependencyFoundException.class, () -> config.getContext());
                assertEquals(2, cyclicDependencyFoundException.getComponents().size());
                assertTrue(cyclicDependencyFoundException.getComponents().contains(Component.class));
                assertTrue(cyclicDependencyFoundException.getComponents().contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependency_exist() {
                config.bind(Component.class, ComponentWithInjectionConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
                assertThrows(CyclicDependencyFoundException.class, () -> config.getContext());
            }
        }

        @Nested
        class FieldInjection {

            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            @Test
            public void should_inject_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(Component.class, ComponentWithFieldInjection.class);
                Component component = config.getContext().get(Component.class).get();
                assertSame(dependency, ((ComponentWithFieldInjection) component).dependency);
            }

            @Test
            public void should_include_field_inject_dependencies_info() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> constructorInjectionProvider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, constructorInjectionProvider.getDependencies().toArray());
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {}

            @Test
            public void should_inject_via_superclass() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(Component.class, SubclassWithFieldInjection.class);
                Component component = config.getContext().get(Component.class).get();
                assertSame(dependency, ((SubclassWithFieldInjection) component).dependency);
            }
        }

        @Nested
        class MethodInjection {
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
                config.bind(Component.class, ComponentWithInjectMethodButNoDependencyDeclared.class);
                Component component = config.getContext().get(Component.class).get();
                assertTrue(((ComponentWithInjectMethodButNoDependencyDeclared) component).called);
            }
            @Test
            public void should_inject_via_inject_method() {
                Dependency dependency = new Dependency() {};
                config.bind(Dependency.class, dependency);
                config.bind(Component.class, ComponentWithMethodInjection.class);
                Component component = config.getContext().get(Component.class).get();
                assertSame(dependency, ((ComponentWithMethodInjection) component).dependency);
            }

            @Test
            public void should_include_method_dependency_info() {
                ConstructorInjectionProvider<ComponentWithMethodInjection>  constructorInjectionProvider = new ConstructorInjectionProvider<>(ComponentWithMethodInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, constructorInjectionProvider.getDependencies().toArray());
            }
            //TODO override method only call once
            static class SuperClassWithInjectMethod{
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
            public void should_inject_method_only_call_once_if_overridden() {
                config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);
                SubClassWithInjectMethod component = config.getContext().get(SubClassWithInjectMethod.class).get();
                assertEquals(1, component.superCalled);
            }

            static class SubClassWithSuperClassInjectMethod extends SuperClassWithInjectMethod {}
            @Test
            public void should_inject_via_super_class_inject_method() {
                config.bind(SubClassWithSuperClassInjectMethod.class, SubClassWithSuperClassInjectMethod.class);
                SubClassWithSuperClassInjectMethod subClassWithSuperClassInjectMethod = config.getContext().get(SubClassWithSuperClassInjectMethod.class).get();
                assertEquals(1, subClassWithSuperClassInjectMethod.superCalled);
            }
            //TODO not call if override method no inject annotation
            static class SubClassWithOverrideMethodNoInjectAnnotation extends SuperClassWithInjectMethod {
                public void install() {
                    super.install();
                }
            }
            @Test
            public void should_not_inject_if_overridden_method_without_inject_annotation() {
                config.bind(SubClassWithOverrideMethodNoInjectAnnotation.class, SubClassWithOverrideMethodNoInjectAnnotation.class);
                SubClassWithOverrideMethodNoInjectAnnotation component = config.getContext().get(SubClassWithOverrideMethodNoInjectAnnotation.class).get();
                assertEquals(0, component.superCalled);
            }
        }
    }


}

interface Component {

}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name, Double value) {
    }
}

interface Dependency {
}

interface AnotherDependency {}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private final Component component;
    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

record DependencyWithInjectionConstructor(String value) implements Dependency {
    @Inject
    DependencyWithInjectionConstructor {
    }
}

record ComponentWithInjectionConstructor(Dependency dependency) implements Component {
    @Inject
    ComponentWithInjectionConstructor {
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private final AnotherDependency anotherDependency;
    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    @Getter
    private final Component component;
    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}
