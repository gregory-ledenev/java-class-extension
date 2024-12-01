package com.gl.classext.com.gl.classext.shipment;

public class Item {
    private final String name;

    public Item(String aName) {
        name = aName;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }
}