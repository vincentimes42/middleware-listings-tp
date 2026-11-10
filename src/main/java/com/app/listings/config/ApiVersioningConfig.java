package com.app.listings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API VERSIONING configuration (native Spring mechanism).
 *
 * To do (exercise.md Step 6 - Versioning V1 + V2). The version is carried
 * by the query parameter only: ?version=V1|V2.
 *  - override configureApiVersioning(ApiVersionConfigurer): query parameter
 *    `version`, supported versions 1 and 2, default version 1 (no parameter
 *    -> V1; unsupported version -> InvalidApiVersionException, 400)
 *  - configure the V1 deprecation headers (Deprecation / Sunset) in the same
 *    method
 *  - declare the endpoints with the `version` attribute of the mapping
 *    annotations (version = "1" / "2")
 *
 * Reference: https://docs.spring.io/spring-framework/reference/7.0/web/webmvc-versioning.html
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    // TODO (Step 6): override configureApiVersioning(ApiVersionConfigurer).
}
