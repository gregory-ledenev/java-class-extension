package com.gl.classext;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IsolationAroundAdviceTest {

    interface User {
        String userName();
        String firstName();
        String password();
        String uid();

        void setUserName(String userName);
        void setFirstName(String firstName);
        void setPassword(String password);
    }

    static final class UserImpl implements User, Cloneable {
        private final String uid;
        private String userName;
        private String firstName;
        private String password;

        UserImpl(String uid, String userName, String firstName, String password) {
            this.uid = uid;
            this.userName = userName;
            this.firstName = firstName;
            this.password = password;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String uid() {
            return uid;
        }

        @Override
        public String userName() {
            return userName;
        }

        @Override
        public String firstName() {
            return firstName;
        }

        @Override
        public String password() {
            return password;
        }

        @Override
        public void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        @Override
        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (UserImpl) obj;
            return Objects.equals(this.uid, that.uid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uid);
        }

        @Override
        public String toString() {
            return "UserImpl[" +
                    "uid=" + uid + ", " +
                    "userName=" + userName + ", " +
                    "firstName=" + firstName + ", " +
                    "password=" + password + ']';
        }

    }

    interface UserService {
        User createUser(String userName, String firstName, String password);
        User createUser(String userName, String password);

        User updateUser(User user);

        boolean deleteUser(User user);

        User findUserByUserName(String userName);
        User findUserByUID(String uuid);
        int getUserCount();
    }

    static class UserServiceImpl implements UserService {
        private final List<User> users = new ArrayList<>();

        public UserServiceImpl() {
            users.addAll(List.of(
                    new UserImpl("1", "alice", "Alice", "password1"),
                    new UserImpl("2", "bob", "Bob", "password2"),
                    new UserImpl("3", "charlie", "Charlie", "password3")
            ));
        }

        @Override
        public User createUser(String userName, String firstName, String password) {
            User user = new UserImpl(UUID.randomUUID().toString(), userName, firstName, password);
            users.add(user);
            return user;
        }

        @Override
        public User createUser(String userName, String password) {
            return createUser(userName, null, password);
        }

        @Override
        public User updateUser(User user) {
            return users.stream()
                    .filter(existingUser -> existingUser.uid().equals(user.uid()))
                    .findFirst()
                    .map(existingUser -> {
                        existingUser.setUserName(user.userName());
                        existingUser.setFirstName(user.firstName());
                        existingUser.setPassword(user.password());

                        return existingUser;
                    })
                    .orElse(null);
        }

        @Override
        public boolean deleteUser(User user) {
            return users.removeIf(existingUser -> existingUser.uid().equals(user.uid()));
        }

        @Override
        public User findUserByUserName(String userName) {
            return users.stream()
                    .filter(user -> user.userName().equals(userName))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findUserByUID(String uuid) {
            return users.stream()
                    .filter(user -> user.uid().equals(uuid))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int getUserCount() {
            return users.size();
        }
    }

    @Test
    void updateUserTest() {

        UserServiceImpl userServiceImpl = new UserServiceImpl();
        User user = userServiceImpl.findUserByUID("1");
        user.setPassword("new_password");

        assertEquals("new_password", userServiceImpl.findUserByUID("1").password());

        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                extensionInterface(UserService.class).objectClass("*").
                operation("*").
                    around(new Aspects.DeepCloneIsolationAroundAdvice()).
                build();
        UserService userService = dynamicClassExtension.extension(userServiceImpl, UserService.class);
        user = userService.findUserByUID("1");

        user.setPassword("new_password");
        assertNotEquals("new_password", userService.findUserByUID("1").password());

        userService.updateUser(user);
        assertEquals("new_password", userService.findUserByUID("1").password());

    }

    @Test
    void userServiceWithDeepCloneTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                extensionInterface(UserService.class).
                objectClass("*").
                operation("*").
                    around(new Aspects.DeepCloneIsolationAroundAdvice()).
                build();
        UserServiceImpl userServiceImpl = new UserServiceImpl();
        UserService userService = dynamicClassExtension.extension(userServiceImpl, UserService.class);

        User user = userService.createUser("john_doe", "John", "password123");
        System.out.println("Created user:" + user);
        System.out.println("User count: " + userService.getUserCount());

        assertNotSame(user, userService.findUserByUID(user.uid()));

        user.setFirstName("Johnathan");
        user.setPassword("123");
        User updatedUser = userService.updateUser(user);
        System.out.println("Updated user:" + updatedUser);

        assertNotSame(updatedUser, userService.findUserByUID(user.uid()), "User should not be the same instance after isolation");

        System.out.println(userService.deleteUser(updatedUser) ? "User deleted successfully" : "Failed to delete user");
        System.out.println("User count: " + userService.getUserCount());
    }

    @Test
    void userServiceWithDeepCloneExclusionTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                extensionInterface(UserService.class).
                objectClass("*").
                operation("! findUserByUserName(*)").
                around(new Aspects.DeepCloneIsolationAroundAdvice()).
                build();
        UserServiceImpl userServiceImpl = new UserServiceImpl();
        UserService userService = dynamicClassExtension.extension(userServiceImpl, UserService.class);

        User user = userService.createUser("john_doe", "John", "password123");
        System.out.println("Created user:" + user);
        System.out.println("User count: " + userService.getUserCount());

        assertNotSame(user, userService.findUserByUID(user.uid()), "User should not be the same instance after isolation.");
        assertNotSame(userService.findUserByUID(user.uid()), userService.findUserByUID(user.uid()), "User should not be the same instance after isolation.");
        assertSame(userService.findUserByUserName(user.userName()), userService.findUserByUserName(user.userName()), "User should not be the same instance after isolation.");
    }

    record Department(String name)  {}
    interface DepartmentService {
        List<Department> getAllDepartments();
    }

    class DepartmentServiceImpl implements DepartmentService {
        private final List<Department> departments = new ArrayList<>();

        public DepartmentServiceImpl() {
            departments.add(new Department("HR"));
            departments.add(new Department("Engineering"));
            departments.add(new Department("Marketing"));
        }

        @Override
        public List<Department> getAllDepartments() {
            return new ArrayList<>(departments);
        }
    }

    @Test
    void deepCloneDepartmentsWithNoClonerTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                extensionInterface(DepartmentService.class).
                objectClass("*").
                operation("*").
                around(new Aspects.DeepCloneIsolationAroundAdvice()).
                build();
        List<Department> departments = List.of(
                new Department("HR"),
                new Department("Engineering"),
                new Department("Marketing")
        );

        DepartmentServiceImpl departmentServiceImpl = new DepartmentServiceImpl();
        DepartmentService departmentService = dynamicClassExtension.extension(departmentServiceImpl, DepartmentService.class);
        assertThrows(UnsupportedOperationException.class, departmentService::getAllDepartments);
    }

    @Test
    void deepCloneDepartmentsWithClonerTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                extensionInterface(DepartmentService.class).
                objectClass("*").
                operation("*").
                around(new Aspects.DeepCloneIsolationAroundAdvice(o -> o instanceof Department d ? new Department(d.name()) : null)).
                build();
        List<Department> departments = List.of(
                new Department("HR"),
                new Department("Engineering"),
                new Department("Marketing")
        );

        DepartmentServiceImpl departmentServiceImpl = new DepartmentServiceImpl();
        DepartmentService departmentService = dynamicClassExtension.extension(departmentServiceImpl, DepartmentService.class);
        List<Department> clonedDepartments = departmentService.getAllDepartments();
        assertNotSame(departments, clonedDepartments);
        assertEquals(departments, clonedDepartments);
    }
}
