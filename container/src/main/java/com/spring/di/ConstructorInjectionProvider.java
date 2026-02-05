package com.spring.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

class ConstructorInjectionProvider<T> implements ContextContainer.ComponentProvider<T> {
    private final Constructor<T> constructor;

    public ConstructorInjectionProvider(Class<T> implementation) {
        this.constructor = getConstructor(implementation);
    }

    private static <T> Constructor<T> getConstructor(Class<T> componentImplClass) {
        List<Constructor<?>> injectConstructors = Arrays.stream(componentImplClass.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return componentImplClass.getDeclaredConstructor();
            } catch (Exception e) {
                throw new IllegalComponentException(e);
            }
        });
    }

    @Override
    public T get(Context context) {
        try {
            Object[] params = Arrays.stream(constructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray();
            return constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(constructor.getParameterTypes()).toList();
    }
}
