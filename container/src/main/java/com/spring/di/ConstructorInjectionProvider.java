package com.spring.di;

import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> constructor;
    private List<Field> fields;
    private List<Method> methods;

    public ConstructorInjectionProvider(Class<T> implementation) {
        if (Modifier.isAbstract(implementation.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.constructor = getInjectConstructor(implementation);
        this.fields = getInjectFieldList(implementation);
        this.methods = getInjectMethodList(implementation);

        this.methods.forEach((method) -> {
            if (method.getTypeParameters().length > 0) {
                throw new IllegalComponentException();
            }
        });
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
            for (Method method : methods) {
                method.setAccessible(true);
                method.invoke(instance, Arrays.stream(method.getParameterTypes()).map(t -> context.get(t).get()).toArray());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(methods.stream().flatMap(m -> Arrays.stream(m.getParameterTypes())),concat(fields.stream().map(Field::getType), Arrays.stream(constructor.getParameterTypes()))).toList();
    }

    private static <T> List<Method> getInjectMethodList(Class<T> implementation) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = implementation;
        while (Object.class != current) {
            injectMethods.addAll(Arrays.stream(current.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Inject.class))
                    .filter(m -> injectMethods.stream().noneMatch(c -> c.getName().equals(m.getName()) &&
                            Arrays.equals(c.getParameterTypes(), m.getParameterTypes()))
                    ).filter(m -> Arrays.stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(c -> c.getName().equals(m.getName()) &&
                            Arrays.equals(c.getParameterTypes(), m.getParameterTypes())))
                    .toList());
            current = current.getSuperclass();
        }
        return injectMethods;
    }

    private static <T> List<Field> getInjectFieldList(Class<T> implementation) {
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = implementation;
        while (currentClass != Object.class) {
            allFields.addAll(Arrays.stream(currentClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    private static <T> Constructor<T> getInjectConstructor(Class<T> componentImplClass) {
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
