-- ============================================================
-- 家庭小孩任务与灵宠成长管理系统 - PostgreSQL 表结构
-- Schema: family_kids
-- 用法：在已存在的 ontobricks 数据库中执行，自动创建 schema 与表
-- ============================================================

CREATE SCHEMA IF NOT EXISTS family_kids;
SET search_path TO family_kids;

-- 孩子
CREATE TABLE IF NOT EXISTS child (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    nickname        VARCHAR(50),
    grade           VARCHAR(50),
    avatar          VARCHAR(20) DEFAULT '👦',
    color           VARCHAR(20) DEFAULT '#0d6efd',
    growth_points   INT NOT NULL DEFAULT 0 CHECK (growth_points >= 0),
    balance_points  INT NOT NULL DEFAULT 0 CHECK (balance_points >= 0),
    earned_points   INT NOT NULL DEFAULT 0 CHECK (earned_points >= 0),
    deducted_points INT NOT NULL DEFAULT 0 CHECK (deducted_points >= 0),
    highest_level   INT NOT NULL DEFAULT 1 CHECK (highest_level >= 1),
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE child IS '孩子信息，积分与成长值';

-- 灵宠
CREATE TABLE IF NOT EXISTS pet (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT NOT NULL UNIQUE REFERENCES child(id) ON DELETE CASCADE,
    name            VARCHAR(50) DEFAULT '小灵宠',
    species         VARCHAR(50) DEFAULT '灵龙',
    avatar          VARCHAR(20) DEFAULT '🥚',
    level           INT NOT NULL DEFAULT 1,
    stage           VARCHAR(50) DEFAULT '灵蛋',
    exp             INT NOT NULL DEFAULT 0,
    mood            INT NOT NULL DEFAULT 80 CHECK (mood BETWEEN 0 AND 100),
    last_level_up_at TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE pet IS '每个孩子的灵宠';

-- 灵宠等级配置
CREATE TABLE IF NOT EXISTS pet_level (
    level           INT PRIMARY KEY,
    stage_name      VARCHAR(50) NOT NULL,
    min_exp         INT NOT NULL,
    icon            VARCHAR(20),
    reward_title    VARCHAR(100),
    reward_desc     TEXT
);

COMMENT ON TABLE pet_level IS '灵宠等级、阶段、奖励配置';

-- 任务分类
CREATE TABLE IF NOT EXISTS task_category (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL,
    icon        VARCHAR(20),
    color       VARCHAR(20),
    sort_order  INT DEFAULT 0,
    point_hint  VARCHAR(100)
);

COMMENT ON TABLE task_category IS '任务分类：校内作业、校外补习、家庭作业、家务劳动、习惯表现';

-- 任务模板
CREATE TABLE IF NOT EXISTS task_template (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT REFERENCES child(id) ON DELETE SET NULL,
    category_id     INT NOT NULL REFERENCES task_category(id),
    title           VARCHAR(100) NOT NULL,
    description     TEXT,
    default_points  INT NOT NULL DEFAULT 5,
    frequency       VARCHAR(20) DEFAULT 'DAILY' CHECK (frequency IN ('DAILY','WEEKLY','ONCE')),
    weekdays        VARCHAR(30),
    require_approval BOOLEAN DEFAULT TRUE,
    active          BOOLEAN DEFAULT TRUE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE task_template IS '任务模板；child_id 为空表示两个孩子通用';

-- 任务实例（今日任务）
CREATE TABLE IF NOT EXISTS task_instance (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT NOT NULL REFERENCES child(id) ON DELETE CASCADE,
    template_id     BIGINT REFERENCES task_template(id) ON DELETE SET NULL,
    category_id     INT NOT NULL REFERENCES task_category(id),
    title           VARCHAR(100) NOT NULL,
    points          INT NOT NULL,
    plan_date       DATE NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','DONE','APPROVED','REJECTED','MISSED')),
    source          VARCHAR(20) DEFAULT 'AUTO' CHECK (source IN ('AUTO','MANUAL')),
    submitted_at    TIMESTAMP,
    approved_at     TIMESTAMP,
    approver        VARCHAR(50),
    remark          TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (child_id, template_id, plan_date)
);

COMMENT ON TABLE task_instance IS '每日具体任务实例';
CREATE INDEX IF NOT EXISTS idx_task_instance_date ON task_instance(child_id, plan_date);
CREATE INDEX IF NOT EXISTS idx_task_instance_status ON task_instance(status);

-- 坏习惯/扣分项
CREATE TABLE IF NOT EXISTS habit_item (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    points      INT NOT NULL,
    icon        VARCHAR(20),
    active      BOOLEAN DEFAULT TRUE,
    sort_order  INT DEFAULT 0
);

COMMENT ON TABLE habit_item IS '坏习惯扣分项，points 为负数';

-- 积分流水
CREATE TABLE IF NOT EXISTS point_record (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT NOT NULL REFERENCES child(id) ON DELETE CASCADE,
    change_points   INT NOT NULL,
    balance_after   INT NOT NULL,
    growth_after    INT NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('EARN','DEDUCT','EXCHANGE','ADJUST')),
    source          VARCHAR(30) NOT NULL CHECK (source IN ('TASK','HABIT','MANUAL','REWARD','LEVELUP')),
    ref_id          BIGINT,
    reason          VARCHAR(255),
    operator        VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE point_record IS '积分与成长值变动流水';
CREATE INDEX IF NOT EXISTS idx_point_child_time ON point_record(child_id, created_at);

-- 奖励目录
CREATE TABLE IF NOT EXISTS reward_item (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    icon            VARCHAR(20),
    cost_points     INT NOT NULL,
    stock           INT DEFAULT -1,
    active          BOOLEAN DEFAULT TRUE,
    sort_order      INT DEFAULT 0
);

COMMENT ON TABLE reward_item IS '奖励商城目录；stock=-1 表示不限';

-- 奖励兑换记录
CREATE TABLE IF NOT EXISTS reward_claim (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT NOT NULL REFERENCES child(id) ON DELETE CASCADE,
    reward_id       BIGINT NOT NULL REFERENCES reward_item(id),
    cost_points     INT NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING','GRANTED','REJECTED')),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_at      TIMESTAMP
);

COMMENT ON TABLE reward_claim IS '奖励兑换申请与兑现记录';
CREATE INDEX IF NOT EXISTS idx_claim_child ON reward_claim(child_id);

-- 灵宠大事记
CREATE TABLE IF NOT EXISTS pet_log (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT NOT NULL REFERENCES child(id) ON DELETE CASCADE,
    pet_id      BIGINT REFERENCES pet(id) ON DELETE CASCADE,
    event_type  VARCHAR(30) NOT NULL,
    title       VARCHAR(100) NOT NULL,
    detail      TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE pet_log IS '灵宠升级、心情、事件记录';
CREATE INDEX IF NOT EXISTS idx_petlog_child ON pet_log(child_id, created_at DESC);

-- 孩子真实头像（照片），为空则回退显示 avatar emoji
ALTER TABLE child ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);
COMMENT ON COLUMN child.avatar_url IS '真实头像图片相对URL，如 /uploads/avatars/avatar-1-20261018.jpg；为空则显示 avatar emoji';

-- 登录账号（表名避开 PostgreSQL 保留字 user）
CREATE TABLE IF NOT EXISTS family_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'CHILD' CHECK (role IN ('PARENT','CHILD')),
    child_id      BIGINT REFERENCES child(id) ON DELETE CASCADE,
    display_name  VARCHAR(50)  NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_family_user_username UNIQUE (username),
    CONSTRAINT ck_family_user_role_child CHECK (
        (role = 'PARENT' AND child_id IS NULL) OR
        (role = 'CHILD'  AND child_id IS NOT NULL)
    )
);

COMMENT ON TABLE  family_user               IS '登录账号；父母 role=PARENT 且 child_id 为空，孩子 role=CHILD 且绑定 child.id';
COMMENT ON COLUMN family_user.username      IS '登录名，唯一；父母 parent，孩子用拼音';
COMMENT ON COLUMN family_user.password_hash IS 'PBKDF2-HMAC-SHA256，格式 PBKDF2$<iterations>$<saltB64>$<hashB64>';
COMMENT ON COLUMN family_user.role          IS '角色：PARENT=家长 / CHILD=孩子';
COMMENT ON COLUMN family_user.child_id      IS '孩子账号绑定的 child.id；父母账号为 NULL';
COMMENT ON COLUMN family_user.display_name  IS '导航栏显示名，如 爸爸 / 宋子凡';

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_user_username ON family_user(username);
CREATE INDEX IF NOT EXISTS idx_family_user_child ON family_user(child_id);
