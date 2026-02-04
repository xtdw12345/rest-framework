package com.spring.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class ContextContainer {

    private final Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();
    private Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        componentProviderMap.put(componentClass, context -> component);
        dependencies.put(componentClass, new ArrayList<>());
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?> constructor = getConstructor(componentImplClass);
        componentProviderMap.put(componentClass, new InjectioinProvider<>(componentClass, constructor));
        dependencies.put(componentClass, Arrays.stream(constructor.getParameterTypes()).toList());
    }

    public Context getContext() {
        for (Class<?> component : dependencies.keySet()) {
            for (Class<?> dependency : dependencies.get(component)) {
                if (!dependencies.containsKey(dependency)) {
                    throw new DependencyNotFoundException(component, dependency);
                }
            }
        }
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> componentClass) {
                return Optional.ofNullable(componentProviderMap.get(componentClass)).map(p -> (Type) p.get(this));
            }
        };
    }

    interface ComponentProvider<Type> {
        Type get(Context context);
    }

    class InjectioinProvider<T> implements ComponentProvider<T> {
        private Class<?> componentClass;
        private final Constructor<T> constructor;
        private boolean constructing;

        public InjectioinProvider(Class<?> componentClass, Constructor<T> constructor) {
            this.componentClass = componentClass;
            this.constructor = constructor;
        }

        @Override
        public T get(Context context) {
            if (constructing) {
                throw new CyclicDependencyFoundException(componentClass);
            }
            constructing = true;
            try {
                Object[] params = Arrays.stream(constructor.getParameters())
                        .map(p -> context.get(p.getType()).orElseThrow(() -> new DependencyNotFoundException(componentClass, p.getType())))
                        .toArray();
                return constructor.newInstance(params);
            } catch (CyclicDependencyFoundException e) {
                throw new CyclicDependencyFoundException(componentClass, e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
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
