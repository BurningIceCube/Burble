package com.bic.burble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bic.burble")
public class BurbleApplication {

  private static final Logger log = LoggerFactory.getLogger(BurbleApplication.class);

  static void main(String[] args) {
    log.info("Starting Burble application...");
    SpringApplication.run(BurbleApplication.class, args);
    log.info("Burble application started.");
  }
}
