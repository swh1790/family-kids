# family-kids 架构与代码健康度诊断报告

> 诊断人：高见远（架构师）
> 诊断时间：2026-10-18 16:40 ~ 17:10（代码快照时刻）
> 诊断方式：全量源码静态阅读（52 个 Java 文件 / 2637 行 + 13 个模板 + 2 个静态资源）+ 运行时 HTTP 实测（curl 打 8090，含登录态越权测试）+ 数据库实测（JDBC 直连核对字段、索引、积分自洽性）
> 约束：只读诊断，未修改任何源码

---

## 0. 重要前提：这是一份"移动靶"快照

诊断期间（16:34–16:38）项目正在**新增登录鉴权模块**：`LoginController` / `AuthService` / `LoginInterceptor` / `CurrentUserAdvice` / `AuthContext` / `UserMapper` / `User` / `LoginUser` / `PasswordHashUtil` / `login.html` 全部是新增文件。

我已**重新读取全部新增代码并按最新状态评估**（16:40 实测运行时已包含鉴权：`/` 返回 302 → `/login`）。
下文所有结论均以"含登录模块"的当前快照为准；若你手里的结论来自更早版本，请以本报告为准。

---

## 1. 总体评价（一句话）

> **业务骨架（分层 + 积分双轨 + 灵宠升级）设计得相当干净，主要风险不在"结构"而在"并发与交付"：积分读写是读-算-写非原子、审批/兑换无幂等，叠加"构建产物即运行时文件"的打包方式，是目前最该先解决的两件事。**

分层评分（5 分制）：

| 维度 | 评分 | 说明 |
|------|------|------|
| 分层清晰度 | ★★★★☆ | controller/service/mapper 三层主脉络清晰，但 3 个 controller 绕过 service 直连 mapper |
| 领域模型 | ★★★★☆ | 积分双轨规则集中收口在 `PointService`，设计正确 |
| 数据一致性 | ★★☆☆☆ | 有事务但无并发保护，非原子更新 + 无幂等 |
| 数据访问效率 | ★★★☆☆ | 无 N+1 经典陷阱，但配置表被重复查 8 次/请求 |
| 安全 | ★★★☆☆ | 刚补上登录与角色越权，**实测孩子越权已被正确拦截**；但 CSRF / 会话固定 / 弱口令未处理 |
| 配置与部署 | ★★☆☆☆ | 硬编码绝对路径 + 构建产物与运行时同一文件（jar 损坏根因） |
| 前端/模板 | ★★★★☆ | fragment 复用做得好，无 XSS，仅有片段重复 |
| 可测试性 | ★☆☆☆☆ | **零自动化测试**（无 `src/test`） |

---

## 2. 代码规模与各层分布

| 层 | 文件数 | 行数 | 说明 |
|----|--------|------|------|
| controller | 13 | 778 | 含 `GlobalExceptionHandler`、`LoginController` |
| service | 8 | 824 | 含新增 `AuthService` |
| mapper | 11 | 417 | 10 个注解式 + 1 个 XML |
| entity | 13 | 298 | 全部 Lombok `@Data` |
| config | 3 | 160 | `WebConfig` / `LoginInterceptor` / `CurrentUserAdvice` |
| util | 3 | 146 | `Constants` / `AuthContext` / `PasswordHashUtil` |
| 启动类 | 1 | 14 | `KidsApplication` |
| **合计** | **52** | **2637** | 平均每个类 50 行，单体规模健康 |

### 最"臃肿"的 3 个类

| 排名 | 类 | 行数 | 问题 |
|------|-----|------|------|
| 1 | `service/PointService.java` | 182 | 唯一的积分引擎，`addPoints()`（72–153）一个方法干 6 件事：算 balance/growth、算 earned/deducted、算等级、更新 child、更新 pet、写流水、写大事记。**不是 god class，但方法内聚度偏低** |
| 2 | `service/TaskService.java` | 173 | 尚可，`generateDaily`（44–83）含模板筛选的三重规则，可抽私有方法 |
| 3 | `controller/TaskController.java` | 171 | **本层唯一真正该拆的**：既管任务实例（12 个映射），又管任务模板（5 个映射），且直接注入 2 个 Mapper |

> 结论：**没有 god class**。2637 行 / 52 类，平均 50 行，属于"小而清晰"区间。最该动刀的是 `TaskController`（建议拆出 `TaskTemplateController`）。

---

## 3. 分层与依赖（对应问题 1）

### 3.1 确认存在的问题

| # | 问题 | 证据 | 影响 |
|---|------|------|------|
| A1 | **Controller 直接注入 Mapper 并写业务规则** | `TaskController.java:30,34`（注入 `TaskMapper`/`TaskCategoryMapper`）；`TaskController.java:129-154`（模板保存/删除的全部默认值逻辑写在 Controller）；`HabitController.java:20`（注入 `HabitMapper`）、`:38-45`（查习惯项、拼原因、调扣分全在 Controller）；`PointController.java:24,29`（注入 `PointRecordMapper` 直接查流水） | 业务规则散落在两层；`TaskService`（173 行）里没有 `saveTemplate/deleteTemplate`，说明 service 层对模板是"空"的，日后加校验（如分类必填）会不知道写哪 |
| A2 | **Controller 承担了 DTO/视图组装** | `KidController.java:59-66`（`groupByCategory` 分组逻辑）、`BoardController.java:33-36`（把 `List<Map>` 转成 `Map<Object,Integer>`） | 无法复用、无法单测 |
| A3 | **视图模型用 `Map<String,Object>`，且用平行数组配对** | `PetService.getPetCard()` 返回 `HashMap`（`PetService.java:40-47`）；`dashboard.html:43` 用 `${petCards[cStat.index]}` 按下标取灵宠卡片 | **两个列表一旦顺序不一致（如 `listActive()` 排序变化），就会把哥哥的灵宠显示到弟弟头上**；且模板里到处 `${card.pet.avatar}` 是弱类型，字段改名编译器不报错 |
| A4 | `TaskController` 双职责 | `TaskController.java:32-117`（任务实例）+ `:120-154`（模板管理） | 可以接受，但继续加功能会先腐化这里 |

