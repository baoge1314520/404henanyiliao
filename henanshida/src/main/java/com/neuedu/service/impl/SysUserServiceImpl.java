package com.neuedu.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysUser;
import com.neuedu.mapper.SysUserMapper;
import com.neuedu.service.SysScheduleService;
import com.neuedu.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 医护人员表 服务层实现。
 *
 * @author wangyu
 * @since 2026-11-09
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    PasswordEncoder passwordEncoder;
    @Resource
    SysScheduleService sysScheduleService;
    @Override
    public Page<SysUser> list(Integer pageNo, Integer pageSize, String realName) {
        QueryWrapper queryWrapper = new QueryWrapper();
        // 如果 realName 没有值，就不按条件查，认为查询全部
        if (StringUtils.isNotBlank(realName)) {
            queryWrapper.like("real_name", realName.trim());
        }
        queryWrapper.orderBy("id", false);
        return this.page(Page.of(pageNo, pageSize), queryWrapper);
    }

    @Override
    public boolean check(String column, String value, Long id) {
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(column, value);
        if (null != id) {
            wrapper.ne("id", id);
        }
        return this.count(wrapper) == 0;
    }

    @Override
    public boolean add(SysUser sysUser, String password) {
        String encode = passwordEncoder.encode(password);
        sysUser.setPassword(encode);
        // 工号在页面上选填，但库列是 NOT NULL UNIQUE：留空时按注册流程的约定补 U+时间戳，避免插入违反非空约束
        if (StringUtils.isBlank(sysUser.getEmployeeNo())) {
            sysUser.setEmployeeNo("U" + System.currentTimeMillis());
        }
        boolean saved = this.save(sysUser);
        // 新增的是带科室与职称的医生时，自动生成今起 7 天默认排班（上午 30 / 下午 20），
        // 患者端挂号大厅无需再手工排班即可出现该医生的挂号按钮
        if (saved && sysUser.getDeptId() != null && sysUser.getTitleId() != null) {
            try {
                sysScheduleService.ensureDefaultSchedules(sysUser.getId(), sysUser.getDeptId(), LocalDate.now(), 7);
            } catch (Exception e) {
                // 排班生成失败不影响医生本身的新增，可在排班管理里手工补
                log.warn("为新医生 {} 生成默认排班失败：{}", sysUser.getId(), e.getMessage());
            }
        }
        return saved;
    }

    @Override
    public boolean update(SysUser sysUser, String password) {
        if (StringUtils.isBlank(password)) {
            sysUser.setPassword(null);
        }
        return this.updateById(sysUser);
    }

    @Override
    public List<SysUser> getByDept(Long deptId) {
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("dept_id", deptId)
                .eq("enabled", true);
        return this.list(wrapper);
    }
}
