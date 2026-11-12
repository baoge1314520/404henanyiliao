package com.neuedu.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单表 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_menu")
public class SysMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 父菜单ID，0 表示顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 路由路径 */
    private String path;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    private Date createDate;
    private Time createTime;
    private Date updateDate;
    private Time updateTime;
}