### 3.2 确认**没有**的问题（避免含糊）

- **无循环依赖**：依赖方向单一 —— `TaskService`/`RewardService`/`PetService` → `PointService` → 各 Mapper；`PointService` 不反向依赖任何 Service。✅
- **无 Service 接口层**（都是具体类）。对此规模（8 个 Service）是**合理取舍**，不列为问题。
- **无 God Class**：最大类 182 行。✅

---

## 4. 数据一致性：积分双轨（对应问题 2）

### 4.1 双轨规则的实现位置 —— 收敛良好 ✅

全部收口在 `PointService.addPoints()`（`PointService.java:72-153`），四个对外入口语义清晰：

| 入口 | 行号 | delta | type | balance | growth | deducted |
|------|------|-------|------|---------|--------|----------|
| `earnFromTask` | 42-45 | + | EARN | ✅ 增 | ✅ 增 | — |
| `deductFromHabit` | 50-53 | − | DEDUCT | ✅ 减 | ✅ 减 | ✅ 计入 |
| `exchangeReward` | 58-61 | − | EXCHANGE | ✅ 减 | ❌ **不变** | ❌ 不计入 |
| `manualAdjust` | 66-70 | ± | ADJUST/DEDUCT | ✅ | ✅ | TYPE_DEDUCT 时计入 |

**核对结论：与团队给定的业务规则完全一致** —— 兑换只扣 balance 不掉级（`PointService.java:86-90`）、只有 `TYPE_DEDUCT` 计入 `deducted`（`:97-99`）。规则实现只有这一处，**不存在多处实现**。✅

数据库实测（16:45）也印证一致：

| child | growth | balance | earned | deducted | pet.exp | Σpoint_record |
|-------|--------|---------|--------|----------|---------|---------------|
| 1 宋子凡 | 85 | 55 | 90 | 5 | 85 | 55 |
| 2 宋子赫 | 20 | 20 | 20 | 0 | 20 | 20 |

（`growth = earned − deducted`、`balance = Σ流水` 全部对得上，当前无脏数据。）

### 4.2 ⚠️ 确认存在的风险：并发丢更新（P0）

`addPoints()` 是典型的 **读 → 内存算 → 写绝对值**：

```
PointService.java:74   Child child = childMapper.getById(childId);     // 读
              :84      int balance = Math.max(0, child.getBalancePoints() + delta);  // 算
              :112     childMapper.updatePoints(child);                // 写绝对值
```

`@Transactional` 有（:42/:50/:58/:66），但**默认隔离级别 READ_COMMITTED + 无行锁**，两个并发操作会互相覆盖：
- 爸爸在手机 A 审批任务（+10），妈妈在手机 B 同时记坏习惯（−5）→ 最终 balance 只反映其中一个。
- 两个孩子同时提交、家长连点两次"通过"→ 见 4.3。

`pet` 同理（:116-128 写 `exp/level/mood` 绝对值）。

### 4.3 ⚠️ 确认存在的风险：审批 / 兑换无幂等（P0）

- `TaskService.approve()`（:114-126）：只判断 `if (TASK_APPROVED.equals(status)) return;`，**判断与更新之间没有原子屏障**。两个请求同时读到 `DONE`，都会走到 `pointService.earnFromTask()` → **同一个任务加两次分**。
  - 同理 `updateInstanceStatus`（`TaskMapper.java:105-111`）是 `WHERE id = #{id}` 无条件更新，不能用来做 CAS。
  - ⚠️ **口径修正（重要）**：QA 已实测 `approve ×3` 顺序重复提交**只产生 1 条流水**（`docs/qa-diagnosis.md` §6.1），说明**串行路径是安全的**。本条剩余风险**仅限并发窗口**（读状态 → 判断 → 写状态 → 加分，四步之间无锁），属代码路径明确存在、但尚未实测触发的风险，**不是已复现缺陷**。
- `RewardService.claim()`（:64-94）：校验余额 → 扣分 → 减库存 → 插记录，全程无任何"同一 (child, reward) 短时间内不可重复"的约束（表上也无唯一索引）。串行下第二次会因余额不足被拦（QA 已验证），**并发下则可能双重扣分**。
- 对比：`generateDaily` 是**正确示范** —— `TaskMapper.java:69` 用 `ON CONFLICT (child_id, template_id, plan_date) DO NOTHING`，天然幂等。✅

### 4.3-bis ⚠️ 新增确认缺陷：负分任务被静默"转正"发放（P0）

`PointService.java:44`：`addPoints(childId, Math.abs(points), TYPE_EARN, SRC_TASK, ...)` —— **无条件取绝对值**。

配合两处上游缺失校验：
- `TaskController.java:63` `@RequestParam(defaultValue="5") Integer points` 无范围校验；
- `schema.sql:99` `points INT NOT NULL` **无 CHECK 约束**，负数可入库。

**链路**：家长/脚本创建 `points=-3` 的"惩罚型任务" → 审批通过 → QA 实测 child2 balance 20 **→ 23（增加）**、growth 20 **→ 23**，流水记 `EARN +3`（`docs/qa-diagnosis.md` §P1-4）。

**这是"意图与实现相反"的缺陷**（扣分任务变成加分），且发生在积分引擎入口，比并发问题更容易被真实触发。

### 4.4 其它确认项

- `TaskService.reject()`（:135-149）后**仍可再 `approve()`**：状态是 `REJECTED`，`approve` 只对 `APPROVED` 拦截（:120），所以"驳回 → 通过"会正常加分。这符合直觉（改主意），**不是 bug**，但要知晓。
- `TaskService.submit()`（:88-109）对 `DONE`（待审批中）**不拦截重复提交**，只重置 `submittedAt`。无害但语义不严谨。
- `AvatarService.saveAvatar()`（:49-82）**无 `@Transactional`**：先写文件再更新 DB，DB 失败会留下孤儿文件（低危）。

