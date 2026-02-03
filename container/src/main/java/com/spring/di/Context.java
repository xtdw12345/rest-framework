package com.spring.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class Context {

    private final Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providerMap.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?> constructor = getConstructor(componentImplClass);
        providerMap.put(componentClass, new ComponentProvider<>(componentClass, constructor));
    }

    public <Type> Optional<Type> get(Class<Type> componentClass) {
        return Optional.ofNullable(providerMap.get(componentClass)).map(p -> (Type) p.get());
    }

    class ComponentProvider<T> implements Provider<T> {
        private Class<?> componentClass;
        private final Constructor<T> constructor;
        private boolean constructing;

        public ComponentProvider(Class<?> componentClass, Constructor<T> constructor) {
            this.componentClass = componentClass;
            this.constructor = constructor;
        }

        @Override
        public T get() {
            if (constructing) {
                throw new CyclicDependencyFoundException(componentClass);
            }
            constructing = true;
            try {
                Object[] params = Arrays.stream(constructor.getParameters())
                        .map(p ->
                                ((Optional<?>) Optional.ofNullable(providerMap.get(p.getType()))
                                        .map(p1 -> (Object) p1.get()))
                                        .orElseThrow(() -> new DependencyNotFoundException(componentClass, p.getType()))
                        )
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
