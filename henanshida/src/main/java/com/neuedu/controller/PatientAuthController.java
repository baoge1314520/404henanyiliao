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
 * 患者端认证与自助服务 控制层。
 * 患者档案表没有密码字段，演示环境约定：登录密码 = 身份证号后 6 位（界面已明示）。
 * 患者令牌与管理端令牌隔离（TokenService 不同前缀），本控制器所有业务接口
 * 都从令牌解析 patientId，只能操作本人数据。
 */
@RestController
@RequestMapping("/patientAuth")
public class PatientAuthController {

    @Resource
    private SysPatientService sysPatientService;
    @Resource
    private SysRegistrationService sysRegistrationService;
    @Resource
    private SysScheduleService sysScheduleService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysDeptService sysDeptService;
    @Resource
    private SysTitleService sysTitleService;
    @Resource
    private TokenService tokenService;

    /** 患者登录：手机号 + 身份证后 6 位（演示密码约定） */
    @PostMapping("login")
    Result login(String phone, String password) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(password)) {
            return Result.fail("请输入手机号和密码");
        }
        List<SysPatient> list = sysPatientService.list(QueryWrapper.create()
                .eq("phone", phone.trim()).eq("enabled", true));
        if (list.isEmpty()) {
            return Result.fail("该手机号未建档，请先注册");
        }
        SysPatient patient = list.get(0);
        if (!demoPassword(patient).equalsIgnoreCase(password.trim())) {
            return Result.fail("手机号或密码错误（演示密码为身份证号后 6 位）");
        }
        String token = tokenService.createPatient(patient.getId());
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("token", token);
        content.put("patient", patient);
        return Result.success(content, "登录成功");
    }

    /** 患者注册：姓名 + 手机号 + 身份证号，注册成功即自动登录 */
    @PostMapping("register")
    Result register(String patientName, String phone, String idCard) {
        if (StringUtils.isBlank(patientName) || StringUtils.isBlank(phone) || StringUtils.isBlank(idCard)) {
            return Result.fail("请填写姓名、手机号和身份证号");
        }
        String p = phone.trim();
        String card = idCard.trim().toUpperCase();
        if (!p.matches("^1[3-9]\\d{9}$")) {
            return Result.fail("手机号格式不正确");
        }
        if (!card.matches("^\\d{17}[\\dX]$")) {
            return Result.fail("身份证号需为 18 位（末位可为 X）");
        }
        if (sysPatientService.getByIdCard(card) != null) {
            return Result.fail("该身份证已建档，请直接登录");
        }
        if (!sysPatientService.list(QueryWrapper.create().eq("phone", p)).isEmpty()) {
            return Result.fail("该手机号已被使用，请更换或直接登录");
        }
        long now = System.currentTimeMillis();
        // 性别 / 出生日期按身份证规则解析：第 17 位奇数为男、偶数为女，第 7-14 位为生日
        String gender = (card.charAt(16) - '0') % 2 == 1 ? "1" : "2";
        LocalDate birthday = LocalDate.of(
                Integer.parseInt(card.substring(6, 10)),
                Integer.parseInt(card.substring(10, 12)),
                Integer.parseInt(card.substring(12, 14)));
        SysPatient patient = SysPatient.builder()
                .patientName(patientName.trim())
                .phone(p)
                .idCard(card)
                .gender(gender)
                .birthday(birthday)
                .enabled(true)
                .createDate(new java.sql.Date(now))
                .createTime(new java.sql.Time(now))
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        if (!sysPatientService.save(patient)) {
            return Result.fail("注册失败，请稍后重试");
        }
        String token = tokenService.createPatient(patient.getId());
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("token", token);
        content.put("patient", patient);
        return Result.success(content, "注册成功");
    }

    @GetMapping("info")
    Result info(HttpServletRequest request) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysPatient patient = sysPatientService.getById(patientId);
        if (patient == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        return Result.success(patient);
    }

    @PostMapping("logout")
    Result logout(HttpServletRequest request) {
        tokenService.deletePatient(readToken(request));
        return Result.success(null, "已退出登录");
    }

    /** 我的挂号：本人与家人的记录，可按状态过滤 */
    @GetMapping("registrations")
    Result registrations(HttpServletRequest request, String status) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        List<Long> ids = new ArrayList<>();
        ids.add(patientId);
        ids.addAll(familyIds(patientId));
        return Result.success(sysRegistrationService.list(1, 200, null, null, status, ids));
    }

    /**
     * 挂号大厅号源：某日排班 + 医生/科室/职称信息 + 余号。
     * 余号 = 号源数 - 该医生当日该时段未取消的挂号数。
     */
    @GetMapping("schedules")
    Result schedules(HttpServletRequest request, String date, Long deptId) {
        if (currentPatientId(request) == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (StringUtils.isBlank(date)) {
            return Result.fail("缺少查询日期");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(date.trim());
        } catch (Exception e) {
            return Result.fail("日期格式不正确");
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("schedule_date", day).eq("enabled", true);
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        wrapper.orderBy("reg_period", true).orderBy("id", true);
        List<SysSchedule> schedules = sysScheduleService.list(wrapper);
        if (schedules.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        Map<Long, SysUser> doctorMap = batchMap(sysUserService.list(QueryWrapper.create().in("id",
                schedules.stream().map(SysSchedule::getDoctorId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))), SysUser::getId);
        Map<Long, SysDept> deptMap = batchMap(sysDeptService.list(QueryWrapper.create().in("id",
                schedules.stream().map(SysSchedule::getDeptId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))), SysDept::getId);
        List<Long> titleIds = doctorMap.values().stream().map(SysUser::getTitleId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysTitle> titleMap = titleIds.isEmpty() ? new HashMap<>()
                : batchMap(sysTitleService.list(QueryWrapper.create().in("id", titleIds)), SysTitle::getId);

        List<Map<String, Object>> vos = new ArrayList<>(schedules.size());
        for (SysSchedule s : schedules) {
            SysUser doctor = doctorMap.get(s.getDoctorId());
            // 医生被禁用时不展示该号源
            if (doctor == null || Boolean.FALSE.equals(doctor.getEnabled())) {
                continue;
            }
            long booked = sysRegistrationService.count(QueryWrapper.create()
                    .eq("doctor_id", s.getDoctorId())
                    .eq("reg_date", day)
                    .eq("reg_period", s.getRegPeriod())
                    .ne("status", "3"));
            int quota = s.getQuota() == null ? 0 : s.getQuota();
            SysDept dept = deptMap.get(s.getDeptId());
            SysTitle title = titleMap.get(doctor.getTitleId());
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", s.getId());
            vo.put("doctorId", doctor.getId());
            vo.put("doctorName", doctor.getRealName());
            vo.put("avatar", doctor.getAvatar());
            vo.put("intro", doctor.getIntro());
            vo.put("deptId", s.getDeptId());
            vo.put("deptName", dept != null ? dept.getDeptName() : "");
            vo.put("titleName", title != null ? title.getTitleName() : "");
            vo.put("regFee", title != null ? title.getRegFee() : null);
            vo.put("scheduleDate", s.getScheduleDate());
            vo.put("regPeriod", s.getRegPeriod());
            vo.put("quota", quota);
            vo.put("remaining", Math.max(0, quota - (int) booked));
            vos.add(vo);
        }
        return Result.success(vos);
    }

    /** 患者自助挂号：校验排班与余号后落单（默认待支付），返回挂号记录供发起支付宝支付。
     *  patientId 可选：为家人代挂号时传家人档案ID，仅限本人或已添加的家人。 */
    @PostMapping("book")
    Result book(HttpServletRequest request, String regDate, String regPeriod, Long doctorId, Long deptId, String remark, Long patientId) {
        Long selfId = currentPatientId(request);
        if (selfId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        Long targetId = selfId;
        if (patientId != null && !patientId.equals(selfId)) {
            if (!familyIds(selfId).contains(patientId)) {
                return Result.fail("只能为本人或已添加的家人挂号");
            }
            targetId = patientId;
        }
        SysPatient target = sysPatientService.getById(targetId);
        if (target == null || Boolean.FALSE.equals(target.getEnabled())) {
            return Result.fail("就诊人档案不存在或已禁用");
        }
        if (StringUtils.isBlank(regDate) || StringUtils.isBlank(regPeriod) || doctorId == null) {
            return Result.fail("请选择就诊日期、时段和医生");
        }
        if (!"1".equals(regPeriod) && !"2".equals(regPeriod)) {
            return Result.fail("就诊时段不正确");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(regDate.trim());
        } catch (Exception e) {
            return Result.fail("日期格式不正确");
        }
        if (day.isBefore(LocalDate.now())) {
            return Result.fail("不能挂过去日期的号");
        }
        SysUser doctor = sysUserService.getById(doctorId);
        if (doctor == null || Boolean.FALSE.equals(doctor.getEnabled())) {
            return Result.fail("医生不存在或已停诊");
        }
        List<SysSchedule> schedules = sysScheduleService.list(QueryWrapper.create()
                .eq("doctor_id", doctorId).eq("schedule_date", day)
                .eq("reg_period", regPeriod).eq("enabled", true));
        if (schedules.isEmpty()) {
            return Result.fail("该医生在所选日期无排班");
        }
        SysSchedule schedule = schedules.get(0);
        long booked = sysRegistrationService.count(QueryWrapper.create()
                .eq("doctor_id", doctorId).eq("reg_date", day)
                .eq("reg_period", regPeriod).ne("status", "3"));
        int quota = schedule.getQuota() == null ? 0 : schedule.getQuota();
        if (booked >= quota) {
            return Result.fail("该时段号源已约满，请选择其他时段");
        }
        SysRegistration registration = SysRegistration.builder()
                .patientId(targetId)
                .deptId(deptId != null ? deptId : schedule.getDeptId())
                .doctorId(doctorId)
                .regDate(day)
                .regPeriod(regPeriod)
                .regType("2")
                .remark(remark)
                .build();
        boolean ok = sysRegistrationService.add(registration, null);
        return ok ? Result.success(registration, "挂号成功") : Result.fail("挂号失败，请稍后重试");
    }

    /** 患者自助退号：仅限本人、待支付/待就诊且未过就诊日期 */
    @PostMapping("cancel")
    Result cancel(HttpServletRequest request, Long id) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (id == null) {
            return Result.fail("缺少挂号记录ID");
        }
        SysRegistration registration = sysRegistrationService.getById(id);
        if (registration == null || !owns(patientId, registration.getPatientId())) {
            return Result.fail("无权操作该挂号单");
        }
        if (!"0".equals(registration.getStatus()) && !"1".equals(registration.getStatus())) {
            return Result.fail("当前状态不可退号");
        }
        if (registration.getRegDate() != null && registration.getRegDate().isBefore(LocalDate.now())) {
            return Result.fail("已过就诊日期，无法自助退号");
        }
        boolean ok = sysRegistrationService.updateStatus(id, "3");
        return ok ? Result.success(null, "退号成功") : Result.fail("退号失败，请稍后重试");
    }

    /** 我的家人：当前患者添加的家人档案（owner_id = 本人），按添加先后排序 */
    @GetMapping("family")
    Result family(HttpServletRequest request) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        List<SysPatient> members = sysPatientService.list(QueryWrapper.create()
                .eq("owner_id", patientId).orderBy("id", true));
        return Result.success(members);
    }

    /** 修改本人个人信息：姓名 / 手机号（登录账号）/ 住址；身份证、性别、生日不可改 */
    @PostMapping("updateProfile")
    Result updateProfile(HttpServletRequest request, String patientName, String phone, String address) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (sysPatientService.getById(patientId) == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (StringUtils.isBlank(patientName)) {
            return Result.fail("请填写姓名");
        }
        String p = phone == null ? "" : phone.trim();
        if (StringUtils.isBlank(p)) {
            return Result.fail("请填写手机号（登录账号）");
        }
        if (!p.matches("^1[3-9]\\d{9}$")) {
            return Result.fail("手机号格式不正确");
        }
        if (!sysPatientService.list(QueryWrapper.create().eq("phone", p).ne("id", patientId)).isEmpty()) {
            return Result.fail("该手机号已被其他档案使用");
        }
        long now = System.currentTimeMillis();
        SysPatient update = SysPatient.builder()
                .id(patientId)
                .patientName(patientName.trim())
                .phone(p)
                .address(address == null ? "" : address.trim())
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        boolean ok = sysPatientService.updateById(update);
        return ok ? Result.success(sysPatientService.getById(patientId), "个人信息已更新")
                : Result.fail("更新失败，请稍后重试");
    }

    /** 添加家人档案：写入同一张 sys_patient 表并标记 owner_id = 本人，管理员患者管理自动可见 */
    @PostMapping("addFamily")
    Result addFamily(HttpServletRequest request, String patientName, String phone, String idCard) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (StringUtils.isBlank(patientName) || StringUtils.isBlank(idCard)) {
            return Result.fail("请填写家人姓名和身份证号");
        }
        String card = idCard.trim().toUpperCase();
        if (!card.matches("^\\d{17}[\\dX]$")) {
            return Result.fail("身份证号需为 18 位（末位可为 X）");
        }
        if (sysPatientService.getByIdCard(card) != null) {
            return Result.fail("该身份证已建档，不能重复添加");
        }
        String p = phone == null ? "" : phone.trim();
        if (StringUtils.isNotBlank(p)) {
            if (!p.matches("^1[3-9]\\d{9}$")) {
                return Result.fail("手机号格式不正确");
            }
            if (!sysPatientService.list(QueryWrapper.create().eq("phone", p)).isEmpty()) {
                return Result.fail("该手机号已被使用，请更换");
            }
        }
        long now = System.currentTimeMillis();
        // 性别 / 出生日期按身份证规则解析：第 17 位奇数为男、偶数为女，第 7-14 位为生日
        String gender = (card.charAt(16) - '0') % 2 == 1 ? "1" : "2";
        LocalDate birthday = LocalDate.of(
                Integer.parseInt(card.substring(6, 10)),
                Integer.parseInt(card.substring(10, 12)),
                Integer.parseInt(card.substring(12, 14)));
        SysPatient member = SysPatient.builder()
                .patientName(patientName.trim())
                .phone(p)
                .idCard(card)
                .gender(gender)
                .birthday(birthday)
                .ownerId(patientId)
                .enabled(true)
                .createDate(new java.sql.Date(now))
                .createTime(new java.sql.Time(now))
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        boolean ok = sysPatientService.save(member);
        return ok ? Result.success(member, "家人档案已添加，管理员患者管理中同步可见")
                : Result.fail("添加失败，请稍后重试");
    }

    /** 修改家人信息：仅限本人添加的家人（owner_id = 本人），姓名 / 手机号 / 住址 */
    @PostMapping("updateFamily")
    Result updateFamily(HttpServletRequest request, Long id, String patientName, String phone, String address) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (id == null) {
            return Result.fail("缺少家人档案ID");
        }
        SysPatient member = sysPatientService.getById(id);
        if (member == null || !patientId.equals(member.getOwnerId())) {
            return Result.fail("家人档案不存在或无权修改");
        }
        if (StringUtils.isBlank(patientName)) {
            return Result.fail("请填写姓名");
        }
        String p = phone == null ? "" : phone.trim();
        if (StringUtils.isNotBlank(p)) {
            if (!p.matches("^1[3-9]\\d{9}$")) {
                return Result.fail("手机号格式不正确");
            }
            if (!sysPatientService.list(QueryWrapper.create().eq("phone", p).ne("id", id)).isEmpty()) {
                return Result.fail("该手机号已被其他档案使用");
            }
        }
        long now = System.currentTimeMillis();
        SysPatient update = SysPatient.builder()
                .id(id)
                .patientName(patientName.trim())
                .phone(p)
                .address(address == null ? "" : address.trim())
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        boolean ok = sysPatientService.updateById(update);
        return ok ? Result.success(sysPatientService.getById(id), "家人信息已更新")
                : Result.fail("更新失败，请稍后重试");
    }

    /** 删除家人档案：仅限本人添加的家人；已有挂号记录时禁止删除，保留历史就诊归属 */
    @PostMapping("removeFamily")
    Result removeFamily(HttpServletRequest request, Long id) {
        Long patientId = currentPatientId(request);
        if (patientId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        if (id == null) {
            return Result.fail("缺少家人档案ID");
        }
        SysPatient member = sysPatientService.getById(id);
        if (member == null || !patientId.equals(member.getOwnerId())) {
            return Result.fail("家人档案不存在或无权删除");
        }
        long regCount = sysRegistrationService.count(QueryWrapper.create().eq("patient_id", id));
        if (regCount > 0) {
            return Result.fail("该家人已有挂号记录，不能删除档案");
        }
        boolean ok = sysPatientService.removeById(id);
        return ok ? Result.success(null, "家人档案已删除") : Result.fail("删除失败，请稍后重试");
    }

    /** 当前患者添加的家人档案ID列表 */
    private List<Long> familyIds(Long selfId) {
        return sysPatientService.list(QueryWrapper.create().eq("owner_id", selfId))
                .stream().map(SysPatient::getId).collect(Collectors.toList());
    }

    /** 目标档案是否属于当前患者：本人或已添加的家人 */
    private boolean owns(Long selfId, Long targetId) {
        return selfId.equals(targetId) || familyIds(selfId).contains(targetId);
    }

    /** 演示密码约定：身份证号后 6 位（不足 6 位时取全部） */
    private String demoPassword(SysPatient patient) {
        String card = patient.getIdCard();
        if (StringUtils.isBlank(card)) {
            return "";
        }
        card = card.trim();
        return card.length() >= 6 ? card.substring(card.length() - 6) : card;
    }

    private Long currentPatientId(HttpServletRequest request) {
        Object attr = request.getAttribute("patientId");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        // 拦截器放行的登录/注册接口之外，兜底再解析一次令牌
        return tokenService.resolvePatient(readToken(request));
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