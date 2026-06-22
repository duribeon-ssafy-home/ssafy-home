import os
from langchain_community.document_loaders import TextLoader, UnstructuredMarkdownLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.schema import Document
from app.config import settings


class DocumentLoader:

    def __init__(self):
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.chunk_size,
            chunk_overlap=settings.chunk_overlap,
            separators=["\n\n", "\n", "。", ".", " ", ""],
        )

    def load_from_file(self, file_path: str) -> list[Document]:
        ext = os.path.splitext(file_path)[1].lower()
        if ext == ".md":
            loader = UnstructuredMarkdownLoader(file_path)
        elif ext == ".txt":
            loader = TextLoader(file_path, encoding="utf-8")
        else:
            raise ValueError(f"지원하지 않는 파일 형식: {ext}")
        raw_docs = loader.load()
        chunks = self.splitter.split_documents(raw_docs)
        return self._add_metadata(chunks, os.path.basename(file_path))

    def load_from_text(self, content: str, metadata: dict) -> list[Document]:
        doc = Document(page_content=content, metadata=dict(metadata))
        chunks = self.splitter.split_documents([doc])
        for i, chunk in enumerate(chunks):
            chunk.metadata["chunk_index"] = i
        return chunks

    def load_all_from_directory(self, dir_path: str) -> list[Document]:
        all_docs = []
        for filename in sorted(os.listdir(dir_path)):
            file_path = os.path.join(dir_path, filename)
            if not os.path.isfile(file_path):
                continue
            try:
                all_docs.extend(self.load_from_file(file_path))
            except ValueError:
                pass
        return all_docs

    def _add_metadata(self, docs: list[Document], source_name: str) -> list[Document]:
        for i, doc in enumerate(docs):
            doc.metadata["source"] = source_name
            doc.metadata["chunk_index"] = i
        return docs
