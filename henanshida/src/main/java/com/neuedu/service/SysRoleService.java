package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neuedu.entity.SysRole;
import com.neuedu.mapper.SysRoleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    public Page<SysRole> list(Integer pageNo, Integer pageSize, String roleName) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (StringUtils.isNotBlank(roleName)) {
            wrapper.like("role_name", roleName.trim());
        }
        wrapper.orderBy("id", true);
        return this.page(Page.of(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
    }

    public List<SysRole> listAll() {
        return this.list(QueryWrapper.create().eq("enabled", true).orderBy("id", true));
    }
}