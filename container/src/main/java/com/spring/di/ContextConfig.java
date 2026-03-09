package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class ContextConfig {

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public ContextConfig() {
        scopes.put(Singleton.class, SingleProvider::new);
    }

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
        bind(componentType, componentImplClass, componentImplClass.getAnnotations());
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass,  Annotation... annotations) {
        if (Arrays.stream(annotations).map(Annotation::annotationType).anyMatch(a -> !a.isAnnotationPresent(Qualifier.class) && !a.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }
        List<Annotation> qualifiers = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        List<Annotation> annotatedScopes = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).toList();
        if (annotatedScopes.isEmpty()) {
            annotatedScopes = Arrays.stream(componentImplClass.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).toList();
        }

        ComponentProvider<?> provider = new InjectionProvider<>(componentImplClass);
        if (!annotatedScopes.isEmpty()) {
            provider = scopes.get(annotatedScopes.get(0).annotationType()).apply(provider);
        }
        if(qualifiers.isEmpty()) {
            components.put(new Component(componentClass, null), provider);
        }
        for (Annotation qualifier : qualifiers){
            components.put(new Component(componentClass, qualifier), provider);
        }
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scopeType, Function<ComponentProvider<?>, ComponentProvider<?>> providerFactory) {
        scopes.put(scopeType, providerFactory);
    }

    static class SingleProvider<T> implements  ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingleProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) {
                singleton = provider.get(context);
            }
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencyRefs() {
            return provider.getDependencyRefs();
        }
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional get(ComponentRef ref) {
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

        default List<ComponentRef<?>> getDependencyRefs() {
            return List.of();
        }
    }

}
