"""Local environment loading for Django settings.

The loader is intentionally small and non-invasive: values already exported in
the shell win over `.env` values, which keeps deployment environments explicit.
"""

import os
from pathlib import Path

from dotenv import dotenv_values, load_dotenv


def load_local_env(base_dir: Path) -> dict[str, str]:
    """Load `<base_dir>/.env` without overriding existing environment values."""
    env_path = base_dir / ".env"
    if not env_path.exists():
        return {}

    declared_values = {
        key: value
        for key, value in dotenv_values(env_path).items()
        if key and value is not None
    }
    load_dotenv(env_path, override=False)

    return {
        key: os.environ[key]
        for key in declared_values
        if key in os.environ
    }
