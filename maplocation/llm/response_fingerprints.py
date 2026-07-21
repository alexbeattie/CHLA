"""Short-lived, answer-bound tokens for anonymous assistant response reports."""

import hashlib
import hmac
import secrets

from django.core import signing


FINGERPRINT_SALT = "llm.response-report.v1"
FINGERPRINT_MAX_AGE_SECONDS = 24 * 60 * 60
INVALID_RESPONSE_FINGERPRINT_CODE = "invalid_response_fingerprint"
INVALID_RESPONSE_FINGERPRINT_DETAIL = "Invalid or expired response fingerprint."


class InvalidResponseFingerprint(ValueError):
    """Raised when a report token is invalid, expired, or answer-mismatched."""


def _answer_digest(response: str) -> str:
    return hashlib.sha256(response.encode("utf-8")).hexdigest()


def issue_response_fingerprint(response: str) -> str:
    """Sign an answer digest and random nonce without embedding user content."""
    return signing.dumps(
        {
            "answer_sha256": _answer_digest(response),
            "nonce": secrets.token_urlsafe(16),
        },
        salt=FINGERPRINT_SALT,
        compress=True,
    )


def response_fingerprint_digest(token: str) -> str:
    """Return the one-way value used for database uniqueness checks."""
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def validate_response_fingerprint(token: str, response: str) -> str:
    """Validate age, structure, and exact answer binding; return storage digest."""
    try:
        payload = signing.loads(
            token,
            salt=FINGERPRINT_SALT,
            max_age=FINGERPRINT_MAX_AGE_SECONDS,
        )
    except signing.BadSignature as error:
        raise InvalidResponseFingerprint from error

    if not isinstance(payload, dict) or set(payload) != {"answer_sha256", "nonce"}:
        raise InvalidResponseFingerprint

    answer_sha256 = payload.get("answer_sha256")
    nonce = payload.get("nonce")
    if not isinstance(answer_sha256, str) or not isinstance(nonce, str) or not nonce:
        raise InvalidResponseFingerprint

    if not hmac.compare_digest(answer_sha256, _answer_digest(response)):
        raise InvalidResponseFingerprint

    return response_fingerprint_digest(token)
