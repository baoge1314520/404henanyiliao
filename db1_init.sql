-- ============================================================
-- HIS db1 完整初始化脚本（表结构 + 演示数据）
-- 由运行中的 db1 只读导出；含基础表与全部模块表。
-- 用法：先 CREATE DATABASE db1; 再 psql -d db1 -f db1_init.sql
-- 注意：脚本开头会 DROP 同名表，重复执行会清空重建。
-- 其中业务数据为演示数据（界面已标注），非真实患者信息。
-- ============================================================

-- reset（可重复执行）
DROP TABLE IF EXISTS sys_dept CASCADE;
DROP TABLE IF EXISTS sys_dict CASCADE;
DROP TABLE IF EXISTS sys_menu CASCADE;
DROP TABLE IF EXISTS sys_patient CASCADE;
DROP TABLE IF EXISTS sys_registration CASCADE;
DROP TABLE IF EXISTS sys_role CASCADE;
DROP TABLE IF EXISTS sys_schedule CASCADE;
DROP TABLE IF EXISTS sys_title CASCADE;
DROP TABLE IF EXISTS sys_user CASCADE;
DROP SEQUENCE IF EXISTS sys_dept_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_dict_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_menu_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_patient_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_registration_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_role_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_schedule_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_title_id_seq CASCADE;
DROP SEQUENCE IF EXISTS sys_user_id_seq CASCADE;

-- sequences
CREATE SEQUENCE IF NOT EXISTS sys_dept_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_dict_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_menu_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_patient_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_registration_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_role_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_schedule_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_title_id_seq;
CREATE SEQUENCE IF NOT EXISTS sys_user_id_seq;

