package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysSchedule;
import com.neuedu.mapper.SysScheduleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SysScheduleService extends ServiceImpl<SysScheduleMapper, SysSchedule> {

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
}