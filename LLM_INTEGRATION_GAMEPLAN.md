# KiNDD LLM Integration Game Plan

## Vision
Transform KiNDD from a static provider directory into an **intelligent navigator** that understands eligibility, synthesizes across systems, and reduces the cognitive load on families navigating neurodevelopmental services.

---

## Phase 1: Foundation (Weeks 1-2)

### 1.1 Extend Current Data Model

Your existing `ProviderV2` model is a good start. Extend it:

```python
# maplocation/locations/models.py

class ProviderV2(models.Model):
    # Existing fields...
    
    # NEW: Structured eligibility data
    insurance_networks = models.JSONField(default=list)  # ["Kaiser", "Blue Shield", "Medi-Cal"]
    ages_served_min = models.IntegerField(default=0)
    ages_served_max = models.IntegerField(default=99)
    accepts_regional_center = models.BooleanField(default=True)
    
    # NEW: Freshness tracking
    last_verified = models.DateTimeField(null=True)
    verification_source = models.CharField(max_length=50, null=True)  # "scraper", "provider", "user_report"
    waitlist_weeks = models.IntegerField(null=True)  # Current estimated wait
    accepting_new_patients = models.BooleanField(default=True)
    
    # NEW: For semantic search
    embedding = VectorField(dimensions=1536, null=True)  # pgvector
    
    class Meta:
        indexes = [
            # Your existing indexes...
            HnswIndex(fields=['embedding'], name='provider_embedding_idx', opclasses=['vector_cosine_ops']),
        ]
```

### 1.2 Add pgvector to PostgreSQL

```bash
# On RDS (or local)
psql -c "CREATE EXTENSION IF NOT EXISTS vector;"

# Django migration
pip install pgvector django-pgvector
```

### 1.3 Create Embedding Pipeline

```python
# maplocation/locations/embeddings.py

import openai
from django.conf import settings

def generate_provider_embedding(provider: ProviderV2) -> list[float]:
    """Generate embedding from provider's searchable attributes."""
    
    text = f"""
    Provider: {provider.name}
    Services: {', '.join(provider.therapy_types or [])}
    Specialties: {provider.diagnoses_treated}
    Age groups: {', '.join(provider.age_groups or [])}
    Location: {provider.address}
    Insurance: {', '.join(provider.insurance_networks or [])}
    Regional Center: {provider.regional_center}
    Description: {provider.description or ''}
    """
    
    response = openai.embeddings.create(
        model="text-embedding-3-small",
        input=text.strip()
    )
    
    return response.data[0].embedding

def embed_all_providers():
    """Batch embed all providers."""
    from .models import ProviderV2
    
    providers = ProviderV2.objects.filter(embedding__isnull=True)
    
    for provider in providers:
        provider.embedding = generate_provider_embedding(provider)
        provider.save(update_fields=['embedding'])
```

---

## Phase 2: LLM Query Layer (Weeks 2-3)

### 2.1 RAG Architecture

