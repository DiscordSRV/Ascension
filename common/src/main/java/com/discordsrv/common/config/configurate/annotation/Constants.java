package com.discordsrv.common.config.configurate.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A config annotation that should be used to define config (comment) parts that should not be translated,
 * remaining the same for all languages (for example, config option names referenced in comments, urls, etc.).
 * <p>
 * Replacements are {@code %i} where {@code i} start counting up from {@code 1}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Constants {

    String[] value();

    /**
     * Needs to go after {@link org.spongepowered.configurate.objectmapping.meta.Comment}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Comment {

        String[] value();
    }
}
