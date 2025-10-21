package com.example.sanjiai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("SanjiAI API")
                        .description("AI 기반 탄소 절감량 계산 마이크로서비스 API 문서")
                        .version("v1.0.0")
                        .license(new License().name("MIT License")));
    }
}
