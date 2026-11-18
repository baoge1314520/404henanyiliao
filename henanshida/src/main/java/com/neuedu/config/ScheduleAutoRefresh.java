package com.neuedu.config;

import com.mybatisflex.core.query.QueryWrapper;
import com.neuedu.entity.SysUser;
import com.neuedu.service.SysScheduleService;
import com.neuedu.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班滚动刷新：启动时与每天 00:05 各跑一次，
 * 为所有启用且带科室/职称的医生幂等补齐「今天起 7 天」的默认排班（上午 30 / 下午 20）。
 * 患者端挂号大厅固定展示今天起 7 天，窗口每天前移一天，
 * 这里同步补出最新一天的号源，避免出现「无号」挂不了的情况；
 * 已有排班（含手工调整过的号源）只跳过不覆盖。
 */
@Slf4j
@Component
public class ScheduleAutoRefresh {

    /** 与挂号大厅展示的日期窗口保持一致：今天 + 6 天 */
    private static final int WINDOW_DAYS = 7;

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysScheduleService sysScheduleService;

    /** 启动即补一次：停机几天后重启也能把窗口内缺的号源补齐 */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refresh("启动");
    }

    /** 每天 00:05 补出窗口最新一天，号源随日期滚动 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void daily() {
        refresh("每日");
    }

    private void refresh(String trigger) {
        try {
            List<SysUser> doctors = sysUserService.list(QueryWrapper.create()
                    .eq("enabled", true)
                    .isNotNull("dept_id")
                    .isNotNull("title_id"));
            LocalDate today = LocalDate.now();
            int created = 0;
            for (SysUser doctor : doctors) {
                created += sysScheduleService.ensureDefaultSchedules(doctor.getId(), doctor.getDeptId(), today, WINDOW_DAYS);
            }
            log.info("排班滚动刷新（{}）：医生 {} 名，补建排班 {} 条，窗口 {} ~ {}",
                    trigger, doctors.size(), created, today, today.plusDays(WINDOW_DAYS - 1));
        } catch (Exception e) {
            // 刷新失败不影响主流程，下次启动/次日定时会再补
            log.warn("排班滚动刷新（{}）失败：{}", trigger, e.getMessage());
        }
    }
}