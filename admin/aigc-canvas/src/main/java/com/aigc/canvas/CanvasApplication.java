package com.aigc.canvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aigc")
public class CanvasApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanvasApplication.class, args);
    }
}
