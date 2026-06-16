package com.example.jobofferservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class JobOfferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobOfferServiceApplication.class, args);
    }
}
