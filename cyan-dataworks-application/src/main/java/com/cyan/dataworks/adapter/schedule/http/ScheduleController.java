package com.cyan.dataworks.adapter.schedule.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataworks.adapter.schedule.http.convert.ScheduleAdapterConvert;
import com.cyan.dataworks.adapter.schedule.http.dto.ScheduleConfigDTO;
import com.cyan.dataworks.application.schedule.ScheduleService;
import com.cyan.dataworks.application.schedule.bo.ScheduleConfigBO;
import com.cyan.dataworks.application.schedule.cmd.ScheduleConfigCmd;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 调度配置接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/data-work/tasks/{taskId}/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 获取任务调度配置
     */
    @GetMapping
    public Response<ScheduleConfigDTO> findByTaskId(@PathVariable String taskId) {
        ScheduleConfigBO bo = scheduleService.findByTaskId(taskId);
        if (bo == null) {
            return Response.success(null);
        }
        ScheduleConfigDTO dto = ScheduleAdapterConvert.INSTANCE.toScheduleConfigDTO(bo);
        return Response.success(dto);
    }

    /**
     * 保存调度配置
     */
    @PutMapping
    public Response<ScheduleConfigDTO> saveOrUpdate(@PathVariable String taskId,
                                                     @RequestBody @Valid ScheduleConfigCmd cmd) {
        ScheduleConfigBO bo = scheduleService.saveOrUpdate(taskId, cmd);
        ScheduleConfigDTO dto = ScheduleAdapterConvert.INSTANCE.toScheduleConfigDTO(bo);
        return Response.success(dto);
    }
}
