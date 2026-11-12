package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysDict;
import com.neuedu.mapper.SysDictMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictService extends ServiceImpl<SysDictMapper, SysDict> {

    public Page<SysDict> list(Integer pageNo, Integer pageSize, String dictType, String dictLabel) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (StringUtils.isNotBlank(dictType)) {
            wrapper.eq("dict_type", dictType.trim());
        }
        if (StringUtils.isNotBlank(dictLabel)) {
            wrapper.like("dict_label", dictLabel.trim());
        }
        wrapper.orderBy("dict_type", true).orderBy("sort", true).orderBy("id", true);
        return this.page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
    }

    public List<SysDict> listByType(String dictType) {
        return this.list(QueryWrapper.create().eq("dict_type", dictType).eq("enabled", true).orderBy("sort", true));
    }
}