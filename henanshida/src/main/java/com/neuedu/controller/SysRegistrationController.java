package com.neuedu.controller;

import com.neuedu.entity.SysPatient;
import com.neuedu.entity.SysRegistration;
import com.neuedu.service.SysRegistrationService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 挂号记录表 控制层（患者信息由挂号服务统一处理，不单独提供接口）。
 *
 * @author wangyu
 * @since 2026-11-09
 */
@RestController
@RequestMapping("/sysRegistration")
public class SysRegistrationController {

    @Resource
    private SysRegistrationService sysRegistrationService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String patientName, String regDate, String status, Long patientId) {
        return Result.success(sysRegistrationService.list(pageNo, pageSize, patientName, regDate, status, patientId));
    }

    /**
     * 根据身份证号识别患者是否存在，存在则返回患者信息用于前端回填。
     */
    @GetMapping("patient")
    Result patient(String idCard) {
        return Result.success(sysRegistrationService.getPatientByIdCard(idCard));
    }

    /**
     * 挂号：患者信息与挂号信息一并提交，患者已存在则只保存挂号记录。
     * 成功后返回保存的挂号记录（含 id / regNo / regFee），供前端发起支付宝支付。
     */
    @PostMapping("add")
    Result add(SysRegistration registration, SysPatient patient) {
        boolean success = sysRegistrationService.add(registration, patient);
        return success ? Result.success(registration, "挂号成功") : Result.fail("患者姓名不能为空");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysRegistrationService.getById(id));
    }

    /**
     * 完成就诊
     */
    @PostMapping("finish")
    Result finish(Long id) {
        return Result.success(sysRegistrationService.updateStatus(id, "2"), "完成就诊成功");
    }

    /**
     * 取消挂号
     */
    @PostMapping("cancel")
    Result cancel(Long id) {
        return Result.success(sysRegistrationService.updateStatus(id, "3"), "取消挂号成功");
    }

    /**
     * 叫号队列（待就诊）
     */
    @GetMapping("queue")
    Result queue() {
        return Result.success(sysRegistrationService.queue());
    }

    /**
     * 叫号
     */
    @PostMapping("call")
    Result call(Long id) {
        return Result.success(sysRegistrationService.call(id), "叫号成功");
    }

    /**
     * 分诊：调整科室/医生/职称
     */
    @PostMapping("triage")
    Result triage(Long id, Long deptId, Long doctorId, Long titleId) {
        return Result.success(sysRegistrationService.triage(id, deptId, doctorId, titleId), "分诊成功");
    }
}
