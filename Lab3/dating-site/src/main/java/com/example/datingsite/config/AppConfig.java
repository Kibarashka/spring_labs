
package com.example.datingsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AppConfig {

    @Bean
    public String applicationName() {
        return "Сайт знайомств";
    }

    @Bean
    @Scope("prototype")
    public StringBuilder searchHistory() {
        return new StringBuilder();
    }
}
