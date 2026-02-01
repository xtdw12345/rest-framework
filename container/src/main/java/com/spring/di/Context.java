package com.spring.di;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<Class<?>, Object> components = new HashMap<>();
    private Map<Class<?>, Class<?>> implementations = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        components.put(componentClass, component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        implementations.put(componentClass, componentImplClass);
    }

    @SuppressWarnings("unchecked")
    public <ComponentType> ComponentType getInstance(Class<ComponentType> componentClass) {
        if (components.containsKey(componentClass)) {
            return (ComponentType) components.get(componentClass);
        }
        Class<?> implementationClass = implementations.get(componentClass);
        try {
            return (ComponentType) implementationClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
