package com.cyan.dataworks.application.job.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * ODS到DWD节点配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OdsToDwdNodeConfig {

    /**
     * 输入ODS CDC流水表
     */
    private String inputTable;

    /**
     * 输出DWD当前态表
     */
    private String outputTable;

    /**
     * 主键字段列表
     */
    private List<String> primaryKeys;

    /**
     * 操作类型字段
     */
    private String opField = "_op";

    /**
     * 事件时间字段
     */
    private String eventTimeField = "_ts";

    /**
     * 入湖时间字段
     */
    private String ingestionTimeField = "_ingestion_time";
}
