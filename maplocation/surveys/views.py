from django.db import transaction
from rest_framework import status
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .notifications import send_survey_notification
from .serializers import SurveyResponseSerializer
from .throttles import GlobalSurveyThrottle


class SurveyResponseView(APIView):
    """Accept one anonymous family feedback survey submission."""

    permission_classes = [AllowAny]
    authentication_classes = []
    throttle_classes = [GlobalSurveyThrottle]

    def post(self, request):
        serializer = SurveyResponseSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            survey = serializer.save()
            transaction.on_commit(lambda: send_survey_notification(survey))
        return Response(
            {"id": survey.pk, "status": "received"},
            status=status.HTTP_201_CREATED,
        )
