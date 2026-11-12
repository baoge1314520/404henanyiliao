package com.neuedu.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排班表 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_schedule")
public class SysSchedule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 医生ID（关联 sys_user.id） */
    private Long doctorId;

    /** 科室ID（关联 sys_dept.id） */
    private Long deptId;

    /** 排班日期 */
    private LocalDate scheduleDate;

    /** 时段: 1-上午 2-下午 */
    private String regPeriod;

    /** 号源数 */
    private Integer quota;

    /** 是否启用 */
    private Boolean enabled;

    private Date createDate;
    private Time createTime;
    private Date updateDate;
    private Time updateTime;
}