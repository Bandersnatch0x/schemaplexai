package com.schemaplexai.system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for controller methods or classes that require a specific permission code.
 * <p>
 * Usage:
 * <pre>{@code
 * @RequirePermission("user:read")
 * @GetMapping("/users")
 * public Result<List<UserVO>> listUsers() { ... }
 * }</pre>
 * <p>
 * When applied at the class level, the permission is required for all methods
 * in the class unless overridden by a method-level annotation.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** The permission code required to access the annotated method or class. */
    String value();
}
