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
 * 字典表 实体类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dict")
public class SysDict implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 字典值 */
    private String dictValue;

    /** 排序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    private Date createDate;
    private Time createTime;
    private Date updateDate;
    private Time updateTime;
}