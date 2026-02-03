package com.spring.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Context {

    private final Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providerMap.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?> constructor = getConstructor(componentImplClass);
        providerMap.put(componentClass, () -> {
            try {
                Object[] params = Arrays.stream(constructor.getParameters()).map(p -> get(p.getType()).orElseThrow(DependencyNotFoundException::new)).toArray();
                return constructor.newInstance(params);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
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

    public <Type> Optional<Type> get(Class<Type> componentClass) {
        return Optional.ofNullable(providerMap.get(componentClass)).map(p -> (Type) p.get());
    }
}
