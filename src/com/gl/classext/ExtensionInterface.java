/*
Copyright 2024 Gregory Ledenev (gregory.ledenev37@gmail.com)

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.gl.classext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation that allows to mark some interfaces as extension interfaces. Particularly, the
 * {@code StaticClassExtension} utilizes that annotation to find extension interfaces and use them to:
 * <ol>
 * <li>Compose proper extension class names</li>
 * <li>Determine packages to lookup for extension classes via the {@code packages} parameter</li>
 * </ol>
 * Both dynamic ands static extensions use that annotation to determine:
 * <ol>
 * <li>An extension type via the {@code type} parameter</li>
 * <li>An extension caching policy via the {@code cachePolicy} parameter</li>
 * <li>An aspects handling policy via the {@code aspectsPolicy} parameter</li>
 * </ol>
 * <p>
 * Dynamic nature of the {@code DynamicClassExtension} prevents detecting some errors at compile time, so be careful
 * during refactoring of extension interfaces and check operation handling after any refactorings. It is recommended to
 * mark any extension interfaces with {@code @ExtensionInterface} annotation to let developers know that they should check and
 * test dynamic operations after any refactorings.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionInterface {
    ClassExtension.Type type() default ClassExtension.Type.STATIC_PROXY;
    ClassExtension.CachePolicy cachePolicy() default ClassExtension.CachePolicy.DEFAULT;
    ClassExtension.AspectsPolicy aspectsPolicy() default ClassExtension.AspectsPolicy.DEFAULT;
    String[] packages() default {};
    boolean adoptRecord() default false;
}