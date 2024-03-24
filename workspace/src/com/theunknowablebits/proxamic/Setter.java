package com.theunknowablebits.proxamic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates that this is a setter method.
 * <br/>
 * Setter methods follow one of 2 forms:
 * <pre>
 *     * form 1: Fluent Setter. Method name must start with "with" and will accept a new value and return the container
 *     * form 2: Standard setter, Method name must not start with "with" and will accept a new value and return the old onw
 * </pre>
 * Some types (notably, maps) do not have setters as they are implemented completely dynamically.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Setter {

	/** The name of the key at which the value is stored in the container */
	public String value();

	
}
