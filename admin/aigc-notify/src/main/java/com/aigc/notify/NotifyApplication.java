package com.aigc.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aigc")
public class NotifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}
