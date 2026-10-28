package com.eshoppingzone.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class DeliveryPartnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryPartnerApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer forwardToIndex() {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void addViewControllers(org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
                registry.addViewController("/login").setViewName("forward:/index.html");
                registry.addViewController("/portal").setViewName("forward:/index.html");
                registry.addViewController("/dashboard").setViewName("forward:/index.html");
            }
        };
    }
}