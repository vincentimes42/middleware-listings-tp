package com.app.listings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API VERSIONING configuration (native Spring mechanism).
 *
 * To do (exercise.md Step 6a - Versioning: routing, default, deprecation):
 *  - override configureApiVersioning(ApiVersionConfigurer):
 *      * the version is read from the query parameter ?version= (only)
 *      * supported versions V1, V2; default V1 (rules 6.1, 6.2)
 *  - declare the versioned mappings with version = "V1" / "V2"
 *  - an unknown version raises the framework's InvalidApiVersionException:
 *    map it to 400 in GlobalExceptionHandler (rule 6.3)
 *
 * Reference: Spring Framework 7, "Web MVC - API Versioning".
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    // TODO (Step 6a): override configureApiVersioning(ApiVersionConfigurer).
}
