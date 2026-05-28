package com.cyan.dataworks.infra.remote.rustfs;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.infra.config.ScriptRuntimeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RustFS日志读取服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class RustFsLogService {

    /**
     * 脚本运行配置
     */
    private final ScriptRuntimeProperties properties;

    public RustFsLogService(ScriptRuntimeProperties properties) {
        this.properties = properties;
    }

    /**
     * 读取脚本日志
     *
     * @param objectKey 日志对象Key
     * @return 日志内容
     */
    public String readScriptLog(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return "";
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getLogBucket())
                .key(objectKey)
                .build();
        try (S3Client client = s3Client();
             ResponseInputStream<GetObjectResponse> inputStream = client.getObject(request)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取RustFS脚本日志失败: bucket={}, objectKey={}, error={}",
                    properties.getLogBucket(), objectKey, e.getMessage());
            throw new SilentException("读取RustFS脚本日志失败：" + objectKey);
        }
    }

    /**
     * 创建RustFS S3客户端
     */
    private S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.getRustfsAccessKey(), properties.getRustfsSecretKey());
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getRustfsEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }
}
