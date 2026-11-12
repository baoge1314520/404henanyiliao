package com.neuedu.controller;

import com.neuedu.entity.SysMenu;
import com.neuedu.service.SysMenuService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysMenu")
public class SysMenuController {

    @Resource
    private SysMenuService sysMenuService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String menuName) {
        return Result.success(sysMenuService.list(pageNo, pageSize, menuName));
    }

    @GetMapping("listAll")
    Result listAll() {
        return Result.success(sysMenuService.listAll());
    }

    @PostMapping("add")
    Result add(SysMenu sysMenu) {
        return Result.success(sysMenuService.save(sysMenu), "添加菜单成功");
    }

    @PostMapping("update")
    Result update(SysMenu sysMenu) {
        return Result.success(sysMenuService.updateById(sysMenu), "修改菜单成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysMenu sysMenu = SysMenu.builder().id(id).enabled(enabled).build();
        return Result.success(sysMenuService.updateById(sysMenu), enabled ? "启用菜单成功" : "删除菜单成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysMenuService.getById(id));
    }
}