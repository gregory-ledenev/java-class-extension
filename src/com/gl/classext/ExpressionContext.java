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
     * Sets a value in an object at the specified property expression path.
     *
     * @param expression The property expression indicating where to set the value
     * @param value      The value to set
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     */
    void setExpressionValue(String expression, Object value);
}
