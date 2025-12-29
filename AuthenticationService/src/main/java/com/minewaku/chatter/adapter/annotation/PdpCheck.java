package com.minewaku.chatter.adapter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PdpCheck {
    // resource kind: e.g., "document", "user"
    String resourceType(); 

    // The unique identifier of the resource instance (e.g., database primary key)
    // Required by Cerbos in every authorization request, even if the policy doesn't use it
    // The official resource checking API only requires resource Ids for batch calling
    String resourceIdParam() default "";
    String action();
    Attribute[] resourceAttrs() default {};
}
