-- =====================================================================
-- 补齐「无医生科室」：保证每个启用科室至少有一名启用医生（演示数据）
-- 背景：挂号登记选择科室后按 dept_id 过滤医生，若该科室没有医生，
--       「就诊医生」下拉为空且必填校验失败（如 心理科）。
-- 本脚本只为「当前没有启用医生」的启用科室各补一名演示医生，可重复执行不会重复插入。
-- 密码复用 id=1（医生甲）的 BCrypt 哈希，演示登录口令与现有账号一致。
-- 运行方式见交付说明（Windows 用 py+pg8000，或虚拟机 Docker psql）。
-- =====================================================================

WITH uncovered AS (
    SELECT d.id, d.dept_name
    FROM sys_dept d
    WHERE d.enabled = true
      AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.dept_id = d.id AND u.enabled = true)
),
mapping(dept_id, real_name, title_id, gender) AS (
    VALUES (5,'孙立群',3,'1'), (8,'周正雄',2,'2'), (9,'吴建国',3,'1'), (13,'冯守义',2,'2'),
           (14,'许文婷',4,'1'), (15,'何晓东',4,'2'), (16,'吕秀英',4,'1'), (17,'沈玉梅',4,'2'),
           (18,'韩志远',4,'1'), (19,'曹慧敏',4,'2'), (20,'苏婉清',3,'1'), (21,'邓丽华',3,'2')
),
base AS (
    SELECT COALESCE(MAX(CAST(SUBSTRING(employee_no FROM 2) AS INTEGER)), 0) AS m
    FROM sys_user WHERE employee_no LIKE 'D%'
),
pw AS (SELECT password FROM sys_user WHERE id = 1)
INSERT INTO sys_user (real_name, password, phone, gender, dept_id, title_id, employee_no,
                      address, intro, enabled, create_date, create_time, update_date, update_time)
SELECT COALESCE(m.real_name, u.dept_name || '医师'),
       pw.password,
       '138' || LPAD((10 + ROW_NUMBER() OVER (ORDER BY u.id))::text, 8, '0'),
       COALESCE(m.gender, '1'),
       u.id,
       COALESCE(m.title_id, 3),
       'D' || LPAD((base.m + ROW_NUMBER() OVER (ORDER BY u.id))::text, 4, '0'),
       '河南省郑州市',
       CASE WHEN u.id IN (5,8,9,13,20,21)
            THEN '擅长' || u.dept_name || '常见病、多发病的诊治。'
            ELSE '负责' || u.dept_name || '日常业务与技术支持。' END,
       true, CURRENT_DATE, CURRENT_TIME, CURRENT_DATE, CURRENT_TIME
FROM uncovered u
LEFT JOIN mapping m ON m.dept_id = u.id
CROSS JOIN base
CROSS JOIN pw;

-- 验证：每个启用科室的启用医生数都应 >= 1
SELECT d.id, d.dept_name, COUNT(u.id) FILTER (WHERE u.enabled = true) AS on_docs
FROM sys_dept d LEFT JOIN sys_user u ON u.dept_id = d.id
WHERE d.enabled = true
GROUP BY d.id, d.dept_name
ORDER BY d.id;