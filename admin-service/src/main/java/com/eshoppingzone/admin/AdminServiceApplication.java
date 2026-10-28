package com.eshoppingzone.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
        System.out.println("=================================================");
        System.out.println(">>> ShopNexa Admin Service Running on Port 8089 <<<");
        System.out.println(">>> Dashboard: http://localhost:8089/          <<<");
        System.out.println("=================================================");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
