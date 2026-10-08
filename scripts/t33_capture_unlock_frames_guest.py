# -*- coding: utf-8 -*-
"""T33/G4：孩子端「拼图解锁」多帧证据采集（客端录屏版）

背景：主机侧批量 `screencap` + `pull` 会触发主机内存尖峰并 OOM 杀模拟器（曾 5 次失败）。
本脚本改用 **guest 侧单进程录屏** `screenrecord`，主机只在末尾 pull 一次 mp4，
再交给 ffmpeg 抽帧，内存开销与稳定性完全不同量级（2026-10-08 实测 1 次成功）。

另：不再把 child token 编译进 APK（TTL 15 分钟 vs 构建 111s+ 的架构性死结），
改为安装后写入 App 的 shared_prefs（明文 XML，键 flutter.wishpool.accessToken）。

用法：
  python t33_capture_unlock_frames_guest.py inject
  python t33_capture_unlock_frames_guest.py frame --tag before
  python t33_capture_unlock_frames_guest.py record --wish-id <id> --mode A|B --secs 22 --out <dir>
  python t33_capture_unlock_frames_guest.py extract --mp4 <dir>/anim.mp4 --out <dir> --fps 5

环境变量（默认值按本机）：ADB、SERIAL、CORE、ITOK、FAMILY、CHILD、PKG
"""
import argparse, base64, hashlib, json, os, subprocess, sys, time, urllib.request, urllib.error

ADB    = os.environ.get("ADB", r"D:\Android\sdk\platform-tools\adb.exe")
SERIAL = os.environ.get("SERIAL", "emulator-5554")
FFMPEG = os.environ.get("FFMPEG", "ffmpeg")
PKG    = os.environ.get("PKG", "com.example.wishpool_mobile")
CORE   = os.environ.get("CORE", "http://127.0.0.1:18080")
ITOK   = os.environ.get("ITOK", "wishpool-local-internal-token")
FAMILY = os.environ.get("FAMILY", "")
CHILD  = os.environ.get("CHILD", "")
PHONE  = os.environ.get("PHONE", "")
PREFS  = "/data/data/%s/shared_prefs/FlutterSharedPreferences.xml" % PKG
TAB_XINYUAN = (324, 2242)  # 底部「心愿」tab（1080x2400）


def sh(args, timeout=180, inp=None):
    p = subprocess.run(args, capture_output=True, input=inp, timeout=timeout)
    return ((p.stdout or b"") + (p.stderr or b"")).decode("utf-8", "replace").strip()


def adb(*args, **kw):
    return sh([ADB, "-s", SERIAL, *args], **kw)


def req(method, path, token=None, body=None):
    h = {"Accept": "application/json"}
    if body is not None:
        h["Content-Type"] = "application/json"
    if token:
        h["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(CORE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            raw = resp.read().decode("utf-8", "replace")
            return resp.status, (json.loads(raw) if raw.strip().startswith(("{", "[")) else raw)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:400]
    except Exception as e:
        return -1, str(e)


def fresh_token():
    """走 配对会话 → consume 取 child token（TTL 15 分钟 = 会话 expiresAt）。"""
    _, b = req("POST", "/auth/phone-codes", body={"phoneNumber": PHONE, "purpose": "login"})
    _, b2 = req("POST", "/auth/login", body={"provider": "phone", "credential": "%s:123456" % b["verificationToken"]})
    _, s = req("POST", "/families/%s/pairing-sessions" % FAMILY, token=b2["accessToken"], body={"childId": CHILD})
    _, c = req("POST", "/pairing/consume", body={"pairingCode": s["pairingCode"],
                                                 "device": {"platform": "android", "deviceName": "hermes-t33-frames"}})
    tk = c["accessToken"]
    seg = tk.split(".")[0]
    exp = int(base64.urlsafe_b64decode(seg + "=" * (-len(seg) % 4)).decode("utf-8", "replace").split("|")[-1])
    return tk, exp


def cmd_inject(a):
    tk, exp = fresh_token()
    xml = ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
           '    <string name="flutter.wishpool.familyId">%s</string>\n'
           '    <string name="flutter.wishpool.displayName">WishPool</string>\n'
           '    <string name="flutter.wishpool.role">child_device</string>\n'
           '    <string name="flutter.wishpool.refreshToken"></string>\n'
           '    <string name="flutter.wishpool.childId">%s</string>\n'
           '    <string name="flutter.wishpool.accessToken">%s</string>\n'
           "</map>\n") % (FAMILY, CHILD, tk)
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1.0)
    # adb shell 不做参数引号保护：必须用「纯参数」命令，避免 `sh -c "..."` 被拆散
    adb("shell", "run-as", PKG, "mkdir", "-p", "/data/data/%s/shared_prefs" % PKG)
    sh([ADB, "-s", SERIAL, "shell", "run-as", PKG, "tee", PREFS], inp=xml.encode("utf-8"))
    got = adb("shell", "run-as", PKG, "cat", PREFS)
    ok = tk[:40] in got
    print(json.dumps({"injected": ok, "ttl_s": int(exp - time.time()),
                      "exp": time.strftime("%H:%M:%S", time.localtime(exp))}, ensure_ascii=False))
    return 0 if ok else 1


def cmd_frame(a):
    p = os.path.join(a.out, "cap_%s.png" % a.tag) if a.out else "cap_%s.png" % a.tag
    with open(p, "wb") as fh:
        subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=fh, timeout=60)
    print(json.dumps({"file": p, "bytes": os.path.getsize(p)}))
    return 0


