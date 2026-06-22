import os
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain.prompts import ChatPromptTemplate
from langchain.schema.runnable import RunnableParallel, RunnablePassthrough
from langchain.schema.output_parser import StrOutputParser
from app.services.vector_store import VectorStoreManager
from app.services.document_loader import DocumentLoader
from app.schemas.response import ChatResponse, SourceCitation
from app.config import settings

PROMPT = """당신은 한국의 전세 및 임대차 법률 전문가입니다.
아래 제공된 법률 문서와 공식 자료만을 근거로 답변하세요.
문서에 없는 내용은 "제공된 문서에 해당 정보가 없습니다"라고 답하세요.

[참고 문서]
{context}

[질문]
{question}

핵심 답변을 먼저 제시하고, 근거 조항이나 문서를 명시하세요."""


class RAGService:

    def __init__(self):
        self.vs_manager = VectorStoreManager.get_instance()
        self.loader = DocumentLoader()
        self.llm = ChatGoogleGenerativeAI(
            model="gemini-1.5-flash",
            google_api_key=settings.google_api_key,
            temperature=0.1,
            max_output_tokens=2048,
        )
        self._chain = self._build_chain()

    def _build_chain(self):
        retriever = self.vs_manager.get_retriever()

        def format_docs(docs):
            return "\n\n---\n\n".join(
                f"[출처: {d.metadata.get('source', '알 수 없음')}]\n{d.page_content}"
                for d in docs
            )

        prompt = ChatPromptTemplate.from_template(PROMPT)

        return (
            RunnableParallel(
                context=retriever | format_docs,
                question=RunnablePassthrough(),
                source_docs=retriever,
            )
            | {
                "answer": prompt | self.llm | StrOutputParser(),
                "source_docs": lambda x: x["source_docs"],
                "question": lambda x: x["question"],
            }
        )

    def chat(self, question: str) -> ChatResponse:
        result = self._chain.invoke(question)
        sources = [
            SourceCitation(
                source=doc.metadata.get("source", "알 수 없음"),
                content=doc.page_content[:200],
            )
            for doc in result["source_docs"]
        ]
        unique_sources = list({s.source: s for s in sources}.values())
        return ChatResponse(
            answer=result["answer"],
            sources=unique_sources,
            query=question,
        )

    def add_documents(self, content: str, metadata: dict) -> int:
        docs = self.loader.load_from_text(content, metadata)
        self.vs_manager.add_documents(docs)
        return len(docs)

    def initialize_from_directory(self, dir_path: str) -> int:
        docs = self.loader.load_all_from_directory(dir_path)
        if docs:
            self.vs_manager.add_documents(docs)
        return len(docs)
