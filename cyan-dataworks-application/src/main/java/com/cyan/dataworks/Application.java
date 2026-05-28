package com.cyan.dataworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 数据加工服务启动类
 *
 * @author cy.Y
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.cyan"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cyan")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
