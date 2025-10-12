package com.stu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.io.Serial;

@Data
@TableName("category")
@Schema(description = "分类实体类，存储垃圾回收物品的分类信息（如可回收物、有害垃圾等）")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID", example = "1")
    private Long id;

    @TableField("name")
    @Schema(description = "分类名称", example = "可回收物")
    private String name;

    @TableField("parent_id")
    @Schema(description = "父分类ID（0表示一级分类）", example = "0")
    private Long parentId;


    @TableField("price_per_kg")
    @Schema(description = "每公斤回收价格（元）", example = "2.5")
    private BigDecimal pricePerKg;

    @TableField("sort")
    @Schema(description = "排序权重（数字越小越靠前）", example = "10")
    private Integer sort;


    @TableField("description")
    @Schema(description = "分类描述", example = "包括纸张、塑料、玻璃等可循环利用的物品")
    private String description;

    @TableField("status")
    @Schema(description = "状态（0-禁用，1-启用）", example = "1")
    private Integer status;

    @TableField("created_at")
    @Schema(description = "创建时间", example = "2024-04-01 09:00:00")
    private Date createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间", example = "2024-04-02 14:30:00")
    private Date updatedAt;
}