package com.family.kids.util;

/**
 * 常量定义
 */
public final class Constants {

    private Constants() {}

    // 任务实例状态
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_DONE = "DONE";
    public static final String TASK_APPROVED = "APPROVED";
    public static final String TASK_REJECTED = "REJECTED";
    public static final String TASK_MISSED = "MISSED";

    // 任务来源
    public static final String SOURCE_AUTO = "AUTO";
    public static final String SOURCE_MANUAL = "MANUAL";

    // 积分类型
    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_DEDUCT = "DEDUCT";
    public static final String TYPE_EXCHANGE = "EXCHANGE";
    public static final String TYPE_ADJUST = "ADJUST";

    // 积分来源
    public static final String SRC_TASK = "TASK";
    public static final String SRC_HABIT = "HABIT";
    public static final String SRC_MANUAL = "MANUAL";
    public static final String SRC_REWARD = "REWARD";
    public static final String SRC_LEVELUP = "LEVELUP";

    // 兑换状态
    public static final String CLAIM_PENDING = "PENDING";
    public static final String CLAIM_GRANTED = "GRANTED";
    public static final String CLAIM_REJECTED = "REJECTED";

    // 灵宠事件
    public static final String EVT_LEVEL_UP = "LEVEL_UP";
    public static final String EVT_MOOD_GOOD = "MOOD_GOOD";
    public static final String EVT_MOOD_BAD = "MOOD_BAD";
    public static final String EVT_HABIT_DEDUCT = "HABIT_DEDUCT";
    public static final String EVT_TASK_DONE = "TASK_DONE";

    // 登录会话
    public static final String SESSION_LOGIN_USER = "loginUser";
    public static final String SESSION_AUTH_ERROR = "authError";
    public static final String SESSION_REDIRECT_AFTER_LOGIN = "redirectAfterLogin";

    // 角色
    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_CHILD = "CHILD";
}
