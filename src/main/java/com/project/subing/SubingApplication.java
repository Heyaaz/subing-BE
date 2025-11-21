package com.project.subing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubingApplication {

  public static void main(String[] args) {
    SpringApplication.run(SubingApplication.class, args);
  }

}
