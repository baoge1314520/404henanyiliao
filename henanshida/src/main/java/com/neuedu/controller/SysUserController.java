package com.neuedu.controller;

import com.mybatisflex.core.paginate.Page;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import com.neuedu.entity.SysUser;
import com.neuedu.service.SysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医护人员表 控制层。
 *
 * @author wangyu
 * @since 2026-11-09
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String realName) {
        return Result.success(sysUserService.list(pageNo, pageSize, realName));
    }

    @PostMapping("add")
    Result add(SysUser sysUser, String password) {
        return Result.success(sysUserService.add(sysUser, password), "添加医护人员成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysUser sysUser = SysUser.builder()
                .id(id)
                .enabled(enabled)
                .build();
        return Result.success(sysUserService.updateById(sysUser), enabled ? "启用医护人员成功" : "删除医护人员成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysUserService.getById(id));
    }

    @PostMapping("update")
    Result update(SysUser sysUser, String password) {
        return Result.success(sysUserService.update(sysUser, password), "修改医护人员成功");
    }

    @GetMapping("check")
    Result check(String column, String value, Long id) {
        return Result.success(sysUserService.check(column, value, id));
    }
    @GetMapping("getByDept")
    Result getByDept(Long deptId) {
        return Result.success(sysUserService.getByDept(deptId));
    }
}
