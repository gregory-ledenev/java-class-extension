package com.gl.classext;

public interface ClassExtension {
    <T> T extension(Object anObject, Class<T> anExtensionInterface);
}
