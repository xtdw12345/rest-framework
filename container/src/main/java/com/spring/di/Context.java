package com.spring.di;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private final Map<Class<?>, Provider<?>> providerMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providerMap.put(componentClass, () -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        providerMap.put(componentClass, () -> {
            try {
                return componentImplClass.getDeclaredConstructor().newInstance();
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
