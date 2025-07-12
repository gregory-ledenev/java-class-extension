## Java Class Extension Library - Expressions

The Java Class Extension Library provides built-in extensions to allow dynamically accessing and modifying object
properties using string-based expressions. It allows for complex data manipulations on an object graph without writing
verbose boilerplate code. For example, it is possible to use `"departments[2].employees[1].name"` expression to get or
change an employee name.

### Expression Format

Expression format supports:

* **Simple Properties:** Access a direct property of the object (e.g., `"name"`).
* **Nested Properties:** Navigate through nested objects using dot notation (e.g., `"employee.name"`).
* **List/Array Access:** Retrieve an element from a list by its index (e.g., `"departments[2]"`).
* **Limited List Operations:** Add an element to a list: `"items[+]"` and remove an element from a list `"items[-]"`
* **Map Access:** Retrieve a value from a map by its key (e.g., `"departments['IT']"`).
* **Null-Safe Navigation:** Safely traverse potentially null properties using the `?.` operator. If any part of the
  chain before the `?.` is null, the expression evaluates to null instead of throwing an exception (e.g.,
  `"employee?.name"`).

### Benefits of Using Expressions

Using expressions to get or update data offers several advantages over traditional programmatic access:

* **Flexibility and Dynamic Access:** Expressions can be created and manipulated at runtime. This is ideal for building
  dynamic systems like rule engines, template solutions, data mapping utilities, or user interfaces where the data
  fields to be accessed are not known at compile time.
* **Reduced Boilerplate and Improved Readability:** It replaces long chains of getter/setter calls and manual null
  checks with a single, concise string. For example, instead of
  `if (getDepartments()[2] != null && getDepartments()[2].getEmployees()[1] != null) return getDepartments()[2].getEmployees()[1].name`,
  you can simply use
  `"departments[2]?.employees[1]?.name"` to get an employee name.
* **Built-in Null Safety:** The null-safe operator (`?.`) simplifies handling of potentially null objects within a
  navigation path, preventing `NullPointerException`s and making the code cleaner and more robust.
* **Decoupling:** The component generating the expression does not need to be tightly coupled to the specific data model
  it operates on. This allows for better separation of concerns, as the logic for data access is abstracted away into a
  string.

### Usage via Class Extension

There is `ExpressionContext` extension interface that provides two methods to work with expressions:

* `getExpressionValue(String expression)` - retrieves a value from the context object based on the provided expression.
* `setExpressionValue(String expression, Object value)` - sets a property on the context object at the location
  specified by the expression.

An object that implements the `ExpressionContext` interface can be queried or updated using expressions that navigate
through its properties, including nested objects, lists, and maps.

You can leverage `ExpressionContext` without directly implementing it in your data classes. By using a dynamic class
extension mechanism, you can "attach" `ExpressionContext` functionality to any object at runtime.

This approach is powerful because it allows you to work with expression-based access on existing classes, including
third-party ones, without modifying their original source code.

There are two primary ways to do this:

1. **Directly obtain an `ExpressionContext` extension:**
   You can get a pure `ExpressionContext` extension for any object. This is useful when you only need expression
   evaluation capabilities.

```java
Organization organization = new Organization(/*...*/);
ExpressionContext context = DynamicClassExtension.sharedExtension(organization, ExpressionContext.class);
String employeeName = (String) context.getExpressionValue("departments[0].employees[0].name");
```

2. **Combine with a business interface:**
   For a more integrated approach, you can create a new interface that extends both your custom business interface and
   `ExpressionContext`. This combines the standard methods of your object with the power of expression evaluation.

   First, define an interface that inherits from both:

```java
interface OrganizationInterfaceEx extends OrganizationInterface, ExpressionContext {
}
```

Then, get an extension for this combined interface. You can now use methods from both `OrganizationInterface` and
`ExpressionContext` on the same object.

```java
OrganizationInterfaceEx extension = DynamicClassExtension.sharedExtension(organization, OrganizationInterfaceEx.class);

// Call a method from ExpressionContext extension.
String employeeName = (String) context.getExpressionValue("departments[0].employees[0].name");

setExpressionValue("departments[2].employees[1].name","Frank Jr");

// Call a method from OrganizationInterface
String orgName = extension.getName();
```

This technique promotes clean design by keeping data objects separate from the logic that operates on them, while still
providing a flexible and powerful way to interact with the object graph.

### Known Issues, Peculiarities, and Limitations

1. Setting the `null` value for an expression whose last property has overloaded setters may produce unexpected results.
   For this reason, an exception will be thrown to prevent such situations.
2. Although both `is` and `get` accessor methods are supported for `boolean` properties, it is recommended to always use
   `get` accessors for better performance and faster processing.
3. Utility methods like `List.of(...)` may return objects of private internal classes whose properties could be
   inaccessible due to security restrictions. To resolve this issue, wrap such objects in well-known public classes
   like `ArrayList`.
4. Index out-of-bounds access for arrays or lists will throw an exception rather than returning a default value like
   `null`

Next >> [Utilities](utilities.md)