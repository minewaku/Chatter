package com.minewaku.chatter.apigateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitPolicy {

    private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

    @NotBlank(message = "pathPattern not be blank")
    private String pathPattern;

    private PathPattern compiledPattern;

    @NotNull
    @Min(value = 1, message = "the initialTokens must be at least 1")
    private Integer initialTokens = 10;

    @NotNull
    @Min(value = 1, message = "the capacity must be at least 1")
    private Integer capacity = 10;

    @NotNull
    @Min(value = 1, message = "the refilltokens must be at least 1")
    private Integer refilltokens = 10;

    @NotNull(message = "the period not be null")
    private Duration period = Duration.ofMinutes(1);

    @NotNull
    private Boolean isGreedyRefill = true;

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
        if (pathPattern != null) {
            this.compiledPattern = PARSER.parse(pathPattern);
        }
    }

    public boolean pathMatches(String requestPath) {
        if (requestPath == null || this.compiledPattern == null) {
            return false;
        }
        return this.compiledPattern.matches(PathContainer.parsePath(requestPath));
    }
}