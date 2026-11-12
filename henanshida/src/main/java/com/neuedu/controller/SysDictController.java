package com.neuedu.controller;

import com.neuedu.entity.SysDict;
import com.neuedu.service.SysDictService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysDict")
public class SysDictController {

    @Resource
    private SysDictService sysDictService;

    @GetMapping("list")
    Result list(Integer pageNo, Integer pageSize, String dictType, String dictLabel) {
        return Result.success(sysDictService.list(pageNo, pageSize, dictType, dictLabel));
    }

    @GetMapping("listByType")
    Result listByType(String dictType) {
        return Result.success(sysDictService.listByType(dictType));
    }

    @PostMapping("add")
    Result add(SysDict sysDict) {
        return Result.success(sysDictService.save(sysDict), "添加字典成功");
    }

    @PostMapping("update")
    Result update(SysDict sysDict) {
        return Result.success(sysDictService.updateById(sysDict), "修改字典成功");
    }

    @PostMapping("del")
    Result remove(Long id, Boolean enabled) {
        SysDict sysDict = SysDict.builder().id(id).enabled(enabled).build();
        return Result.success(sysDictService.updateById(sysDict), enabled ? "启用字典成功" : "删除字典成功");
    }

    @GetMapping("getInfo")
    Result getById(Long id) {
        return Result.success(sysDictService.getById(id));
    }
}