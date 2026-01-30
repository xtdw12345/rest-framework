package com.spring;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RestFramework {

    @Test
    public void should_inject_by_constructor() {
        Injector injector = RestFw.createInector(new RestFwModule() {

            @Override
            protected void configure() {
                bind(Car.class).to(ConstructorCar.class);
                bind(Engine.class).to(V6Engine.class);
            }
        });

        Car car = injector.getInstance(Car.class);
        Assert.assertEquals("V6", car.start());
    }

    @Test
    public void should_inject_by_field() {
        Injector injector = RestFw.createInector(new RestFwModule() {

            @Override
            protected void configure() {
                bind(Car.class).to(FieldCar.class);
                bind(Engine.class).to(V6Engine.class);
            }
        });

        Car car = injector.getInstance(Car.class);
        Assert.assertEquals("V6", car.start());
    }

    @Test
    public void should_inject_by_method() {
        Injector injector = RestFw.createInector(new RestFwModule() {

            @Override
            protected void configure() {
                bind(Car.class).to(MethodCar.class);
                bind(Engine.class).to(V6Engine.class);
            }
        });

        Car car = injector.getInstance(Car.class);
        Assert.assertEquals("V6", car.start());
    }



    class RestFw {
        public static Injector createInector(RestFwModule module) {
            return null;
        }
    }

    class Injector {
        public <T> T getInstance(Class<T> clazz) {
            return null;
        }
    }

    abstract class RestFwModule {
        protected abstract void configure();

        protected Binder bind(Class<?> clazz) {
            return null;
        }

        class Binder {
            public void to(Class<?> clazz) {

            }
        }
    }

    interface Car {
        String start();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
    @interface Inject {

    }

    class ConstructorCar implements Car {
        private Engine engine;

        @Inject
        ConstructorCar(Engine engine) {
            this.engine = engine;
        }

        @Override
        public String start() {
            return engine.start();
        }
    }

    class FieldCar implements Car {
        @Inject
        private Engine engine;

        @Override
        public String start() {
            return engine.start();
        }
    }

    class MethodCar implements Car {
        private Engine engine;

        @Override
        public String start() {
            return engine.start();
        }

        @Inject
        private void install(Engine engine) {
            this.engine = engine;
        }
    }


    interface Engine {
        String start();
    }


    class V6Engine implements Engine {
        @Override
        public String start() {
            System.out.println("start v6");
            return "v6";
        }
    }

    class V8Engine implements Engine {
        @Override
        public String start() {
            System.out.println("start v8");
            return "v8";
        }
    }


    //支持bean的选择

    //支持scope

}
