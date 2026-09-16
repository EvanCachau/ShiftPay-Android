"""Exercise the packaged debug APK on a disposable Android emulator."""
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.shiftpay.app"
REPORTS = pathlib.Path("smoke-reports")
REPORTS.mkdir(exist_ok=True)


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, timeout=45)


def snapshot():
    adb("shell", "uiautomator", "dump", "/sdcard/shiftpay-ui.xml")
    xml = adb("shell", "cat", "/sdcard/shiftpay-ui.xml")
    (REPORTS / "ui.xml").write_text(xml, encoding="utf-8")
    return ET.fromstring(xml)


def find(label):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        for node in snapshot().iter("node"):
            if any(label in node.get(key, "") for key in ("text", "content-desc")):
                bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                    return bounds
        time.sleep(2)
    raise AssertionError(f"Visible UI element not found: {label}")


def tap(label):
    left, top, right, bottom = find(label)
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(1)


def launch():
    output = adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity")
    assert "Status: ok" in output, output


try:
    adb("install", "-r", "apk/app-debug.apk")
    adb("logcat", "-c")
    launch()
    tap("Commencer mon shift")
    find("Terminer mon shift")
    # Check that an active shift survives a full process restart.
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap("Terminer mon shift")
    tap("Enregistrer")
    find("Commencer mon shift")
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    tap("Historique")
    find("Supprimer")
    assert adb("shell", "pidof", PACKAGE).strip(), "Application process exited"
    print("PASS: APK installation, launch, start/end shift and persistence after restart")
finally:
    (REPORTS / "logcat.txt").write_text(adb("logcat", "-d"), encoding="utf-8")
    with (REPORTS / "screen.png").open("wb") as image:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, check=True)
