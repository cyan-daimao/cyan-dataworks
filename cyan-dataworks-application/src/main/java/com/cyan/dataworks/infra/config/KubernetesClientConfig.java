package com.cyan.dataworks.infra.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kubernetes客户端配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Configuration
public class KubernetesClientConfig {

    /**
     * 创建Kubernetes客户端
     *
     * @return Kubernetes客户端
     */
    @Bean(destroyMethod = "close")
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