```python
# maplocation/llm/query.py

from openai import OpenAI
from django.db import connection
from locations.models import ProviderV2, RegionalCenter

client = OpenAI()

SYSTEM_PROMPT = """You are KiNDD, an expert navigator for neurodevelopmental services in Los Angeles County.

You help families find:
- ABA therapy providers
- Speech-language pathologists  
- Occupational therapists
- Physical therapists
- Developmental pediatricians
- Regional Center services

You understand:
- California Regional Center system (7 centers in LA County)
- Insurance networks (Medi-Cal, Kaiser, commercial plans)
- Age-based eligibility (Early Start 0-3, school-age, transition 18+)
- Waitlist realities and alternatives

Always be specific, cite provider names, and acknowledge when information may be outdated.
When you don't know something, say so clearly."""


def semantic_search(query: str, limit: int = 10) -> list[ProviderV2]:
    """Find relevant providers using vector similarity."""
    
    # Generate query embedding
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=query
    )
    query_embedding = response.data[0].embedding
    
    # Cosine similarity search via pgvector
    with connection.cursor() as cursor:
        cursor.execute("""
            SELECT id, 1 - (embedding <=> %s::vector) as similarity
            FROM locations_providerv2
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """, [query_embedding, query_embedding, limit])
        
        results = cursor.fetchall()
    
    provider_ids = [r[0] for r in results]
    return list(ProviderV2.objects.filter(id__in=provider_ids))


def answer_query(user_query: str, user_context: dict = None) -> str:
    """
    Answer a natural language query about neurodevelopmental services.
    
    user_context example:
    {
        "zip_code": "90210",
        "child_age": 4,
        "diagnosis": "autism",
        "insurance": "Kaiser",
        "regional_center": "Westside Regional Center",
        "current_services": ["speech therapy"]
    }
    """
    
    # Step 1: Retrieve relevant providers
    relevant_providers = semantic_search(user_query, limit=15)
    
    # Step 2: Get user's regional center if we have their ZIP
    regional_center = None
    if user_context and user_context.get('zip_code'):
        regional_center = RegionalCenter.find_by_zip_code(user_context['zip_code'])
    
    # Step 3: Build context for LLM
    provider_context = "\n\n".join([
        f"""**{p.name}**
        - Services: {', '.join(p.therapy_types or ['Not specified'])}
        - Ages: {', '.join(p.age_groups or ['All ages'])}
        - Address: {p.address}
        - Phone: {p.phone}
        - Accepts: {', '.join(p.insurance_networks or ['Contact for insurance info'])}
        - Waitlist: {f'{p.waitlist_weeks} weeks' if p.waitlist_weeks else 'Unknown'}
        - Last verified: {p.last_verified or 'Not recently verified'}"""
        for p in relevant_providers
    ])
    
    user_context_str = ""
    if user_context:
        user_context_str = f"""
        
USER CONTEXT:
- Location: {user_context.get('zip_code', 'Unknown')}
- Child's age: {user_context.get('child_age', 'Unknown')}
- Diagnosis: {user_context.get('diagnosis', 'Unknown')}
- Insurance: {user_context.get('insurance', 'Unknown')}
- Regional Center: {regional_center.name if regional_center else 'Unknown'}
- Current services: {', '.join(user_context.get('current_services', []))}
"""
    
    # Step 4: Call LLM
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": f"""
AVAILABLE PROVIDERS IN DATABASE:
{provider_context}
{user_context_str}

USER QUESTION: {user_query}

Provide a helpful, specific answer. Recommend specific providers when appropriate.
If the user's insurance or location affects recommendations, explain how.
"""}
    ]
    
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=messages,
        temperature=0.3,
        max_tokens=1000
    )
    
    return response.choices[0].message.content
```

### 2.2 API Endpoints

```python
# maplocation/llm/views.py

from rest_framework.views import APIView
from rest_framework.response import Response
from .query import answer_query

class AskKiNDDView(APIView):
    """Natural language query endpoint."""
    
    def post(self, request):
        query = request.data.get('query')
        user_context = request.data.get('context', {})
        
        if not query:
            return Response({'error': 'Query required'}, status=400)
        
        try:
            answer = answer_query(query, user_context)
            return Response({
                'query': query,
                'answer': answer,
            })
        except Exception as e:
            return Response({'error': str(e)}, status=500)


# maplocation/llm/urls.py
from django.urls import path
from .views import AskKiNDDView

urlpatterns = [
    path('ask/', AskKiNDDView.as_view(), name='ask-kindd'),
]
```

---

## Phase 3: Data Freshness System (Weeks 3-5)

### 3.1 Multi-Source Data Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                     DATA SOURCES                                 │
├──────────────┬──────────────┬──────────────┬───────────────────┤
│  Scrapers    │  Provider    │  User        │  Manual           │
│  (automated) │  Portal      │  Reports     │  Research         │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  RAW DATA LAKE (S3)                              │
│   - Timestamped snapshots                                        │
│   - Source attribution                                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              NORMALIZATION PIPELINE                              │
│   - Schema mapping                                               │
│   - Deduplication (fuzzy matching on name/address)              │
│   - Conflict resolution                                          │
│   - Staleness detection                                          │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              POSTGRES + PGVECTOR                                 │
│   - Canonical provider records                                   │
│   - Embeddings for semantic search                              │
│   - Audit trail of changes                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Scraper Framework

