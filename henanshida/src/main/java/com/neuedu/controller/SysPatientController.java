package com.neuedu.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.neuedu.entity.SysPatient;
import com.neuedu.service.SysPatientService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者信息表 控制层。
 */
@RestController
@RequestMapping("/sysPatient")
public class SysPatientController {

    @Resource
    private SysPatientService sysPatientService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String patientName) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (StringUtils.isNotBlank(patientName)) {
            wrapper.like("patient_name", patientName.trim());
        }
        wrapper.orderBy("id", false);
        Page<SysPatient> page = sysPatientService.page(
                Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        return Result.success(page);
    }

    @PostMapping("add")
    Result add(SysPatient sysPatient) {
        return Result.success(sysPatientService.save(sysPatient), "添加患者成功");
    }

    @PostMapping("update")
    Result update(SysPatient sysPatient) {
        return Result.success(sysPatientService.updateById(sysPatient), "修改患者成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysPatient sysPatient = SysPatient.builder().id(id).enabled(enabled).build();
        return Result.success(sysPatientService.updateById(sysPatient), enabled ? "启用患者成功" : "删除患者成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysPatientService.getById(id));
    }
}