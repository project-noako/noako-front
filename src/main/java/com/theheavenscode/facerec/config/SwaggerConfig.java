package com.theheavenscode.facerec.config;
import com.google.common.base.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(postPaths())
                .build();
    }

    private Predicate<String> postPaths() {
        return or(regex("/api.*"));
    }


    private final ApiInfo apiInfo() {

        return new ApiInfoBuilder().title("FaceRec  Swagger")
                .termsOfServiceUrl("http://theheavenscode.com").contact("viroj@theheavenscode.com")
                .license("Heaven'sCode License")
                .contact(new Contact("Heaven'sCode", "http://theheavenscode.com", "viroj@theheavenscode.com"))
                .licenseUrl("viroj@theheavenscode.com").version("1.0")

                .build();
    }
}