---

## 5. SQL 与数据访问（对应问题 3）

### 5.1 实测：一次页面请求 17 条 SQL，其中 8 条是同一句

方法：`curl /board` 前后对比 `app-run.log` 中 `Preparing:` 条数（MyBatis debug 日志已开）。

```
一次 GET /board 触发 17 条 SQL，分布：
   8  SELECT * FROM pet_level ORDER BY level            ← 同一句重复 8 次（47%）
   2  SELECT * FROM pet_log WHERE child_id = ? LIMIT ?
   2  SELECT * FROM pet   WHERE child_id = ?
   2  SELECT * FROM child WHERE id = ?                  ← listActive() 已查过，又单查一遍
   1  point_record 最近流水 / dailySummary / child.listActive
```

**根因**（`pet_level` 是一张 10 行的静态配置表，却被反复查）：

| 调用点 | 行号 | 每次请求次数 |
|--------|------|------------|
| `PetService.getPetCard → pointService.computeLevel` | `PointService.java:158-167` → `listAll()` | 1 × 孩子数 |
| `PetService.getPetCard → pointService.expToNextLevel` | `PointService.java:172-181`（内部再调 `computeLevel` + 自己 `listAll`） | 2 × 孩子数 |
| `PetService.progressPercent → nextLevel` | `PetService.java:67-75` → `listAll()` | 1 × 孩子数 |

即**每个孩子 4 次 `pet_level` 全表查**，2 个孩子 = 8 次。
叠加 `board.html:62` 的 `setTimeout(location.reload, 60000)`（每分钟自动刷新），一天约 **2.4 万次无效查询**。数据量小不至于拖垮，但这是最典型也最容易修的性能债。

### 5.2 MyBatis 注解 / XML 混用（确认存在）

- 11 个 Mapper 中 **10 个纯注解**，`ReportMapper` 单独走 XML（`resources/mapper/ReportMapper.xml`）。
- `TaskMapper.java:74-100` 在 `@Select` 字符串里内嵌 `<script><if test=...>` 做动态 SQL —— 能跑，但可读性差、无语法高亮、无法复用 `<sql>` 片段。
- `RewardMapper.java:13` 等大量 `SELECT *`：加字段时 `Child`/`Pet` 等实体会自动多出列，风险低但不够明确。
- 所有 Mapper **都不带 `family_kids.` schema 前缀**，依赖 JDBC URL 上的 `currentSchema=family_kids`（`application.yml:16`）。目前一致，但 `UserMapper.java:11` 写的是 `family_user`（无前缀）与其它表混在一起，未来拆库/多 schema 会踩坑。

### 5.3 索引 / 全表扫描 —— 基本健康 ✅

实测 `pg_indexes`（schema=family_kids）：核心路径都有索引 ——
`idx_task_instance_date(child_id, plan_date)`、`idx_point_child_time(child_id, created_at)`、`idx_petlog_child(child_id, created_at DESC)`、`idx_claim_child(child_id)`、`task_instance(child_id, template_id, plan_date)` 唯一索引（支撑 `ON CONFLICT`）。

- 数据量：`task_instance` 70 行、`point_record` 27 行、**没有全表扫描风险**（家庭级数据量，5 年后也就万级）。
- ⚠️ 小问题：`TaskMapper.listInstancesByDate` 不传 `childId` 时按 `plan_date` 单列过滤，而索引是 `(child_id, plan_date)` 复合索引，**用不上前导列**。当前数据量无所谓，属潜在风险。
- ⚠️ 冗余索引：`family_user` 上有**两个重复的唯一索引** —— `uq_family_user_username`（`schema.sql:203` 表级约束）和 `ux_family_user_username`（`schema.sql:217` 单独 CREATE）。应删掉一个。

### 5.4 schema / 实体对齐 —— 已对齐 ✅

实测 `information_schema.columns`（`family_kids.child`）15 个字段，与 `entity/Child.java`（含后加的 `avatarUrl`，:17）**完全对齐**，`avatar_url VARCHAR(255)` 已存在（`schema.sql:189` 的 `ALTER TABLE ... IF NOT EXISTS` 负责补齐）。

未发现字段漂移。**唯一隐患**：`spring.sql.init.mode: never`（`application.yml:31`），schema 变更靠人手执行 `schema.sql`，没有任何版本管理（Flyway/Liquibase）。现在只有 1 次 `ALTER`，还可控；再攒几个就会出事。

### 5.5 SQL 注入 —— 确认不存在 ✅

全量 grep mapper 注解与 XML，**没有任何 `${}` 拼接**，全部 `#{}` 预编译。✅

---

## 6. 安全与健壮性（对应问题 4）

> 说明：登录模块是诊断期间新增的，我对其做了完整评审 + 运行时实测。

### 6.1 实测结果：角色越权防御**有效** ✅

| 测试 | 方法 | 结果 |
|------|------|------|
| 未登录访问 `/` `/tasks` `/points` `/rewards` | curl | 302 → `/login` ✅ |
| 孩子 `zifan/123456` 登录后访问 `/kid/2`（弟弟主页） | 带 session cookie | 302 → `/kid/1` ✅ |
| 孩子访问 `/tasks` `/points` `/rewards` | 同上 | 302 → `/kid/1` ✅ |
| 孩子访问 `/kid/1`（自己） | 同上 | 200 ✅ |
| 家长 `parent/123456` 登录 | 同上 | `/` `/tasks` `/points` `/kid/2` 均 200 ✅ |

资源级二次校验也已就位：`ApiController.java:40-45`、`AvatarController.java:33-35,50-52`、`KidController.java:42-44`、`TaskController.java:76-79`。

### 6.2 确认存在的安全风险

