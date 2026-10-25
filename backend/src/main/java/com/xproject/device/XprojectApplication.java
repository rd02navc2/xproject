package com.xproject.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XprojectApplication {
    public static void main(String[] args) { SpringApplication.run(XprojectApplication.class, args); }
}