```python
# maplocation/scrapers/base.py

from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime
import httpx

@dataclass
class RawProvider:
    """Standardized intermediate format from any source."""
    source: str
    source_id: str
    name: str
    address: str | None
    phone: str | None
    services: list[str]
    insurance: list[str]
    ages: str | None
    raw_data: dict  # Original scraped data
    scraped_at: datetime


class BaseScraper(ABC):
    """Base class for all data scrapers."""
    
    source_name: str
    
    def __init__(self):
        self.client = httpx.Client(timeout=30)
    
    @abstractmethod
    def scrape(self) -> list[RawProvider]:
        """Fetch and parse data from source."""
        pass
    
    def run(self) -> list[RawProvider]:
        """Execute scrape with error handling and logging."""
        print(f"[{self.source_name}] Starting scrape...")
        try:
            providers = self.scrape()
            print(f"[{self.source_name}] Found {len(providers)} providers")
            return providers
        except Exception as e:
            print(f"[{self.source_name}] Error: {e}")
            return []


# maplocation/scrapers/regional_centers.py

class WestsideRCScraper(BaseScraper):
    """Scrape Westside Regional Center vendor list."""
    
    source_name = "westside_rc"
    base_url = "https://www.westsiderc.org"
    
    def scrape(self) -> list[RawProvider]:
        # Implementation specific to their site structure
        # Usually involves:
        # 1. Fetch directory page
        # 2. Parse HTML tables or pagination
        # 3. Extract provider details
        pass


class HarborRCScraper(BaseScraper):
    source_name = "harbor_rc"
    # ...


# maplocation/scrapers/insurance.py

class KaiserProviderScraper(BaseScraper):
    """Scrape Kaiser's provider directory."""
    
    source_name = "kaiser"
    
    def scrape(self) -> list[RawProvider]:
        # Kaiser has a search API
        # Filter by specialty codes for developmental services
        pass
```

### 3.3 Normalization Pipeline

```python
# maplocation/scrapers/normalize.py

from fuzzywuzzy import fuzz
from locations.models import ProviderV2

def normalize_and_merge(raw_providers: list[RawProvider]) -> list[dict]:
    """
    Convert raw scraped data to canonical format,
    merging with existing records where matched.
    """
    
    results = []
    
    for raw in raw_providers:
        # Try to match existing provider
        existing = find_matching_provider(raw)
        
        if existing:
            # Update existing record
            updates = reconcile_data(existing, raw)
            if updates:
                results.append({
                    'action': 'update',
                    'provider_id': existing.id,
                    'updates': updates,
                    'source': raw.source
                })
        else:
            # New provider
            results.append({
                'action': 'create',
                'data': raw_to_canonical(raw),
                'source': raw.source
            })
    
    return results


def find_matching_provider(raw: RawProvider) -> ProviderV2 | None:
    """Fuzzy match against existing providers."""
    
    candidates = ProviderV2.objects.filter(
        name__icontains=raw.name.split()[0]  # First word of name
    )
    
    for candidate in candidates:
        # Name similarity
        name_score = fuzz.ratio(raw.name.lower(), candidate.name.lower())
        
        # Address similarity (if both have addresses)
        addr_score = 0
        if raw.address and candidate.address:
            addr_score = fuzz.partial_ratio(
                raw.address.lower(), 
                candidate.address.lower()
            )
        
        # High confidence match
        if name_score > 85 or (name_score > 70 and addr_score > 80):
            return candidate
    
    return None


def reconcile_data(existing: ProviderV2, raw: RawProvider) -> dict:
    """
    Determine what fields to update.
    Newer data wins, but flag conflicts for review.
    """
    updates = {}
    
    # Phone: prefer non-null, newer
    if raw.phone and raw.phone != existing.phone:
        updates['phone'] = raw.phone
    
    # Services: merge lists
    if raw.services:
        new_services = list(set((existing.therapy_types or []) + raw.services))
        if new_services != existing.therapy_types:
            updates['therapy_types'] = new_services
    
    # Always update verification timestamp
    updates['last_verified'] = raw.scraped_at
    updates['verification_source'] = raw.source
    
    return updates
```

### 3.4 Scheduled Jobs

```python
# maplocation/scrapers/tasks.py

from celery import shared_task
from .regional_centers import WestsideRCScraper, HarborRCScraper
from .normalize import normalize_and_merge

@shared_task
def run_all_scrapers():
    """Daily scrape job."""
    
    scrapers = [
        WestsideRCScraper(),
        HarborRCScraper(),
        # ... other scrapers
    ]
    
    all_raw = []
    for scraper in scrapers:
        all_raw.extend(scraper.run())
    
    # Normalize and apply updates
    changes = normalize_and_merge(all_raw)
    
    # Log summary
    creates = sum(1 for c in changes if c['action'] == 'create')
    updates = sum(1 for c in changes if c['action'] == 'update')
    
    print(f"Scrape complete: {creates} new, {updates} updated")
    
    return {'new': creates, 'updated': updates}


@shared_task
def check_staleness():
    """Flag providers not verified in 90 days."""
    from datetime import timedelta
    from django.utils import timezone
    from locations.models import ProviderV2
    
    threshold = timezone.now() - timedelta(days=90)
    stale = ProviderV2.objects.filter(
        last_verified__lt=threshold
    ).update(needs_verification=True)
    
    print(f"Flagged {stale} providers as needing verification")
```

