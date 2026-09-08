"""V1.1 write-path acceptance checks against an isolated demo instance.

The password is read from APP_TEST_PASSWORD. Credentials, cookies and device keys
are never written to the report.
"""
import argparse
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://127.0.0.1:18081")
parser.add_argument("--output", default="docs/evidence/v11-http-qa.json")
args = parser.parse_args()
password = os.environ.get("APP_TEST_PASSWORD")
if not password:
    raise SystemExit("Set APP_TEST_PASSWORD.")

cookies = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))


def call(path, method="GET", body=None, csrf=None, device_key=None):
    headers = {"Content-Type": "application/json"}
    if csrf:
        headers[csrf["headerName"]] = csrf["token"]
    if device_key:
        headers["X-Device-Key"] = device_key
    request = urllib.request.Request(
        args.base_url + path,
        data=None if body is None else json.dumps(body).encode(),
        headers=headers,
        method=method,
    )
    with client.open(request, timeout=30) as response:
        return response.status, json.load(response)


def expect_error(status, path, method="GET", body=None, csrf=None, device_key=None):
    try:
        call(path, method, body, csrf, device_key)
    except urllib.error.HTTPError as error:
        payload = json.load(error)
        assert error.code == status, payload
        return payload
    raise AssertionError(f"Expected HTTP {status}: {path}")


checks = []


def check(name, action):
    began = time.perf_counter()
    detail = action()
    checks.append({
        "name": name,
        "passed": True,
        "milliseconds": round((time.perf_counter() - began) * 1000, 2),
        **({"detail": detail} if detail is not None else {}),
    })


_, csrf_body = call("/api/v1/auth/csrf")
csrf = csrf_body["data"]
call("/api/v1/auth/login", "POST", {"username": "admin", "password": password}, csrf)
_, csrf_body = call("/api/v1/auth/csrf")
csrf = csrf_body["data"]

_, regions_body = call("/api/v1/regions")
region_id = regions_body["data"][0]["id"]
_, devices_body = call("/api/v1/devices/distribution")
device = next(item for item in devices_body["data"] if item.get("elderId"))
device_id = device["id"]
related_elder_id = device["elderId"]
stamp = datetime.now(timezone.utc) - timedelta(minutes=20)
suffix = str(int(stamp.timestamp() * 1000))


def demo_batch():
    points = [
        {
            "eventId": f"qa-demo-{suffix}-{index}",
            "longitude": 120.1600 + index * 0.0001,
            "latitude": 30.2500 + index * 0.0001,
            "recordedAt": (stamp + timedelta(seconds=index * 30)).isoformat(),
        }
        for index in range(12)
    ]
    _, response = call(
        "/api/v1/demo/locations/batch",
        "POST",
        {"deviceId": device_id, "points": points},
        csrf,
    )
    assert response["data"]["accepted"] == 12, response
    _, repeated = call(
        "/api/v1/demo/locations/batch",
        "POST",
        {"deviceId": device_id, "points": points},
        csrf,
    )
    assert repeated["data"]["duplicates"] == 12, repeated
    return {"accepted": 12, "duplicatesOnReplay": 12}


check("管理员批量模拟轨迹并保持幂等", demo_batch)


def delete_unused():
    _, created = call(
        "/api/v1/elders",
        "POST",
        {
            "code": f"QA-DELETE-{suffix}",
            "name": "V1.1 删除验收",
            "gender": "UNKNOWN",
            "regionId": region_id,
        },
        csrf,
    )
    elder_id = created["data"]["id"]
    call(f"/api/v1/elders/{elder_id}", "DELETE", csrf=csrf)
    expect_error(404, f"/api/v1/elders/{elder_id}")
    return {"deletedId": elder_id}


check("管理员可删除无业务关系的误录档案", delete_unused)


def reject_related_delete():
    payload = expect_error(409, f"/api/v1/elders/{related_elder_id}", "DELETE", csrf=csrf)
    assert "只能归档" in payload["message"], payload
    return {"status": 409, "message": payload["message"]}


check("有关联档案拒绝物理删除", reject_related_delete)


def device_location():
    _, credential = call(f"/api/v1/devices/{device_id}/credential", "POST", csrf=csrf)
    key = credential["data"]["apiKey"]
    body = {
        "deviceId": device_id,
        "points": [{
            "eventId": f"qa-real-location-{suffix}",
            "longitude": 120.1701,
            "latitude": 30.2601,
            "recordedAt": (stamp + timedelta(minutes=10)).isoformat(),
        }],
    }
    expect_error(401, "/api/device/v1/locations", "POST", body)
    _, response = call("/api/device/v1/locations", "POST", body, device_key=key)
    assert response["data"]["accepted"] == 1, response
    return key, {"missingKeyStatus": 401, "accepted": 1}


device_key_box = {}


def run_device_location():
    key, detail = device_location()
    device_key_box["key"] = key
    return detail


check("真实定位接口校验设备密钥并接收定位", run_device_location)


def heartbeat_idempotency():
    body = {
        "deviceId": device_id,
        "eventId": f"qa-heartbeat-{suffix}",
        "recordedAt": (stamp + timedelta(minutes=11)).isoformat(),
    }
    call("/api/device/v1/heartbeats", "POST", body, device_key=device_key_box["key"])
    call("/api/device/v1/heartbeats", "POST", body, device_key=device_key_box["key"])
    conflicting = dict(body)
    conflicting["recordedAt"] = (stamp + timedelta(minutes=12)).isoformat()
    expect_error(409, "/api/device/v1/heartbeats", "POST", conflicting, device_key=device_key_box["key"])
    return {"replayAccepted": True, "conflictingReplayStatus": 409}


check("设备心跳持久幂等并拒绝冲突重放", heartbeat_idempotency)

report = {
    "runAt": datetime.now(timezone(timedelta(hours=8))).isoformat(),
    "baseUrl": args.base_url,
    "database": "MySQL 8.0.46 / isolated port 13317 / 18 tables",
    "checks": checks,
}
output = Path(args.output)
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"passed": len(checks), "total": len(checks), "report": str(output)}, ensure_ascii=False))
