"""Tests for local .env loading."""


def test_load_local_env_sets_missing_values_without_overriding_existing(
    tmp_path,
    monkeypatch,
):
    from maplocation.env import load_local_env

    env_file = tmp_path / ".env"
    env_file.write_text(
        "\n".join(
            [
                "LANGSMITH_TRACING=true",
                "LANGSMITH_API_KEY=from-file",
                "LANGSMITH_PROJECT=from-file",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.delenv("LANGSMITH_TRACING", raising=False)
    monkeypatch.setenv("LANGSMITH_API_KEY", "from-shell")
    monkeypatch.delenv("LANGSMITH_PROJECT", raising=False)

    loaded = load_local_env(tmp_path)

    assert "LANGSMITH_TRACING" in loaded
    assert "LANGSMITH_API_KEY" in loaded
    assert "LANGSMITH_PROJECT" in loaded
    assert loaded["LANGSMITH_TRACING"] == "true"
    assert loaded["LANGSMITH_API_KEY"] == "from-shell"
    assert loaded["LANGSMITH_PROJECT"] == "from-file"
