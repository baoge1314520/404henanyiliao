package com.neuedu.controller;

import com.neuedu.entity.SysRole;
import com.neuedu.service.SysRoleService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysRole")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String roleName) {
        return Result.success(sysRoleService.list(pageNo, pageSize, roleName));
    }

    @GetMapping("listAll")
    Result listAll() {
        return Result.success(sysRoleService.listAll());
    }

    @PostMapping("add")
    Result add(SysRole sysRole) {
        return Result.success(sysRoleService.save(sysRole), "添加角色成功");
    }

    @PostMapping("update")
    Result update(SysRole sysRole) {
        return Result.success(sysRoleService.updateById(sysRole), "修改角色成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysRole sysRole = SysRole.builder().id(id).enabled(enabled).build();
        return Result.success(sysRoleService.updateById(sysRole), enabled ? "启用角色成功" : "删除角色成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysRoleService.getById(id));
    }
}