---

## Phase 4: iOS Integration (Weeks 5-6)

### 4.1 Chat Interface in iOS App

```swift
// CHLA-iOS/Services/LLMService.swift

import Foundation

class LLMService: ObservableObject {
    static let shared = LLMService()
    
    @Published var isLoading = false
    @Published var messages: [ChatMessage] = []
    
    struct ChatMessage: Identifiable {
        let id = UUID()
        let role: Role
        let content: String
        let timestamp = Date()
        
        enum Role {
            case user, assistant
        }
    }
    
    func ask(_ query: String, context: UserContext? = nil) async throws -> String {
        isLoading = true
        defer { isLoading = false }
        
        let url = URL(string: "\(APIService.shared.baseURL)/llm/ask/")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "query": query,
            "context": context?.toDictionary() ?? [:]
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        let response = try JSONDecoder().decode(LLMResponse.self, from: data)
        
        // Add to message history
        await MainActor.run {
            messages.append(ChatMessage(role: .user, content: query))
            messages.append(ChatMessage(role: .assistant, content: response.answer))
        }
        
        return response.answer
    }
}

struct UserContext: Codable {
    var zipCode: String?
    var childAge: Int?
    var diagnosis: String?
    var insurance: String?
    var currentServices: [String]?
    
    func toDictionary() -> [String: Any] {
        var dict: [String: Any] = [:]
        if let zip = zipCode { dict["zip_code"] = zip }
        if let age = childAge { dict["child_age"] = age }
        if let dx = diagnosis { dict["diagnosis"] = dx }
        if let ins = insurance { dict["insurance"] = ins }
        if let services = currentServices { dict["current_services"] = services }
        return dict
    }
}

struct LLMResponse: Codable {
    let query: String
    let answer: String
}
```

### 4.2 Chat View

```swift
// CHLA-iOS/Views/ChatView.swift

import SwiftUI

struct ChatView: View {
    @StateObject private var llmService = LLMService.shared
    @EnvironmentObject var appState: AppState
    @State private var inputText = ""
    @FocusState private var isFocused: Bool
    
    var body: some View {
        VStack(spacing: 0) {
            // Messages
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        // Welcome message
                        if llmService.messages.isEmpty {
                            WelcomeCard()
                                .padding()
                        }
                        
                        ForEach(llmService.messages) { message in
                            MessageBubble(message: message)
                                .id(message.id)
                        }
                        
                        if llmService.isLoading {
                            TypingIndicator()
                        }
                    }
                    .padding()
                }
                .onChange(of: llmService.messages.count) { _ in
                    if let last = llmService.messages.last {
                        withAnimation {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
            }
            
            // Input bar
            ChatInputBar(text: $inputText, isFocused: $isFocused) {
                sendMessage()
            }
        }
        .navigationTitle("Ask KiNDD")
    }
    
    private func sendMessage() {
        let query = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }
        
        inputText = ""
        
        Task {
            let context = UserContext(
                zipCode: appState.userZipCode,
                childAge: appState.childAge,
                diagnosis: appState.selectedDiagnosis,
                insurance: appState.insurance
            )
            
            try? await llmService.ask(query, context: context)
        }
    }
}

struct WelcomeCard: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 40))
                .foregroundColor(.purple)
            
            Text("Ask me anything about developmental services")
                .font(.headline)
                .multilineTextAlignment(.center)
            
            VStack(alignment: .leading, spacing: 8) {
                SuggestionChip("What ABA providers accept Medi-Cal near 90210?")
                SuggestionChip("My child is turning 3, what changes?")
                SuggestionChip("How do I get a Regional Center assessment?")
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(16)
    }
}
```

---

## Phase 5: Advanced Features (Weeks 6-8)

### 5.1 Eligibility Reasoning Engine

