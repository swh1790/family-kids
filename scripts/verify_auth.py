# -*- coding: utf-8 -*-
"""登录与角色权限端到端验证脚本"""
import http.cookiejar
import urllib.request
import urllib.parse
import urllib.error

BASE = 'http://localhost:8090'
KID_HOME = '/kid/1'


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def make_opener():
    cj = http.cookiejar.CookieJar()
    return urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(cj), NoRedirect)


def req(opener, method, path, data=None):
    url = BASE + path
    body = urllib.parse.urlencode(data).encode() if data else None
    r = urllib.request.Request(url, data=body, method=method)
    try:
        resp = opener.open(r, timeout=10)
        return resp.status, resp.read().decode('utf-8', 'ignore'), resp.headers.get('Location')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'ignore'), e.headers.get('Location')


def show(desc, ok, status, loc=''):
    flag = 'OK  ' if ok else 'FAIL'
    print(f'  [{flag}] {desc} -> {status} {loc}')


def section(t):
    print('\n========== ' + t + ' ==========')


# ---------- A. 未登录 ----------
section('A. 未登录')
anon = make_opener()
for p in ['/', '/tasks', '/report', '/rewards', '/points', '/kid/1']:
    s, _, loc = req(anon, 'GET', p)
    show('GET ' + p + ' 应跳转登录页', s == 302 and loc is not None and '/login' in str(loc), s, loc)

s, body, _ = req(anon, 'GET', '/login')
show('GET /login 登录页可访问', s == 200, s)
show('登录页包含按钮与 css v8', '登 录' in body and 'app.css?v=8' in body, s)

s, _, _ = req(anon, 'GET', '/css/app.css')
show('GET /css/app.css 静态资源未被拦截', s == 200, s)

s, _, _ = req(anon, 'GET', '/board')
show('GET /board 客厅看板放行匿名', s == 200, s)

s, body, _ = req(anon, 'POST', '/api/tasks/1/submit')
show('POST /api 未登录返回 401 JSON', s == 401, s, body[:50])

# ---------- B. 家长登录 ----------
section('B. 家长登录 parent / 123456')
parent = make_opener()
s, _, loc = req(parent, 'POST', '/login', {'username': 'parent', 'password': '123456'})
show('登录后跳控制台', s == 302 and str(loc).endswith('/'), s, loc)

for p in ['/', '/tasks', '/tasks/templates', '/habits', '/pets', '/pets/1',
          '/points', '/rewards', '/report', '/board', '/kid/1', '/kid/2']:
    s, _, _ = req(parent, 'GET', p)
    show('GET ' + p + ' 家长可访问', s == 200, s)

# ---------- C. 孩子登录 zifan ----------
section('C. 孩子登录 zifan / 123456')
kid = make_opener()
s, _, loc = req(kid, 'POST', '/login', {'username': 'zifan', 'password': '123456'})
show('登录后自动跳自己主页', s == 302 and KID_HOME in str(loc), s, loc)

s, body, _ = req(kid, 'GET', KID_HOME)
show('GET /kid/1 自己的主页', s == 200, s)
show('孩子端无家长菜单、有退出登录', '退出登录' in body and '习惯扣分' not in body, s)

s, _, loc = req(kid, 'GET', '/kid/2')
show('GET /kid/2 兄弟主页被弹回', s == 302 and KID_HOME in str(loc), s, loc)

for p in ['/tasks', '/report', '/points', '/rewards', '/habits', '/tasks/templates']:
    s, _, loc = req(kid, 'GET', p)
    show('GET ' + p + ' 被拦回自己主页', s == 302 and KID_HOME in str(loc), s, loc)

s, body, _ = req(kid, 'POST', '/api/tasks/1/submit')
show('POST /api 越权任务被拒', s in (400, 403), s, body[:50])

# ---------- D. 错误密码 ----------
section('D. 错误密码')
bad = make_opener()
s, body, _ = req(bad, 'POST', '/login', {'username': 'parent', 'password': 'wrong'})
show('错误密码留在登录页并提示', s == 200 and '用户名或密码错误' in body, s)

# ---------- E. 退出登录 ----------
section('E. 退出登录')
s, _, loc = req(parent, 'GET', '/logout')
show('GET /logout 跳登录页', s == 302 and '/login' in str(loc), s, loc)
s, _, loc = req(parent, 'GET', '/')
show('退出后访问 / 需重新登录', s == 302 and '/login' in str(loc), s, loc)

print('\n验证结束')
