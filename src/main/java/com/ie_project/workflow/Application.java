package com.ie_project.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.ie_project")
public class Application {

  public static void main(String... args) {

    System.out.println("Test Application");

    ApplicationContext context = SpringApplication.run(Application.class, args);

    System.out.println("Application Started 3");

    // Debug: Liste aller Delegate Beans / Debug: List all Delegate beans
    System.out.println("=== REGISTERED DELEGATE BEANS ===");
    String[] beanNames = context.getBeanNamesForType(JavaDelegate.class);
    for (String beanName : beanNames) {
      System.out.println("Found Delegate Bean: " + beanName);
    }
    System.out.println("Total Delegate Beans: " + beanNames.length);
    System.out.println("==================================");

  }
}