package com.minewaku.chatter.identityaccess.start.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class OpenApiE2eTest extends E2eSupport {

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/authenticate",
            "POST /api/v1/auth/verification/resend",
            "GET /api/v1/auth/verification/confirm",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout");

    private static final Set<String> PROTECTED_OPERATIONS = Set.of(
            "GET /api/v1/sessions",
            "POST /api/v1/sessions/{sessionId}/revoke",
            "POST /api/v1/sessions/revoke-others",
            "PUT /api/v1/users/@me/password",
            "DELETE /api/v1/users/@me");

    private JsonNode document() {
        ApiResponse response = api().getRoot("/v3/api-docs");
        assertThat(response.status()).as("api docs are public").isEqualTo(200);
        return response.json();
    }

    private static List<String> operations(JsonNode document) {
        List<String> operations = new ArrayList<>();
        document.get("paths")
                .properties()
                .forEach(path -> path.getValue().properties().forEach(method -> {
                    if (Set.of("get", "post", "put", "delete", "patch").contains(method.getKey())) {
                        operations.add(method.getKey().toUpperCase() + " " + path.getKey());
                    }
                }));
        return operations;
    }

    @Test
    void documentationIsPublicAndListsExactlyTheElevenOperations() {
        List<String> operations = operations(document());

        Set<String> expected = new TreeSet<>(PUBLIC_OPERATIONS);
        expected.addAll(PROTECTED_OPERATIONS);
        assertThat(operations).containsExactlyInAnyOrderElementsOf(expected).hasSize(11);
    }

    @Test
    void everyOperationDocumentsItsErrorShapeAndProtectedOnesRequireTheBearerScheme() {
        JsonNode document = document();

        document.get("paths")
                .properties()
                .forEach(path -> path.getValue().properties().forEach(method -> {
                    String name = method.getKey().toUpperCase() + " " + path.getKey();
                    JsonNode operation = method.getValue();
                    assertThat(operation.get("summary").asString())
                            .as(name + " summary")
                            .isNotBlank();
                    assertThat(operation.get("responses").toString())
                            .as(name + " documents the standard error body")
                            .contains("ApiError");
                    boolean secured = operation.has("security") || document.has("security");
                    assertThat(secured).as(name + " security").isEqualTo(PROTECTED_OPERATIONS.contains(name));
                }));
        assertThat(document.at("/components/schemas/ApiError/properties/code")).isNotNull();
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asString())
                .isEqualTo("bearer");
    }

    @Test
    void statusesMatchTheContractForRepresentativeOperations() {
        JsonNode paths = document().get("paths");

        JsonNode register = paths.at("/~1api~1v1~1auth~1register/post/responses");
        assertThat(register.propertyNames())
                .contains("201", "400", "409", "500")
                .doesNotContain("200");
        assertThat(register.at("/409/description").asString())
                .contains("account_already_exists", "verification_pending");

        JsonNode revoke = paths.at("/~1api~1v1~1sessions~1{sessionId}~1revoke/post/responses");
        assertThat(revoke.propertyNames())
                .contains("204", "401", "403", "404", "503")
                .doesNotContain("200");

        JsonNode refresh = paths.at("/~1api~1v1~1auth~1refresh/post/responses");
        assertThat(refresh.propertyNames()).contains("200", "401");
        assertThat(refresh.at("/200").toString()).contains("TokenPairResponse");
    }

    @Test
    void secretFieldsAreWriteOnlyAndTheCredentialPairIsDocumented() {
        JsonNode schemas = document().at("/components/schemas");

        assertThat(schemas.has("TokenPairResponse")).isTrue();
        assertThat(schemas.at("/TokenPairResponse/properties/refreshToken")).isNotNull();
        assertThat(schemas.at("/RegisterRequest/properties/password/writeOnly").asBoolean())
                .isTrue();
        assertThat(schemas.at("/SessionResponse/properties").toString())
                .doesNotContain("refreshToken")
                .doesNotContain("userId");
    }

    @Test
    void theInteractiveDocumentationNeedsNoCredential() {
        int status = api().getRoot("/swagger-ui.html").status();

        assertThat(status).isIn(200, 302);
    }
}
