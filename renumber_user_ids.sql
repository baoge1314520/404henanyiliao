-- =============================================================
-- 重编 sys_user 主键为连续 1-10，并级联同步 doctor_id
-- 背景：sys_user 现有 id = 1,2,3,4,8,9,10,11,12,13（缺 5,6,7）
--       映射：8->5, 9->6, 10->7, 11->8, 12->9, 13->10（1-4 不变）
--       当前登录"演示医生甲"= id 1，不受影响，不会被踢下线
-- 数据库无外键约束，doctor_id 是逻辑外键，需手动同步：
--       sys_registration.doctor_id（约 28 条）、sys_schedule.doctor_id（无 8-13 引用）
-- 运行方式：在 IDEA 的 Database 控制台里选中 db1 库，整段执行。
--           脚本在一个事务里，中途报错可 ROLLBACK；备份表会保留。
-- =============================================================

BEGIN;

-- ① 备份将被改动的三张表（可重复执行：先删旧备份再建）
DROP TABLE IF EXISTS bak_sys_user_1116;
CREATE TABLE bak_sys_user_1116 AS SELECT * FROM sys_user;

DROP TABLE IF EXISTS bak_sys_registration_1116;
CREATE TABLE bak_sys_registration_1116 AS SELECT * FROM sys_registration;

DROP TABLE IF EXISTS bak_sys_schedule_1116;
CREATE TABLE bak_sys_schedule_1116 AS SELECT * FROM sys_schedule;

-- ② 旧 id -> 新 id 映射表
DROP TABLE IF EXISTS tmp_user_id_map;
CREATE TABLE tmp_user_id_map(old_id int primary key, new_id int);
INSERT INTO tmp_user_id_map VALUES (8,5),(9,6),(10,7),(11,8),(12,9),(13,10);

-- ③ 先级联同步引用列（用旧 id 匹配，趁 sys_user 还没改之前）
UPDATE sys_registration r
   SET doctor_id = m.new_id
  FROM tmp_user_id_map m
 WHERE r.doctor_id = m.old_id;

UPDATE sys_schedule s
   SET doctor_id = m.new_id
  FROM tmp_user_id_map m
 WHERE s.doctor_id = m.old_id;

-- ④ 重编 sys_user 主键：先整体 +1000 腾空低位，再落到连续新值（避免主键撞号）
UPDATE sys_user SET id = id + 1000 WHERE id BETWEEN 8 AND 13;

UPDATE sys_user u
   SET id = m.new_id
  FROM tmp_user_id_map m
 WHERE u.id = m.old_id + 1000;

-- ⑤ 自增序列回到 10，以后新增医生从 11 连续递增，不再跳号
SELECT setval('public.sys_user_id_seq', 10);

-- ⑥ 清理临时映射表
DROP TABLE IF EXISTS tmp_user_id_map;

COMMIT;

-- =============================================================
-- 校验（COMMIT 之后单独逐条执行，看结果是否符合期望）
-- =============================================================
-- 期望：id 连续 1..10
SELECT id, real_name FROM sys_user ORDER BY id;

-- 期望：orphan_reg = 0（没有悬空的 doctor_id）
SELECT count(*) AS orphan_reg
  FROM sys_registration r
 WHERE r.doctor_id NOT IN (SELECT id FROM sys_user);

-- 期望：orphan_sch = 0
SELECT count(*) AS orphan_sch
  FROM sys_schedule s
 WHERE s.doctor_id NOT IN (SELECT id FROM sys_user);

-- 期望：行数不变（挂号 49、排班 6、用户 10）
SELECT count(*) AS reg_cnt FROM sys_registration;
SELECT count(*) AS sch_cnt FROM sys_schedule;
SELECT count(*) AS user_cnt FROM sys_user;

-- 期望：挂号按医生分布，doctor_id 全在 1..10
SELECT doctor_id, count(*) FROM sys_registration GROUP BY doctor_id ORDER BY doctor_id;

-- =============================================================
-- 如需回滚：
--   若还在事务内（COMMIT 前）：直接 ROLLBACK;
--   若已 COMMIT，用备份表整体还原（注意会清空当前数据后重灌）：
--     TRUNCATE sys_user;          INSERT INTO sys_user          SELECT * FROM bak_sys_user_1116;
--     TRUNCATE sys_registration;  INSERT INTO sys_registration  SELECT * FROM bak_sys_registration_1116;
--     TRUNCATE sys_schedule;      INSERT INTO sys_schedule      SELECT * FROM bak_sys_schedule_1116;
--     SELECT setval('public.sys_user_id_seq', 13);
--   验证无误后可删备份表：
--     DROP TABLE bak_sys_user_1116; DROP TABLE bak_sys_registration_1116; DROP TABLE bak_sys_schedule_1116;
-- =============================================================