| # | 风险 | 证据 | 影响 | 等级 |
|---|------|------|------|------|
| S1 | **默认弱口令 `123456`，且无强制改密** | `data.sql:115-120`（注释明写"默认密码均为 123456"，`parent`/`zifan`/`zihe` 三个账号） | 只要系统能被家庭网络以外访问（端口映射/访客 WiFi），等于完全失守 | **P0** |
| S2 | **无 CSRF 防护** | 全部写操作是裸 `@PostMapping`（`TaskController:88/98/109/129/149`、`PointController:33`、`RewardController:30/43/50/63/70`、`AvatarController:23/41`），无 token；`WebConfig:30-40` 只加了登录拦截器 | 家长登录状态下访问恶意页面，可被静默"审批任务加积分"/"手动加分"。**缓解因素**：现代浏览器默认 `SameSite=Lax`，跨站 POST 不带 Cookie，实际可利用性被压低 | **P1** |
| S3 | **会话固定（Session Fixation）** | `LoginInterceptor.java:39` 未登录时 `request.getSession(true)` 建会话并存回跳地址；`LoginController.java:38` 登录成功**复用同一 session**，未调用 `request.changeSessionId()` | 攻击者预置 JSESSIONID → 家长登录后该 ID 即为已认证会话 | **P1** |
| S4 | **`/board` 与 `/uploads/**` 免鉴权** | `WebConfig.java:37` 显式排除 `/board`；`:39` 排除 `/uploads/**`；`board.html:13-31` 展示孩子姓名、真实头像照片、等级、今日积分 | 局域网内任何设备（含访客）无登录即可看到孩子照片与积分。挂墙大屏是合理诉求，但**风险需显式接受** | **P1** |
| S5 | **开放重定向** | `GlobalExceptionHandler.java:20-23`：`return "redirect:" + request.getHeader("Referer")` —— Referer 完全由客户端控制 | 可构造 `Referer: http://evil.com` 让登录后跳转到外部站点（钓鱼） | **P1** |
| S6 | **异常兜底缺失 → 真实 500（QA 已实测复现 3 处）** | `GlobalExceptionHandler.java:15` 只捕获 `IllegalArgumentException` / `IllegalStateException`，无 `Exception` 兜底。<br>QA 实测 500：① `GET /kid/999`（`PetService.getPetCard:35` child 未判空 NPE）② `GET /pets/999`（同源）③ `POST /tasks/99999/delete`（`TaskController.delete` 先 `getInstance()` 再 `.getPlanDate()` NPE）—— 健壮性 17 场景中 500 占 3（82% 降级率） | 与 README:135 "所有非法操作都不会抛 500" 的承诺不符；家长看到 Whitelabel 页无从下手 | **一行级修复**：三处取值后立即判空抛 `IllegalArgumentException`，复用现成友好提示条；另补 `@ExceptionHandler(Exception.class)` + `MissingServletRequestParameterException` / `MethodNotSupported` 等 Spring 标准异常（QA P2-5） | **0.5 人时** |
| S7 | 无登录失败限流/锁定 | `AuthService.java:20-39` | 口令只有 6 位数字时可被暴力破解（PBKDF2 120k 迭代使单次成本高，实际风险中等） | P2 |
| S8 | 头像上传未限制像素尺寸 | `AvatarService.java:62-69` 只校验 5MB 体积 + `ImageIO.read`，未限制宽高 | 高压缩比 PNG（解压炸弹）可在解码时吃掉数百 MB 内存 | P2 |
| S9 | `deleteOldAvatar` 仅按前缀校验 | `AvatarService.java:111` 只判断 `startsWith("avatar-")` | 若 DB 中 `avatar_url` 被污染（如 `avatar-../../../x`）可越界删除；需先污染 DB 才能触发，属纵深防御缺失 | P2 |

### 6.3 确认**安全**的项 ✅

- **路径穿越**：头像文件名由服务端生成 `"avatar-" + childId + "-" + 时间戳 + ".jpg"`（`AvatarService.java:71`），`root.resolve(fileName)` 不拼用户可控字符串 → **安全**。
- **XSS**：13 个模板全量 grep **无任何 `th:utext`**；`report.html:61` 的 `th:inline="javascript"` 使用 `/*[[${...}]]*/` 语法，Thymeleaf 会做 JS/JSON 转义 → **安全**。
- **口令存储**：PBKDF2-HMAC-SHA256 / 120000 迭代 / 16 字节随机盐 / `MessageDigest.isEqual` 定长比较（`PasswordHashUtil.java:15-51`）—— 手写实现质量不错，未引入 Spring Security 是合理取舍。
- **SQL 注入**：见 5.5，不存在。

---

## 7. 配置与部署健康度（对应问题 5）

### 7.1 "打包前必须停 java 进程否则 jar 损坏" —— 根因定位

**根因（确认，非猜测）：构建产物与运行时文件是同一个文件。**

```
scripts/run.bat:28-29
  if exist "target\family-kids-1.0.0.jar" (
     java -jar target\family-kids-1.0.0.jar --server.port=8090 )

scripts/start-server.bat:4,13
  set "JAR=%BASE%\family-kids\target\family-kids-1.0.0.jar"
  start ... java -jar "%JAR%" --server.port=8090
```

而 `mvn package` 的 `spring-boot-maven-plugin:repackage` **正是往 `target/family-kids-1.0.0.jar` 这个文件写**。
在 Windows 上 JVM 以内存映射方式持有该 jar 的文件句柄 → 文件被独占锁定 → Maven 要么删除/覆写失败留下**半截 jar**，要么写成功但运行中的 JVM 仍持有旧映射产生不可预期行为。**这不是 Maven 或 Spring Boot 的 bug，是"构建目录 = 运行目录"这一部署形态的必然结果。**

**架构上的规避方案（推荐 ①，改动最小）：**

