package com.spring.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ContainerTest {

    public interface Component {

    }

    public static class ComponentImpl implements Component {
        public ComponentImpl() {
        }
    }

    @Test
    public void should_bind_type_to_an_instance() {
        Context context = new Context();
        Component component = new Component() {
        };
        context.bind(Component.class, component);

        Component instance = context.getInstance(Component.class);
        Assertions.assertSame(component, instance);
    }

    @Nested
    class ConstructorInjection {
        // TODO constructor with no args
        @Test
        public void should_bind_type_to_a_class_with_no_constructor_args() {
            Context context = new Context();
            context.bind(Component.class, ComponentImpl.class);
            Component instance = context.getInstance(Component.class);
            Assertions.assertNotNull(instance);
            Assertions.assertTrue(instance instanceof ComponentImpl);
        }

        // TODO constructor with type

        // TODO nested constructor
    }

    @Nested
    class FieldInjection {

    }

    @Nested
    class MethodInjection {

    }


}
