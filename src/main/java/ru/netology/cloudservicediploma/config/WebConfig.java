package ru.netology.cloudservicediploma.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.netology.cloudservicediploma.security.CurrentUserArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApplicationProperties applicationProperties;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(
            ApplicationProperties applicationProperties,
            CurrentUserArgumentResolver currentUserArgumentResolver
    ) {
        this.applicationProperties = applicationProperties;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(applicationProperties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
