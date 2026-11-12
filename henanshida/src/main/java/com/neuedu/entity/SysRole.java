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
 * 角色表 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role")
public class SysRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码，唯一 */
    private String roleCode;

    /** 备注 */
    private String remark;

    /** 是否启用 */
    private Boolean enabled;

    private Date createDate;
    private Time createTime;
    private Date updateDate;
    private Time updateTime;
}