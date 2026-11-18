package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysSchedule;
import com.neuedu.mapper.SysScheduleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class SysScheduleService extends ServiceImpl<SysScheduleMapper, SysSchedule> {

    /** 默认号源：上午 30 / 下午 20，与新增医生时的自动排班口径一致 */
    private static final int MORNING_QUOTA = 30;
    private static final int AFTERNOON_QUOTA = 20;

    public Page<SysSchedule> list(Integer pageNo, Integer pageSize, Long doctorId, Long deptId, String scheduleDate) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (doctorId != null) {
            wrapper.eq("doctor_id", doctorId);
        }
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        if (StringUtils.isNotBlank(scheduleDate)) {
            wrapper.eq("schedule_date", LocalDate.parse(scheduleDate.trim()));
        }
        wrapper.orderBy("schedule_date", false).orderBy("id", false);
        return this.page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
    }

    /**
     * 为医生幂等补齐 [from, from+days) 的默认排班（上午 30 / 下午 20）。
     * 同一医生同一天同一时段已有排班则跳过，只补缺失号源，不覆盖手工排班与已约数。
     *
     * @return 本次补建的排班条数
     */
    public int ensureDefaultSchedules(Long doctorId, Long deptId, LocalDate from, int days) {
        int created = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            created += saveIfAbsent(doctorId, deptId, date, "1", MORNING_QUOTA);
            created += saveIfAbsent(doctorId, deptId, date, "2", AFTERNOON_QUOTA);
        }
        return created;
    }

    private int saveIfAbsent(Long doctorId, Long deptId, LocalDate date, String period, int quota) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("doctor_id", doctorId)
                .eq("schedule_date", date)
                .eq("reg_period", period);
        if (this.count(wrapper) > 0) {
            return 0;
        }
        SysSchedule schedule = new SysSchedule();
        schedule.setDoctorId(doctorId);
        schedule.setDeptId(deptId);
        schedule.setScheduleDate(date);
        schedule.setRegPeriod(period);
        schedule.setQuota(quota);
        schedule.setEnabled(true);
        schedule.setCreateDate(Date.valueOf(LocalDate.now()));
        schedule.setCreateTime(Time.valueOf(LocalTime.now()));
        this.save(schedule);
        return 1;
    }
}