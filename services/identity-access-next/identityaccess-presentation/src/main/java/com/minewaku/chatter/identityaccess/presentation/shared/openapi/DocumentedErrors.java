package com.minewaku.chatter.identityaccess.presentation.shared.openapi;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lists the error codes an endpoint can return so the generated documentation shows the standard error body for
 * each status. {@link ErrorCode#INTERNAL_ERROR} is always added.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DocumentedErrors {

    ErrorCode[] value();
}
