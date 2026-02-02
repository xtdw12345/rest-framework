package com.spring.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context {

    private final Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providerMap.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?>[] injectConstructors = Arrays.stream(componentImplClass.getDeclaredConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toArray(Constructor<?>[]::new);
        if (injectConstructors.length > 1) {
            throw new IllegalComponentException();
        }
        if (injectConstructors.length == 0 && Arrays.stream(componentImplClass.getDeclaredConstructors()).noneMatch(c -> c.getParameters().length == 0)) {
            throw new IllegalComponentException();
        }
        providerMap.put(componentClass, () -> {
            try {
                Constructor<?> constructor = Arrays.stream(componentImplClass.getDeclaredConstructors())
                        .filter(c -> c.isAnnotationPresent(Inject.class)).findFirst().orElse(null);
                if (constructor == null) {
                    constructor = componentImplClass.getDeclaredConstructor();
                }
                Object[] params = Arrays.stream(constructor.getParameters()).map(p -> getInstance(p.getType())).toArray();
                return constructor.newInstance(params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <ComponentType> ComponentType getInstance(Class<ComponentType> componentClass) {
        return (ComponentType) providerMap.get(componentClass).get();
    }
}
