from ai_worker.config import load_config
from ai_worker.app import run_server


def main() -> None:
    config = load_config()
    run_server(config)


if __name__ == "__main__":
    main()
