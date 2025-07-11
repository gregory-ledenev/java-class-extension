package com.gl.classext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpressionProcessor {
    public Object getExpressionValue(Object object, String expression) {
        Object current = object;
        String[] expressionParts = splitExpression(expression);

        for (int i = 0; i < expressionParts.length; i++) {
            try {
                current = getPropertyValue(current, expressionParts[i],
                        i > 0 && expressionParts[i - 1].endsWith(NULL_SAFE_OPERATOR),
                        i == expressionParts.length - 1);
            } catch (NullPropertyValue e) {
                npeAtSubExpression(expressionParts, i > 0 ? i - 1 : i, false);
            }
        }

        return current;
    }

    public void setExpressionValue(Object object, String expression, Object value) {
        Object current = object;
        String[] expressionParts = splitExpression(expression);

        for (int i = 0; i < expressionParts.length - 1; i++) {
            boolean isNullSafe = expressionParts[i].endsWith(NULL_SAFE_OPERATOR);
            try {
                current = getPropertyValue(current, expressionParts[i], isNullSafe, i == expressionParts.length - 1);
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
            current = getPropertyValue(current, info.property(), isNullSafe, true);
            if (current != null) {
                setValueForProperty(current, lastExpressionPart, value);
            } else if (! isNullSafe) {
                npeAtSubExpression(expressionParts, expressionParts.length-1, true);
            }
        } else {
            setValueForProperty(current, lastExpressionPart, value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setValueForProperty(Object object, String expression, Object value) {
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
            }
        } else {
            try {
                String setter = "set" + info.name();
                Method method;
                if (value != null) {
                    method = object.getClass().getMethod(setter, getValueClassForSetter(value));
                } else {
                    method = findAnyMethodByName(object.getClass(), setter);
                }

                method.invoke(object, value);
            } catch (Exception e) {
                throw new RuntimeException("Error setting value at: " + expression, e);
            }
        }
    }

    private boolean isSubscript(String expression) {
        return expression.endsWith(SUBSCRIPT_END) || expression.endsWith(SUBSCRIPT_END_OPTIONAL);
    }

    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();

    private Method findAnyMethodByName(Class<?> clazz, String methodName) {
        String cacheKey = clazz.getName() + "#" + methodName;
        return methodCache.computeIfAbsent(cacheKey, k -> {
            List<Method> methods = Arrays.stream(clazz.getMethods())
                    .filter(method -> method.getName().equals(methodName) &&
                            method.getParameterCount() == 1 &&
                            !method.getParameterTypes()[0].isPrimitive())
                    .toList();

            if (methods.size() > 1) {
                throw new IllegalStateException("Multiple matching methods found for: " + methodName);
            }

            return methods.isEmpty() ? null : methods.get(0);
        });
    }

    private static Class<?> getValueClassForSetter(Object value) {
        return switch (value) {
            case Byte _ -> byte.class;
            case Short _ -> short.class;
            case Integer _ -> int.class;
            case Long _ -> long.class;
            case Float _ -> float.class;
            case Double _ -> double.class;
            case Character _ -> char.class;
            case Boolean _ -> boolean.class;
            default -> value.getClass();
        };
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

    private Object getPropertyValue(Object current, String part, boolean isNullSafe, boolean isLastPart) throws NullPropertyValue {
        if (current == null) {
            if (isNullSafe)
                return null;
            throw new NullPropertyValue();
        }

        PropertyInfo info = parseProperty(part);
        Object value = null;
        boolean noGetter = false;
        try {
            // try to use getter
            String getter = "get" + info.name();
            value = current.getClass().getMethod(getter).invoke(current);
        } catch (Exception e) {
            noGetter = true;
        }

        // try to use fallback methods such as List.size()
        if (noGetter) {
            try {
                value = current.getClass().getMethod(info.property()).invoke(current);
            } catch (Exception e) {
                throw new RuntimeException("Error evaluating: " + part, e);
            }
        }

        return getSubscriptValue(value, info.index());
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
