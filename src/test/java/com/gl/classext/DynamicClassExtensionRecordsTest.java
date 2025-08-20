package com.gl.classext;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DynamicClassExtensionRecordsTest {
    public interface Item {
        String name();
    }

    public record Book(String name) implements Item {
    }

    public record Furniture(String name) implements Item {
    }

    public record ElectronicItem(String name) implements Item {
    }

    public record AutoPart(String name) implements Item {
    }

    public record ShippingInfo(String result) {
    }

    public record TrackingInfo(String result) {
    }

    @ExtensionInterface
    public interface Shippable {
        ShippingInfo ship();
        void log(boolean isVerbose);
        void log();
        TrackingInfo track(boolean isVerbose);
        TrackingInfo track();
    }

    public interface ItemShippable extends Item, Shippable {
    }

    private static final StringBuilder LOG = new StringBuilder();

    private static DynamicClassExtension setupDynamicClassExtension() {
        return new DynamicClassExtension().builder(Shippable.class).
                operationName("ship").
                operation(Item.class, book -> new ShippingInfo(book.name() + " item NOT shipped")).
                operation(Book.class, book -> new ShippingInfo(book.name() + " book shipped")).
                operation(Furniture.class, furniture -> new ShippingInfo(furniture.name() + " furniture shipped")).
                operation(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.name() + " electronic item shipped")).
                operationName("log").
                voidOperation(Item.class, (Item item, Boolean isVerbose) -> {
                        if (!LOG.isEmpty())
                            LOG.append("\n");
                        LOG.append(item.name()).append(" is about to be shipped in 1 hour");
                    }).
                voidOperation(Item.class, item -> {
                        if (!LOG.isEmpty())
                            LOG.append("\n");
                        LOG.append(item.name()).append(" is about to be shipped");
                    }).
                operationName("track").
                operation(Item.class, item -> new TrackingInfo(item.name() + " item on its way")).
                operation(Item.class, (Item item, Boolean isVerbose) -> new TrackingInfo(item.name() +
                            " item on its way" + (isVerbose ? "Status: SHIPPED" : ""))).
                build();
    }

    @Test
    void operationUsingBuilderTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension();
        dynamicClassExtension.setVerbose(true);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Shippable extension = dynamicClassExtension.extension(item, Shippable.class);
            extension.log();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(extension.ship());
            shippingInfos.append("\n").append(extension.track());
        }
        System.out.println(LOG);
        System.out.println(shippingInfos);

        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     Sofa is about to be shipped
                     Soundbar is about to be shipped
                     Tire is about to be shipped""", LOG.toString());

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     TrackingInfo[result=The Mythical Man-Month item on its way]
                     ShippingInfo[result=Sofa furniture shipped]
                     TrackingInfo[result=Sofa item on its way]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     TrackingInfo[result=Soundbar item on its way]
                     ShippingInfo[result=Tire item NOT shipped]
                     TrackingInfo[result=Tire item on its way]""",
                shippingInfos.toString());
    }

    @Test
    void overriddenOperationsTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension();
        dynamicClassExtension.setVerbose(true);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder names = new StringBuilder();
        for (Item item : items) {
            ItemShippable extension = dynamicClassExtension.extension(item, ItemShippable.class);
            if (!names.isEmpty())
                names.append("\n");
            names.append(extension.name());
        }
        System.out.println(names.toString());
        assertEquals("""
                     The Mythical Man-Month
                     Sofa
                     Soundbar
                     Tire""", names.toString());

        dynamicClassExtension = dynamicClassExtension.builder(Item.class).
                operationName("name").
                operation(Item.class, item -> item.name()  + "[OVERRIDDEN]").
                operation(AutoPart.class, item -> item.name()  + "[OVERRIDDEN for AutoPart]").
                build();
        names.setLength(0);
        for (Item item : items) {
            ItemShippable extension = dynamicClassExtension.extension(item, ItemShippable.class);
            if (!names.isEmpty())
                names.append("\n");
            names.append(extension.name());
        }
        System.out.println(names.toString());
        assertEquals("""
                     The Mythical Man-Month[OVERRIDDEN]
                     Sofa[OVERRIDDEN]
                     Soundbar[OVERRIDDEN]
                     Tire[OVERRIDDEN for AutoPart]""", names.toString());
    }

    @ExtensionInterface(adoptRecord = true)
    public interface UserInterface {
        String getName();
        String getEmail();
        boolean isEnabled();
        String toString(boolean isVerbose);
    }

    public record User(String name, String email, boolean enabled) implements RecordUserInterface {
        public String toString(boolean isVerbose) {
            return isVerbose ?
                    "User[name=" + name + ", email=" + email + ", enabled=" + enabled + "]" :
                    "User[name=" + name + "]";
        }

    }
    @Test
    void recordAdoptionTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
        dynamicClassExtension.
                builder(UserInterface.class).
                    operationName("toString").
                        operation(User.class, user -> user.toString(false)).build();

        User user = new User("John Doe", "john.doe@gmail.com", false);
        UserInterface extension = dynamicClassExtension.extension(user, UserInterface.class);

        assertEquals("John Doe", extension.getName());
        assertEquals("john.doe@gmail.com", extension.getEmail());
        assertFalse(extension.isEnabled());
        assertEquals("User[name=John Doe, email=john.doe@gmail.com, enabled=false]",
                extension.toString(true));
        assertEquals("User[name=John Doe]",
                extension.toString());
    }

    public interface NoAdoptionUserInterface {
        String getName();
        String getEmail();
        boolean isEnabled();
        String toString(boolean isVerbose);
    }

    public interface RecordUserInterface {
        String name();
        String email();
        boolean enabled();
        String toString(boolean isVerbose);
    }

    @Test
    void recordAdoptionPerformanceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
        User user = new User("John Doe", "john.doe@gmail.com", false);
        UserInterface extension = dynamicClassExtension.extension(user,
                UserInterface.class);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            extension.getName();
