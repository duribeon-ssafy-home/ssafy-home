import os
import pytest
from langchain.schema import Document
from app.services.document_loader import DocumentLoader

DOCS_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "documents")


def test_load_from_text_returns_chunks():
    loader = DocumentLoader()
    content = "임대차보호법 " * 200
    docs = loader.load_from_text(content, {"source": "test"})
    assert len(docs) >= 1
    assert all(isinstance(d, Document) for d in docs)


def test_load_from_text_metadata_preserved():
    loader = DocumentLoader()
    docs = loader.load_from_text("계약 갱신 청구권 설명", {"source": "법률문서", "year": 2024})
    assert docs[0].metadata["source"] == "법률문서"


def test_load_from_text_chunk_index_added():
    loader = DocumentLoader()
    content = "전세 " * 500
    docs = loader.load_from_text(content, {"source": "test"})
    for i, doc in enumerate(docs):
        assert doc.metadata["chunk_index"] == i


def test_load_from_file_markdown(tmp_path):
    md_file = tmp_path / "test.md"
    md_file.write_text("# 제목\n\n내용입니다.", encoding="utf-8")
    loader = DocumentLoader()
    docs = loader.load_from_file(str(md_file))
    assert len(docs) >= 1
    assert docs[0].metadata["source"] == "test.md"


def test_load_all_from_directory():
    loader = DocumentLoader()
    docs = loader.load_all_from_directory(DOCS_DIR)
    assert len(docs) >= 3
    sources = {d.metadata.get("source") for d in docs}
    assert "임대차보호법_핵심조항.md" in sources
