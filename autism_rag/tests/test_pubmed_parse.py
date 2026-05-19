from autism_rag.sources.adapters.pubmed import PubMedAdapter

SAMPLE = b"""<?xml version="1.0"?>
<PubmedArticleSet>
  <PubmedArticle>
    <MedlineCitation>
      <PMID>99999</PMID>
      <Article>
        <ArticleTitle>Autism screening in toddlers</ArticleTitle>
        <Abstract>
          <AbstractText Label="BACKGROUND">Background text.</AbstractText>
          <AbstractText Label="METHODS">Methods text.</AbstractText>
        </Abstract>
        <AuthorList>
          <Author>
            <LastName>Doe</LastName>
            <ForeName>Jane</ForeName>
          </Author>
        </AuthorList>
        <Journal><Title>J Autism</Title>
          <JournalIssue><PubDate><Year>2024</Year></PubDate></JournalIssue>
        </Journal>
      </Article>
    </MedlineCitation>
    <PubmedData>
      <ArticleIdList>
        <ArticleId IdType="doi">10.1234/example</ArticleId>
      </ArticleIdList>
    </PubmedData>
  </PubmedArticle>
</PubmedArticleSet>
"""


def test_parses_pubmed_xml_into_source_document(monkeypatch):
    adapter = PubMedAdapter.__new__(PubMedAdapter)
    # Bypass __init__ to avoid loading settings; we only need _parse_pubmed_xml.
    adapter.source_key = "pubmed"
    docs = list(adapter._parse_pubmed_xml(SAMPLE.decode("utf-8")))
    assert len(docs) == 1
    doc = docs[0]
    assert doc.source_id == "99999"
    assert "Background text." in doc.text
    assert doc.citation_ids["DOI"] == "10.1234/example"
    assert doc.authors == ["Jane Doe"]
    assert doc.published_at is not None and doc.published_at.year == 2024
