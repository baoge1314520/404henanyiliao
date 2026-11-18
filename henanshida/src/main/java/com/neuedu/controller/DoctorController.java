package com.neuedu.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.neuedu.chat.TokenService;
import com.neuedu.core.ResultCode;
import com.neuedu.entity.SysDept;
import com.neuedu.entity.SysPatient;
import com.neuedu.entity.SysRegistration;
import com.neuedu.entity.SysSchedule;
import com.neuedu.entity.SysTitle;
import com.neuedu.entity.SysUser;
import com.neuedu.service.SysDeptService;
import com.neuedu.service.SysPatientService;
import com.neuedu.service.SysRegistrationService;
import com.neuedu.service.SysScheduleService;
import com.neuedu.service.SysTitleService;
import com.neuedu.service.SysUserService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 医生端工作台 控制层。
 * 医生令牌与管理端/患者端令牌隔离（TokenService 不同前缀），拦截器只放行
 * /doctorAuth/** 给医生身份；本控制器所有接口都从令牌解析 doctorId，
 * 只能看自己的排班、叫自己名下的患者，不能触碰管理端数据。
 */
@RestController
@RequestMapping("/doctorAuth")
public class DoctorController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysDeptService sysDeptService;
    @Resource
    private SysTitleService sysTitleService;
    @Resource
    private SysScheduleService sysScheduleService;
    @Resource
    private SysRegistrationService sysRegistrationService;
    @Resource
    private SysPatientService sysPatientService;
    @Resource
    private TokenService tokenService;

    /** 当前医生信息（不回传密码），附科室与职称名称 */
    @GetMapping("info")
    Result info(HttpServletRequest request) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysUser doctor = sysUserService.getById(doctorId);
        if (doctor == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        doctor.setPassword(null);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("user", doctor);
        SysDept dept = doctor.getDeptId() == null ? null : sysDeptService.getById(doctor.getDeptId());
        SysTitle title = doctor.getTitleId() == null ? null : sysTitleService.getById(doctor.getTitleId());
        content.put("deptName", dept != null ? dept.getDeptName() : "");
        content.put("titleName", title != null ? title.getTitleName() : "");
        return Result.success(content);
    }

    @PostMapping("logout")
    Result logout(HttpServletRequest request) {
        tokenService.deleteDoctor(readToken(request));
        return Result.success(null, "已退出登录");
    }

    /** 工作台统计：今日挂号总数 / 候诊 / 已叫号 / 已就诊 + 今日排班时段数 */
    @GetMapping("stats")
    Result stats(HttpServletRequest request) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        LocalDate today = LocalDate.now();
        List<SysRegistration> regs = sysRegistrationService.list(QueryWrapper.create()
                .eq("doctor_id", doctorId).eq("reg_date", today).ne("status", "3"));
        long waiting = regs.stream().filter(r -> "1".equals(r.getStatus()) && r.getCallTime() == null).count();
        long called = regs.stream().filter(r -> "1".equals(r.getStatus()) && r.getCallTime() != null).count();
        long done = regs.stream().filter(r -> "2".equals(r.getStatus())).count();
        long shifts = sysScheduleService.count(QueryWrapper.create()
                .eq("doctor_id", doctorId).eq("schedule_date", today).eq("enabled", true));
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("todayTotal", regs.size());
        content.put("waiting", waiting);
        content.put("called", called);
        content.put("done", done);
        content.put("shifts", shifts);
        return Result.success(content);
    }

    /** 我的排班：今天起的排班（含已约/余号），只查本人 */
    @GetMapping("schedules")
    Result schedules(HttpServletRequest request) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        List<SysSchedule> schedules = sysScheduleService.list(QueryWrapper.create()
                .eq("doctor_id", doctorId)
                .ge("schedule_date", LocalDate.now())
                .orderBy("schedule_date", true).orderBy("reg_period", true).orderBy("id", true));
        if (schedules.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        Map<Long, SysDept> deptMap = batchMap(sysDeptService.list(QueryWrapper.create().in("id",
                schedules.stream().map(SysSchedule::getDeptId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))), SysDept::getId);
        List<Map<String, Object>> vos = new ArrayList<>(schedules.size());
        for (SysSchedule s : schedules) {
            // 已约口径与挂号大厅一致：未取消（含待支付）都占号
            long booked = sysRegistrationService.count(QueryWrapper.create()
                    .eq("doctor_id", doctorId)
                    .eq("reg_date", s.getScheduleDate())
                    .eq("reg_period", s.getRegPeriod())
                    .ne("status", "3"));
            int quota = s.getQuota() == null ? 0 : s.getQuota();
            SysDept dept = deptMap.get(s.getDeptId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", s.getId());
            vo.put("scheduleDate", s.getScheduleDate());
            vo.put("regPeriod", s.getRegPeriod());
            vo.put("deptId", s.getDeptId());
            vo.put("deptName", dept != null ? dept.getDeptName() : "");
            vo.put("quota", quota);
            vo.put("booked", booked);
            vo.put("remaining", Math.max(0, quota - (int) booked));
            vo.put("enabled", s.getEnabled());
            vos.add(vo);
        }
        return Result.success(vos);
    }

    /** 今日就诊：本人名下今日待就诊/已就诊患者，候诊在前（未叫号优先），已就诊在后 */
    @GetMapping("today")
    Result today(HttpServletRequest request) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        List<SysRegistration> regs = sysRegistrationService.list(QueryWrapper.create()
                .eq("doctor_id", doctorId)
                .eq("reg_date", LocalDate.now())
                .in("status", "1", "2"));
        if (regs.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        // 待就诊在前；待就诊里未叫号在前，再按叫号时间、挂号先后
        regs.sort((a, b) -> {
            int sa = "1".equals(a.getStatus()) ? 0 : 1;
            int sb = "1".equals(b.getStatus()) ? 0 : 1;
            if (sa != sb) return Integer.compare(sa, sb);
            int ca = a.getCallTime() == null ? 0 : 1;
            int cb = b.getCallTime() == null ? 0 : 1;
            if (ca != cb) return Integer.compare(ca, cb);
            if (a.getCallTime() != null && b.getCallTime() != null) {
                int t = a.getCallTime().compareTo(b.getCallTime());
                if (t != 0) return t;
            }
            return Long.compare(a.getId(), b.getId());
        });
        Map<Long, SysPatient> patientMap = batchMap(sysPatientService.list(QueryWrapper.create().in("id",
                regs.stream().map(SysRegistration::getPatientId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))), SysPatient::getId);
        List<Map<String, Object>> vos = new ArrayList<>(regs.size());
        for (SysRegistration r : regs) {
            SysPatient p = patientMap.get(r.getPatientId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", r.getId());
            vo.put("regNo", r.getRegNo());
            vo.put("patientId", r.getPatientId());
            vo.put("patientName", p != null ? p.getPatientName() : "");
            vo.put("gender", p != null ? p.getGender() : "");
            vo.put("birthday", p != null ? p.getBirthday() : null);
            vo.put("phone", p != null ? p.getPhone() : "");
            vo.put("regPeriod", r.getRegPeriod());
            vo.put("regFee", r.getRegFee());
            vo.put("status", r.getStatus());
            vo.put("callTime", r.getCallTime());
            vo.put("remark", r.getRemark());
            vo.put("medication", r.getMedication());
            vo.put("advice", r.getAdvice());
            vos.add(vo);
        }
        return Result.success(vos);
    }

    /** 历史记录：本人名下全部日期的挂号分页（过去 + 未来预约），含就诊用药与医嘱 */
    @GetMapping("history")
    Result history(HttpServletRequest request, Integer pageNo, Integer pageSize,
                   String patientName, String status, String dateFrom, String dateTo) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        LocalDate from = null;
        LocalDate to = null;
        try {
            if (StringUtils.isNotBlank(dateFrom)) {
                from = LocalDate.parse(dateFrom.trim());
            }
            if (StringUtils.isNotBlank(dateTo)) {
                to = LocalDate.parse(dateTo.trim());
            }
        } catch (Exception e) {
            return Result.fail("日期格式不正确");
        }
        if (from != null && to != null && from.isAfter(to)) {
            return Result.fail("开始日期不能晚于结束日期");
        }
        return Result.success(sysRegistrationService.listForDoctor(pageNo, pageSize, doctorId, patientName, status, from, to));
    }

    /** 历史记录统计：各状态总数 + 未来预约数（就诊日期在今天及以后、未取消未就诊） */
    @GetMapping("historyStats")
    Result historyStats(HttpServletRequest request) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        List<SysRegistration> regs = sysRegistrationService.list(QueryWrapper.create().eq("doctor_id", doctorId));
        LocalDate today = LocalDate.now();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("total", regs.size());
        content.put("unpaid", regs.stream().filter(r -> "0".equals(r.getStatus())).count());
        content.put("waiting", regs.stream().filter(r -> "1".equals(r.getStatus())).count());
        content.put("done", regs.stream().filter(r -> "2".equals(r.getStatus())).count());
        content.put("cancelled", regs.stream().filter(r -> "3".equals(r.getStatus())).count());
        content.put("future", regs.stream().filter(r -> r.getRegDate() != null && !r.getRegDate().isBefore(today)
                && ("0".equals(r.getStatus()) || "1".equals(r.getStatus()))).count());
        return Result.success(content);
    }

    /** 叫号：仅限本人名下、待就诊的挂号单 */
    @PostMapping("call")
    Result call(HttpServletRequest request, Long id) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysRegistration registration = mine(id, doctorId);
        if (registration == null) {
            return Result.fail("挂号单不存在或无权操作其他医生的患者");
        }
        if (!"1".equals(registration.getStatus())) {
            return Result.fail("该患者当前不可叫号");
        }
        boolean ok = sysRegistrationService.call(id);
        return ok ? Result.success(null, "叫号成功") : Result.fail("叫号失败，请稍后重试");
    }

    /** 完成就诊：待就诊 -> 已就诊，同时写入用药与建议 */
    @PostMapping("finish")
    Result finish(HttpServletRequest request, Long id, String medication, String advice) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysRegistration registration = mine(id, doctorId);
        if (registration == null) {
            return Result.fail("挂号单不存在或无权操作其他医生的患者");
        }
        if (!"1".equals(registration.getStatus())) {
            return Result.fail("该患者当前不可完成就诊");
        }
        long now = System.currentTimeMillis();
        SysRegistration update = SysRegistration.builder()
                .id(id)
                .status("2")
                .medication(medication)
                .advice(advice)
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        boolean ok = sysRegistrationService.updateById(update);
        return ok ? Result.success(null, "就诊完成") : Result.fail("操作失败，请稍后重试");
    }

    /** 补充/修改医嘱：仅限已就诊记录，用药与建议可清空（传空串） */
    @PostMapping("advice")
    Result advice(HttpServletRequest request, Long id, String medication, String advice) {
        Long doctorId = currentDoctorId(request);
        if (doctorId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysRegistration registration = mine(id, doctorId);
        if (registration == null) {
            return Result.fail("挂号单不存在或无权操作其他医生的患者");
        }
        if (!"2".equals(registration.getStatus())) {
            return Result.fail("仅已就诊记录可修改用药与建议");
        }
        long now = System.currentTimeMillis();
        SysRegistration update = SysRegistration.builder()
                .id(id)
                .medication(medication == null ? "" : medication)
                .advice(advice == null ? "" : advice)
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        boolean ok = sysRegistrationService.updateById(update);
        return ok ? Result.success(null, "已保存") : Result.fail("保存失败，请稍后重试");
    }

    /** 归属校验：挂号单必须存在且属于当前医生 */
    private SysRegistration mine(Long id, Long doctorId) {
        if (id == null) {
            return null;
        }
        SysRegistration registration = sysRegistrationService.getById(id);
        if (registration == null || !doctorId.equals(registration.getDoctorId())) {
            return null;
        }
        return registration;
    }

    private Long currentDoctorId(HttpServletRequest request) {
        Object attr = request.getAttribute("doctorId");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return tokenService.resolveDoctor(readToken(request));
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }
        return token;
    }

    private <T> Map<Long, T> batchMap(List<T> list, java.util.function.Function<T, Long> keyFn) {
        Map<Long, T> map = new HashMap<>();
        for (T item : list) {
            map.put(keyFn.apply(item), item);
        }
        return map;
    }
}