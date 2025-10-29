import base64
from django.http import HttpResponse
from django.conf import settings


class BasicAuthMiddleware:
    """
    Simple basic auth middleware for protecting admin pages
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        # Only protect client-portal URLs
        if request.path.startswith("/client-portal/"):
            # Check if basic auth is provided
            auth_header = request.META.get("HTTP_AUTHORIZATION", "")
            if auth_header.startswith("Basic "):
                try:
                    auth_decoded = base64.b64decode(auth_header[6:]).decode("utf-8")
                    username, password = auth_decoded.split(":", 1)

                    # Check against settings
                    if username == getattr(
                        settings, "BASIC_AUTH_USERNAME", "admin"
                    ) and password == getattr(
                        settings, "BASIC_AUTH_PASSWORD", "secure-password"
                    ):
                        return self.get_response(request)
                except:
                    pass

            # Return 401 Unauthorized
            response = HttpResponse("Unauthorized", status=401)
            response["WWW-Authenticate"] = 'Basic realm="Client Portal"'
            return response

        return self.get_response(request)
