package com.spring.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Context {

    private final Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providerMap.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        Constructor<?> constructor = getConstructor(componentImplClass);
        providerMap.put(componentClass, () -> {
            try {
                Object[] params = Arrays.stream(constructor.getParameters()).map(p -> getInstance(p.getType())).toArray();
                return constructor.newInstance(params);
            } catch (Exception e) {
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

    @SuppressWarnings("unchecked")
    public <ComponentType> ComponentType getInstance(Class<ComponentType> componentClass) {
        return (ComponentType) providerMap.get(componentClass).get();
    }
}
