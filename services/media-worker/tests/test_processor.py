from pathlib import Path
import wave

from PIL import Image

from media_worker.config import MediaWorkerConfig
from media_worker.processor import MediaProcessor, derivative_key, read_waveform_peaks


class FakeStorage:
    def __init__(self, source: Path) -> None:
        self.source = source
        self.uploads: dict[str, tuple[Path, str]] = {}

    def download(self, key: str, target: Path) -> None:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(self.source.read_bytes())

    def upload(self, key: str, source: Path, content_type: str) -> int:
        self.uploads[key] = (source, content_type)
        return source.stat().st_size


def test_derivative_key_scopes_under_source_media() -> None:
    assert (
        derivative_key("families/f1/submission/m1.jpg", "thumbnail", "jpg")
        == "families/f1/submission/m1/derivatives/thumbnail.jpg"
    )


def test_image_processing_creates_required_derivatives(tmp_path: Path) -> None:
    source = tmp_path / "source.jpg"
    Image.new("RGB", (640, 480), color=(20, 30, 40)).save(source)
    config = MediaWorkerConfig.from_env({"WISHPOOL_MEDIA_WORK_DIR": str(tmp_path / "work")})
    storage = FakeStorage(source)
    processor = MediaProcessor(config, storage)  # type: ignore[arg-type]

    derivatives = processor.process(
        {
            "media": {
                "id": "media-1",
                "contentType": "image/jpeg",
                "storageKey": "families/f1/submission/media-1.jpg",
            }
        }
    )

    assert [item["kind"] for item in derivatives] == ["thumbnail", "preview", "ai_ready"]
    assert all(item["contentType"] == "image/jpeg" for item in derivatives)
    assert set(storage.uploads) == {item["storageKey"] for item in derivatives}


def test_waveform_returns_empty_for_empty_file(tmp_path: Path) -> None:
    path = tmp_path / "empty.wav"
    with wave.open(str(path), "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(16000)
        audio.writeframes(b"")

    assert read_waveform_peaks(path) == []