| 方案 | 做法 | 代价 |
|------|------|------|
| ① **产物分离（推荐）** | `mvn package` 后把 jar **拷贝到 `dist/family-kids-1.0.0-<时间戳>.jar`**，启动脚本永远从 `dist/` 取最新 jar 运行；`target/` 只当构建目录 | 加一个 `copy` 步骤 + `dist/` 加 .gitignore，约 0.5 人时 |
| ② 版本化 finalName | pom 里 `<finalName>family-kids-${maven.build.timestamp}</finalName>` | 每次产物不同名，永不冲突；但需脚本挑最新，且 target 会堆积 |
| ③ 改用 `mvn spring-boot:run` | 从 `target/classes` 跑 | 仍会热替换 class，且要求开发机装 Maven，**不推荐** |
| ④ 引入 `spring-boot:stop` / 脚本先 kill 再 build | 治标 | 仍依赖人记得执行 |

同时建议给 `scripts/` 加一个 `stop.bat`（`taskkill /F /IM java.exe` 或按端口 8090 找 PID），与 `start-server.bat` 配对。

### 7.2 其它配置问题（确认存在）

| # | 问题 | 证据 | 影响 |
|---|------|------|------|
| C1 | **硬编码机器绝对路径** | `application.yml:58` `avatar-dir: ${AVATAR_DIR:C:/Users/PC/WorkBuddy/2026-10-17-22-41-15/family-kids/uploads/avatars}` | 换机器/换目录即失效；默认值应改为 `./uploads/avatars`（相对 CWD）或 `${user.home}/family-kids/avatars` |
| C2 | **DB 口令明文默认值** | `application.yml:18` `password: ${DB_PASSWORD:devpass}` | 内网应用可接受，但应改用无默认值（缺失即启动失败）避免误提交 |
| C3 | **端口漂移** | `application.yml:2` 写 8080；`run.bat:18,29` / `start-server.bat:13` 强传 8090；`README.md:52-60` 仍写 8080 | 三方不一致，新人按 README 打开 8080 必然打不开 |
| C4 | **DEBUG 日志全量开启** | `application.yml:47-50` `com.family.kids.mapper: debug` + `org.springframework.jdbc: debug` | 每条 SQL 打 3 行日志；`app-run.log` 数小时已 56KB，长期跑在盘上会持续增长且拖慢 I/O。生产应改 `info` |
| C5 | 无 Actuator / 健康检查 | pom.xml 仅 web/thymeleaf/validation/mybatis | 无法探活，出问题只能看日志 |
| C6 | 上传目录与 classpath 耦合已规避 ✅ | `application.yml:56-58` 注释 + `WebConfig:20-27` 外部目录映射 | 这一处设计是对的，重新打包不丢照片 ✅ |

---

## 8. 前端 / 模板（对应问题 6）

### 8.1 fragment 复用 —— 做得好 ✅

`fragments.html` 提供 `head(title)` / `nav` / `kidnav` / `alerts` / `scripts` 五个片段，**13 个模板文件中除 `fragments.html` 自身外的 12 个页面模板 100% 在用**（`th:replace` 共 30+ 处），`head`/`nav`/`alerts`/`scripts` 统一收口。新增的 `login.html` 也正确复用了 `head` 与 `alerts`（`login.html:3,11`）。这个水平高于同类小项目的平均线。

### 8.2 可继续抽取的重复（确认存在，P2）

| 重复内容 | 出现位置 | 建议 |
|----------|----------|------|
| **灵宠进度卡**（头像 + Lv + 阶段 + 进度条 + "还需 N 分升级"） | `board.html:13-31`、`dashboard.html:40-61`、`kid.html:41-52`、`pet-card.html:15-30` —— **4 处** | 抽 `fragments :: petProgress(card)` 参数化片段 |
| **头像照片/emoji 回退** | `dashboard.html:23-27`、`kid.html:15-18` —— 2 处 | 抽 `fragments :: avatar(child, size)` |
| 任务行操作按钮组 | `tasks.html`、`kid.html` | 评估后再定，差异较大 |

### 8.3 CSS 版本戳机制 —— **不可靠（确认，P2）**

`fragments.html:9`：`th:href="@{/css/app.css?v=8}"` —— **纯手工维护的数字**。
（诊断过程中实测到它从 `?v=7` 被改成 `?v=8`，说明每次改 CSS 都要人记得 +1。）

- 漏改的后果：家长手机/平板浏览器缓存旧 CSS，页面错版且不易排查。
- 更可靠做法（任选）：① 用 Maven resource filtering 注入 `${project.version}` 或构建时间戳；② 用内容 MD5 前 8 位（Spring Boot 的 `spring.web.resources.chain.strategy.content` 可自动做）；③ 至少在 `WebConfig` 里给 `/css/**` 配 `no-cache` 兜底（目前只有 `/uploads/avatars/**` 配了 no-cache，`WebConfig:24`）。

### 8.4 XSS —— 确认无风险 ✅

- 全量 grep 无 `th:utext`。
- `report.html:61` 的 `th:inline="javascript"` 用于把报表数据喂给 Chart.js，Thymeleaf 内建 JS 转义可防 `</script>` 与引号逃逸。
- 用户可控文本（任务标题、奖励名、习惯名、孩子名）走 `th:text` 全部转义。

---

## 9. 技术债清单（分级）

### P0 —— 必须马上改（影响数据正确性 / 可被直接攻破）

