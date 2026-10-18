# 家庭小孩任务与灵宠成长管理系统

一套面向家庭的任务激励系统：把孩子的校内作业、校外补习、家庭作业、家务劳动和习惯表现以任务形式跟踪，完成后获得成长积分，坏习惯或任务完成不好则扣分。积分驱动每个孩子的“灵宠”成长升级，达到对应等级解锁家庭奖励。

---

## 技术栈

- Java 17
- Spring Boot 3.3.5
- MyBatis + PostgreSQL
- Thymeleaf + Bootstrap 5

---

## 数据库连接（已按你本地 PG 配置）

```text
Host:     localhost
Port:     5432
Database: ontobricks
Schema:   family_kids
Username: ontobricks
Password: devpass
```

如要修改，编辑 `src/main/resources/application.yml`，或启动时传入环境变量：

```bash
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=ontobricks
set DB_SCHEMA=family_kids
set DB_USER=ontobricks
set DB_PASSWORD=devpass
```

---

## 初始化数据库

系统不会自动建表（避免误删数据），首次使用前请手动执行 SQL：

1. 用 DBeaver / Navicat / psql 连接 `ontobricks` 数据库。
2. 执行 `src/main/resources/db/schema.sql` 建 schema 和表。
3. 执行 `src/main/resources/db/data.sql` 导入默认数据（任务分类、任务模板、坏习惯扣分项、灵宠等级、奖励商城、两个孩子和灵宠）。

---

## 启动项目

> ⚠️ **端口提示**：若你的环境里存在 `SERVER__PORT` 环境变量（Spring Boot 的宽松绑定会把它识别成 `server.port`），
> 它会覆盖 yml 里的 8080。启动脚本 `scripts/run.bat` 已显式传入 `--server.port=8080` 避免被覆盖；
> 若仍被占用，改 run.bat 里的端口号即可。

启动后访问（示例端口 8090，实际以启动参数为准）：

- 登录页：http://localhost:8090/login
- 家长控制台：http://localhost:8090/ （需 `parent` 登录）
- 孩子端打卡：http://localhost:8090/kid/1  或  /kid/2 （需对应孩子账号登录）
- 客厅大屏：http://localhost:8090/board （免登录）

### 方式二：IDEA / Eclipse

直接运行 `com.family.kids.KidsApplication`。

---

## 默认账号数据

系统内建两个孩子：

| id | 姓名 | 家里称呼 | 年级 | 灵宠 |
|----|------|---------|------|------|
| 1 | 宋子凡 | 大宝 | 四年级 | 小火龙（灵龙） |
| 2 | 宋子赫 | 小宝 | 一年级 | 小冰狐（灵狐） |

孩子端地址：`/kid/1`（宋子凡）、`/kid/2`（宋子赫）。

如需改名，改数据库即可：

```sql
UPDATE family_kids.child SET name='新名字' WHERE id=1;
```

姓名、年级、头像 emoji、主题色都在 `child` 表中；灵宠名字在 `pet` 表中。

---

## 登录与权限

系统已接入登录，分两种角色：

| 角色 | 用户名 | 默认密码 | 登录后落地页 | 权限 |
|------|--------|---------|------------|------|
| 家长 | `parent` | `123456` | `/` 家长控制台 | **全部功能**：任务生成/审批/驳回、习惯扣分、灵宠、奖励商城、积分调整、统计报表、查看任意孩子主页 |
| 宋子凡 | `zifan` | `123456` | `/kid/1` 自己的主页 | 只能看自己的主页、提交**自己的**任务、换自己的头像 |
| 宋子赫 | `zihe` | `123456` | `/kid/2` 自己的主页 | 同上 |

规则要点：

- 未登录访问任何页面都会被跳转到 `/login`（客厅看板 `/board` 除外，方便挂墙电视常显）。
- 孩子登录后导航栏只显示「我的主页 + 退出登录」，看不到家长菜单。
- 孩子把地址里的 id 改成兄弟的（如 `/kid/2`）会被弹回自己的主页并提示。
- 孩子提交别人的任务会被拒绝（Web 端弹回主页并提示，API 端返回 403 JSON）。
- 密码用 PBKDF2-HMAC-SHA256 加盐哈希存储（`family_user.password_hash`），不明文保存。

账号表为 `family_user`（避开 PostgreSQL 保留字 `user`）。重置/修改密码：

```sql
-- 把某账号密码重置为 123456（哈希串由 PasswordHashUtil 生成）
UPDATE family_kids.family_user
SET password_hash = '<用 com.family.kids.util.PasswordHashUtil 生成的哈希>'
WHERE username = 'zifan';
```

生成哈希：

```bash
java -cp target/classes com.family.kids.util.PasswordHashUtil 123456
```

---

## 核心功能说明

