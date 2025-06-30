package com.gl.classext;

import com.gl.classext.ThreadSafeWeakCache.ClassExtensionKey;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static com.gl.classext.Aspects.AroundAdvice.applyDefault;
import static org.junit.jupiter.api.Assertions.*;

interface ItemInterface {
    String getName();
}

class Item implements ItemInterface {
    private final String name;

    public Item(String aName) {
        name = aName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }
}

class Book extends Item {
    public Book(String aName) {
        super(aName);
    }
}

class Furniture extends Item {
    public Furniture(String aName) {
        super(aName);
    }
}

class ElectronicItem extends Item {
    public ElectronicItem(String aName) {
        super(aName);
    }
}

class AutoPart extends Item {
    public AutoPart(String aName) {
        super(aName);
    }
}

record ShippingInfo(String result) {
}

@ExtensionInterface
interface Shippable {
    ShippingInfo ship();
    void log();

    static Shippable extensionFor(Item anItem) {
        return StaticClassExtension.sharedExtension(anItem, Shippable.class);
    }
}

interface ShippableItemInterface extends Shippable, ItemInterface {
    static ShippableItemInterface extensionFor(Item anItem) {
        return StaticClassExtension.sharedExtension(anItem, ShippableItemInterface.class);
    }
}

class ItemShippable implements Shippable {
    static final StringBuilder LOG = new StringBuilder();

    public ItemShippable(Item aDelegate) {
        delegate = aDelegate;
    }

    public ItemShippable(Item aDelegate, String anInstructions) {
        this(aDelegate);
        instructions = anInstructions;
    }

    public ShippingInfo ship() {
        String result = MessageFormat.format("{0} NOT shipped", getDelegate());
        if (instructions != null)
            result +=  ". Instructions: " + instructions;
        return new ShippingInfo(result);
    }

    public void log() {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(delegate.getName()).append(" is about to be shipped");
    }

    private Item delegate;

    public Item getDelegate() {
        return delegate;
    }

    public void setDelegate(Item aDelegate) {
        delegate = aDelegate;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String aInstructions) {
        instructions = aInstructions;
    }

    String instructions;
}

@SuppressWarnings("unused")
class BookShippable extends ItemShippable {
    public BookShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@SuppressWarnings("unused")
class FurnitureShippable extends ItemShippable {
    public FurnitureShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@SuppressWarnings("unused")
class ElectronicItemShippable extends ItemShippable {
    public ElectronicItemShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

public class StaticClassExtensionTest {
    /**
     * Tests for exact match when a matching extension is defined for the passed object's class
     */
    @Test
    void shipmentTest() {
        StaticClassExtension.sharedInstance().setVerbose(true);
        try {
            Item[] items = {
                    new Book("book"),
                    new Furniture("furniture"),
                    new ElectronicItem("electronic item")
            };

            StringBuilder shippingInfos = new StringBuilder();
            for (Item item : items) {
                ShippableItemInterface shippable = StaticClassExtension.sharedExtension(item, ShippableItemInterface.class);
                ShippingInfo shippingInfo = shippable.ship();
                assertEquals(item.getName(), shippable.getName());
                assertEquals(item.hashCode(), shippable.hashCode());
                assertEquals(item.toString(), shippable.toString());
                assertEquals((Object) shippable, item);

                if (!shippingInfos.isEmpty())
                    shippingInfos.append("\n");
                shippingInfos.append(shippingInfo);
                System.out.println(shippingInfo);
            }
            assertEquals("""
                         ShippingInfo[result=book shipped]
                         ShippingInfo[result=furniture shipped]
                         ShippingInfo[result=electronic item shipped]""",
                    shippingInfos.toString());
        } finally {
            StaticClassExtension.sharedInstance().setVerbose(false);
        }
    }

    ShippingInfo ship(Item anItem) {
        Shippable shippable = Shippable.extensionFor(anItem);
        return shippable.ship();
    }