| # | 问题 | 证据 | 影响 | 建议改法 | 预估改动量 |
|---|------|------|------|----------|-----------|
| **P0-1** | 积分读写非原子，并发丢更新 | `PointService.java:74-112`（读-算-写绝对值）、`ChildMapper.java:42-49`、`PetMapper.java:21-28` | 两人同时操作 → 积分/成长值静默丢失，灵宠等级错乱且**不可逆、无痕迹** | ① 改原子 SQL：`UPDATE child SET balance_points = GREATEST(0, balance_points + #{delta})`（growth 同理、exchange 时 growth 不参与）；② 或读写前 `SELECT ... FOR UPDATE`；③ 至少加 `@Version` 乐观锁 | 2 个 Mapper + `PointService.addPoints` 重写，约 **1 人日** |
| **P0-2** | 审批 / 兑换在**并发窗口**下无幂等 | `TaskService.java:114-126`、`TaskMapper.java:105-111`、`RewardService.java:64-94` | 串行重复提交 QA 实测已安全（approve×3 仅 1 条流水）；**并发下**读-判断-写之间无原子屏障 → 双份积分 / 双倍扣分 | ① `updateInstanceStatus` 改 `WHERE id=#{id} AND status <> 'APPROVED'`，`rows==1` 才调 `earnFromTask`；② `reward_claim` 加唯一约束（child_id, reward_id, status）或"当日不可重复"校验；③ 前端按钮提交后 disable | 3 个类 + 1 条 DDL，约 **0.5 人日** |
| **P0-3** | 默认弱口令 123456 且无强制改密 | `data.sql:115-120` | 一旦暴露到家庭网络外 → 完全失守 | ① 首登强制改密（登录时校验 `is_default_password` 标志跳改密页）；② 或 `init-db.bat` 后随机生成口令并打印；③ 至少删除 SQL 注释里的明文口令 | 加 1 个字段 + 1 个页面 + `AuthService` 分支，约 **0.5 人日** |
| **P0-4** | 构建产物即运行时文件（jar 损坏坑） | `run.bat:28-29`、`start-server.bat:4,13` vs `mvn package` 输出同一 `target/family-kids-1.0.0.jar` | 每次打包都可能产出坏 jar，且需手工先停进程 | 见 §7.1 方案①：构建后 copy 到 `dist/` 带时间戳命名，启动脚本从 `dist/` 取最新；配 `stop.bat` | 2 个 bat + 1 个 copy 步骤，约 **0.5 人时** |
| **P0-5** | **负分任务被 `Math.abs` 转成正分发放**（QA 交叉验证新增） | `PointService.java:44`（`Math.abs(points)`）+ `TaskController.java:63`（无范围校验）+ `schema.sql:99`（`points` 无 CHECK）。**QA 已实测复现**：`points=-3` 的任务审批后 balance 20→23、growth 20→23，流水 `EARN +3`（`docs/qa-diagnosis.md` §P1-4） | 扣分任务变加分，与 `TYPE_EARN` 语义相反，且掩盖上游未校验问题 | ① `earnFromTask` 去掉 `Math.abs`，改 `if (points <= 0) throw new IllegalArgumentException("任务积分必须为正")`；② `addManual` 校验 1~50；③ `schema.sql` 加 `CHECK (points > 0)` | 3 处，**0.5 人时** |

### P1 —— 近期改（1~2 周内）

| # | 问题 | 证据 | 影响 | 建议改法 | 改动量 |
|---|------|------|------|----------|--------|
| P1-1 | 无 CSRF 防护 | 全部 `@PostMapping`，`WebConfig:30-40` | 登录态被跨站利用（浏览器 SameSite=Lax 部分缓解） | 自写 `CsrfFilter` + `fragments` 注入 hidden token，或直接引 `spring-security`（会顺带解决 P1-2 会话固定） | 自写约 1 人日；引 Spring Security 约 2 人日 |
| P1-2 | 会话固定 | `LoginController.java:38` 未 `changeSessionId()` | 会话劫持 | 登录后 `request.changeSessionId()`（一行）；或用 Spring Security 默认已处理 | **5 分钟** |
| P1-3 | `/board` + `/uploads/**` 免鉴权 | `WebConfig.java:37,39` | 局域网任何人可见孩子照片与积分 | 若必须开放：给 `/board` 加一次性只读 token（`?t=xxx`），`/uploads` 改为 Controller 代理鉴权后输出；若接受风险：在 README 明确记录 | 0.5 人日 |
| P1-4 | `pet_level` 每请求查 8 次 | `PointService.java:158-181`、`PetService.java:54-75`；实测 17 SQL/请求 | 每请求 47% 的 SQL 是无效重复 | ① 加 `@Cacheable("petLevels")` 或启动时载入 `List<PetLevel>` 到内存 Bean；② `computeLevel/expToNextLevel/nextLevel` 合并为一次遍历 | 1 个缓存配置 + 3 个方法，**0.5 人日** |
| P1-5 | Controller 直连 Mapper | `TaskController.java:30,34,129-154`、`HabitController.java:20,38-45`、`PointController.java:24` | 业务规则分散，加校验无处安放 | `TaskService` 补 `saveTemplate/deleteTemplate`；`HabitService` 抽出扣分逻辑；`PointController` 改走 `ReportService` | 3 个类，**0.5 人日** |
| P1-6 | 零自动化测试 | `src/` 下只有 `main`，无 `src/test` | 重构（尤其 P0-1 并发改造）无安全网 | 先补 3 个关键用例：积分双轨（earn/deduct/exchange 后 balance/growth/level 断言）、审批幂等、兑换余额不足。用 `@SpringBootTest` + Testcontainers 或本机 PG | 3~5 个测试类，**1 人日** |
| P1-7 | 视图用 `Map` + 平行数组配对 | `PetService.java:40-47`、`dashboard.html:43` | 顺序错位即数据显示错人，且编译期无保护 | 建 `PetCard` DTO；`DashboardController` 返回 `List<ChildCardVO>`（一个列表而非两个） | 1 个 DTO + 3 个 Controller，**0.5 人日** |
| P1-8 | 开放重定向 + 异常无兜底 | `GlobalExceptionHandler.java:20-25` | 可被钓鱼跳转；未预期异常直接 500（违背 README 承诺） | Referer 只接受同源（校验 host）；补 `@ExceptionHandler(Exception.class)` 记录日志并跳首页带错误条 | **0.5 人时** |
| P1-9 | 配置硬编码与环境漂移 | `application.yml:2,18,58`、`README.md:52-60` | 换机即坏；新人按 README 打不开 | `avatar-dir` 默认改 `./uploads/avatars`；DB 口令去掉默认值；README 端口统一为 8090 | **0.5 人时** |
| P1-10 | 无 schema 版本管理 | `application.yml:31` `sql.init.mode: never`，`schema.sql:189` 靠 ALTER 补字段 | 攒几个变更后必然出现"某人漏执行 SQL" | 引入 Flyway（1 个依赖 + 把 schema.sql/data.sql 迁成 `V1__*.sql`） | 1 人日（可选） |
| P1-11 | **大额负向调整无上限、不可撤销** | QA 实测：`/points/adjust?points=-99999` 一次操作把 child1 growth 88 → **0**、灵宠 Lv2 → **Lv1**，`Math.max(0, ...)` 静默截断（`PointService.java:84,90`），无二次确认、无撤销入口（`docs/qa-diagnosis.md` §P2-4，QA 已自行 SQL 还原） | 手滑输入一位多余数字即毁掉孩子数月成长值，且家长无法自救 | ① 单次调整阈值（如 ±100）越界拒绝；② 前端二次确认；③ 提供"撤销最近一次手动调整"（按 `point_record` 反向冲正） | 0.5 人日 |
| P1-12 | **日志不可审计**（升级自 C4） | QA 实测：诊断期间重启 ≥3 次（PID 13088→16812→10724→3732），`start-server.bat:13` 用 `>` 重定向 → **每次重启截断 `app-run.log`，历史全丢**；`app-err.log` 恒 0 行（`2>&1` 已合并，形同虚设）；DEBUG 常开（2201 条 / 2607 行） | 出问题时拿不到历史证据；长期写盘压力 | ① `start-server.bat` 改用 `>>` 追加；② `application.yml` 配 `logging.file.name` + rolling policy（10MB × 14 天）；③ 常态级别调 INFO，排障用 `--logging.level...=debug` 临时打开 | **0.5 人时** |

