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

/**
 * Interface representing a context for evaluating and managing expressions.
 * Provides methods to retrieve and assign values associated with specific expressions.
 * Expression format supports:
 * <ul>
 *   <li>Simple property access: "firstName"</li>
 *   <li>Nested property access: "address.city"</li>
 *   <li>List/array indexing: "items[0]"</li>
 *   <li>Map access: "data['key']" or "data["key"]"</li>
 *   <li>List operations: "items[+]" (add), "items[-]" (remove)</li>
 *   <li>Null-safe navigation: "address?.city" or "addresses[0]?.city"</li>
 * </ul>
 * <p>
 *     Sample expressions: <br>
 *     "departmentMap['Sales'].employeeMap['Grace'].salary"<br>
 *     "departmentMap['Sales']?.employeeMap['Grace']?.salary"<br>
 *     "departments[0].employees[0].name"
 * </p>
 */
@ExtensionInterface
public interface ExpressionContext {
    /**
     * Retrieves a value from an object using the specified property expression.
     *
     * @param expression The property expression to evaluate
     * @return The value at the specified expression path
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     */
    Object getExpressionValue(String expression);

    /**
     * Retrieves a value from an object using the specified property expression,
     * returning a default value if the evaluated expression yields null.
     *
     * @param expression The property expression to evaluate.
     * @param defaultValue The default value to return if the evaluated expression yields null.
     * @return The value at the specified expression path, or the default value if the result is null.
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation.
     */
    default <T> T getExpressionValue(String expression, T defaultValue) {
        Object value = getExpressionValue(expression);
        //noinspection unchecked
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Retrieves a value from an object using the specified property expression,
     * returning a default value if the evaluated expression yields null.
     * Additionally, allows suppressing a NullPointerException if encountered, returning a default value instead.
     *
     * @param expression The property expression to evaluate.
     * @param defaultValue The default value to return if the evaluated expression yields null.
     * @param suppressNPE A flag to indicate whether to suppress NullPointerException.
     * @return The value at the specified expression path, the default value if the result is null,
     *         or the default value if a NullPointerException is suppressed.
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     *                              and suppressNPE is false.
     */
    default <T> T getExpressionValue(String expression, T defaultValue, boolean suppressNPE) {
        try {
            return getExpressionValue(expression, defaultValue);
        } catch (NullPointerException e) {
            if (suppressNPE)
                return defaultValue;
            else
                throw e;
        }
    }

    /**
     * Sets a value in an object at the specified property expression path.
     *
     * @param expression The property expression indicating where to set the value
     * @param value      The value to set
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     */
    void setExpressionValue(String expression, Object value);
}
