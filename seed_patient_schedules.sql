-- ============================================================
-- 患者端「挂号大厅」演示排班（2026-11-17 ~ 2026-11-23，共 7 天）
-- 规则：每个启用科室取其编号最小的启用医生，每天上午出诊（号源 30）；
--       内科系 6 个科室（id 1~6）另加下午出诊（号源 20）。
-- 幂等：同一医生+日期+时段已存在排班则跳过，可重复执行。
-- 在 Rocky 虚拟机 PostgreSQL(db1) 中执行。
-- ============================================================

INSERT INTO sys_schedule (doctor_id, dept_id, schedule_date, reg_period, quota, enabled,
                          create_date, create_time, update_date, update_time)
SELECT d.doc_id, d.id, day::date, p.period, p.quota, true,
       CURRENT_DATE, CURRENT_TIME, CURRENT_DATE, CURRENT_TIME
FROM (
    SELECT dept.id,
           (SELECT u.id FROM sys_user u
             WHERE u.dept_id = dept.id AND u.enabled = true
             ORDER BY u.id LIMIT 1) AS doc_id
    FROM sys_dept dept
    WHERE dept.enabled = true
) d
CROSS JOIN generate_series(DATE '2026-11-17', DATE '2026-11-23', INTERVAL '1 day') AS day
CROSS JOIN (VALUES ('1', 30), ('2', 20)) AS p(period, quota)
WHERE d.doc_id IS NOT NULL
  AND (p.period = '1' OR d.id <= 6)
  AND NOT EXISTS (
      SELECT 1 FROM sys_schedule s
      WHERE s.doctor_id = d.doc_id
        AND s.schedule_date = day::date
        AND s.reg_period = p.period
  );

-- 验证 1：每日排班数与号源总量
SELECT schedule_date, count(*) AS slots, sum(quota) AS total_quota
FROM sys_schedule
WHERE enabled = true AND schedule_date BETWEEN DATE '2026-11-17' AND DATE '2026-11-23'
GROUP BY schedule_date ORDER BY schedule_date;

-- 验证 2：仍无排班的启用科室（期望 0 行）
SELECT dept.id, dept.dept_name
FROM sys_dept dept
WHERE dept.enabled = true
  AND NOT EXISTS (
      SELECT 1 FROM sys_schedule s
      WHERE s.dept_id = dept.id AND s.enabled = true
        AND s.schedule_date BETWEEN DATE '2026-11-17' AND DATE '2026-11-23'
  );