```python
# maplocation/llm/eligibility.py

ELIGIBILITY_RULES = """
## California Regional Center Eligibility

### Developmental Disabilities (Lanterman Act)
Eligible if diagnosed with:
- Intellectual disability
- Cerebral palsy  
- Epilepsy
- Autism
- Conditions requiring similar services

### Early Start (0-3 years)
Eligible if:
- Developmental delay (33%+ in one area or 25%+ in two areas)
- Established risk condition
- High-risk infant

### Age Transitions
- Early Start → Lanterman: Reassessment at age 3
- School transition: IEP process at age 3
- Adult transition: Begins at 16, critical at 18 and 22

### Insurance Considerations
- Medi-Cal: Covers most services, may require managed care navigation
- Kaiser: In-network providers only, limited ABA network
- Commercial: Check autism mandate compliance (SB 946)
"""

def check_eligibility(user_context: dict) -> dict:
    """
    Analyze eligibility based on user's situation.
    Returns structured eligibility assessment.
    """
    
    prompt = f"""
    Based on these California eligibility rules:
    {ELIGIBILITY_RULES}
    
    Analyze this family's situation:
    - Child's age: {user_context.get('child_age')}
    - Diagnosis: {user_context.get('diagnosis')}
    - Insurance: {user_context.get('insurance')}
    - Regional Center: {user_context.get('regional_center')}
    - Current services: {user_context.get('current_services', [])}
    
    Provide:
    1. Current eligibility status for Regional Center
    2. Insurance coverage analysis
    3. Upcoming transitions or deadlines
    4. Recommended next steps
    
    Format as JSON.
    """
    
    # Call LLM with structured output
    # ...
```

### 5.2 Document Generation

```python
# maplocation/llm/documents.py

def generate_iep_request(user_context: dict) -> str:
    """Generate IEP meeting request letter."""
    
    template = """
    [Date]
    
    [School District]
    Special Education Department
    
    Re: Request for IEP Meeting for {child_name}
    
    Dear Special Education Director,
    
    I am writing to formally request an Individualized Education Program (IEP) 
    meeting for my child, {child_name}, date of birth {dob}.
    
    {child_name} has been diagnosed with {diagnosis} and I believe they may 
    require special education services and supports.
    
    Pursuant to IDEA, I am requesting:
    {requested_assessments}
    
    Please contact me within 15 days to schedule this meeting.
    
    Sincerely,
    {parent_name}
    {contact_info}
    """
    
    # Use LLM to customize based on specific situation
    # ...
```

---

## Implementation Priority

### Must Have (MVP)
1. ✅ Existing provider database with PostGIS
2. Add embeddings to providers (pgvector)
3. Basic `/llm/ask/` endpoint
4. Simple chat UI in iOS app

### Should Have
5. Scraper framework for 2-3 Regional Centers
6. User context integration (age, insurance, location)
7. Staleness tracking and alerts

### Nice to Have
8. Provider self-service portal
9. Document generation
10. Eligibility reasoning engine
11. Waitlist tracking

---

## Cost Estimates

### API Costs (Monthly)
- Embeddings: ~$5-10 (one-time for 370 providers, minimal ongoing)
- GPT-4o queries: ~$50-100 (assuming 1000 queries/month)
- Total: **~$50-100/month**

### Infrastructure
- RDS (current db.t4g.small): $25/month
- pgvector: Included in PostgreSQL
- Celery + Redis (for scrapers): ~$15/month if needed

---

## Quick Start Commands

```bash
# 1. Install dependencies
pip install openai pgvector django-pgvector fuzzywuzzy python-Levenshtein httpx beautifulsoup4

# 2. Add pgvector extension
psql -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 3. Create migration for embeddings
python manage.py makemigrations
python manage.py migrate

# 4. Generate initial embeddings
python manage.py shell
>>> from locations.embeddings import embed_all_providers
>>> embed_all_providers()

# 5. Test query
>>> from llm.query import answer_query
>>> answer_query("What ABA providers near 90210 accept Medi-Cal?")
```

---

## Next Steps

1. **This week**: Add pgvector, create embeddings for all providers
2. **Next week**: Build `/llm/ask/` endpoint, test with sample queries
3. **Week 3**: Add chat UI to iOS app
4. **Week 4**: First scraper (Westside RC), normalization pipeline
5. **Ongoing**: Expand scrapers, refine prompts, gather user feedback