    /**
     * Tests for partial match when only a super class extension is defined for the passed object's class
     */
    @Test
    void partialMatchShipmentTest() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month shipped]
                     ShippingInfo[result=Sofa shipped]
                     ShippingInfo[result=Soundbar shipped]
                     ShippingInfo[result=Tire NOT shipped]""",
                shippingInfos.toString());
    }

    @Test
    void extensionFactoryTest() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };
        final String instructions = "Handle with care";

        StaticClassExtension classExtension = new StaticClassExtension();
        classExtension.setExtensionFactory((anObject, anExtensionInterface, anExtensionClass) -> {
            Object result = null;

            if (anObject instanceof AutoPart)
                result = new ItemShippable((Item) anObject, instructions);

            return result;
        });

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = classExtension.extension(item, Shippable.class).ship();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                      ShippingInfo[result=The Mythical Man-Month shipped]
                      ShippingInfo[result=Sofa shipped]
                      ShippingInfo[result=Soundbar shipped]
                      ShippingInfo[result=Tire NOT shipped. Instructions: Handle with care]""",
                shippingInfos.toString());
    }

    /**
     * Tests for missing extension
     */
    @Test
    @SuppressWarnings({"rawtypes"})
    void noShippableClassFoundTest() {
        try {
            StaticClassExtension.DelegateHolder extension = StaticClassExtension.sharedExtension(new Book("noname"), StaticClassExtension.DelegateHolder.class);
            fail("Unexpected extension found: " + extension);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Test for cached extension
     */
    @Test
    void cacheTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));