### P2 —— 可选优化

| # | 问题 | 证据 | 建议 |
|---|------|------|------|
| P2-1 | 死代码 6 处 | `TaskMapper.listTemplatesForChild`/`countByDateStatus`、`PetLevelMapper.getByLevel`、`PointRecordMapper.listByChild`、`UserMapper.findById`、`RewardService.listClaimsByChild`、`ChildService.getAvatarUrl` —— 全项目无调用方 | 直接删除，或补上使用它们的页面 |
| P2-2 | `family_user` 重复唯一索引 | `schema.sql:203` + `:217` 两个同名索引 | 删掉 `ux_family_user_username`（保留表约束那条） |
| P2-3 | MyBatis 注解/XML 混用 + `<script>` 内嵌 | `TaskMapper.java:74-100`；`ReportMapper.xml` 是唯一 XML | 二选一统一；动态 SQL 迁到 XML |
| P2-4 | 模板片段重复 4 处 | `board:13-31` / `dashboard:40-61` / `kid:41-52` / `pet-card:15-30` | 抽 `fragments :: petProgress` + `fragments :: avatar` |
| P2-5 | CSS 版本戳手工维护 | `fragments.html:9` `?v=8` | 用 `spring.web.resources.chain.strategy.content` 内容指纹，或至少给 `/css/**` 配 no-cache |
| P2-6 | `operator` 硬编码"家长" | `TaskController.java:94,105`、`PointController.java:39`、`HabitController.java:45` | 已有 `loginUser.getDisplayName()`，应写入真人，便于日后查"谁批的" |
| P2-7 | DEBUG 日志全开 | `application.yml:47-50` | 改 `info`，需要时再开 |
| P2-8 | README 与代码不一致 | `README.md:130` 称"成长积分只增不减"，但 `PointService.java:89-90` 对 `TYPE_DEDUCT` 会扣减 growth | **代码是对的**（符合团队规则），改 README：成长值"完成任务增加、坏习惯扣分减少、兑换奖励不变" |
| P2-9 | 头像无像素上限 | `AvatarService.java:62-69` | 解码后校验 `w*h` 上限（如 4000×4000） |
| P2-10 | `AvatarService.saveAvatar` 无事务 | `AvatarService.java:49-82` | 加 `@Transactional`，或先写临时文件再原子 move |
| P2-11 | `TaskController` 双职责 | `TaskController.java:32-154` | 拆出 `TaskTemplateController` |
| P2-12 | 登录页明文暴露用户名清单 | `login.html:26` placeholder 直接写"家长：parent ／ 孩子：zifan、zihe" | 等于把账号名写在门上。改为只提示"请输入用户名"，或改由家长自定义后再显示 |
| P2-13 | `reward_item.stock` 潜在 NPE（QA P2-2 交叉确认） | `RewardService.java:71` `if (item.getStock() == 0)` 对 `Integer` 拆箱；`schema.sql:153` `stock INT DEFAULT -1` **无 NOT NULL**，NULL 即 NPE（当前 8 条数据全非 NULL 故未触发） | 代码改为 `Integer s = item.getStock(); if (s != null && s == 0)`；DB 加 `NOT NULL DEFAULT -1` 从源头消除 |
| P2-14 | `grantClaim/rejectClaim` 用全表扫描找单条（QA P2-3 交叉确认） | `RewardService.java:101-104,118-121`：`rewardMapper.listClaims().stream().filter(id)` —— 每次兑现/拒绝都拉全表（含 2 个 JOIN）再内存过滤；`RewardMapper` 无 `getClaimById` | 新增 `getClaimById(Long id)` 按主键查；顺带避免 `listClaims` 的 JOIN 变化导致漏查 |
| P2-15 | `thymeleaf.cache: false` 写死在主配置（QA P2-8 交叉确认） | `application.yml:24` | 生产发布前切 `true`；或拆 `application-dev.yml` 只在 dev 关闭 |
| P2-16 | 参数类型/缺参异常降级为"跳首页"，用户看不到原因（QA P2-7） | `/kid/abc`、`/tasks?date=not-a-date` 302 → `/`；`GlobalExceptionHandler:20-24` 无 Referer 时吞掉原因 | 无 Referer 时把 `ex.getMessage()` 一并展示，或按来源路由回对应列表页 |

---

## 10. 建议的执行顺序