-- tables
CREATE TABLE sys_dept (
    id bigint DEFAULT nextval('sys_dept_id_seq'::regclass) NOT NULL,
    dept_name character varying(100) NOT NULL,
    dept_code character varying(50) NOT NULL,
    dept_type character(1) DEFAULT '1'::bpchar NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    UNIQUE (dept_code),
    PRIMARY KEY (id)
);
CREATE TABLE sys_dict (
    id bigint DEFAULT nextval('sys_dict_id_seq'::regclass) NOT NULL,
    dict_type character varying(50) NOT NULL,
    dict_label character varying(50) NOT NULL,
    dict_value character varying(50) NOT NULL,
    sort integer DEFAULT 0 NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE sys_menu (
    id bigint DEFAULT nextval('sys_menu_id_seq'::regclass) NOT NULL,
    parent_id bigint DEFAULT 0 NOT NULL,
    menu_name character varying(50) NOT NULL,
    path character varying(100),
    icon character varying(50),
    sort integer DEFAULT 0 NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE sys_patient (
    id bigint DEFAULT nextval('sys_patient_id_seq'::regclass) NOT NULL,
    patient_name character varying(64) NOT NULL,
    gender character(1) DEFAULT '0'::bpchar NOT NULL,
    birthday date,
    phone character varying(20),
    id_card character varying(32) NOT NULL,
    address character varying(255),
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    UNIQUE (id_card),
    PRIMARY KEY (id)
);
CREATE TABLE sys_registration (
    id bigint DEFAULT nextval('sys_registration_id_seq'::regclass) NOT NULL,
    reg_no character varying(32) NOT NULL,
    patient_id bigint NOT NULL,
    dept_id bigint,
    doctor_id bigint,
    title_id bigint,
    reg_fee numeric(10,2),
    reg_date date NOT NULL,
    reg_period character(1),
    reg_type character(1) DEFAULT '1'::bpchar NOT NULL,
    status character(1) DEFAULT '1'::bpchar NOT NULL,
    remark character varying(255),
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    call_time timestamp without time zone,
    PRIMARY KEY (id),
    UNIQUE (reg_no)
);
CREATE TABLE sys_role (
    id bigint DEFAULT nextval('sys_role_id_seq'::regclass) NOT NULL,
    role_name character varying(50) NOT NULL,
    role_code character varying(50) NOT NULL,
    remark character varying(255),
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (role_code)
);
CREATE TABLE sys_schedule (
    id bigint DEFAULT nextval('sys_schedule_id_seq'::regclass) NOT NULL,
    doctor_id bigint NOT NULL,
    dept_id bigint NOT NULL,
    schedule_date date NOT NULL,
    reg_period character(1) DEFAULT '1'::bpchar NOT NULL,
    quota integer DEFAULT 30 NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE sys_title (
    id bigint DEFAULT nextval('sys_title_id_seq'::regclass) NOT NULL,
    title_name character varying(32) NOT NULL,
    reg_level character varying(32),
    reg_fee numeric(10,2) DEFAULT 0 NOT NULL,
    description character varying(255),
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (title_name)
);
CREATE TABLE sys_user (
    id bigint DEFAULT nextval('sys_user_id_seq'::regclass) NOT NULL,
    real_name character varying(64) NOT NULL,
    password character varying(100) NOT NULL,
    phone character varying(20),
    email character varying(64),
    avatar character varying(255),
    gender character(1) DEFAULT '0'::bpchar NOT NULL,
    dept_id bigint,
    title_id bigint,
    employee_no character varying(32) NOT NULL,
    birthday date,
    address character varying(255),
    intro character varying(500),
    login_time timestamp without time zone,
    pwd_update_time timestamp without time zone,
    enabled boolean DEFAULT true NOT NULL,
    create_date date DEFAULT CURRENT_DATE NOT NULL,
    create_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    update_date date DEFAULT CURRENT_DATE NOT NULL,
    update_time time without time zone DEFAULT CURRENT_TIME NOT NULL,
    UNIQUE (email),
    UNIQUE (employee_no),
    UNIQUE (phone),
    PRIMARY KEY (id)
);

-- data（演示数据）
INSERT INTO sys_dept (id,dept_name,dept_code,dept_type,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'心内科','CARDIO','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(2,'呼吸内科','RESPIRATORY','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(3,'消化内科','GASTRO','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(4,'神经内科','NEURO','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(5,'肾内科','NEPHRO','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(6,'普外科','GEN_SURG','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(7,'骨科','ORTHO','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(8,'神经外科','NEURO_SURG','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(9,'泌尿外科','UROLOGY','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(10,'妇产科','OBGYN','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(11,'儿科','PEDIATRICS','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(12,'急诊科','EMERGENCY','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(13,'ICU','ICU','1',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(14,'检验科','LAB','3',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(15,'放射科','RADIOLOGY','3',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(16,'超声科','ULTRASOUND','3',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(17,'药剂科','PHARMACY','3',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(18,'信息科','IT','2',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(19,'财务科','FINANCE','2',true,'2026-11-02','11:35:29.654882','2026-11-02','11:35:29.654882'),
(20,'心理科','10086','3',true,'2026-11-11','15:12:41.361309','2026-11-11','15:12:41.361309'),
(21,'皮肤科','222','1',true,'2026-11-11','16:51:04.248691','2026-11-11','16:51:04.248691');
INSERT INTO sys_dict (id,dict_type,dict_label,dict_value,sort,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'sys_gender','未知','0',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(2,'sys_gender','男','1',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(3,'sys_gender','女','2',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(4,'sys_dept_type','临床科室','1',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(5,'sys_dept_type','行政科室','2',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(6,'sys_dept_type','医技科室','3',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(7,'reg_period','上午','1',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(8,'reg_period','下午','2',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(9,'reg_type','普通挂号','1',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(10,'reg_type','预约挂号','2',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(11,'reg_status','待就诊','1',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(12,'reg_status','已就诊','2',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(13,'reg_status','已取消','3',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802');
INSERT INTO sys_menu (id,parent_id,menu_name,path,icon,sort,enabled,create_date,create_time,update_date,update_time) VALUES
(1,0,'系统管理',NULL,'Setting',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(2,0,'挂号就诊',NULL,'FirstAidKit',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(3,0,'AI 问答','/ai','ChatDotRound',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(4,1,'科室管理','/dept','OfficeBuilding',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(5,1,'医生管理','/doctor','User',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(6,1,'职称管理','/title','Medal',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(7,1,'角色管理','/role','Avatar',4,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(8,1,'菜单管理','/menu','Menu',5,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(9,1,'字典管理','/dict','Collection',6,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(10,2,'挂号管理','/registration','Ticket',1,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(11,2,'患者管理','/patient','UserFilled',2,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(12,2,'排班管理','/schedule','Calendar',3,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(13,2,'叫号队列','/queue','Bell',4,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(14,2,'分诊管理','/triage','Guide',5,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802');
INSERT INTO sys_patient (id,patient_name,gender,birthday,phone,id_card,address,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'演示患者甲','1','1992-05-10','13900000001','DEMO0000000000000001','演示地址-郑州',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(2,'演示患者乙','2','1985-11-02','13900000002','DEMO0000000000000002','演示地址-开封',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(3,'演示患者丙','1','2015-06-18','13900000003','DEMO0000000000000003','演示地址-新乡',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(4,'演示患者丁','1',NULL,'13900000004','DEMO0000000000000004',NULL,true,'2026-11-10','17:42:52.558836','2026-11-10','17:42:52.558836');
INSERT INTO sys_registration (id,reg_no,patient_id,dept_id,doctor_id,title_id,reg_fee,reg_date,reg_period,reg_type,status,remark,create_date,create_time,update_date,update_time,call_time) VALUES
(1,'REGDEMO0000000000001',1,1,1,1,50.00,'2026-11-10','1','1','1','演示数据','2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068',NULL),
(2,'REGDEMO0000000000002',2,2,1,1,50.00,'2026-11-10','2','2','1','演示数据','2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068','2026-11-12 09:57:19.933513'),
(3,'REGDEMO0000000000003',3,6,3,3,20.00,'2026-11-10','1','1','2','演示数据，已就诊','2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068',NULL),
(4,'REG20261110174252822',4,1,1,1,50.00,'2026-11-10','1','1','3',NULL,'2026-11-10','17:42:52.558836','2026-11-10','17:42:52.558836',NULL);
INSERT INTO sys_role (id,role_name,role_code,remark,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'管理员','admin','演示数据：系统管理员',true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(2,'医生','doctor','演示数据：门诊医生',true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(3,'挂号员','registrar','演示数据：挂号收费',true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(4,'护士','nurse','演示数据：分诊护士',true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802');
INSERT INTO sys_schedule (id,doctor_id,dept_id,schedule_date,reg_period,quota,enabled,create_date,create_time,update_date,update_time) VALUES
(1,1,1,'2026-11-12','1',30,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(2,1,1,'2026-11-12','2',20,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(3,2,2,'2026-11-12','1',25,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(4,3,6,'2026-11-12','1',25,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(5,4,1,'2026-11-13','1',30,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802'),
(6,2,2,'2026-11-13','2',20,true,'2026-11-12','08:50:31.535802','2026-11-12','08:50:31.535802');
INSERT INTO sys_title (id,title_name,reg_level,reg_fee,description,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'主任医师','专家号',50.00,'科室带头人，可出专家门诊',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(2,'副主任医师','副主任医师号',30.00,'副高级',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(3,'主治医师','主治医师号',20.00,'中级',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(4,'住院医师','普通号',10.00,'初级',true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068');
INSERT INTO sys_user (id,real_name,password,phone,email,avatar,gender,dept_id,title_id,employee_no,birthday,address,intro,login_time,pwd_update_time,enabled,create_date,create_time,update_date,update_time) VALUES
(1,'演示医生甲','$2a$10$o6C4m1yWE7D9MsFbC96naubnnPQCCxuWGxjgVvUtiqB2ZBx.PIo3W','13800000001','demo1@example.com',NULL,'1',1,1,'D0001','1978-03-01','演示地址-河南','擅长冠心病、高血压的诊治','2026-11-13 10:11:37.13969',NULL,true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(2,'演示医生乙','$2a$10$VSBwTb6sEOew6AdZ8MJkC.6.XXcMd5W5oztK.aUvzR.RApQzw5A2a','13800000002','demo2@example.com',NULL,'2',2,2,'D0002','1983-07-15','演示地址-河南','擅长慢阻肺、哮喘的诊治','2026-11-12 17:23:44.651261',NULL,true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(3,'演示医生丙','$2a$10$VSBwTb6sEOew6AdZ8MJkC.6.XXcMd5W5oztK.aUvzR.RApQzw5A2a','13800000003','demo3@example.com',NULL,'1',6,3,'D0003','1990-01-20','演示地址-河南','擅长腹部常见外科手术','2026-11-12 17:23:45.070989',NULL,true,'2026-11-10','17:41:32.819068','2026-11-10','17:41:32.819068'),
(4,'演示医生丁','$2a$10$p9STm2/kAtzp.E.0uSANOeQ9m9QH6fejjEtoLGy2Ml9xfOuRCfkX6','13800000004',NULL,NULL,'1',1,4,'D0004',NULL,NULL,NULL,NULL,NULL,true,'2026-11-10','17:42:52.448719','2026-11-10','17:42:52.448719');

-- sequences current values
SELECT setval('sys_dept_id_seq', COALESCE((SELECT MAX(id) FROM sys_dept),1), true);
SELECT setval('sys_dict_id_seq', COALESCE((SELECT MAX(id) FROM sys_dict),1), true);
SELECT setval('sys_menu_id_seq', COALESCE((SELECT MAX(id) FROM sys_menu),1), true);
SELECT setval('sys_patient_id_seq', COALESCE((SELECT MAX(id) FROM sys_patient),1), true);
SELECT setval('sys_registration_id_seq', COALESCE((SELECT MAX(id) FROM sys_registration),1), true);
SELECT setval('sys_role_id_seq', COALESCE((SELECT MAX(id) FROM sys_role),1), true);
SELECT setval('sys_schedule_id_seq', COALESCE((SELECT MAX(id) FROM sys_schedule),1), true);
SELECT setval('sys_title_id_seq', COALESCE((SELECT MAX(id) FROM sys_title),1), true);
SELECT setval('sys_user_id_seq', COALESCE((SELECT MAX(id) FROM sys_user),1), true);

-- indexes
CREATE INDEX idx_registration_patient ON public.sys_registration USING btree (patient_id);
CREATE INDEX idx_registration_dept ON public.sys_registration USING btree (dept_id);
CREATE INDEX idx_registration_doctor ON public.sys_registration USING btree (doctor_id);
CREATE INDEX idx_user_dept ON public.sys_user USING btree (dept_id);
