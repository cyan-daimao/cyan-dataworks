package com.cyan.dataworks.client.job_instance.request;

import com.cyan.dataworks.client.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 作业实例回调请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JobInstanceCallbackRequest {

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 结果数据
     */
    private String resultData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * RustFS日志对象Key
     */
    private String logObjectKey;

    /**
     * 进程退出码
     */
    private Integer exitCode;

    /**
     * 运行开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 运行结束时间
     */
    private LocalDateTime finishedAt;
}
