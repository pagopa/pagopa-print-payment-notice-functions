package it.gov.pagopa.print.payment.notice.functions.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${info.application.name}") String appName,
            @Value("${info.application.description}") String appDescription,
            @Value("${info.application.version}") String appVersion) {
        return new OpenAPI()
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "ApiKey",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.APIKEY)
                                                .description("The API key to access this function app.")
                                                .name("Ocp-Apim-Subscription-Key")
                                                .in(SecurityScheme.In.HEADER)))
                .info(
                        new Info()
                                .title(appName)
                                .version(appVersion)
                                .description(appDescription)
                                .termsOfService("https://www.pagopa.gov.it/"));
    }

    @Bean
    public GlobalOpenApiCustomizer sortOperationsAlphabetically() {
        return openApi -> {
            Paths paths = openApi.getPaths()
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Paths::new,
                            (map, item) -> map.addPathItem(item.getKey(), item.getValue()), Paths::putAll);

            paths.forEach((key, value) -> value.readOperations()
                    .forEach(
                            operation -> {
                                var responses = operation.getResponses()
                                        .entrySet()
                                        .stream()
                                        .sorted(Map.Entry.comparingByKey())
                                        .collect(
                                                ApiResponses::new,
                                                (map, item) ->
                                                        map.addApiResponse(item.getKey(), item.getValue()),
                                                ApiResponses::putAll);
                                operation.setResponses(responses);
                            }));
            openApi.setPaths(paths);
        };
    }

}
