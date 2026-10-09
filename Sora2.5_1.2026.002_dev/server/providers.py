"""Провайдеры генерации видео. Сейчас: demo (без ключа) и fal.ai (Wan и др.).
Чтобы добавить свою модель, напиши ещё одну функцию generate_xxx и подключи её в generate()."""
import base64
import os
import time
from pathlib import Path

import httpx

import media

FAL_KEY = os.getenv("FAL_KEY", "")
# ПРОВЕРЬ точные id моделей и параметры на https://fal.ai/models (они меняются).
FAL_T2V = os.getenv("FAL_T2V_MODEL", "fal-ai/wan/v2.2-a14b/text-to-video")
FAL_I2V = os.getenv("FAL_I2V_MODEL", "fal-ai/wan/v2.2-a14b/image-to-video")
PROVIDER = os.getenv("PROVIDER", "fal" if FAL_KEY else "demo")


def _fal(model: str, payload: dict, dest: Path) -> None:
    headers = {"Authorization": f"Key {FAL_KEY}"}
    with httpx.Client(timeout=60) as c:
        r = c.post(f"https://queue.fal.run/{model}", json=payload, headers=headers)
        r.raise_for_status()
        job = r.json()
        deadline = time.time() + 15 * 60
        while time.time() < deadline:
            st = c.get(job["status_url"], headers=headers).json()
            if st.get("status") == "COMPLETED":
                break
            if st.get("status") in ("FAILED", "ERROR"):
                raise RuntimeError("Модель вернула ошибку")
            time.sleep(4)
        else:
            raise RuntimeError("Слишком долгая генерация")
        res = c.get(job["response_url"], headers=headers)
        res.raise_for_status()
        url = res.json()["video"]["url"]
        with c.stream("GET", url) as s:
            s.raise_for_status()
            with open(dest, "wb") as f:
                for chunk in s.iter_bytes():
                    f.write(chunk)


def generate(mode: str, prompt: str, image: bytes | None, duration: int,
             aspect: str, dest: Path) -> None:
    """Создаёт сырое видео (без вотермарки) в dest."""
    if PROVIDER == "demo":
        media.demo_video(dest, aspect, min(duration, 10))
        return
    payload: dict = {"prompt": prompt, "aspect_ratio": aspect}
    if mode in ("image", "character"):
        if not image:
            raise ValueError("Нужно фото")
        payload["image_url"] = "data:image/jpeg;base64," + base64.b64encode(image).decode()
        _fal(FAL_I2V, payload, dest)
    else:
        _fal(FAL_T2V, payload, dest)
