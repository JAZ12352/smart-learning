from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str
    messages: list[dict[str, str]] = Field(default_factory=list)
    user_id: int | None = None
    user_role: str = "student"
    context: dict = Field(default_factory=dict)


class ChatResponse(BaseModel):
    intent: str
    reply: str
    metadata: dict = Field(default_factory=dict)
    sub_agent_results: list[dict] = Field(default_factory=list)


class PaperGenerateRequest(BaseModel):
    subject_id: int
    grade_level: int
    difficulty: int = 3
    question_types: list[int] = Field(default_factory=lambda: [1, 2, 3, 4, 5])
    knowledge_points: list[str] = Field(default_factory=list)
    total_score: int = 100
    question_count: int = 20
    user_id: int | None = None


class StudentAnalysisRequest(BaseModel):
    user_id: int
    subject_id: int | None = None
    exam_ids: list[int] = Field(default_factory=list)
    time_range_days: int = 30


class QuestionRecommendRequest(BaseModel):
    user_id: int
    subject_id: int
    knowledge_point: str | None = None
    difficulty: int | None = None
    count: int = 5