        StaticClassExtension.sharedInstance().setCacheEnabled(false);
        try {
            extension = Shippable.extensionFor(book);
            assertNotSame(extension, Shippable.extensionFor(book));

            StaticClassExtension.sharedInstance().setCacheEnabled(true);
            extension = Shippable.extensionFor(book);
            assertSame(extension, Shippable.extensionFor(book));
        } finally {
            StaticClassExtension.sharedInstance().setCacheEnabled(true);
        }
    }

    /**
     * Test for cached extension
     */
    @SuppressWarnings("unchecked")
    @Test
    void cacheEntryRemovalTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));
        StaticClassExtension.sharedInstance().getExtensionCache().remove(new ClassExtensionKey(book, Shippable.class));
        assertNotSame(extension, Shippable.extensionFor(book));
    }

    /**
     * Test for not cached extension
     */
    @Test
    void nonCacheTest() {
        Book book = new Book("Shining");
        StaticClassExtension.sharedInstance().setCacheEnabled(false);
        try {
            Shippable extension = StaticClassExtension.sharedExtension(book, Shippable.class);
            Shippable actual = StaticClassExtension.sharedExtension(book, Shippable.class);
            assertNotSame(extension, actual);
        } finally {
            StaticClassExtension.sharedInstance().setCacheEnabled(true);
        }
    }

    /**
     * Tests for cached extension release by GC and cleaned manually
     */
    @Test
    void cacheCleanupTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));

        assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
        StaticClassExtension.sharedInstance().cacheCleanup();
        assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
    }

    /**
     * Tests for cached extension cleared
     */
    @Test
    void cacheClearTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));
        StaticClassExtension.sharedInstance().cacheClear();
        assertTrue(StaticClassExtension.sharedInstance().cacheIsEmpty());
        assertNotSame(extension, Shippable.extensionFor(book));
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void scheduledCleanupCacheTest() {
        StaticClassExtension.sharedInstance().scheduleCacheCleanup();
        try {
            Book book = new Book("");
            Shippable.extensionFor(book);
            System.gc();
            assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
            try {
                System.out.println("Waiting 1.5 minutes for automatic cache cleanup...");
                Thread.sleep(90000);
            } catch (InterruptedException aE) {
                // do nothing
            }
            assertTrue(StaticClassExtension.sharedInstance().cacheIsEmpty());
        } finally {
            StaticClassExtension.sharedInstance().shutdownCacheCleanup();
        }
    }

    @Test
    void performanceTest() {
        performanceTestStatic();
        performanceTestFunctional();
    }

    public static void performanceTestStatic() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            for (Item item : items) {
                ShippableItemInterface extension = StaticClassExtension.sharedInstance().extension(item, ShippableItemInterface.class);
                extension.log();
//                extension.getName();
            }
        }
        System.out.println("STATIC - Elapsed time: " + ((System.currentTimeMillis()-startTime) / 1000f));
        ItemShippable.LOG.setLength(0);
    }

    public static void performanceTestFunctional() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            for (Item item : items) {
                log(item);
            }
        }
        System.out.println("FUNCTIONAL - Elapsed time: " + ((System.currentTimeMillis() - startTime) / 1000f));
    }

    private static void log(Item item) {
        switch (item) {
            case Book book -> logBook(book);
            case Furniture furniture -> logFurniture(furniture);
            case ElectronicItem electronicItem -> logElectronicItem(electronicItem);
            case AutoPart autoPart -> logAutoPart(autoPart);
            default -> throw new IllegalStateException("Unexpected value: " + item);
        }
    }


    static StringBuilder LOG = new StringBuilder();

    private static void logElectronicItem(ElectronicItem anElectronicItem) {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(anElectronicItem.getName()).append(" is about to be shipped");
    }

    private static void logFurniture(Furniture aFurniture) {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(aFurniture.getName()).append(" is about to be shipped");
    }

    private static void logBook(Book aBook) {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(aBook.getName()).append(" is about to be shipped");
    }

    private static void logAutoPart(AutoPart anAutoPart) {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(anAutoPart.getName()).append(" is about to be shipped");
    }

    @Test
    void equalsTest() {
        Book book = new Book("The Mythical Man-Month");
        Shippable extension = Shippable.extensionFor(book);
        assertTrue(ClassExtension.equals(book, extension));
        assertTrue(ClassExtension.equals(extension, book));

        assertNotEquals(book, extension);
        assertEquals(extension, book);

        assertFalse(ClassExtension.equals(book, null));
        assertFalse(ClassExtension.equals(extension, null));
        assertFalse(ClassExtension.equals(null, book));
        assertFalse(ClassExtension.equals(null, extension));
    }

    @Test
    void delegateTest() {
        Book book = new Book("The Mythical Man-Month");
        ShippableItemInterface extension = ShippableItemInterface.extensionFor(book);
        assertSame(book, ClassExtension.getDelegate(extension));

        assertEquals(book.getName(), extension.getName());
    }

    @Test
    void checkToString() {
        Book book = new Book("The Mythical Man-Month");
        Shippable shippable = Shippable.extensionFor(book);
        assertEquals(book.toString(), shippable.toString());
    }

    @Test
    void checkHashCodeString() {
        Book book = new Book("The Mythical Man-Month");
        Shippable extension = Shippable.extensionFor(book);
        assertEquals(book.hashCode(), extension.hashCode());
    }

    @Test
    void checkMultipleExtensionsForSameObject() {
        Book book = new Book("The Mythical Man-Month");
        Shippable shippable = Shippable.extensionFor(book);
        assertEquals(book.toString(), shippable.toString());

        ShippableItemInterface shippableItem = ShippableItemInterface.extensionFor(book);
        assertEquals(book.toString(), shippableItem.toString());

        assertSame(shippable, Shippable.extensionFor(book));
    }

    @Test
    void testAspectUsingBuilder() {
        List<String> out = new ArrayList<>();
        StaticClassExtension staticClassExtension = new StaticClassExtension();

        staticClassExtension.aspectBuilder().
                extensionInterface("*").
                    operation("toString()").
                    objectClass(Object.class).
                        before((operation, object, args) -> out.add("BEFORE: " + object + "-> toString()")).
                        after((result, operation, object, args) -> out.add("AFTER: result - " + result)).
                    objectClass(Book.class).
                        before((operation,object, args) -> out.add("BOOK BEFORE: " + object + "-> toString()")).
                        after((result, operation, object, args) -> out.add("BOOK AFTER: result - " + result)).
                    objectClass(AutoPart.class).
                        before((operation, object, args) -> out.add("BEFORE: " + object + "-> toString()")).
                        after((result, operation, object, args) -> out.add("AFTER: result - " + result)).
                        around((performer, operation, object, args) -> "ALTERED AUTO PART: " + applyDefault(performer, operation, object, args));

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            ShippableItemInterface extension = staticClassExtension.extension(item, ShippableItemInterface.class);
            out.add(extension.toString());
        }
        String outString = String.join("\n", out);
        System.out.println(outString);
        assertEquals("""
                     BOOK BEFORE: The Mythical Man-Month-> toString()
                     BOOK AFTER: result - The Mythical Man-Month
                     The Mythical Man-Month
                     BEFORE: Sofa-> toString()
                     AFTER: result - Sofa
                     Sofa
                     BEFORE: Soundbar-> toString()
                     AFTER: result - Soundbar
                     Soundbar
                     ALTERED AUTO PART: Tire""", outString);
    }
}