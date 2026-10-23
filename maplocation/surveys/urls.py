from django.urls import path

from .views import SurveyResponseView

urlpatterns = [
    path("responses/", SurveyResponseView.as_view(), name="survey-response"),
]
