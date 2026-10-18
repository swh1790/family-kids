SET search_path TO family_kids;

-- ============================================================
-- 默认种子数据
-- 说明：如果已经存在则跳过（ON CONFLICT DO NOTHING）
-- ============================================================

-- 孩子（两个男孩：四年级 / 一年级）
INSERT INTO child (id, name, nickname, grade, avatar, color, growth_points, balance_points, earned_points, deducted_points, highest_level, active)
VALUES
    (1, '宋子凡', '大宝', '四年级', '👦', '#0d6efd', 0, 0, 0, 0, 1, TRUE),
    (2, '宋子赫', '小宝', '一年级', '👶', '#198754', 0, 0, 0, 0, 1, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 灵宠
INSERT INTO pet (child_id, name, species, avatar, level, stage, exp, mood)
VALUES
    (1, '小火龙', '灵龙', '🥚', 1, '灵蛋', 0, 80),
    (2, '小冰狐', '灵狐', '🥚', 1, '灵蛋', 0, 80)
ON CONFLICT (child_id) DO NOTHING;

-- 任务分类（孩子端按此顺序展示：校内→课外→常规学习→家庭活动→生活习惯）
INSERT INTO task_category (id, code, name, icon, color, sort_order, point_hint)
VALUES
    (1, 'SCHOOL',   '校内任务',     '📚', '#0d6efd', 1, '完成校内各科作业'),
    (2, 'TUTOR',    '课外任务',     '🎓', '#6610f2', 2, '补习课与课后巩固'),
    (3, 'HOMEWORK', '常规学习任务', '✏️', '#fd7e14', 3, '额外练习与阅读'),
    (4, 'CHORE',    '家庭活动任务', '🧹', '#198754', 4, '主动参与家务'),
    (5, 'HABIT',    '生活习惯',     '⭐', '#dc3545', 5, '生活好习惯')
ON CONFLICT (id) DO NOTHING;

-- 任务模板（child_id 为空表示两个孩子通用；仅在表为空时初始化，避免重复执行产生重复数据）
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM task_template) = 0 THEN
    INSERT INTO task_template (category_id, child_id, title, description, default_points, frequency, weekdays, require_approval, active, sort_order)
    VALUES
    (1, NULL, '完成当天校内作业', '语文、数学、英语等校内作业全部完成', 5, 'DAILY', '1,2,3,4,5', TRUE, TRUE, 1),
    (1, NULL, '作业订正完成', '把老师批改的错误重新订正', 3, 'DAILY', '1,2,3,4,5', TRUE, TRUE, 2),
    (1, NULL, '朗读/背诵打卡', '语文课文朗读或英语单词背诵', 2, 'DAILY', '1,2,3,4,5', FALSE, TRUE, 3),

    (2, NULL, '按时上补习课', '准时到达并认真听课', 5, 'WEEKLY', '6,7', TRUE, TRUE, 10),
    (2, NULL, '补习课作业完成', '课后作业当天完成', 4, 'WEEKLY', '1,2,3,4,5,6,7', TRUE, TRUE, 11),
    (2, NULL, '课后复习15分钟', '上完课后自主复习', 3, 'WEEKLY', '1,2,3,4,5,6,7', FALSE, TRUE, 12),

    (3, NULL, '练字/口算一页', '每天一页练字或口算练习', 3, 'DAILY', '1,2,3,4,5', TRUE, TRUE, 20),
    (3, NULL, '课外阅读30分钟', '阅读课外书并记录好词好句', 4, 'DAILY', '1,2,3,4,5,6,7', FALSE, TRUE, 21),
    (3, NULL, '英语打卡', '读英语绘本/单词打卡', 4, 'DAILY', '1,2,3,4,5,6,7', FALSE, TRUE, 22),

    (4, NULL, '洗碗', '晚饭吃完后把碗洗干净', 5, 'DAILY', '1,2,3,4,5,6,7', TRUE, TRUE, 30),
    (4, NULL, '扫地/拖地', '保持客厅/房间地面干净', 6, 'WEEKLY', '6,7', TRUE, TRUE, 31),
    (4, NULL, '倒垃圾', '主动清理并倒掉垃圾', 3, 'WEEKLY', '1,3,5,7', TRUE, TRUE, 32),
    (4, NULL, '整理自己房间', '床铺、书桌、玩具归位', 6, 'DAILY', '1,2,3,4,5,6,7', TRUE, TRUE, 33),
    (4, NULL, '晾/收衣服', '帮忙把衣服晾好或收好', 5, 'WEEKLY', '2,4,6', TRUE, TRUE, 34),
    (4, NULL, '擦桌子', '饭后擦餐桌和茶几', 3, 'DAILY', '1,2,3,4,5,6,7', FALSE, TRUE, 35),

    (5, NULL, '按时起床', '闹钟响后自己起床穿衣', 3, 'DAILY', '1,2,3,4,5', FALSE, TRUE, 40),
    (5, NULL, '按时睡觉', '到点主动上床睡觉', 3, 'DAILY', '1,2,3,4,5,6,7', FALSE, TRUE, 41),
    (5, NULL, '自己整理书包', '睡前自己收拾好书包和文具', 3, 'DAILY', '1,2,3,4,5', FALSE, TRUE, 42),
    (5, NULL, '吃饭不挑食', '光盘、不挑食、不剩饭', 2, 'DAILY', '1,2,3,4,5,6,7', FALSE, TRUE, 43);
  END IF;
END $$;

