import { afterEach, describe, expect, it, vi } from "vitest";
import {
  buildSurveyPayload,
  emptyAnswers,
  hasAnyAnswer,
  useSurvey,
} from "@/composables/useSurvey";

function mockFetch(status: number) {
  const fetchMock = vi.fn().mockResolvedValue({ ok: status < 400, status });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("survey answers", () => {
  it("treats whitespace-only text and null ratings as unanswered", () => {
    expect(hasAnyAnswer({ ...emptyAnswers(), diagnosis: "   " })).toBe(false);
    expect(hasAnyAnswer({ ...emptyAnswers(), needs_addressed: 2 })).toBe(true);
    expect(
      hasAnyAnswer({ ...emptyAnswers(), diagnosis_status: "under_evaluation" })
    ).toBe(true);
  });

  it("builds a payload with only contract keys, trimmed text, and null for skipped ratings", () => {
    const payload = buildSurveyPayload(
      { ...emptyAnswers(), helpfulness: 5, unmet_needs: "  Feeding therapy  " },
      "es"
    );

    expect(Object.keys(payload).sort()).toEqual(
      [
        "additional_comments",
        "app_version",
        "confidence_after",
        "diagnosis",
        "diagnosis_status",
        "difficulty_before",
        "ease_of_use",
        "experience_before",
        "helpfulness",
        "improvements",
        "locale",
        "needs_addressed",
        "platform",
        "unmet_needs",
      ].sort()
    );
    expect(payload).toMatchObject({
      helpfulness: 5,
      ease_of_use: null,
      unmet_needs: "Feeding therapy",
      locale: "es",
      platform: "web",
    });
  });
});

describe("useSurvey submit", () => {
  it("does not call the API when nothing is answered", async () => {
    const fetchMock = mockFetch(201);
    const { status, submit } = useSurvey();

    await submit("en");

    expect(status.value).toBe("empty");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("posts the survey and clears the form on success", async () => {
    const fetchMock = mockFetch(201);
    const { answers, status, submit } = useSurvey();
    answers.ease_of_use = 4;

    await submit("en");

    expect(status.value).toBe("success");
    expect(answers.ease_of_use).toBeNull();
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/surveys\/responses\/$/);
    expect(JSON.parse(init.body)).toMatchObject({
      ease_of_use: 4,
      platform: "web",
    });
  });

  it("reports rate limiting separately from other failures", async () => {
    mockFetch(429);
    const limited = useSurvey();
    limited.answers.diagnosis = "ASD";
    await limited.submit("en");
    expect(limited.status.value).toBe("rateLimited");
    expect(limited.answers.diagnosis).toBe("ASD");

    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    const offline = useSurvey();
    offline.answers.diagnosis = "ASD";
    await offline.submit("en");
    expect(offline.status.value).toBe("error");
  });
});