```
第 1 批（本周，0.5 天）：P0-4 产物分离 + stop.bat                     ← 先解决"每次打包都心惊胆战"
                        P0-5 去掉 Math.abs + 积分范围校验 + CHECK 约束 ← 一行级，收益最高
                        S6   三处判空 + 异常兜底                       ← 一行级，消灭实测 500
第 2 批（本周，1 天）  ：P0-1 积分原子化 + P0-2 并发幂等               ← 数据正确性
第 3 批（本周，0.5 天）：P1-6 补 4 个核心测试                         ← 给第 2 批上保险
                        （双轨加减含 EXCHANGE 不扣 growth / claim 余额库存 / approve 幂等 / 负分拒绝）
第 4 批（下周）      ：P0-3 强改密 + P1-1 CSRF + P1-2 会话固定 + P1-11 调整上限
第 5 批（有余力）    ：P1-4 缓存 pet_level + P1-5 分层收口 + P1-7 DTO + P1-12 日志治理
```

> 完整批次与跨报告合并口径见 §12。

---

## 11. 已确认**没有问题**的项（明确排除，避免含糊）

| 项 | 结论 |
|----|------|
| SQL 注入 | 不存在。全量 `#{}`，无 `${}` 拼接 |
| XSS | 不存在。无 `th:utext`，唯一 `th:inline="javascript"` 由 Thymeleaf 转义 |
| 头像上传路径穿越 | 安全。文件名服务端生成，不拼接用户输入 |
| 循环依赖 | 不存在。依赖方向单向收敛到 `PointService` |
| God Class | 不存在。最大类 182 行，平均 50 行 |
| 积分双轨规则正确性 | 正确且**只有一处实现**，与业务规则完全一致；当前 DB 数据自洽 |
| 实体与 schema 对齐 | 已对齐（含后加的 `child.avatar_url`），已实测确认 |
| 关键索引 | 齐备，数据量极小（task_instance 70 行），无全表扫描风险 |
| 每日任务生成幂等 | 正确。`ON CONFLICT DO NOTHING`（`TaskMapper:69`） |
| 审批**串行**重复提交幂等 | **通过**（QA 实测 `approve ×3` 仅 1 条流水）；未解风险仅限并发窗口，见 P0-2 |
| 口令哈希实现 | 合格。PBKDF2/SHA256/120k/随机盐/定长比较 |
| 孩子角色越权 | 实测已被正确拦截（路径级 + 资源级双重校验） |
| 灵宠不掉级（兑换场景） | 正确。`PointService:86-90`；QA 用 live + 历史流水（#98 EXCHANGE −30 时 `growth_after` 保持 62）双重验证 ✅ |
| 孤儿 / 悬挂数据 | QA 9 项检查全 0 ✅ |

---

## 12. 与 QA 报告（`docs/qa-diagnosis.md`）的交叉验证

QA（严过关）的运行时实测与本次静态诊断**结论一致、互为补充**。合并口径如下：

| QA 编号 | 问题 | 与本报告的关系 | 最终处置 |
|---------|------|----------------|----------|
| QA P1-1 | `/kid/{id}`、`/pets/{id}` 不存在 ID → 500 NPE | = 本报告 S6 / P1-8，**QA 提供了实测复现**（我原本只标"潜在风险"） | **升级为确认缺陷**，纳入第 1 批（一行判空） |
| QA P1-2 | `/tasks/{id}/delete` 不存在 ID → 500 NPE | 同上 | 同上 |
| QA P1-3 | 日志截断 + DEBUG 全开 + `app-err.log` 恒空 | 本报告 C4 只写了 DEBUG 全开，**遗漏截断与 stderr 合并** | 并入 **P1-12** |
| QA **P1-4** | **负分任务被 `Math.abs` 转正** | **本报告原版遗漏**，本次交叉验证最大收获 | 新增 **P0-5** |
| QA P2-1 | 零测试 = 无回归网 | = 本报告 P1-6，两边独立得出同一结论 | 保留高位优先级 |
| QA P2-2 | `reward_item.stock` 拆箱 NPE | 本报告遗漏 | 新增 **P2-13** |
| QA P2-3 | `grantClaim` 全表扫描 | 本报告只记为"死代码"，未识别性能问题 | 新增 **P2-14** |
| QA P2-4 | 大额负向调整不可逆 | 本报告遗漏 | 新增 **P1-11** |
| QA P2-5 | 缺参/错方法返回裸 400 | 并入兜底建议 | 并入 **S6** |
| QA P2-6 | `/board` 免鉴权 | = 本报告 S4 / P1-3，两边一致 | 保留 P1 |
| QA P2-7 | 类型错误被降级跳首页 | 本报告遗漏 | 新增 **P2-16** |
| QA P2-8 | `thymeleaf.cache=false` | 本报告遗漏 | 新增 **P2-15** |

### 两处需要明确的口径差异（避免团队误判）

1. **幂等性**：QA 实测"顺序重复提交安全"（`approve ×3` → 仅 1 条流水）。我原报告写的"连点两下双份积分"**在串行下不成立**，已修正为"并发窗口风险（代码路径明确、未实测触发）"。→ 修 P0-2 不必恐慌，但并发保护仍建议加。
2. **P0 数量**：QA 判定 P0 = 0（"是否阻塞"角度），我判定 P0 = 5（"数据是否会静默错 + 工程是否可持续"角度）。**两者不矛盾**：QA 看系统可用性，我看正确性与可维护性。建议按本报告 P0 清单执行 —— 它覆盖的都是"不修不会立刻崩、但错了很难发现"的类型。
3. **积分双轨规则本身**：两边独立验证均通过（我：代码路径 + DB 对账；QA：live + 历史流水 #98）。**规则实现无误，缺的是测试保护**，这是两边共识的第一优先级。

---

*报告结束（v2，已合并 QA 交叉验证）。所有结论均可按"证据"列的文件与行号复核；未实测的推断已在正文中标注为"潜在风险"。*
