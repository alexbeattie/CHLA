"""Tests for the shared-secret bearer token verifier.

This deliberately avoids FastMCP's ``StaticTokenVerifier`` (documented as
unsafe for production) and full OAuth/JWT (overkill for a handful of
trusted clients). It validates a single secret from an environment
variable using a constant-time comparison.
"""

import asyncio


def test_build_auth_provider_returns_none_when_token_not_configured():
    from kindd_mcp.auth import build_auth_provider

    assert build_auth_provider(expected_token=None) is None
    assert build_auth_provider(expected_token="") is None


def test_build_auth_provider_returns_verifier_when_token_configured():
    from kindd_mcp.auth import SharedSecretTokenVerifier, build_auth_provider

    provider = build_auth_provider(expected_token="super-secret-token")

    assert isinstance(provider, SharedSecretTokenVerifier)


def test_verify_token_accepts_matching_token():
    from kindd_mcp.auth import SharedSecretTokenVerifier

    verifier = SharedSecretTokenVerifier("super-secret-token")

    result = asyncio.run(verifier.verify_token("super-secret-token"))

    assert result is not None
    assert result.token == "super-secret-token"


def test_verify_token_rejects_mismatched_token():
    from kindd_mcp.auth import SharedSecretTokenVerifier

    verifier = SharedSecretTokenVerifier("super-secret-token")

    result = asyncio.run(verifier.verify_token("wrong-token"))

    assert result is None


def test_verify_token_rejects_empty_token():
    from kindd_mcp.auth import SharedSecretTokenVerifier

    verifier = SharedSecretTokenVerifier("super-secret-token")

    result = asyncio.run(verifier.verify_token(""))

    assert result is None
