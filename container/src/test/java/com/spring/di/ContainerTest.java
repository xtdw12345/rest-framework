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
    ContextContainer container;

    @BeforeEach
    public void setUp() {
        container = new ContextContainer();
    }

    @Nested
    class InjectConstruction {
        @Test
        public void should_bind_type_to_an_instance() {
            Component component = new Component() {
            };
            container.bind(Component.class, component);

            Component instance = container.getContext().get(Component.class).get();
            Assertions.assertSame(component, instance);
        }

        @Test
        public void should_return_null_is_component_not_defined() {
            Optional<Component> component = container.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                container.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = container.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                Dependency dependency = new Dependency() {
                };
                container.bind(Dependency.class, dependency);
                Component instance = container.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectionConstructor) instance).dependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_nested_injection_constructor() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                container.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                container.bind(String.class, "Hello World!");
                Component instance = container.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertInstanceOf(DependencyWithInjectionConstructor.class, ((ComponentWithInjectionConstructor) instance).dependency());
                assertEquals("Hello World!", ((DependencyWithInjectionConstructor) ((((ComponentWithInjectionConstructor) instance).dependency()))).value());
            }

            @Test
            public void should_throw_illegal_exception_if_multi_inject_constructors_exist() {
                assertThrows(IllegalComponentException.class, () -> {
                    container.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });

            }

            @Test
            public void should_throw_illegal_exception_if_no_inject_constructor_nor_default_constructor_exists() {
                assertThrows(IllegalComponentException.class, () -> {
                    container.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> container.getContext());
                assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                container.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> container.getContext());
                assertEquals(AnotherDependency.class, dependencyNotFoundException.getDependency());
                assertEquals(Dependency.class, dependencyNotFoundException.getComponent());
            }

            @Test
            public void should_throw_exception_if_cyclic_dependency_exist() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                container.bind(Dependency.class, DependencyDependedOnComponent.class);
                CyclicDependencyFoundException cyclicDependencyFoundException = assertThrows(CyclicDependencyFoundException.class, () -> container.getContext());
                assertEquals(2, cyclicDependencyFoundException.getComponents().size());
                assertTrue(cyclicDependencyFoundException.getComponents().contains(Component.class));
                assertTrue(cyclicDependencyFoundException.getComponents().contains(Dependency.class));
            }

            @Test
            public void should_throw_exception_if_transitive_cyclic_dependency_exist() {
                container.bind(Component.class, ComponentWithInjectionConstructor.class);
                container.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                container.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
                assertThrows(CyclicDependencyFoundException.class, () -> container.getContext());
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
                container.bind(Dependency.class, dependency);
                container.bind(Component.class, ComponentWithFieldInjection.class);
                Component component = container.getContext().get(Component.class).get();
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
                container.bind(Dependency.class, dependency);
                container.bind(Component.class, SubclassWithFieldInjection.class);
                Component component = container.getContext().get(Component.class).get();
                assertSame(dependency, ((SubclassWithFieldInjection) component).dependency);
            }
        }

        @Nested
        class MethodInjection {

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
