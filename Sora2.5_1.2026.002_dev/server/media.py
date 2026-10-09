"""Работа с видео: тестовый ролик и наложение вотермарки Sora 2.5 через ffmpeg."""
import os
import subprocess
from pathlib import Path

BASE = Path(__file__).parent
WM_PATH = BASE / "watermark" / "wm.mp4"
# Обрезка пустых полей вокруг знака (w:h:x:y в исходном wm.mp4 1920x816)
WM_CROP = os.getenv("WM_CROP", "1560:390:141:223")
# Цвет фона вотермарки (хромакей) и допуск
WM_BG = os.getenv("WM_BG", "0x8900CB")
WM_SIMILARITY = os.getenv("WM_SIMILARITY", "0.10")
WM_BLEND = os.getenv("WM_BLEND", "0.06")
WM_OPACITY = os.getenv("WM_OPACITY", "0.6")       # 60%
WM_WIDTH_RATIO = float(os.getenv("WM_WIDTH_RATIO", "0.22"))  # ширина знака от ширины видео
WM_MARGIN_RATIO = float(os.getenv("WM_MARGIN_RATIO", "0.02"))
WM_STEP_SEC = int(os.getenv("WM_STEP_SEC", "5"))  # сколько секунд знак стоит на месте


def _run(cmd: list[str]) -> str:
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.strip()[-500:] or "ffmpeg error")
    return r.stdout


def video_size(path: Path) -> tuple[int, int]:
    out = _run(["ffprobe", "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", str(path)])
    w, h = out.strip().split("x")
    return int(w), int(h)


def demo_video(dest: Path, aspect: str = "9:16", seconds: int = 5) -> None:
    """Демо-режим: тестовая картинка вместо настоящей генерации."""
    size = "720x1280" if aspect == "9:16" else "1280x720"
    _run(["ffmpeg", "-y", "-v", "error", "-f", "lavfi",
          "-i", f"testsrc2=s={size}:d={seconds}:r=30",
          "-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "veryfast", str(dest)])


def burn_watermark(src: Path, dest: Path) -> None:
    """Вшивает вотермарку: хромакей фона, прозрачность 60%, каждые 5 с
    меняет угол: левый верх -> правый низ -> левый верх ..."""
    w, _ = video_size(src)
    wm_w = max(64, int(w * WM_WIDTH_RATIO) // 2 * 2)
    m = max(8, int(w * WM_MARGIN_RATIO))
    s = WM_STEP_SEC
    flt = (
        f"[1:v]crop={WM_CROP},format=rgba,"
        f"chromakey={WM_BG}:{WM_SIMILARITY}:{WM_BLEND},"
        f"colorchannelmixer=aa={WM_OPACITY},scale={wm_w}:-1[wm];"
        f"[0:v][wm]overlay="
        f"x='if(lt(mod(t,{2*s}),{s}),{m},W-w-{m})':"
        f"y='if(lt(mod(t,{2*s}),{s}),{m},H-h-{m})':shortest=1[v]"
    )
    _run(["ffmpeg", "-y", "-v", "error", "-i", str(src),
          "-stream_loop", "-1", "-i", str(WM_PATH),
          "-filter_complex", flt, "-map", "[v]", "-map", "0:a?",
          "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p",
          "-c:a", "aac", "-movflags", "+faststart", str(dest)])
