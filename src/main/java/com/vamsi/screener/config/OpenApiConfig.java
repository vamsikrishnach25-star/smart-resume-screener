package com.vamsi.screener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI metadata, served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartResumeScreenerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Resume Screener API")
                        .version("1.0.0")
                        .description("""
                                Parses resumes (PDF/TXT), extracts structured candidate data, and ranks \
                                candidates against a job description using an LLM with a deterministic \
                                fallback scorer.""")
                        .contact(new Contact().name("Vamsi Krishna Choppara"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
