package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;
import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
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
            public <Type> Optional<Type> get(Class<Type> componentClass) {
                return Optional.ofNullable(providers.get(componentClass)).map(p -> (Type) p.get(this));
            }

            @Override
            public Optional get(ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                Class<?> componentType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType)).map(p -> (Provider<Object>) () -> p.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
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
    }

    interface ComponentProvider<Type> {
        Type get(Context context);

        default List<Class<?>> getDependencies() {
            return List.of();
        }
    }

}
