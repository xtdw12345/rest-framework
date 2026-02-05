package com.spring.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ContextContainer {

    private final Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        componentProviderMap.put(componentClass, new ComponentProvider<ComponentType>() {
            @Override
            public ComponentType get(Context context) {
                return component;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return List.of();
            }
        });
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?> constructor = getConstructor(componentImplClass);
        componentProviderMap.put(componentClass, new ConstructorInjectioinProvider<>(constructor, Arrays.stream(constructor.getParameterTypes()).toList()));
    }

    public Context getContext() {
        componentProviderMap.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> componentClass) {
                return Optional.ofNullable(componentProviderMap.get(componentClass)).map(p -> (Type) p.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : componentProviderMap.get(component).getDependencies()) {
            if (!componentProviderMap.containsKey(dependency)) {
                throw new DependencyNotFoundException(component, dependency);
            }
            if (visiting.contains(dependency)) {
                throw new CyclicDependencyFoundException(new HashSet<>(visiting));
            }
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        }
    }

    interface ComponentProvider<Type> {
        Type get(Context context);

        List<Class<?>> getDependencies();
    }

    static class ConstructorInjectioinProvider<T> implements ComponentProvider<T> {
        private final Constructor<T> constructor;
        private List<Class<?>> dependencies;

        public ConstructorInjectioinProvider(Constructor<T> constructor, List<Class<?>> dependencies) {
            this.constructor = constructor;
            this.dependencies = dependencies;
        }

        @Override
        public T get(Context context) {
            try {
                Object[] params = Arrays.stream(constructor.getParameters())
                        .map(p -> context.get(p.getType()).get())
                        .toArray();
                return constructor.newInstance(params);
            }catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Class<?>> getDependencies() {
            return dependencies;
        }
    }

    private static <ComponentType, ComponentImplTpe extends ComponentType> Constructor<?> getConstructor(Class<ComponentImplTpe> componentImplClass) {
        List<Constructor<?>> injectConstructors = Arrays.stream(componentImplClass.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return componentImplClass.getDeclaredConstructor();
            } catch (Exception e) {
                throw new IllegalComponentException(e);
            }
        });
    }
}