-- 坏习惯/扣分项
INSERT INTO habit_item (id, name, points, icon, active, sort_order)
VALUES
    (1, '拖延磨蹭', -3, '🐌', TRUE, 1),
    (2, '顶嘴/发脾气', -5, '😤', TRUE, 2),
    (3, '作业漏做/敷衍', -5, '📝', TRUE, 3),
    (4, '电子产品超时', -5, '📱', TRUE, 4),
    (5, '房间脏乱不收拾', -3, '🧺', TRUE, 5),
    (6, '睡前吵闹', -3, '🌙', TRUE, 6),
    (7, '挑食浪费', -2, '🍚', TRUE, 7)
ON CONFLICT (id) DO NOTHING;

-- 灵宠等级配置
INSERT INTO pet_level (level, stage_name, min_exp, icon, reward_title, reward_desc)
VALUES
    (1, '灵蛋',     0,   '🥚',  '孵化成功', '小灵宠破壳而出'),
    (2, '幼崽 I',   60,  '🐣',  '动画片时间', '周末多看 30 分钟动画片'),
    (3, '幼崽 II',  150, '🐥',  '我选晚餐', '今晚晚饭菜式由你挑'),
    (4, '少年 I',   280, '🐲',  '周末半日游', '一次骑行/公园半日游'),
    (5, '少年 II',  450, '🦊',  '小惊喜', '一个小盲盒或小玩具'),
    (6, '成长期 I', 680, '🐯',  '电影之夜', '家庭电影之夜，由你选片'),
    (7, '成长期 II',980, '🦁',  '游乐场/科技馆', '任选游乐场或科技馆一日'),
    (8, '成熟期 I', 1350,'🐉',  '书与美食', '一本心仪课外书 + 零食自由一次'),
    (9, '成熟期 II',1800,'🦄',  '短途自驾', '周边两日自驾游'),
    (10,'觉醒',     2400,'🔥',  '心愿大奖', '主题乐园心愿大奖（迪士尼/长隆）')
ON CONFLICT (level) DO NOTHING;

-- 奖励商城默认目录
INSERT INTO reward_item (id, name, description, icon, cost_points, stock, active, sort_order)
VALUES
    (1, '挑一次晚餐菜', '今晚吃什么由你决定', '🍽️', 30, -1, TRUE, 1),
    (2, '30 分钟动画片', '额外看一集动画片', '📺', 50, -1, TRUE, 2),
    (3, '小零食一份', '任选一包健康小零食', '🍿', 80, -1, TRUE, 3),
    (4, '周末多玩 1 小时', '周末外出/游戏时间 +1 小时', '⏰', 120, -1, TRUE, 4),
    (5, '买一本课外书', '挑一本自己喜欢的书', '📖', 200, -1, TRUE, 5),
    (6, '玩具/盲盒', '一个小玩具或盲盒', '🎁', 300, 10, TRUE, 6),
    (7, '游乐场半日', '去一次游乐园/淘气堡', '🎡', 500, 4, TRUE, 7),
    (8, '短途旅行', '一次周边短途旅行', '🚗', 800, 2, TRUE, 8)
ON CONFLICT (id) DO NOTHING;

-- 修正自增序列（因为上面显式插入了 id，避免后续新增记录主键冲突）
SELECT setval(pg_get_serial_sequence('child', 'id'), COALESCE((SELECT MAX(id) FROM child), 1));
SELECT setval(pg_get_serial_sequence('task_category', 'id'), COALESCE((SELECT MAX(id) FROM task_category), 1));
SELECT setval(pg_get_serial_sequence('habit_item', 'id'), COALESCE((SELECT MAX(id) FROM habit_item), 1));
SELECT setval(pg_get_serial_sequence('reward_item', 'id'), COALESCE((SELECT MAX(id) FROM reward_item), 1));
SELECT setval(pg_get_serial_sequence('task_template', 'id'), COALESCE((SELECT MAX(id) FROM task_template), 1));

-- 初始真实头像（宋子凡 id=1 / 宋子赫 id=2），仅当字段为空时写入
UPDATE child SET avatar_url = '/uploads/avatars/avatar-1.jpg' WHERE id = 1 AND COALESCE(avatar_url, '') = '';
UPDATE child SET avatar_url = '/uploads/avatars/avatar-2.jpg' WHERE id = 2 AND COALESCE(avatar_url, '') = '';

-- 登录账号：1 个家长 + 2 个孩子（默认密码均为 123456，登录后建议修改）
INSERT INTO family_user (id, username, password_hash, role, child_id, display_name, active)
VALUES
    (1, 'parent', 'PBKDF2$120000$FXIIBMogR6dHUkkXOpxrsQ==$Ssn0g5z3c9MhTsthgIsvk43cjDQ4ddyF4KIn8p+yeEg=', 'PARENT', NULL, '爸爸', TRUE),
    (2, 'zifan',  'PBKDF2$120000$W1fAEFqWH66AOrx/czDdqA==$xmleSYXKLvt648GcWJaYIpQLVa/2rrSn4w4rFG+y2Gc=', 'CHILD',  1,     '宋子凡', TRUE),
    (3, 'zihe',   'PBKDF2$120000$NnrO6VT5myEuaqBl+eiZsg==$dtcW9P9IznAjoBQmjprm+ZXWKkOPaQ/a4ZcIXG3WFsk=', 'CHILD',  2,     '宋子赫', TRUE)
ON CONFLICT (username) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        display_name  = EXCLUDED.display_name,
        child_id      = EXCLUDED.child_id,
        updated_at    = NOW();

SELECT setval(pg_get_serial_sequence('family_user', 'id'), COALESCE((SELECT MAX(id) FROM family_user), 1));
