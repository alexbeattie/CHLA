"""Shared-secret bearer token authentication for the kindd_mcp server.

This intentionally avoids FastMCP's ``StaticTokenVerifier`` (documented by
FastMCP as unsafe for production because tokens live in source) and full
OAuth/JWT (overkill for a small, known set of trusted clients such as
KiNDD's own agent and a handful of team members' AI clients). Instead it
validates a single secret loaded from an environment variable using a
constant-time comparison, matching the operational shape teams want for
remote MCP clients: one ``Authorization: Bearer <token>`` value per
deployment, rotated by updating the environment variable (e.g. in AWS
Secrets Manager / Elastic Beanstalk configuration), never committed to
source control.
"""

from __future__ import annotations

import hmac
import os
from typing import Optional

from fastmcp.server.auth import AccessToken, TokenVerifier

ENV_VAR_NAME = "KINDD_MCP_AUTH_TOKEN"


class SharedSecretTokenVerifier(TokenVerifier):
    """Validates a single shared-secret bearer token."""

    def __init__(self, expected_token: str, *, client_id: str = "kindd-mcp-client") -> None:
        super().__init__()
        self._expected_token = expected_token
        self._client_id = client_id

    async def verify_token(self, token: str) -> Optional[AccessToken]:
        if not self._expected_token or not token:
            return None
        if not hmac.compare_digest(token, self._expected_token):
            return None
        return AccessToken(token=token, client_id=self._client_id, scopes=[])


def build_auth_provider(
    expected_token: Optional[str] = None,
) -> Optional[SharedSecretTokenVerifier]:
    """Build the auth provider from ``KINDD_MCP_AUTH_TOKEN``, or ``None``.

    Returning ``None`` leaves the server unauthenticated. That is only
    appropriate for local development; production deployments should set
    ``KINDD_MCP_AUTH_TOKEN`` so a verifier is always returned.
    """
    token = expected_token if expected_token is not None else os.environ.get(ENV_VAR_NAME)
    if not token:
        return None
    return SharedSecretTokenVerifier(token)
