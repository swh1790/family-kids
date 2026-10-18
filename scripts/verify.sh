#!/usr/bin/env bash
# 端到端冒烟验证：启动应用 -> 家长登录 -> 重置数据 -> 跑完整业务链路 -> 校验结果 -> 关闭应用
set -u

BASE="C:/Users/PC/WorkBuddy/2026-10-17-22-41-15"
APP="$BASE/family-kids"
JAVA="$BASE/.tools/jdk-17.0.13+11/bin/java.exe"
PG="C:/Users/PC/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar"
PORT=8090
URL="http://127.0.0.1:$PORT"
CK="$BASE/parent.cookie"

Q() { "$JAVA" -cp "$PG" "$APP/scripts/dbinit/SqlQuery.java" \
        "jdbc:postgresql://localhost:5432/ontobricks" ontobricks devpass "$1" 2>&1; }
# SqlQuery 输出：第1行空行 / 第2行表头 / 第3行分隔线 / 第4行起为数据
QV() { Q "$1" | sed -n '3,20p'; }
# 取单个标量值（第 4 行）
QS() { Q "$1" | sed -n '4p' | tr -d ' '; }

echo "########## 0. 启动应用 ##########"
cd "$APP" || exit 1
"$JAVA" -Dfile.encoding=UTF-8 -jar target/family-kids-1.0.0.jar --server.port=$PORT > "$BASE/app-run.log" 2>&1 &
APPPID=$!
for i in $(seq 1 30); do
  sleep 1
  if curl -s -o /dev/null --max-time 2 "$URL/login"; then echo "应用就绪 (${i}s, pid=$APPPID)"; break; fi
done

echo
echo "########## 0.1 家长登录 ##########"
rm -f "$CK"
curl -s -c "$CK" -o /dev/null -w "  POST /login (parent) -> %{http_code}\n" \
  -X POST -d "username=parent&password=123456" "$URL/login"

echo
echo "########## 1. 重置业务数据 ##########"
Q "SET search_path TO family_kids; DELETE FROM point_record; DELETE FROM reward_claim; DELETE FROM pet_log; DELETE FROM task_instance; UPDATE child SET growth_points=0, balance_points=0, earned_points=0, deducted_points=0, highest_level=1; UPDATE pet SET level=1, stage='灵蛋', exp=0, mood=80, last_level_up_at=NULL;" > /dev/null
echo "已重置"

echo
echo "########## 2. 生成今日任务 ##########"
curl -s -b "$CK" -o /dev/null -w "  POST /tasks/generate -> %{http_code}\n" -X POST "$URL/tasks/generate"
QV "SET search_path TO family_kids; SELECT c.name AS child, COUNT(ti.id) AS task_cnt FROM child c LEFT JOIN task_instance ti ON ti.child_id=c.id GROUP BY c.name ORDER BY c.name"

echo
echo "########## 3. 孩子提交任务 ##########"
PENDING=$(QS "SET search_path TO family_kids; SELECT string_agg(id::text, ',' ORDER BY id) FROM task_instance WHERE child_id=1 AND status='PENDING'")
IFS=',' read -ra ARR <<< "$PENDING"
for id in "${ARR[@]}"; do curl -s -b "$CK" -o /dev/null -X POST "$URL/tasks/$id/submit?redirect=kid"; done
echo "  哥哥提交 ${#ARR[@]} 个任务"
QV "SET search_path TO family_kids; SELECT status, COUNT(*) AS cnt FROM task_instance WHERE child_id=1 GROUP BY status ORDER BY status"

echo
echo "########## 4. 家长审批 ##########"
TODO=$(QS "SET search_path TO family_kids; SELECT string_agg(id::text, ',' ORDER BY id) FROM task_instance WHERE child_id=1 AND status='DONE'")
IFS=',' read -ra ARR2 <<< "$TODO"
for id in "${ARR2[@]}"; do curl -s -b "$CK" -o /dev/null -X POST "$URL/tasks/$id/approve"; done
echo "  审批 ${#ARR2[@]} 个需确认任务"
QV "SET search_path TO family_kids; SELECT status, COUNT(*) AS cnt FROM task_instance WHERE child_id=1 GROUP BY status ORDER BY status"

