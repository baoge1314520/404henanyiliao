package com.neuedu.controller;

import com.neuedu.entity.SysSchedule;
import com.neuedu.service.SysScheduleService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysSchedule")
public class SysScheduleController {

    @Resource
    private SysScheduleService sysScheduleService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, Long doctorId, Long deptId, String scheduleDate) {
        return Result.success(sysScheduleService.list(pageNo, pageSize, doctorId, deptId, scheduleDate));
    }

    @PostMapping("add")
    Result add(SysSchedule sysSchedule) {
        return Result.success(sysScheduleService.save(sysSchedule), "添加排班成功");
    }

    @PostMapping("update")
    Result update(SysSchedule sysSchedule) {
        return Result.success(sysScheduleService.updateById(sysSchedule), "修改排班成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysSchedule sysSchedule = SysSchedule.builder().id(id).enabled(enabled).build();
        return Result.success(sysScheduleService.updateById(sysSchedule), enabled ? "启用排班成功" : "删除排班成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysScheduleService.getById(id));
    }
}