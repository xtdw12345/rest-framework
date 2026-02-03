package com.spring.di;

public class DependencyNotFoundException extends RuntimeException {
    private Class<?> component;
    private Class<?> dependency;
    public DependencyNotFoundException(Class<?> dependency) {
        this.dependency = dependency;
    }
    public DependencyNotFoundException(Class<?> component, Class<?> dependency) {
        this.component = component;
        this.dependency = dependency;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }
}