def cmd_record(a):
    os.makedirs(a.out, exist_ok=True)
    dev = "/sdcard/t33_anim.mp4"
    adb("shell", "rm", "-f", dev)
    t0 = time.time()
    log = {"wishId": a.wish_id, "mode": a.mode, "secs": a.secs, "events": [{"ev": "screenrecord started"}]}
    rec = subprocess.Popen([ADB, "-s", SERIAL, "shell", "screenrecord", "--bit-rate", "8M",
                            "--time-limit", str(a.secs), dev], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    time.sleep(3.0)
    if a.mode == "B":
        # 冷启动：录屏中重启 App（覆盖「挂载即播放」的实现）
        adb("shell", "am", "force-stop", PKG); time.sleep(0.5)
        adb("shell", "am", "start", "-n", "%s/.MainActivity" % PKG)
        log["events"].append({"t": round(time.time() - t0, 2), "ev": "restart app"})
        time.sleep(14)
        adb("shell", "input", "tap", str(TAB_XINYUAN[0]), str(TAB_XINYUAN[1]))
        adb("shell", "input", "swipe", "540", "700", "540", "1900", "250")
        log["events"].append({"t": round(time.time() - t0, 2), "ev": "tap 心愿 + pull-to-refresh"})
    else:
        r = urllib.request.Request("%s/internal/debug/wishes/%s/unlock" % (CORE, a.wish_id), data=b"{}",
                                   headers={"X-Internal-Token": ITOK, "Content-Type": "application/json"}, method="POST")
        try:
            with urllib.request.urlopen(r, timeout=60) as resp:
                st, body = resp.status, resp.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            st, body = e.code, e.read().decode("utf-8", "replace")
        log["unlock"] = {"http": st, "body": body[:800], "t": round(time.time() - t0, 2)}
        log["events"].append({"t": round(time.time() - t0, 2), "ev": "unlock http=%s" % st})
        time.sleep(0.5)
        adb("shell", "input", "swipe", "540", "700", "540", "1900", "250")   # 下拉刷新触发重新拉快照
        log["events"].append({"t": round(time.time() - t0, 2), "ev": "pull-to-refresh"})
    try:
        rec.wait(timeout=a.secs + 40)
    except subprocess.TimeoutExpired:
        rec.kill()
    local = os.path.join(a.out, "anim.mp4")
    log["pull"] = adb("pull", dev, local)[-200:]
    log["mp4_bytes"] = os.path.getsize(local) if os.path.exists(local) else 0
    json.dump(log, open(os.path.join(a.out, "record_log.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(json.dumps(log, ensure_ascii=False, indent=1)[:1500])
    return 0 if log["mp4_bytes"] > 10000 else 1


def cmd_extract(a):
    fdir = os.path.join(a.out, "frames")
    os.makedirs(fdir, exist_ok=True)
    for f in os.listdir(fdir):
        if f.endswith(".png"):
            os.remove(os.path.join(fdir, f))
    subprocess.run([FFMPEG, "-y", "-i", a.mp4, "-vf", "fps=%s" % a.fps, os.path.join(fdir, "f-%03d.png")],
                   capture_output=True, timeout=900)
    files = sorted(os.path.join(fdir, f) for f in os.listdir(fdir) if f.endswith(".png"))
    if not files:
        print("NO FRAMES")
        return 1
    from PIL import Image, ImageChops, ImageDraw
    meta, groups = [], {}
    for i, p in enumerate(files):
        md5 = hashlib.md5(open(p, "rb").read()).hexdigest()[:10]
        meta.append({"i": i, "file": p, "t": round(i / float(a.fps), 2), "bytes": os.path.getsize(p), "md5": md5})
        groups.setdefault(md5, []).append(i)
    thumb, cols = (270, 600), 6
    rows = (len(files) + cols - 1) // cols
    sheet = Image.new("RGB", (thumb[0] * cols, thumb[1] * rows), "black")
    dr = ImageDraw.Draw(sheet)
    for m in meta:
        im = Image.open(m["file"]).convert("RGB"); im.thumbnail(thumb)
        x, y = (m["i"] % cols) * thumb[0], (m["i"] // cols) * thumb[1]
        sheet.paste(im, (x, y)); dr.text((x + 5, y + 5), "#%d %ss" % (m["i"], m["t"]), fill="red")
    sheet_path = os.path.join(a.out, "contact-sheet.png")
    sheet.save(sheet_path)
    summ = {"frames": len(files), "fps": a.fps, "distinct_hashes": len(groups), "groups": groups,
            "contact_sheet": sheet_path}
    json.dump({"summary": summ, "frames": meta}, open(os.path.join(a.out, "timeline.json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    print(json.dumps(summ, ensure_ascii=False, indent=1))
    return 0


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("inject").set_defaults(f=cmd_inject)
    p = sub.add_parser("frame"); p.add_argument("--tag", default="frame"); p.add_argument("--out", default="")
    p.set_defaults(f=cmd_frame)
    p = sub.add_parser("record"); p.add_argument("--wish-id", required=True)
    p.add_argument("--mode", default="A", choices=["A", "B"]); p.add_argument("--secs", type=int, default=22)
    p.add_argument("--out", required=True); p.set_defaults(f=cmd_record)
    p = sub.add_parser("extract"); p.add_argument("--mp4", required=True); p.add_argument("--out", required=True)
    p.add_argument("--fps", default="5"); p.set_defaults(f=cmd_extract)
    a = ap.parse_args()
    sys.exit(a.f(a))


if __name__ == "__main__":
    main()
