package com.ledgercore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI configuration for Swagger UI.
 * Configures API metadata and JWT Bearer authentication scheme.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LedgerCore Digital Banking API")
                        .version("1.0")
                        .description("""
                                Production-quality Digital Banking REST API featuring:
                                - JWT-based authentication
                                - Multi-account management (Current & Savings)
                                - Secure deposits, withdrawals, and transfers
                                - Transaction history with category-based spending analysis
                                - Budget management with threshold alerts
                                - Rate limiting and idempotency protection
                                """)
                        .contact(new Contact()
                                .name("LedgerCore Team")
                                .email("support@ledgercore.dev"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token")));
    }
}
