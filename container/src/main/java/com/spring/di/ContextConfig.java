package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentType, ComponentType component) {
        components.put(new Component(componentType, null),  context -> component);
    }

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            if (!qualifier.annotationType().isAnnotationPresent(Qualifier.class)) {
                throw new IllegalComponentException();
            }
            components.put(new Component(componentClass, qualifier), context -> component);
        }
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentType, Class<ComponentImplTpe> componentImplClass) {
        components.put(new Component(componentType, null), new InjectionProvider<>(componentImplClass));
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass,  Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers){
            if (!qualifier.annotationType().isAnnotationPresent(Qualifier.class)) {
                throw new IllegalComponentException();
            }
            components.put(new Component(componentClass, qualifier), new InjectionProvider<>(componentImplClass));
        }
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional getType(ComponentRef ref) {
                if (ref.component().qualifier() != null) {
                    return Optional.ofNullable(components.get(ref.component())).map(p -> p.get(this));
                }
                if (ref.isContainer()) {
                    return getContainer(ref);
                }
                return getComponent(ref);
            }

            private Optional getComponent(ComponentRef ref) {
                return Optional.ofNullable(components.get(ref.component())).map(p -> p.get(this));
            }

            private Optional getContainer(ComponentRef ref) {
                Type container = ref.getContainer();
                if (container != Provider.class) {
                    return Optional.empty();
                }
                return Optional.ofNullable(components.get(ref.component())).map(p -> (Provider<Object>) () -> p.get(this));
            }
        };
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencyRefs()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()){
                if (visiting.contains(dependency.component())) {
                    throw new CyclicDependencyFoundException(new HashSet<>(visiting));
                }
                visiting.push(dependency.component());
                checkDependencies(new Component(dependency.component().componentType(), dependency.component().qualifier()), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef> getDependencyRefs() {
            return List.of();
        }
    }

}
