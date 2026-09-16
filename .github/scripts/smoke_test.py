"Exercise the packaged debug APK on a disposable Android emulator."
import pathlib
import subprocess
import time

PACKAGE = "com.shiftpay.app"
ACTIVITY = f"{PACKAGE}/.MainActivity"
TAG = "ShiftPaySmoke"
REPORTS = pathlib.Path("smoke-reports")
REPORTS.mkdir(exist_ok=True)


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, timeout=45)


def launch(stage):
    output = adb(
        "shell", "am", "start", "-W",
        "-n", ACTIVITY,
        "--es", "shiftpaySmoke", stage,
    )
    assert "Status: ok" in output, output


def wait_for(stage, timeout=45):
    expected = f"{stage.upper()}_PASS"
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        logs = adb("logcat", "-d", "-s", f"{TAG}:I", "*:S")
        if expected in logs:
            return
        time.sleep(1)
    logs = adb("logcat", "-d")
    raise AssertionError(f"Missing {expected}\n{logs[-12000:]}")


try:
    adb("install", "-r", "apk/app-debug.apk")
    adb("logcat", "-c")

    launch("stage1")
    wait_for("stage1")

    adb("shell", "am", "force-stop", PACKAGE)
    launch("stage2")
    wait_for("stage2")

    adb("shell", "am", "force-stop", PACKAGE)
    launch("stage3")
    wait_for("stage3")

    assert adb("shell", "pidof", PACKAGE).strip(), "Application process exited"
    print("PASS: APK launch, start/end shift, localStorage persistence, and history persistence")
finally:
    (REPORTS / "logcat.txt").write_text(adb("logcat", "-d"), encoding="utf-8")
    with (REPORTS / "screen.png").open("wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, check=True)
