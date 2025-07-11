package com.ie_project.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String... args) {

    System.out.println("Test Application");

    SpringApplication.run(Application.class, args);

    System.out.println("Application Started 2");

  }

}