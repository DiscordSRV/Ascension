package com.discordsrv.api.placeholder.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a prefix for all placeholders declared in this type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PlaceholderPrefix {

    /**
     * The prefix for all placeholders in this type
     * @return the prefix
     */
    String value();

    /**
     * If this prefix should not follow {@link PlaceholderPrefix} of classes that are using this class/interface as a superclass.
     * @return {@code true} to not allow overwriting this prefix
     */
    boolean ignoreParents() default false;

}
