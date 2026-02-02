package com.spring.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    Context context;
    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Nested
    class InstanceBinding {
        @Test
        public void should_bind_type_to_an_instance() {

            Component component = new Component() {
            };
            context.bind(Component.class, component);

            Component instance = context.getInstance(Component.class);
            Assertions.assertSame(component, instance);
        }
    }

    @Nested
    class ConstructorInjection {
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {
            context.bind(Component.class, ComponentWithDefaultConstructor.class);
            Component instance = context.getInstance(Component.class);
            assertNotNull(instance);
            assertInstanceOf(ComponentWithDefaultConstructor.class, instance);
        }

        @Test
        public void should_bind_type_to_a_class_with_injection_constructor() {
            context.bind(Component.class, ComponentWithInjectionConstructor.class);
            Dependency dependency = new Dependency() {
            };
            context.bind(Dependency.class, dependency);
            Component instance = context.getInstance(Component.class);
            assertNotNull(instance);
            assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
            assertSame(dependency, ((ComponentWithInjectionConstructor) instance).dependency());
        }

        @Test
        public void should_bind_type_to_a_class_with_nested_injection_constructor() {
            context.bind(Component.class, ComponentWithInjectionConstructor.class);
            context.bind(Dependency.class, DependencyWithInjectionConstructor.class);
            context.bind(String.class, "Hello World!");
            Component instance = context.getInstance(Component.class);
            assertNotNull(instance);
            assertInstanceOf(ComponentWithInjectionConstructor.class, instance);
            assertInstanceOf(DependencyWithInjectionConstructor.class, ((ComponentWithInjectionConstructor) instance).dependency());
            assertEquals("Hello World!", ((DependencyWithInjectionConstructor)((((ComponentWithInjectionConstructor) instance).dependency()))).value());
        }
    }

    @Nested
    class FieldInjection {

    }

    @Nested
    class MethodInjection {

    }


}

interface Component {

}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

interface Dependency {}

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
