
package com.example.datingsite;

import com.example.datingsite.demo.BeanDemo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DatingSiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatingSiteApplication.class, args);
    }

    @Bean
    public CommandLineRunner runDemo(BeanDemo beanDemo) {
        return args -> beanDemo.demonstrate();
    }
}
