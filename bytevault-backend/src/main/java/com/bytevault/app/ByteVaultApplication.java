package com.bytevault.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ByteVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(ByteVaultApplication.class, args);
    }
} 