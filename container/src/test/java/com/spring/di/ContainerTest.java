package com.spring.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    class InjectConstruction {
        @Test
        public void should_bind_type_to_an_instance() {
            Component component = new Component() {
            };
            context.bind(Component.class, component);

            Component instance = context.get(Component.class).get();
            Assertions.assertSame(component, instance);
        }

        @Test
        public void should_return_null_is_component_not_defined() {
            Optional<Component> component = context.get(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
            }

            @Test
            public void should_bind_type_to_a_class_with_injection_constructor() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                Dependency dependency = new Dependency() {
                };
                context.bind(Dependency.class, dependency);
                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertSame(dependency, ((ComponentWithInjectionConstructor) instance).dependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_nested_injection_constructor() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
                context.bind(String.class, "Hello World!");
                Component instance = context.get(Component.class).get();
                assertNotNull(instance);
                assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
                assertInstanceOf(DependencyWithInjectionConstructor.class, ((ComponentWithInjectionConstructor) instance).dependency());
                assertEquals("Hello World!", ((DependencyWithInjectionConstructor) ((((ComponentWithInjectionConstructor) instance).dependency()))).value());
            }

            @Test
            public void should_throw_illegal_exception_if_multi_inject_constructors_exist() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });

            }

            @Test
            public void should_throw_illegal_exception_if_no_inject_constructor_nor_default_constructor_exists() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Component.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_found() {
                context.bind(Component.class, ComponentWithInjectionConstructor.class);
                assertThrows(DependencyNotFoundException.class, () -> {
                    context.get(Component.class);
                });
            }
        }

        @Nested
        class FieldInjection {

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
