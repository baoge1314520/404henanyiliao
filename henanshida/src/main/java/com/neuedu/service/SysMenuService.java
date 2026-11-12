package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysMenu;
import com.neuedu.mapper.SysMenuMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    public Page<SysMenu> list(Integer pageNo, Integer pageSize, String menuName) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (StringUtils.isNotBlank(menuName)) {
            wrapper.like("menu_name", menuName.trim());
        }
        wrapper.orderBy("parent_id", true).orderBy("sort", true).orderBy("id", true);
        return this.page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
    }

    public List<SysMenu> listAll() {
        return this.list(QueryWrapper.create().eq("enabled", true).orderBy("parent_id", true).orderBy("sort", true));
    }
}