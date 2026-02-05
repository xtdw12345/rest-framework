package com.spring.di;

import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class ConstructorInjectionProvider<T> implements ContextContainer.ComponentProvider<T> {
    private final Constructor<T> constructor;
    private List<Field> fields;

    public ConstructorInjectionProvider(Class<T> implementation) {
        this.constructor = getConstructor(implementation);
        this.fields = getFieldList(implementation);
    }

    @Override
    public T get(Context context) {
        try {
            Object[] params = Arrays.stream(constructor.getParameters())
                    .map(p -> context.get(p.getType()).get())
                    .toArray();
            T instance = constructor.newInstance(params);
            for (Field field : fields) {
                field.setAccessible(true);
                field.set(instance, context.get(field.getType()).get());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(fields.stream().map(Field::getType), Arrays.stream(constructor.getParameterTypes())).toList();
    }

    private static <T> List<Field> getFieldList(Class<T> implementation) {
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = implementation;
        while (currentClass != Object.class) {
            allFields.addAll(Arrays.stream(currentClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
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
}
