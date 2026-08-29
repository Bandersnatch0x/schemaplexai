package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec")
public class SfSpec extends BaseEntity {

    private String title;
    private String type;

    /**
     * Spec lifecycle status, stored as VARCHAR(32) in sf_spec (DDL default 'draft').
     * Unified vocabulary: draft / in_review / approved / published / archived.
     */
    private String status;
    private String content;

    /**
     * Optimistic-lock revision counter (sf_spec.version, DDL default 1).
     * Mapped with {@link Version} so every {@code updateById} carries a
     * {@code WHERE version = ?} guard and auto-increments the column — a
     * concurrent writer that got there first makes this update affect 0 rows
     * (spec-management §7 "并发编辑 → 乐观锁", REQ-21).
     */
    @Version
    private Integer version;
}
