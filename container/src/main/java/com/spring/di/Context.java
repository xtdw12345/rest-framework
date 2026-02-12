package com.spring.di;

import jakarta.inject.Provider;import java.lang.reflect.ParameterizedType;import java.util.Optional;

public interface Context {

    <Type> Optional<Type> get(Class<Type> componentClass);

    Optional get(ParameterizedType parameterizedType);
}