| 模块 | 路径 | 说明 | 谁可访问 |
|------|------|------|---------|
| 登录 / 退出 | `/login`、`/logout` | 分角色登录 | 所有人 |
| 家长控制台 | `/` | 查看今日完成情况、最近积分变动 | 家长 |
| 今日任务 | `/tasks` | 生成任务、审批孩子提交、驳回、删除 | 家长 |
| 任务模板 | `/tasks/templates` | 管理每日/每周自动生成任务的模板 | 家长 |
| 习惯扣分 | `/habits` | 记录坏习惯并自动扣分 | 家长 |
| 灵宠 | `/pets` | 查看灵宠等级、成长进度、大事记 | 家长 |
| 奖励商城 | `/rewards` | 积分兑换奖励、家长确认兑现 | 家长 |
| 积分流水 | `/points` | 查看所有积分变动、家长手动调整 | 家长 |
| 统计报表 | `/report` | 7 日趋势、分类占比 | 家长 |
| 孩子端 | `/kid/{id}` | 孩子自己查看任务并点击完成 | 家长看全部 / 孩子只看自己 |
| 客厅看板 | `/board` | 全屏展示，每分钟自动刷新 | 免登录 |

---

## 灵宠等级与奖励（默认）

| 等级 | 阶段 | 所需成长值 | 解锁奖励 |
|------|------|-----------|---------|
| Lv1 | 灵蛋 | 0 | 孵化成功 |
| Lv2 | 幼崽 I | 60 | 周末多看 30 分钟动画片 |
| Lv3 | 幼崽 II | 150 | 自己点一次周末晚餐菜 |
| Lv4 | 少年 I | 280 | 周末骑行/公园半日游 |
| Lv5 | 少年 II | 450 | 一个小盲盒/小玩具 |
| Lv6 | 成长期 I | 680 | 家庭电影之夜，自己选片 |
| Lv7 | 成长期 II | 980 | 游乐场/科技馆一日 |
| Lv8 | 成熟期 I | 1350 | 一本心仪课外书 + 零食自由一次 |
| Lv9 | 成熟期 II | 1800 | 周边两日自驾游 |
| Lv10 | 觉醒 | 2400 | 主题乐园心愿大奖 |

等级、奖励可在 `pet_level` 表中自定义。

---

## 注意事项

1. 首次启动前务必执行 schema.sql + data.sql。
2. 生成的任务实例在 `task_instance` 表中，不会删除历史数据。
3. 积分分两套：
   - **成长积分**：决定灵宠等级，只增不减（完成任务增加，坏习惯扣分也按规则作用于成长值）。
   - **可用积分**：用于兑换奖励。
4. **兑换奖励只扣可用积分，不扣成长积分** —— 否则孩子攒分换奖励会导致灵宠掉级，打击积极性。
   同理，兑换不计入「累计扣分」统计（消费 ≠ 惩罚）。
5. 孩子端适合放在平板或手机上打开，点击"我完成了"即可提交。
6. 所有非法操作（积分不足、记录不存在等）都会给出友好提示，不会抛 500。

---

## 一键初始化数据库（无需 psql 客户端）

如果你机器上没有 `psql`，可以用项目自带的 JDBC 小工具执行 SQL：

```bash
scripts/init-db.bat
```

它会自动执行 `schema.sql` 和 `data.sql`。脚本可重复执行（建表用 `IF NOT EXISTS`，种子数据用 `ON CONFLICT DO NOTHING`）。

---

## 端到端自检

```bash
bash scripts/verify.sh
```

会启动应用 → **家长登录** → 重置数据 → 跑完「生成任务 / 孩子提交 / 家长审批 / 坏习惯扣分 / 兑换奖励 / 兑现 / 手动加分 / 异常场景」，
并打印最终积分、灵宠状态、积分流水、大事记与全部页面状态码。

登录与角色权限的专项自检（无需重启应用，需服务已在 8090 运行）：

```bash
python scripts/verify_auth.py     # 未登录跳转 / 家长全权限 / 孩子越权拦截 / 退出登录
python scripts/verify_submit.py   # 孩子只能提交自己的任务
```

已验证结果（示例）：

| 项 | 哥哥 | 弟弟 |
|----|------|------|
| 今日任务 | 18 个，全部通过审批 | 18 个 |
| 累计得分 | 67 | 20 |
| 扣分 | 5（电子产品超时） | 0 |
| 成长积分 / 灵宠 | 62 → Lv2 幼崽 I | 20 → Lv1 灵蛋 |
| 可用积分 | 32（兑换 30 分奖励后） | 20 |
| 灵宠心情 | 100 | 85 |

关键校验点：兑换 30 分奖励后，**成长积分仍为 62、灵宠不掉级**（流水 `EXCHANGE -30 → balance 32 / growth 62`）。
