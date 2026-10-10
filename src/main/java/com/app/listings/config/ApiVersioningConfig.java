package com.app.listings.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API VERSIONING configuration (native Spring mechanism).
 *
 * To do (exercise.md Step 6 - Versioning V1 + V2):
 *  - define the versioning strategies via WebMvcConfigurer:
 *      * header  -> X-API-Version  (versionRequestHeader)
 *      * query   -> ?version=      (versionRequestParameter)
 *  - declare the versioned controllers with @RequestMapping(version = "V1"/"V2")
 *  - implement the FALLBACK (exercise.md Step 6, requirements 6.2 to 6.4):
 *      * no version given   -> V1 by default + header X-API-Version-Default
 *      * unknown version    -> 400
 *      * header/param clash -> 400
 *
 * For the RequestMappingHandlerMapping bean and its versioning strategy,
 * see the Spring Framework documentation (class
 * org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping).
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    // TODO: implement configureMessageConverters / addArgumentResolvers
    //       according to the chosen versioning strategies (header + param).
}