//            extension.getEmail();
//            extension.isEnabled();
//            extension.toString(true);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Static adoption extension access execution time: " + (endTime - startTime) + "ms");

        NoAdoptionUserInterface noAdoptionRecordExtension = new DynamicClassExtension().
                builder(NoAdoptionUserInterface.class).
                operationName("getName").
                    operation(User.class, User::name).
                operationName("getEmail").
                    operation(User.class, User::email).
                operationName("isEnabled").
                    operation(User.class, User::enabled).
                operationName("toString").
                    operation(User.class, (User user1, Boolean verbose) -> user1.toString(verbose)).
                build().
                extension(user, NoAdoptionUserInterface.class);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            noAdoptionRecordExtension.getName();
//            noAdoptionRecordExtension.getEmail();
//            noAdoptionRecordExtension.isEnabled();
//            noAdoptionRecordExtension.toString(true);
        }
        endTime = System.currentTimeMillis();
        System.out.println("Dynamic adoption extension access execution time: " + (endTime - startTime) + "ms");

        RecordUserInterface recordExtension = dynamicClassExtension.extension(user,
                RecordUserInterface.class);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            recordExtension.name();
//            recordExtension.email();
//            recordExtension.enabled();
//            extension.toString(true);
        }
        endTime = System.currentTimeMillis();
        System.out.println("Record extension access execution time: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            user.name();
//            user.email();
//            user.enabled();
//            extension.toString(true);
        }
        endTime = System.currentTimeMillis();
        System.out.println("Direct record access execution time: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            try {
                Method method = user.getClass().getMethod("name");
                method.invoke(user);
            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("Method lookup and invoke execution time: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        try {
            Method method = user.getClass().getMethod("name");
            for (int i = 0; i < 1000000; i++) {
                method.invoke(user);
            }
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
        endTime = System.currentTimeMillis();
        System.out.println("Method cached lookup and invoke execution time: " + (endTime - startTime) + "ms");

        PropertyValueSupport propertyValueSupport = new PropertyValueSupport();
        try {
            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                propertyValueSupport.getPropertyValue(user, "name");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        endTime = System.currentTimeMillis();
        System.out.println("Methodhandle cached lookup and invoke execution time: " + (endTime - startTime) + "ms");

    }
}
