package com.bic.burble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bic.burble")
public class BurbleApplication {
  public static void main(String[] args) {
    SpringApplication.run(BurbleApplication.class, args);
  }
}
