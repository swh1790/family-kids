# -*- coding: utf-8 -*-
"""孩子提交任务归属校验：自己的能提交，兄弟的不能"""
import http.cookiejar
import urllib.request
import urllib.parse
import urllib.error

BASE = 'http://localhost:8090'


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def make_opener():
    return urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()), NoRedirect)


def req(opener, method, path, data=None):
    body = urllib.parse.urlencode(data).encode() if data else None
    r = urllib.request.Request(BASE + path, data=body, method=method)
    try:
        resp = opener.open(r, timeout=10)
        return resp.status, resp.read().decode('utf-8', 'ignore'), resp.headers.get('Location')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'ignore'), e.headers.get('Location')


kid = make_opener()
s, _, loc = req(kid, 'POST', '/login', {'username': 'zifan', 'password': '123456'})
print('login ->', s, loc)

# 1. 提交自己的任务（child_id=1）
s, _, loc = req(kid, 'POST', '/tasks/462/submit', {'redirect': 'kid'})
print('submit own task 462 ->', s, loc)

# 2. 提交兄弟的任务（child_id=2）
s, _, loc = req(kid, 'POST', '/tasks/433/submit', {'redirect': 'kid'})
print('submit brother task 433 ->', s, loc)

# 3. api 提交兄弟任务
s, body, _ = req(kid, 'POST', '/api/tasks/433/submit')
print('api submit brother task 433 ->', s, body[:80])

# 4. api 提交自己的另一个任务
s, body, _ = req(kid, 'POST', '/api/tasks/463/submit')
print('api submit own task 463 ->', s, body[:80])
