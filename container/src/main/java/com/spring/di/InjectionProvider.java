package com.spring.di;

import com.spring.di.exception.IllegalComponentException;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> constructor;
    private List<Field> fields;
    private List<Method> methods;
    private List<ComponentRef> dependencies;

    public InjectionProvider(Class<T> implementation) {
        checkImplementation(implementation);

        this.constructor = getInjectConstructor(implementation);
        this.fields = getInjectFieldList(implementation);
        this.methods = getInjectMethodList(implementation);

        checkFields(this.fields);
        checkMethods(this.methods);
        dependencies = getDependencyRefs();
    }

    @Override
    public T get(Context context) {
        try {
            Object[] params = toDependencies(context, constructor);
            T instance = constructor.newInstance(params);
            for (Field field : fields) {
                field.set(instance, toDependency(context, field));
            }
            for (Method method : methods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencyRefs() {
        return Stream.concat(Stream.concat(Arrays.stream(constructor.getParameters()).map(InjectionProvider::toComponentRef),
                        fields.stream().map(InjectionProvider::toComponentRef)),
                methods.stream().flatMap(m -> Arrays.stream(m.getParameters()).map(InjectionProvider::toComponentRef))).toList();
    }

    private static ComponentRef toComponentRef(Field f) {
        return ComponentRef.of(f.getGenericType(), getQualifier(f));
    }

    private static ComponentRef toComponentRef(Parameter p) {
        return ComponentRef.of(p.getParameterizedType(), getQualifier(p));
    }

    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> annotations = Arrays.stream(element.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        if (annotations.size() > 1) {
            throw new IllegalComponentException();
        }
        return annotations.stream().findFirst().orElse(null);
    }

    private static <T> List<Method> getInjectMethodList(Class<T> component) {
        return traverse(component, (current, injectMethods) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(m, injectMethods))
                .filter(m -> notOverrideByNoInjectMethod(m, component.getDeclaredMethods()))
                .toList());
    }

    private static <T> List<Field> getInjectFieldList(Class<T> implementation) {
        return traverse(implementation, (currentClass, injectFields) -> injectable(currentClass.getDeclaredFields()).toList());
    }

    private static <T> Constructor<T> getInjectConstructor(Class<T> componentImplClass) {
        List<Constructor<?>> injectConstructors = injectable(componentImplClass.getDeclaredConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(componentImplClass));
    }

    private static <T> Constructor<T> defaultConstructor(Class<T> componentImplClass) {
        try {
            return componentImplClass.getDeclaredConstructor();
        } catch (Exception e) {
            throw new IllegalComponentException(e);
        }
    }

    private static void checkMethods(List<Method> methods1) {
        if (methods1.stream().anyMatch(m -> m.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }
    }

    private static void checkFields(List<Field> fields1) {
        if (fields1.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
    }

    private static <T> void checkImplementation(Class<T> implementation) {
        if (Modifier.isAbstract(implementation.getModifiers())) {
            throw new IllegalComponentException();
        }
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return Arrays.stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverrideByInjectMethod(Method m, List<Method> injectMethods) {
        return injectMethods.stream().noneMatch(c -> isOverride(m, c));
    }

    private static boolean isOverride(Method m, Method c) {
        return c.getName().equals(m.getName()) &&
                Arrays.equals(c.getParameterTypes(), m.getParameterTypes());
    }

    private static boolean notOverrideByNoInjectMethod(Method m, Method[] declaredMethods) {
        return notInjectable(declaredMethods).noneMatch(c -> isOverride(m, c));
    }

    private static Stream<Method> notInjectable(Method[] declaredMethods) {
        return Arrays.stream(declaredMethods).filter(m1 -> !m1.isAnnotationPresent(Inject.class));
    }

    private static Object toDependency(Context context, Field field) {
        return toDependency(context, field.getGenericType(), getQualifier(field));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getParameters()).map(p -> getDependency(context, p)).toArray();
    }

    private static Object getDependency(Context context, Parameter p) {
        return toDependency(context, p.getParameterizedType(), getQualifier(p));
    }

    private static Object toDependency(Context context, Type type, Annotation qualifier) {
        return context.getType(ComponentRef.of(type, qualifier)).get();
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<Class<?>, List<T>, List<T>> toInjections) {
        List<T> injectFields = new ArrayList<>();
        Class<?> currentClass = component;
        while (currentClass != Object.class) {
            injectFields.addAll(toInjections.apply(currentClass, injectFields));
            currentClass = currentClass.getSuperclass();
        }
        return injectFields;
    }
}
