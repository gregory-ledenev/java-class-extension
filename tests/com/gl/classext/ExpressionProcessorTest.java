package com.gl.classext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionProcessorTest {

    private ExpressionProcessor processor;
    private RootObject rootObject;

    @BeforeEach
    void setUp() {
        processor = new ExpressionProcessor();
        rootObject = new RootObject();
        rootObject.setName("root");
        rootObject.setNumber(42);

        NestedObject nested = new NestedObject();
        nested.setProperty("nestedValue");
        rootObject.setNested(nested);

        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        rootObject.setList(list);

        List<NestedObject> nestedList = new ArrayList<>();
        NestedObject nestedInList = new NestedObject();
        nestedInList.setProperty("nestedInListValue");
        nestedList.add(nestedInList);
        rootObject.setNestedList(nestedList);

        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        rootObject.setMap(map);

        Map<String, NestedObject> nestedMap = new HashMap<>();
        NestedObject nestedInMap = new NestedObject();
        nestedInMap.setProperty("nestedInMapValue");
        nestedMap.put("nestedKey", nestedInMap);
        rootObject.setNestedMap(nestedMap);
    }

    public static class RootObject {
        private String name;
        private int number;
        private NestedObject nested;
        private NestedObject nullNested;
        private List<String> list;
        private List<NestedObject> nestedList;
        private Map<String, String> map;
        private Map<String, NestedObject> nestedMap;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public NestedObject getNested() { return nested; }
        public void setNested(NestedObject nested) { this.nested = nested; }
        public NestedObject getNullNested() { return nullNested; }
        public void setNullNested(NestedObject nullNested) { this.nullNested = nullNested; }
        public List<String> getList() { return list; }
        public void setList(List<String> list) { this.list = list; }
        public List<NestedObject> getNestedList() { return nestedList; }
        public void setNestedList(List<NestedObject> nestedList) { this.nestedList = nestedList; }
        public Map<String, String> getMap() { return map; }
        public void setMap(Map<String, String> map) { this.map = map; }
        public Map<String, NestedObject> getNestedMap() { return nestedMap; }
        public void setNestedMap(Map<String, NestedObject> nestedMap) { this.nestedMap = nestedMap; }
    }

    public static class NestedObject {
        private String property;

        public String getProperty() { return property; }
        public void setProperty(String property) { this.property = property; }
    }

    @Test
    void getExpressionValue_simpleProperty() {
        assertEquals("root", processor.getExpressionValue(rootObject, "name"));
    }

    @Test
    void getExpressionValue_primitiveProperty() {
        assertEquals(42, processor.getExpressionValue(rootObject, "number"));
    }

    @Test
    void getExpressionValue_nestedProperty() {
        assertEquals("nestedValue", processor.getExpressionValue(rootObject, "nested.property"));
    }

    @Test
    void getExpressionValue_fromList() {
        assertEquals("one", processor.getExpressionValue(rootObject, "list[0]"));
    }

    @Test
    void getExpressionValue_fromMap() {
        assertEquals("value1", processor.getExpressionValue(rootObject, "map['key1']"));
        assertEquals("value2", processor.getExpressionValue(rootObject, "map[\"key2\"]"));
    }

    @Test
    void getExpressionValue_nestedPropertyInList() {
        assertEquals("nestedInListValue", processor.getExpressionValue(rootObject, "nestedList[0].property"));
    }

    @Test
    void getExpressionValue_nestedPropertyInMap() {
        assertEquals("nestedInMapValue", processor.getExpressionValue(rootObject, "nestedMap['nestedKey'].property"));
    }

    @Test
    void getExpressionValue_nullSafe_shouldReturnNull() {
        assertNull(processor.getExpressionValue(rootObject, "nullNested?.property"));
    }

    @Test
    void getExpressionValue_nullValueWithoutNullSafe_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> processor.getExpressionValue(rootObject, "nullNested.property"));
    }

    @Test
    void getExpressionValue_whenExpressionReturnsNull() {
        rootObject.setName(null);
        assertNull(processor.getExpressionValue(rootObject, "name"));
    }

    @Test
    void setExpressionValue_simpleProperty() {
        processor.setExpressionValue(rootObject, "name", "newName");
        assertEquals("newName", rootObject.getName());
    }

    @Test
    void setExpressionValue_nestedProperty() {
        processor.setExpressionValue(rootObject, "nested.property", "newNestedValue");
        assertEquals("newNestedValue", rootObject.getNested().getProperty());
    }

    @Test
    void setExpressionValue_onNonExistentProperty_shouldThrowException() {
        assertThrows(RuntimeException.class, () -> processor.setExpressionValue(rootObject, "nonExistent", "value"));
    }

    interface OrganizationInterface {
        void setName(String name);
        String getName();

        List<Department> getDepartments();
        Map<String, Department> getDepartmentMap();
    }

    interface OrganizationInterfaceEx extends OrganizationInterface, ExpressionContext {
    }

    static final class Organization implements OrganizationInterface {
        private String name;
        private final List<Department> departments;

        Organization(String name, List<Department> departments) {
            this.name = name;
            this.departments = departments;
        }

        public List<Department> getDepartments() {
            return departments;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Map<String, Department> getDepartmentMap() {
            return departments.stream()
                    .collect(Collectors.toMap(Department::getName, d -> d));
        }

        public List<Department> departments() {
            return departments;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Organization) obj;
            return Objects.equals(this.name, that.name) &&
                    Objects.equals(this.departments, that.departments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, departments);
        }

        @Override
        public String toString() {
            return "Organization[" +
                    "name=" + name + ", " +
                    "departments=" + departments + ']';
        }

    }

    static final class Department {
        private String name;

        public boolean isValid() {
            return name != null && !name.isBlank();
        }

        public List<Employee> getEmployees() {
            return employees;
        }

        public Map<String, Employee> getEmployeeMap() {
            return employees == null ? null :
                    employees.stream()
                    .collect(Collectors.toMap(Employee::getName, e -> e));
        }

        private final List<Employee> employees;

        Department(String name, List<Employee> employees) {
            this.name = name;
            this.employees = employees;
        }


        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<Employee> employees() {
            return employees;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Department) obj;
            return Objects.equals(this.name, that.name) &&
                    Objects.equals(this.employees, that.employees);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, employees);
        }

        @Override
        public String toString() {
            return "Department[" +
                    "name=" + name + ", " +
                    "employees=" + employees + ']';
        }
    }

    static final class Employee {
        private String name;

        private int salary;

        Employee(String name, int salary) {
            this.name = name;
            this.salary = salary;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setSalary(int salary) {
            this.salary = salary;
        }

        public int getSalary() {
            return salary;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Employee) obj;
            return Objects.equals(this.name, that.name) &&
                    this.salary == that.salary;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, salary);
        }

        @Override
        public String toString() {
            return "Employee[" +
                    "name=" + name + ", " +
                    "salary=" + salary + ']';
        }
    }

    Organization setupOrganization() {
        List<Employee> itEmployees = new ArrayList<>(Arrays.asList(
                new Employee("John", 100000),
                new Employee("Alice", 95000),
                new Employee("Bob", 90000)
        ));
        Department itDepartment = new Department("com.IT", itEmployees);

        List<Employee> hrEmployees = Arrays.asList(
                new Employee("Carol", 85000),
                new Employee("David", 82000)
        );
        Department hrDepartment = new Department("HR", hrEmployees);

        List<Employee> salesEmployees = Arrays.asList(
                new Employee("Eve", 120000),
                new Employee("Frank", 110000),
                new Employee("Grace", 105000)
        );
        Department salesDepartment = new Department("Sales", salesEmployees);

        return new Organization("Acme", new ArrayList<>(Arrays.asList(itDepartment, hrDepartment, salesDepartment)));
    }

    @Test
    void testOrganizationGetValues() {
        Organization organization = setupOrganization();

        assertEquals("Acme", processor.getExpressionValue(organization, "name"));
        assertEquals(organization.departments.get(0), processor.getExpressionValue(organization, "departments[0]"));
        assertEquals("com.IT", processor.getExpressionValue(organization, "departments[0].name"));
        assertEquals("John", processor.getExpressionValue(organization, "departments[0].employees[0].name"));
        assertEquals(3, ((Integer) processor.getExpressionValue(organization, "departments[0].employees.size")));
        assertEquals(105000, ((Integer) processor.getExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"].salary")));
        assertEquals(105000, ((Integer) processor.getExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"].salary")));

        assertEquals(true, processor.getExpressionValue(organization, "departments[0].valid"));
        assertEquals(false, processor.getExpressionValue(new Organization("", new ArrayList<>(List.of(new Department(null, null)))),
                "departments[0].valid"));
    }

    @Test
    void testOrganizationGetValuesNullable() {
        Organization organization = setupOrganization();

        NullPointerException npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(organization, "departmentMap[\"\"].name"));
        assertEquals("Null value at: departmentMap[\"\"]", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(organization, "departments[0].employeeMap[\"\"].name"));
        assertEquals("Null value at: departments[0].employeeMap[\"\"]", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(new Organization("Acme", null), "departments[0].employeeMap[\"\"].name"));
        assertEquals("Null value at: departments[0]", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(new Organization("Acme", List.of(new Department(null, null))),
                "departments[0].employees[0].name"));
        assertEquals("Null value at: departments[0].employees[0]", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(new Organization("Acme", new ArrayList<>(List.of(new Department(null, null)))),
                "departments[0].employees.size"));
        assertEquals("Null value at: departments[0].employees", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(new Organization("Acme", new ArrayList<>(List.of(new Department(null, null)))),
                "departments[0].employeeMap[''].name"));
        assertEquals("Null value at: departments[0].employeeMap['']", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.getExpressionValue(null,
                "departments[0].employeeMap[''].name"));
        assertEquals("Null value at: object", npe.getMessage());
    }

    @Test
    void testOrganizationGetValuesSafeNullable() {
        Organization organization = setupOrganization();

        assertNull(processor.getExpressionValue(organization, "departmentMap[\"\"]?.name"));
        assertNull(processor.getExpressionValue(organization, "departments[0]?.employeeMap[\"\"]?.name"));

        assertNull(processor.getExpressionValue(new Organization(null, null), "name?"));
        assertNull(processor.getExpressionValue(new Organization(null, null), "name"));

        assertNull(processor.getExpressionValue(new Organization("Acme", List.of(new Department(null, null))),
                "departments[0]?.employees"));
        assertNull(processor.getExpressionValue(new Organization("Acme", new ArrayList<>(List.of(new Department(null, null)))),
                "departments[0]?.employees?.size"));
    }

    @Test
    void testOrganizationSetValues() {
        Organization organization = setupOrganization();

        processor.setExpressionValue(organization, "name", "Acme123");
        assertEquals("Acme123", processor.getExpressionValue(organization, "name"));

        processor.setExpressionValue(organization, "departments[0].name", "IT123");
        assertEquals("IT123", processor.getExpressionValue(organization, "departments[0].name"));

        processor.setExpressionValue(organization, "departments[0].employees[0].name", "John123");
        assertEquals("John123", processor.getExpressionValue(organization, "departments[0].employees[0].name"));

        processor.setExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"].salary", 1050000);
        assertEquals(1050000, ((Integer) processor.getExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"].salary")));

        processor.setExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"]",
                new Employee("Grace123", 1050000));
    }

    @Test
    void testOrganizationSetValuesNullable() {
        NullPointerException npe;

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.setExpressionValue(
                new Organization("Acme", null),
                "departments[0]", new Department("com.IT", null)));
        assertEquals("Null value at: departments", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.setExpressionValue(new Organization("Acme", List.of(new Department("IT", null))),
                "departments[0].employees[0]", new Employee("John123", 1050000)));
        assertEquals("Null value at: departments[0].employees", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.setExpressionValue(null,
                "departments[0].employees[0]", new Employee("John123", 1050000)));
        assertEquals("Null value at: object", npe.getMessage());
    }

    @Test
    void testOrganizationSetValuesSafeNullable() {
        processor.setExpressionValue(
                new Organization("Acme", null),
                "departments[0]?", new Department("com.IT", null));

        processor.setExpressionValue(new Organization("Acme", List.of(new Department("com.IT", null))),
                "departments[0]?.employees[0]?", new Employee("John123", 1050000));
    }

    @Test
    void testOrganizationSetNullValues() {
        Organization organization = setupOrganization();

        processor.setExpressionValue(organization, "name", null);
        assertNull(processor.getExpressionValue(organization, "name"));

        processor.setExpressionValue(organization, "departments[0].name", null);
        assertNull(processor.getExpressionValue(organization, "departments[0].name"));

        processor.setExpressionValue(organization, "departments[0].employees[0].name", null);
        assertNull(processor.getExpressionValue(organization, "departments[0].employees[0].name"));

        assertThrows(RuntimeException.class, () -> processor.setExpressionValue(organization, "departmentMap[\"Sales\"].employeeMap[\"Grace\"].salary", null));
    }

    @Test
    void testOrganizationSetNulValuesNullable() {
        Organization organization = setupOrganization();
        NullPointerException npe;

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.setExpressionValue(organization, "departmentMap[\"\"].name", null));
        assertEquals("Null value at: departmentMap[\"\"]", npe.getMessage());

        npe = assertThrowsExactly(NullPointerException.class, () -> processor.setExpressionValue(organization, "departments[0].employeeMap[\"\"].name", null));
        assertEquals("Null value at: departments[0].employeeMap[\"\"]", npe.getMessage());
    }

    @Test
    void testOrganizationSetNulValuesSafeNullable() {
        Organization organization = setupOrganization();

        processor.setExpressionValue(organization, "departmentMap[\"\"]?.name", null);
        processor.setExpressionValue(organization, "departments[0]?.employeeMap[\"\"]?.name", null);
    }

    @Test
    void testOrganizationDepartmentOperations() {
        Organization organization = setupOrganization();

        Department department = new Department("Finance", null);
        processor.setExpressionValue(organization, "departments[+]?", department);
        assertEquals("Finance", processor.getExpressionValue(organization, "departments[3]?.name"));

        processor.setExpressionValue(organization, "departments[-]?", department);
        assertEquals(3, ((Integer) processor.getExpressionValue(organization, "departments.size")));

        processor.setExpressionValue(organization, "departments[0]?.employeeMap[\"\"]?.name", null);
    }

    @Test
    void testExtensionWithExpressionContext() {
        Organization organization = setupOrganization();

        // get an extension to work with expression for any object
        ExpressionContext expressionContext = DynamicClassExtension.sharedExtension(organization, ExpressionContext.class);
        // get the name of a first employee from a first department
        assertEquals("John", expressionContext.getExpressionValue("departments[0].employees[0].name"));

        // get an extension to work with composition of interfaces
        OrganizationInterface extension1 = DynamicClassExtension.sharedInstance().extension(organization,
                OrganizationInterface.class, ExpressionContext.class);
        // get the name of a first employee from a first department
        assertEquals("John", ((ExpressionContext) extension1).getExpressionValue("departments[0].employees[0].name"));
        // use it as an organization
        assertEquals("Acme", extension1.getName());

        // get an extension to work with any interface + expression support for any object
        OrganizationInterfaceEx extension2 = DynamicClassExtension.sharedExtension(organization, OrganizationInterfaceEx.class);

        // get the name of a second employee from a third department
        assertEquals("Frank", extension2.getExpressionValue("departments[2].employees[1].name"));
        // get the name of a first employee from the "Sales" department
        assertEquals("Eve", extension2.getExpressionValue("departmentMap['Sales'].employees[0].name"));

        // handle nullability, suppressing NPE and providing default values
        assertEquals("N/A", extension2.getExpressionValue("departments[2].employeeMap['Doe']?.name",
                "N/A"));
        assertEquals("N/A", extension2.getExpressionValue("departments[2].employeeMap['Doe'].name",
                "N/A", true));

        // update the name of an employee
        extension2.setExpressionValue("departments[2].employees[1].name", "Frank Jr");
        assertEquals("Frank Jr", extension2.getExpressionValue("departments[2].employees[1].name"));
    }
}
