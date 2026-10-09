"""Сервер Sora 2.5: принимает запрос из приложения, генерирует видео,
вшивает вотермарку, раздаёт файл и обновляет черновик в Firestore."""
import os
import threading
import uuid
from pathlib import Path

import firebase_admin
from fastapi import BackgroundTasks, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.staticfiles import StaticFiles
from firebase_admin import auth as fb_auth
from firebase_admin import firestore

import media
import providers

BASE = Path(__file__).parent
VIDEOS = BASE / "videos"
TMP = BASE / "tmp"
VIDEOS.mkdir(exist_ok=True)
TMP.mkdir(exist_ok=True)

PUBLIC_URL = os.getenv("PUBLIC_URL", "http://10.0.2.2:8000").rstrip("/")
MAX_ACTIVE_PER_USER = int(os.getenv("MAX_ACTIVE_PER_USER", "2"))  # защита от расхода денег

firebase_admin.initialize_app()  # ключ сервисного аккаунта: GOOGLE_APPLICATION_CREDENTIALS
db = firestore.client()

app = FastAPI(title="Sora 2.5 server")
app.mount("/videos", StaticFiles(directory=VIDEOS), name="videos")

_active: dict[str, int] = {}
_lock = threading.Lock()


def _uid(authorization: str) -> str:
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "Нет токена")
    try:
        return fb_auth.verify_id_token(authorization[7:])["uid"]
    except Exception:
        raise HTTPException(401, "Неверный токен")


def _job(doc_id: str, uid: str, mode: str, prompt: str, image: bytes | None,
         duration: int, aspect: str) -> None:
    ref = db.collection("videos").document(doc_id)
    raw = TMP / f"{doc_id}_raw.mp4"
    out = VIDEOS / f"{doc_id}.mp4"
    try:
        ref.update({"status": "generating"})
        providers.generate(mode, prompt, image, duration, aspect, raw)
        media.burn_watermark(raw, out)
        ref.update({"status": "ready", "videoUrl": f"{PUBLIC_URL}/videos/{doc_id}.mp4"})
    except Exception as e:  # noqa: BLE001
        ref.update({"status": "failed", "error": str(e)[:200]})
    finally:
        raw.unlink(missing_ok=True)
        with _lock:
            _active[uid] = max(0, _active.get(uid, 1) - 1)


@app.post("/generate")
async def generate(
    bg: BackgroundTasks,
    mode: str = Form(...),
    prompt: str = Form(...),
    duration: int = Form(5),
    aspect: str = Form("9:16"),
    character: str = Form(""),
    image: UploadFile | None = File(None),
    authorization: str = Header(""),
):
    uid = _uid(authorization)
    if mode not in ("text", "character", "image"):
        raise HTTPException(400, "Неверный режим")
    if aspect not in ("9:16", "16:9"):
        raise HTTPException(400, "Неверный формат")
    with _lock:
        if _active.get(uid, 0) >= MAX_ACTIVE_PER_USER:
            raise HTTPException(429, "Дождись окончания текущих генераций")
        _active[uid] = _active.get(uid, 0) + 1

    img = await image.read() if image else None
    full_prompt = f"{character}: {prompt}" if character else prompt
    ref = db.collection("videos").document(uuid.uuid4().hex)
    ref.set({
        "uid": uid, "prompt": prompt, "mode": mode, "status": "queued",
        "published": False, "videoUrl": "", "error": "",
        "createdAt": firestore.SERVER_TIMESTAMP,
    })
    bg.add_task(_job, ref.id, uid, mode, full_prompt, img, duration, aspect)
    return {"id": ref.id}


@app.get("/health")
def health():
    return {"ok": True, "provider": providers.PROVIDER}
