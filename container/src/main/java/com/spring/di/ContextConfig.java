package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        providers.put(componentClass, context -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        providers.put(componentClass, new InjectionProvider<>(componentImplClass));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional getType(Type type) {
                if (isContainerType(type)) {
                    return getContainer((ParameterizedType) type);
                }
                return getComponent((Class<?>) type);
            }

            private <Type> Optional<Type> getComponent(Class<Type> componentClass) {
                return Optional.ofNullable(providers.get(componentClass)).map(p -> (Type) p.get(this));
            }

            private Optional getContainer(ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                Class<?> componentType = (Class<?>) getComponentType(parameterizedType);
                return Optional.ofNullable(providers.get(componentType)).map(p -> (Provider<Object>) () -> p.get(this));
            }
        };
    }

    private static Type getComponentType(ParameterizedType parameterizedType) {
        return parameterizedType.getActualTypeArguments()[0];
    }

    private static boolean isContainerType(Type dependencyType) {
        return dependencyType instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependencyType : providers.get(component).getDependencyTypes()) {
            Class<?> dependency;
            if (isContainerType(dependencyType)) {
                dependency = (Class<?>) getComponentType(((ParameterizedType) dependencyType));
                if (!providers.containsKey(dependency)) {
                    throw new DependencyNotFoundException(component, dependency);
                }
            } else {
                dependency = (Class<?>) dependencyType;
                checkDependency(component, visiting, dependency);
            }
        }
    }

    private void checkDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(component, dependency);
        }
        if (visiting.contains(dependency)) {
            throw new CyclicDependencyFoundException(new HashSet<>(visiting));
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencyTypes() {
            return List.of();
        }
    }

}
