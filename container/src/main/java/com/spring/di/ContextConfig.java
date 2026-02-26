package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providers.put(componentClass, context -> component);
    }

    record Component(Class<?> componentClass, Annotation qualifier) {

    }

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(componentClass, qualifier), context -> component);
        }
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        providers.put(componentClass, new InjectionProvider<>(componentImplClass));
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass,  Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers)
            components.put(new Component(componentClass, qualifier), new InjectionProvider<>(componentImplClass));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional getType(Ref ref) {
                if (ref.getQualifier() != null) {
                    return Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier()))).map(p -> p.get(this));
                }
                if (ref.isContainer()) {
                    return getContainer(ref);
                }
                return getComponent(ref);
            }

            private Optional getComponent(Ref ref) {
                Class<?> component = ref.getComponent();
                return Optional.ofNullable(providers.get(component)).map(p -> p.get(this));
            }

            private Optional getContainer(Ref ref) {
                Type container = ref.getContainer();
                Class<?> component = ref.getComponent();

                if (container != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(providers.get(component)).map(p -> (Provider<Object>) () -> p.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencyRefs()) {
            if (!providers.containsKey(dependency.getComponent())) {
                throw new DependencyNotFoundException(component, dependency.getComponent());
            }
            if (!dependency.isContainer()){
                if (visiting.contains(dependency.getComponent())) {
                    throw new CyclicDependencyFoundException(new HashSet<>(visiting));
                }
                visiting.push(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencyRefs() {
            return List.of();
        }
    }

}
