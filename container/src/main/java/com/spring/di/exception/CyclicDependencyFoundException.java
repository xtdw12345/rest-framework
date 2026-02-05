package com.spring.di.exception;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();
    public CyclicDependencyFoundException(Class<?> component) {
        components.add(component);
    }

    public CyclicDependencyFoundException(Set<Class<?>> components) {
        this.components = components;
    }

    public CyclicDependencyFoundException(Class<?> component, CyclicDependencyFoundException e) {
        components.add(component);
        components.addAll(e.components);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
