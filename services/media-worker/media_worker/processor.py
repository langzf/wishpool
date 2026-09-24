from __future__ import annotations

import json
import subprocess
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from PIL import Image, ImageOps, UnidentifiedImageError

from .config import MediaWorkerConfig
from .storage import ObjectStorage


@dataclass(frozen=True)
class MediaProcessor:
    config: MediaWorkerConfig
    storage: ObjectStorage

    def process(self, item: dict[str, Any]) -> list[dict[str, Any]]:
        media = item["media"]
        media_id = media["id"]
        content_type = media["contentType"].split(";")[0].strip().lower()
        source_key = media["storageKey"]
        work_root = Path(self.config.work_dir) / media_id
        source_path = work_root / "source"
        self.storage.download(source_key, source_path)

        if content_type.startswith("image/"):
            return self._process_image(media, source_path)
        if content_type.startswith("audio/"):
            return self._process_audio(media, source_path)
        if content_type.startswith("video/"):
            return self._process_video(media, source_path)
        raise MediaProcessingError("unsupported_content_type", f"Unsupported media content type: {content_type}")

    def _process_image(self, media: dict[str, Any], source_path: Path) -> list[dict[str, Any]]:
        try:
            with Image.open(source_path) as image:
                normalized = ImageOps.exif_transpose(image).convert("RGB")
                return [
                    self._save_image_derivative(media, normalized, "thumbnail", 320),
                    self._save_image_derivative(media, normalized, "preview", 1280),
                    self._save_image_derivative(media, normalized, "ai_ready", 1600),
                ]
        except UnidentifiedImageError as exc:
            raise MediaProcessingError("invalid_media_input", f"Source is not a valid image: {exc}") from None

    def _save_image_derivative(
        self,
        media: dict[str, Any],
        image: Image.Image,
        kind: str,
        max_side: int,
    ) -> dict[str, Any]:
        output = Path(self.config.work_dir) / media["id"] / f"{kind}.jpg"
        copy = image.copy()
        copy.thumbnail((max_side, max_side))
        output.parent.mkdir(parents=True, exist_ok=True)
        copy.save(output, format="JPEG", quality=86, optimize=True)
        key = derivative_key(media["storageKey"], kind, "jpg")
        size = self.storage.upload(key, output, "image/jpeg")
        return {
            "kind": kind,
            "storageKey": key,
            "contentType": "image/jpeg",
            "sizeBytes": size,
            "metadata": {"width": copy.width, "height": copy.height, "maxSide": max_side},
        }

    def _process_audio(self, media: dict[str, Any], source_path: Path) -> list[dict[str, Any]]:
        work_root = Path(self.config.work_dir) / media["id"]
        transcoded = work_root / "transcoded.m4a"
        waveform = work_root / "waveform.json"
        ai_ready = work_root / "ai_ready.wav"

        run_ffmpeg("-i", str(source_path), "-vn", "-ac", "1", "-ar", "44100", "-c:a", "aac", "-b:a", "128k", str(transcoded))
        run_ffmpeg("-i", str(source_path), "-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", str(ai_ready))
        peaks = read_waveform_peaks(ai_ready)
        waveform.write_text(json.dumps({"peaks": peaks, "sampleCount": len(peaks)}, separators=(",", ":")), encoding="utf-8")

        return [
            upload_file(self.storage, media, "transcoded", transcoded, "audio/mp4", "m4a", {"codec": "aac", "sampleRate": 44100}),
            upload_file(self.storage, media, "waveform", waveform, "application/json", "json", {"sampleCount": len(peaks)}),
            upload_file(self.storage, media, "ai_ready", ai_ready, "audio/wav", "wav", {"sampleRate": 16000, "channels": 1}),
        ]

    def _process_video(self, media: dict[str, Any], source_path: Path) -> list[dict[str, Any]]:
        work_root = Path(self.config.work_dir) / media["id"]
        thumbnail = work_root / "thumbnail.jpg"
        keyframe = work_root / "keyframe.jpg"
        preview = work_root / "preview.mp4"
        ai_ready = work_root / "ai_ready.mp4"
        transcoded = work_root / "transcoded.mp4"

        run_ffmpeg("-ss", "00:00:01", "-i", str(source_path), "-frames:v", "1", "-vf", "scale=640:-2", str(thumbnail))
        run_ffmpeg("-ss", "00:00:03", "-i", str(source_path), "-frames:v", "1", "-vf", "scale=1280:-2", str(keyframe))
        run_ffmpeg("-i", str(source_path), "-t", "8", "-vf", "scale=960:-2", "-an", "-c:v", "libx264", "-preset", "veryfast", "-crf", "28", str(preview))
        run_ffmpeg("-i", str(source_path), "-vf", "scale=1280:-2", "-c:v", "libx264", "-preset", "veryfast", "-crf", "24", "-c:a", "aac", "-b:a", "128k", str(transcoded))
        run_ffmpeg("-i", str(source_path), "-t", "20", "-vf", "fps=1,scale=768:-2", "-an", "-c:v", "libx264", "-preset", "veryfast", "-crf", "30", str(ai_ready))

        return [
            upload_file(self.storage, media, "thumbnail", thumbnail, "image/jpeg", "jpg", {}),
            upload_file(self.storage, media, "keyframe", keyframe, "image/jpeg", "jpg", {}),
            upload_file(self.storage, media, "preview", preview, "video/mp4", "mp4", {"durationLimitSec": 8}),
            upload_file(self.storage, media, "transcoded", transcoded, "video/mp4", "mp4", {"codec": "h264"}),
            upload_file(self.storage, media, "ai_ready", ai_ready, "video/mp4", "mp4", {"fps": 1, "durationLimitSec": 20}),
        ]


class MediaProcessingError(RuntimeError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


def derivative_key(source_key: str, kind: str, extension: str) -> str:
    stem = source_key.rsplit(".", 1)[0]
    return f"{stem}/derivatives/{kind}.{extension}"


def upload_file(
    storage: ObjectStorage,
    media: dict[str, Any],
    kind: str,
    path: Path,
    content_type: str,
    extension: str,
    metadata: dict[str, Any],
) -> dict[str, Any]:
    key = derivative_key(media["storageKey"], kind, extension)
    size = storage.upload(key, path, content_type)
    return {
        "kind": kind,
        "storageKey": key,
        "contentType": content_type,
        "sizeBytes": size,
        "metadata": metadata,
    }


def run_ffmpeg(*args: str) -> None:
    command = ["ffmpeg", "-y", "-hide_banner", "-loglevel", "error", *args]
    completed = subprocess.run(command, check=False, capture_output=True, text=True)
    if completed.returncode != 0:
        raise MediaProcessingError("ffmpeg_failed", completed.stderr.strip() or "ffmpeg failed")


def read_waveform_peaks(wav_path: Path, buckets: int = 96) -> list[float]:
    with wave.open(str(wav_path), "rb") as audio:
        frames = audio.readframes(audio.getnframes())
        sample_width = audio.getsampwidth()
    if sample_width != 2 or not frames:
        return []
    values = [int.from_bytes(frames[index : index + 2], "little", signed=True) for index in range(0, len(frames), 2)]
    bucket_size = max(len(values) // buckets, 1)
    peaks: list[float] = []
    for index in range(0, len(values), bucket_size):
        segment = values[index : index + bucket_size]
        peak = max(abs(value) for value in segment) / 32768
        peaks.append(round(peak, 4))
        if len(peaks) >= buckets:
            break
    return peaks
