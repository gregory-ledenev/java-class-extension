package com.gl.classext;

import com.gl.classext.AbstractClassExtension.InvokeResult;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gl.classext.PropertyValueSupport.setArrayValue;

/**
 * Processes property expressions to get or set values in object hierarchies.
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
public class ExpressionProcessor {

    /**
     * Constructs an ExpressionProcessor with a specified PropertyValueSupport instance.
     * This allows for custom behavior, such as disabling caching of method handles.
     *
     * @param aPropertyValueSupport The PropertyValueSupport instance to use
     */
    public ExpressionProcessor(PropertyValueSupport aPropertyValueSupport) {
        propertyValueSupport = aPropertyValueSupport;
    }

    /**
     * Default constructor that initializes the processor with a new PropertyValueSupport instance.
     * This instance will use caching for method handles.
     */
    public ExpressionProcessor() {
        this(new PropertyValueSupport());
    }

    /**
     * Retrieves a value from an object using the specified property expression.
     *
     * @param object     The root object to evaluate the expression against
     * @param expression The property expression to evaluate
     * @return The value at the specified expression path
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     */
    public Object getExpressionValue(Object object, String expression) {
        if (object == null)
            throw new NullPointerException("Null value at: object");

        Object current = object;
        String[] expressionParts = splitExpression(expression);

        for (int i = 0; i < expressionParts.length; i++) {
            try {
                current = getPropertyValue(current, expressionParts[i],
                        i > 0 && expressionParts[i - 1].endsWith(NULL_SAFE_OPERATOR)
                );
            } catch (NullPropertyValue e) {
                npeAtSubExpression(expressionParts, i > 0 ? i - 1 : i, false);
            }
        }

        return current;
    }

    /**
     * Sets a value in an object at the specified property expression path.
     *
     * @param object     The root object to set the value in
     * @param expression The property expression indicating where to set the value
     * @param value      The value to set
     * @throws NullPointerException if a null value is encountered in non-null-safe navigation
     */
    public void setExpressionValue(Object object, String expression, Object value) {
        if (object == null)
            throw new NullPointerException("Null value at: object");

        Object current = object;
        String[] expressionParts = splitExpression(expression);

        for (int i = 0; i < expressionParts.length - 1; i++) {
            boolean isNullSafe = expressionParts[i].endsWith(NULL_SAFE_OPERATOR);
            try {
                current = getPropertyValue(current, expressionParts[i], isNullSafe);
            } catch (NullPropertyValue e) {
                npeAtSubExpression(expressionParts, i, false);
            }

            if (current == null) {
                if (isNullSafe)
                    return;
                else
                    npeAtSubExpression(expressionParts, i, false);
            }
        }

        // handle the last part that can be a subscript
        String lastExpressionPart = expressionParts[expressionParts.length - 1];
        if (isSubscript(lastExpressionPart)) {
            PropertyInfo info = parseProperty(lastExpressionPart);
            boolean isNullSafe = lastExpressionPart.endsWith(NULL_SAFE_OPERATOR);
            current = getPropertyValue(current, info.property(), isNullSafe);
            if (current != null) {
                setPropertyValue(current, lastExpressionPart, value);
            } else if (! isNullSafe) {
                npeAtSubExpression(expressionParts, expressionParts.length-1, true);
            }
        } else {
            setPropertyValue(current, lastExpressionPart, value);
        }
    }

    private boolean isSubscript(String expression) {
        return expression.endsWith(SUBSCRIPT_END) || expression.endsWith(SUBSCRIPT_END_OPTIONAL);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setPropertyValue(Object object, String expression, Object value) {
        PropertyInfo info = parseProperty(expression);

        if (info.index() != null) {
            if (object instanceof List list) {
                if ("+".equals(info.index()))
                    list.add(value);
                else if ("-".equals(info.index()))
                    list.remove(value);
                else
                    list.set(Integer.parseInt(info.index()), value);
            } else if (object instanceof Map) {
                ((Map) object).put(info.unquotedIndex(), value);
            } else if (object.getClass().isArray()) {
                setArrayValue(object, Integer.parseInt(info.index()), value);
            }
        } else {
            propertyValueSupport.setPropertyValue(object, info.property(), value);
        }
    }

    private static final String NULL_SAFE_OPERATOR = "?";
    private static final String SUBSCRIPT_START = "[";
    private static final String SUBSCRIPT_END = "]";
    private static final String SUBSCRIPT_END_OPTIONAL = "]?";

    private record PropertyInfo(String property, String index, boolean nullSafe) {
        String name() {
            return property().substring(0, 1).toUpperCase() + property().substring(1);
        }

        String unquotedIndex() {
            return index().replace("'", "").replace("\"", "");
        }
    }

    private static String[] splitExpression(String expression) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (!inQuotes && (c == '\'' || c == '"')) {
                inQuotes = true;
                quoteChar = c;
                currentPart.append(c);
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                currentPart.append(c);
            } else if (!inQuotes && c == '.') {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                }
            } else {
                currentPart.append(c);
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts.toArray(new String[0]);
    }

    private static void npeAtSubExpression(String[] parts, int i, boolean trimSubscript) {
        String[] elements = Arrays.copyOfRange(parts, 0, i + 1);
        if (trimSubscript)
            elements[elements.length - 1] = parseProperty(elements[elements.length - 1]).property();
        throw new NullPointerException("Null value at: " + String.join(".", elements));
    }

    private static PropertyInfo parseProperty(String part) {
        String property;
        String index = null;

        if (part.contains(SUBSCRIPT_START)) {
            property = part.substring(0, part.indexOf(SUBSCRIPT_START));
            index = part.substring(part.indexOf(SUBSCRIPT_START) + 1, part.lastIndexOf(SUBSCRIPT_END));
        } else {
            property = part;
        }

        boolean nullSafe = property.endsWith(NULL_SAFE_OPERATOR);
        if (nullSafe) {
            property = property.substring(0, property.length() - 1);
        }

        return new PropertyInfo(property, index, nullSafe);
    }

    static class NullPropertyValue extends NullPointerException {}


    public PropertyValueSupport getPropertyValueSupport() {
        return propertyValueSupport;
    }

    private final PropertyValueSupport propertyValueSupport;

    private Object getPropertyValue(Object current, String part, boolean isNullSafe) throws NullPropertyValue {
        if (current == null) {
            if (isNullSafe)
                return null;
            throw new NullPropertyValue();
        }

        PropertyInfo info = parseProperty(part);
        InvokeResult result = new InvokeResult(propertyValueSupport.getPropertyValue(current, info.property()));

        return getSubscriptValue(result.result(), info.index());
    }

    private Object getSubscriptValue(Object value, String index) {
        if (index == null) {
            return value;
        }

        if (value instanceof List) {
            return ((List<?>) value).get(Integer.parseInt(index));
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).get(index.
                    replace("'", "").
                    replace("\"", ""));
        }

        return value;
    }
}
