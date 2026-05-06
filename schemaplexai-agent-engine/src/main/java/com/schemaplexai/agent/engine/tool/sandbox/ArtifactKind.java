package com.schemaplexai.agent.engine.tool.sandbox;

/**
 * 沙箱会话产出物的种类。
 */
public enum ArtifactKind {

    /** 普通文件（写入或被命令产出） */
    FILE,

    /** 命令日志 */
    LOG,

    /** 会话快照（rehydration 时用） */
    SNAPSHOT
}