echo
echo "########## 5. 坏习惯扣分（哥哥 电子产品超时）##########"
curl -s -b "$CK" -o /dev/null -w "  POST /habits/deduct -> %{http_code}\n" -X POST "$URL/habits/deduct?childId=1&habitId=4"

echo
echo "########## 6. 兑换奖励（哥哥 挑一次晚餐菜 30分）##########"
curl -s -b "$CK" -o /dev/null -w "  POST /rewards/claim -> %{http_code}\n" -X POST "$URL/rewards/claim?childId=1&rewardId=1"

echo
echo "########## 7. 家长兑现奖励 ##########"
CID=$(QS "SET search_path TO family_kids; SELECT MAX(id) FROM reward_claim")
curl -s -b "$CK" -o /dev/null -w "  POST /rewards/claim/$CID/grant -> %{http_code}\n" -X POST "$URL/rewards/claim/$CID/grant"

echo
echo "########## 8. 手动加分（弟弟 +20 帮妈妈拿外卖）##########"
curl -s -b "$CK" -o /dev/null -w "  POST /points/adjust -> %{http_code}\n" \
  -X POST "$URL/points/adjust?childId=2&points=20&reason=%E5%B8%AE%E5%A6%88%E5%A6%88%E6%8B%BF%E5%A4%96%E5%8D%96"

echo
echo "########## 9. 异常场景（应友好提示而非 500）##########"
curl -s -b "$CK" -o /dev/null -w "  兑换不存在的奖励记录 grant/999 -> %{http_code}\n" \
  -X POST -H "Referer: $URL/rewards" "$URL/rewards/claim/999/grant"
curl -s -b "$CK" -o /dev/null -w "  积分不足兑换(弟弟 20分换500分)  -> %{http_code}\n" \
  -X POST -H "Referer: $URL/rewards" "$URL/rewards/claim?childId=2&rewardId=7"

echo
echo "########## 10. 最终数据校验 ##########"
echo "-- 孩子积分 --"
QV "SET search_path TO family_kids; SELECT name, growth_points AS growth, balance_points AS balance, earned_points AS earned, deducted_points AS deducted, highest_level AS hi_lv FROM child ORDER BY id"
echo "-- 灵宠状态 --"
QV "SET search_path TO family_kids; SELECT name, species, level, stage, exp, mood FROM pet ORDER BY id"
echo "-- 积分流水（哥哥最近 8 条）--"
QV "SET search_path TO family_kids; SELECT type, source, change_points AS delta, balance_after AS bal, growth_after AS growth, reason FROM point_record WHERE child_id=1 ORDER BY id DESC LIMIT 8"
echo "-- 兑换记录 --"
QV "SET search_path TO family_kids; SELECT rc.id, c.name, ri.name AS reward, rc.cost_points, rc.status FROM reward_claim rc JOIN reward_item ri ON ri.id=rc.reward_id JOIN child c ON c.id=rc.child_id"
echo "-- 灵宠大事记 --"
QV "SET search_path TO family_kids; SELECT title, detail FROM pet_log ORDER BY id"

echo
echo "########## 11. 全量页面状态码（家长已登录）##########"
for p in "" "tasks" "tasks/templates" "habits" "points" "rewards" "pets" "pets/1" "report" "kid/1" "kid/2" "board"; do
  code=$(curl -s -b "$CK" -o /dev/null -w "%{http_code}" --max-time 15 "$URL/$p")
  echo "  GET /$p -> $code"
done

echo
echo "########## 12. 应用错误日志 ##########"
grep -c "ERROR" "$BASE/app-run.log" 2>/dev/null | sed 's/^/  ERROR 行数: /'

kill $APPPID 2>/dev/null
rm -f "$CK"
echo
echo "验证结束，应用已关闭。"
