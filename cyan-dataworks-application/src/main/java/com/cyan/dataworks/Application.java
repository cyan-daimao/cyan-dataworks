package com.cyan.dataworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数据加工服务启动类
 *
 * @author cy.Y
 * @since 1.0.0
 */
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.cyan.datagateway.client"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
