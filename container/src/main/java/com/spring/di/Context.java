package com.spring.di;

import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> getType(ComponentRef<ComponentType> ref);
}
