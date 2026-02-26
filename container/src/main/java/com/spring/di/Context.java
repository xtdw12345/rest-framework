package com.spring.di;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {
    <ComponentType> Optional<ComponentType> getType(Ref<ComponentType> ref);

    class Ref<ComponentType> {

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> type) {
            return new Ref<>(type);
        }

        public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> type, Annotation qualifier) {
            return new Ref<>(type, qualifier);
        }

        public static Ref of(Type type) {
            return new Ref(type);
        }

        private Type container;
        private Class<ComponentType> component;
        @Getter
        private Annotation qualifier;

        Ref(Type type) {
            init(type);
        }

        public Ref(Class<ComponentType> component, Annotation qualifier) {
            this.component = component;
            this.qualifier = qualifier;
        }

        Ref(Class<ComponentType> component) {
            init(component);
        }

        protected Ref() {
            Type type = ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            init(type);
        }

        private void init(Type type) {
            if (type instanceof ParameterizedType container) {
                this.container = container.getRawType();
                this.component = (Class<ComponentType>) container.getActualTypeArguments()[0];
            } else {
                this.component = (Class<ComponentType>) type;
            }
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return container != null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Ref ref = (Ref) o;
            return Objects.equals(container, ref.container) && Objects.equals(component, ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }
}
