package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.Getter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component component = new Component() {
            };
            config.bind(Component.class, component);

            Context context = config.getContext();
            Component instance = context.getType(Context.Ref.of(Component.class)).get();
            Assertions.assertSame(component, instance);
        }

        @ParameterizedTest(name = "Supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentClass) {
            Dependency dependency = new Dependency() {};
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentClass);

            Context context = config.getContext();
            Optional<Component> component = context.getType(Context.Ref.of(Component.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().getDependency());
        }

        private static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor injection", ComponentWithInjectionConstructor.class)),
                    Arguments.of(Named.of("Field injection", ComponentWithInjectionField.class)),
                    Arguments.of(Named.of("Method injection", ComponentWithInjectionMethod.class))
            );
        }

        @Test
        public void should_return_null_is_component_not_defined() {
            Context context = config.getContext();
            Optional<Component> component = context.getType(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_provided_component() throws Exception {
            Component component = new Component() {};
            config.bind(Component.class, component);
            Context context = config.getContext();
            Provider<Component> instance = context.getType(new Context.Ref<Provider<Component>>(){}).get();
            assertSame(component, instance.get());
        }

        private List<Component> componentList;
        @Test
        public void should_not_retrieve_provided_component_if_unsupported_container() throws Exception {
            Component component = new Component() {};
            config.bind(Component.class, component);
            ParameterizedType componentProviderType = (ParameterizedType)TypeBinding.class.getDeclaredField("componentList").getGenericType();
            Context context = config.getContext();
            assertFalse(context.getType(Context.Ref.of(componentProviderType)).isPresent());
        }

        @Nested
        class WithQualifier {
            static class NamedLiteral implements jakarta.inject.Named {

                private String value;
                NamedLiteral(String value) {
                    this.value = value;
                }
                @Override
                public String value() {
                    return value;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return jakarta.inject.Named.class;
                }

                @Override
                public boolean equals(Object o) {
                    if (o == null || getClass() != o.getClass()) return false;
                    NamedLiteral that = (NamedLiteral) o;
                    return Objects.equals(value, that.value);
                }

                @Override
                public int hashCode() {
                    return Objects.hashCode(value);
                }
            }
            @Test
            public void should_bind_instance_by_qualifier() {
                Component component = new Component() {
                };
                config.bind(Component.class, component, new NamedLiteral("chosenOne"));

                Context context = config.getContext();
                Component instance = context.getType(Context.Ref.of(Component.class, new NamedLiteral("chosenOne"))).get();
                Assertions.assertSame(component, instance);
            }

            @Test
            public void should_bind_type_by_qualifier() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class, new NamedLiteral("chosenOne"));
                Context context = config.getContext();
                Component instance = context.getType(Context.Ref.of(Component.class, new NamedLiteral("chosenOne"))).get();
                Assertions.assertNotNull(instance);
            }

            @Test
            public void should_bind_instance_by_multiple_qualifiers() {
                Component component = new Component() {
                };
                config.bind(Component.class, component, new NamedLiteral("chosenOne"), new NamedLiteral("skyWalker"));

                Context context = config.getContext();
                Component chosenOne = context.getType(Context.Ref.of(Component.class, new NamedLiteral("chosenOne"))).get();
                Component skyWalker = context.getType(Context.Ref.of(Component.class, new NamedLiteral("skyWalker"))).get();
                Assertions.assertSame(component, chosenOne);
                Assertions.assertSame(component, skyWalker);
            }

            @Test
            public void should_bind_type_by_multiple_qualifiers() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class, new NamedLiteral("chosenOne"), new NamedLiteral("skyWalker"));
                Context context = config.getContext();
                Component chosenOne = context.getType(Context.Ref.of(Component.class, new NamedLiteral("chosenOne"))).get();
                Component skyWalker = context.getType(Context.Ref.of(Component.class, new NamedLiteral("skyWalker"))).get();
                Assertions.assertNotNull(chosenOne);
                Assertions.assertNotNull(skyWalker);
            }
        }


    }

    @Nested
    class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> componentClass) {
            config.bind(Component.class, componentClass);
            DependencyNotFoundException dependencyNotFoundException = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, dependencyNotFoundException.getDependency());
            assertEquals(Component.class, dependencyNotFoundException.getComponent());
        }

        static class ComponentWithProviderInjectionConstructor {
            Provider<Dependency> dependencyProvider;

            @Inject
            public ComponentWithProviderInjectionConstructor(Provider<Dependency> dependencyProvider) {
                this.dependencyProvider = dependencyProvider;
            }
        }
        static class ComponentWithProviderInjectionField {
            @Inject
            Provider<Dependency> dependencyProvider;
        }
        static class ComponentWithProviderInjectionMethod {
            Provider<Dependency> dependencyProvider;

            @Inject
            public void install(Provider<Dependency> dependencyProvider) {
                this.dependencyProvider = dependencyProvider;
            }
        }

        private static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor injection", ComponentWithInjectionConstructor.class)),
                    Arguments.of(Named.of("Field injection", ComponentWithInjectionField.class)),
                    Arguments.of(Named.of("Method injection", ComponentWithInjectionMethod.class))
                    ,Arguments.of(Named.of("Constructor provider injection", ComponentWithProviderInjectionConstructor.class))
                    ,Arguments.of(Named.of("Field provider injection", ComponentWithProviderInjectionField.class))
                    ,Arguments.of(Named.of("Method provider injection", ComponentWithProviderInjectionMethod.class))
                    //TODO field provider injection
                    //TODO method provider injection
            );
        }

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_cyclic_dependency_exist(Class<? extends Component>  componentClass, Class<? extends Dependency> dependencyClass) {
            config.bind(Component.class, componentClass);
            config.bind(Dependency.class, dependencyClass);
            CyclicDependencyFoundException cyclicDependencyFoundException = assertThrows(CyclicDependencyFoundException.class, () -> config.getContext());
            assertEquals(2, cyclicDependencyFoundException.getComponents().size());
            assertTrue(cyclicDependencyFoundException.getComponents().contains(Component.class));
            assertTrue(cyclicDependencyFoundException.getComponents().contains(Dependency.class));
        }

        private static Stream<Arguments> should_throw_exception_if_cyclic_dependency_exist() {
            List<Arguments>  arguments = new ArrayList<>();
            for (Class<? extends Component> componentClass : Arrays.asList(ComponentInjectDependencyWithConstructor.class, ComponentInjectDependencyWithMethod.class, ComponentInjectDependencyWithField.class)) {
                for (Class<? extends Dependency> dependencyClass : Arrays.asList(DependencyInjectComponentWithConstructor.class, DependencyInjectComponentWithMethod.class, DependencyInjectComponentWithField.class)) {
                    arguments.add(Arguments.of(componentClass, dependencyClass));
                }
            }
            return arguments.stream();
        }

        static class ComponentInjectDependencyWithConstructor implements Component {
            Dependency dependency;
            @Inject
            ComponentInjectDependencyWithConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }
        static class DependencyInjectComponentWithConstructor implements Dependency {
            Component component;
            @Inject
            DependencyInjectComponentWithConstructor(Component component) {
                this.component = component;
            }
        }
        static class ComponentInjectDependencyWithMethod  implements Component {
            Dependency dependency;
            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }
        static class DependencyInjectComponentWithMethod implements Dependency {
            Component component;
            @Inject
            void install(Component component) {
                this.component = component;
            }
        }
        static class ComponentInjectDependencyWithField implements Component {
            @Inject
            Dependency dependency;
        }
        static class DependencyInjectComponentWithField  implements Dependency {
            @Inject
            Component component;
        }

        @Test
        public void should_throw_exception_if_transitive_cyclic_dependency_exist() {
            config.bind(Component.class, ComponentWithInjectionConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);
            assertThrows(CyclicDependencyFoundException.class, () -> config.getContext());
        }

        static class DependencyDependedProvidedComponentWithConstructor implements Dependency {
            Provider<Component> componentProvider;
            @Inject
            DependencyDependedProvidedComponentWithConstructor(Provider<Component> componentProvider) {
                this.componentProvider = componentProvider;
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_provide_exist() {
            config.bind(Component.class, ComponentWithInjectionConstructor.class);
            config.bind(Dependency.class, DependencyDependedProvidedComponentWithConstructor.class);
            Context context = config.getContext();
            assertTrue(context.getType(Context.Ref.of(Component.class)).isPresent());
        }
    }
}

interface Component {
    default Dependency getDependency(){
        return null;
    };
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

interface AnotherDependency {
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private final Component component;

    @Inject
    public AnotherDependencyDependedOnComponent(Component component) {
        this.component = component;
    }
}

class DependencyWithInjectionConstructor implements Dependency {
    String value;

    @Inject
    DependencyWithInjectionConstructor(String value) {
        this.value = value;
    }
}

class ComponentWithInjectionConstructor implements Component {
    Dependency dependency;

    @Inject
    ComponentWithInjectionConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithInjectionField implements Component {
    @Inject
    Dependency dependency;

    @Override
    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithInjectionMethod implements Component {
    Dependency dependency;
    @Inject
    ComponentWithInjectionMethod(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public Dependency getDependency() {
        return dependency;
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
