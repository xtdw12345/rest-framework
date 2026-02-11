package com.spring.di;

import com.spring.di.exception.CyclicDependencyFoundException;
import com.spring.di.exception.DependencyNotFoundException;

import java.util.*;

public class ContextConfig {

    private final Map<Class<?>, ComponentProvider<?>> componentProviderMap = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType component) {
        componentProviderMap.put(componentClass, context -> component);
    }

    public <ComponentType, ComponentImplTpe extends ComponentType> void bind(Class<ComponentType> componentClass, Class<ComponentImplTpe> componentImplClass) {
        componentProviderMap.put(componentClass, new InjectionProvider<>(componentImplClass));
    }

    public Context getContext() {
        componentProviderMap.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> componentClass) {
                return Optional.ofNullable(componentProviderMap.get(componentClass)).map(p -> (Type) p.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : componentProviderMap.get(component).getDependencies()) {
            if (!componentProviderMap.containsKey(dependency)) {
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
