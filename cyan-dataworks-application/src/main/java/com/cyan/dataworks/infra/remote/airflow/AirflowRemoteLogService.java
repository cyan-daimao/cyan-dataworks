package com.cyan.dataworks.infra.remote.airflow;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataworks.domain.job_instance.JobInstance;
import com.cyan.dataworks.infra.config.AirflowProperties;
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
import java.util.Optional;

/**
 * Airflow远程日志读取服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class AirflowRemoteLogService {

    /**
     * Airflow配置
     */
    private final AirflowProperties airflowProperties;

    public AirflowRemoteLogService(AirflowProperties airflowProperties) {
        this.airflowProperties = airflowProperties;
    }

    /**
     * 判断实例是否有关联的Airflow远程日志
     *
     * @param instance 作业实例
     * @return 是否存在Airflow追踪信息
     */
    public boolean supports(JobInstance instance) {
        return instance != null
                && notBlank(instance.getSchedulerDagId())
                && notBlank(instance.getSchedulerDagRunId())
                && notBlank(instance.getSchedulerTaskId());
    }

    /**
     * 读取Airflow任务远程日志
     *
     * @param instance 作业实例
     * @return 远程日志内容
     */
    public String readTaskLog(JobInstance instance) {
        if (!supports(instance)) {
            return "";
        }
        AirflowProperties.RemoteLog remoteLog = airflowProperties.getRemoteLog();
        String objectKey = buildObjectKey(instance);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(remoteLog.getBucket())
                .key(objectKey)
                .build();
        try (S3Client client = s3Client(remoteLog);
             ResponseInputStream<GetObjectResponse> inputStream = client.getObject(request)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("读取Airflow远程日志失败: instanceId={}, bucket={}, objectKey={}, error={}",
                    instance.getId(), remoteLog.getBucket(), objectKey, e.getMessage());
            throw new SilentException("读取Airflow远程日志失败：" + objectKey);
        }
    }

    /**
     * 构建Airflow远程日志对象Key
     *
     * @param instance 作业实例
     * @return 对象Key
     */
    public String buildObjectKey(JobInstance instance) {
        AirflowProperties.RemoteLog remoteLog = airflowProperties.getRemoteLog();
        String basePrefix = Optional.ofNullable(remoteLog.getBasePrefix())
                .filter(value -> !value.isBlank())
                .map(value -> value.replaceAll("^/+", "").replaceAll("/+$", ""))
                .orElse("dataworks-airflow");
        Integer tryNumber = Optional.ofNullable(instance.getSchedulerTryNumber()).orElse(1);
        return "%s/dag_id=%s/run_id=%s/task_id=%s/attempt=%d.log".formatted(
                basePrefix,
                instance.getSchedulerDagId(),
                instance.getSchedulerDagRunId(),
                instance.getSchedulerTaskId(),
                tryNumber
        );
    }

    /**
     * 创建RustFS S3客户端
     *
     * @param remoteLog 远程日志配置
     * @return S3客户端
     */
    private S3Client s3Client(AirflowProperties.RemoteLog remoteLog) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(remoteLog.getAccessKey(), remoteLog.getSecretKey());
        return S3Client.builder()
                .endpointOverride(URI.create(remoteLog.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    /**
     * 判断字符串非空
     *
     * @param value 字符串
     * @return 是否非空
     */
    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
