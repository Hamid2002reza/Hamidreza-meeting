import os, json, secrets
from typing import Optional
import httpx
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="Hamidreza Meeting AI Backend", version="11.0.0")
BACKEND_TOKEN = os.getenv("BACKEND_TOKEN", "")
AI_BASE_URL = os.getenv("AI_BASE_URL", "https://api.openai.com/v1")
AI_API_KEY = os.getenv("AI_API_KEY", "")
AI_MODEL = os.getenv("AI_MODEL", "gpt-4.1-mini")
MAX_TRANSCRIPT_CHARS = min(int(os.getenv("MAX_TRANSCRIPT_CHARS", "200000")), 200000)

class MeetingRequest(BaseModel):
    title: str = Field(max_length=500)
    date: str = Field(default="", max_length=100)
    host: str = Field(default="", max_length=500)
    participants: str = Field(default="", max_length=5000)
    category: str = Field(default="", max_length=500)
    confidentiality: str = Field(default="", max_length=100)
    agenda: str = Field(default="", max_length=20000)
    transcript: str = Field(min_length=1, max_length=200000)

SYSTEM_PROMPT = """You are a professional Persian meeting analyst for technical and management meetings.
Return ONLY valid JSON with: executive_summary, decisions, action_items, risks, technical_issues, management_attention, follow_ups, unresolved_items.
Never invent facts. Preserve IPs, hostnames, ticket IDs, VLANs, protocols, versions and numeric values exactly.
Write concise Persian suitable for senior management. Distinguish decisions from suggestions."""

@app.get("/health")
def health():
    return {"status": "ok", "service": "hamidreza-meeting-ai", "version": "11.0.0"}

@app.post("/v1/meeting/analyze")
async def analyze(req: MeetingRequest, authorization: Optional[str] = Header(default=None)):
    if not BACKEND_TOKEN:
        raise HTTPException(status_code=503, detail="BACKEND_TOKEN is not configured")
    if not authorization or not secrets.compare_digest(authorization, f"Bearer {BACKEND_TOKEN}"):
        raise HTTPException(status_code=401, detail="Unauthorized")
    if not AI_API_KEY:
        raise HTTPException(status_code=503, detail="AI_API_KEY is not configured on backend")
    if not AI_BASE_URL.startswith("https://"):
        raise HTTPException(status_code=500, detail="AI_BASE_URL must use HTTPS")

    user = {"meeting": req.model_dump(exclude={"transcript"}), "transcript": req.transcript}
    payload = {
        "model": AI_MODEL,
        "temperature": 0.1,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": json.dumps(user, ensure_ascii=False)},
        ],
    }
    headers = {"Authorization": f"Bearer {AI_API_KEY}", "Content-Type": "application/json"}
    try:
        async with httpx.AsyncClient(timeout=120, follow_redirects=False) as client:
            r = await client.post(AI_BASE_URL.rstrip("/") + "/chat/completions", headers=headers, json=payload)
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"AI provider connection failed: {type(e).__name__}")
    if r.status_code >= 400:
        raise HTTPException(status_code=502, detail=f"AI provider error {r.status_code}")
    try:
        content = r.json()["choices"][0]["message"]["content"]
        return json.loads(content)
    except Exception:
        raise HTTPException(status_code=502, detail="Invalid AI JSON response")
