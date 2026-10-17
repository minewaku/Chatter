package com.minewaku.chatter.identityaccess.presentation.shared.openapi;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiError;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ErrorCode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API documentation: title, the bearer scheme, the shared error schema, and per-endpoint error responses. */
@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI identityAccessOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity Access API")
                        .version("v1")
                        .description("Registration, email verification, sign-in, session renewal and management."))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer errorSchema() {
        return openApi -> ModelConverters.getInstance()
                .read(ApiError.class)
                .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
    }

    @Bean
    OperationCustomizer documentedErrors() {
        return (operation, handlerMethod) -> {
            DocumentedErrors documented = handlerMethod.getMethodAnnotation(DocumentedErrors.class);
            if (documented == null) {
                return operation;
            }
            Set<ErrorCode> codes = EnumSet.of(ErrorCode.INTERNAL_ERROR);
            codes.addAll(List.of(documented.value()));
            Map<Integer, StringBuilder> byStatus = new LinkedHashMap<>();
            for (ErrorCode code : codes) {
                byStatus.computeIfAbsent(code.status().value(), status -> new StringBuilder())
                        .append(byStatus.get(code.status().value()).isEmpty() ? "" : ", ")
                        .append('`')
                        .append(code.code())
                        .append('`');
            }
            byStatus.forEach((status, description) -> operation
                    .getResponses()
                    .addApiResponse(
                            Integer.toString(status),
                            new ApiResponse()
                                    .description("Error " + description)
                                    .content(new Content()
                                            .addMediaType(
                                                    "application/json",
                                                    new MediaType()
                                                            .schema(new Schema<>()
                                                                    .$ref("#/components/schemas/ApiError"))))));
            return operation;
        };
    }
}
