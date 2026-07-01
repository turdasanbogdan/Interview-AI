# AI Interviewer

An AI-powered interview system built with Spring Boot, Spring AI, and PostgreSQL.
Conducts adaptive interviews with active listening and sufficiency checking.

## Tech Stack

- **Backend:** Spring Boot 3 + Spring AI
- **LLM:** Groq API (Llama 3.3-70b) — free tier available
- **Database:** PostgreSQL 16
- **Infrastructure:** Docker + Docker Compose

## Prerequisites

- Docker Desktop installed and running
- Groq API key (free) → https://console.groq.com

## Setup

**1. Clone the repo**
```bash
git clone https://github.com/turdasanbogdan/Interview-AI.git
cd Interview-AI
```

**2. Get a free Groq API key**
- Go to https://console.groq.com
- Sign up with Google
- Go to API Keys → Create API Key
- Copy the key (starts with `gsk_...`)

**3. Configure environment**

Create .env using .env.example
```bash
cp .env.example .env
```
Open `.env` and add your key:

**4. Run**
```bash
docker-compose up --build
```

API is available at: `http://localhost:8080`

## Test with Postman

**1. Import collection**
- Open Postman
- Click **Import**
- Select `AI-Interviewer.postman_collection.json`

**2. Important**
- Run requests **in order**, top to bottom
- Request 1 saves `interviewId` automatically via Tests script
- You can change the topic in Request 1 body: `{"topic": "your topic here"}`

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/interviews/start` | Start interview `{"topic": "..."}` |
| POST | `/api/interviews/{id}/answer` | Submit answer `{"answer": "..."}` |
| GET | `/api/interviews/{id}` | Get full transcript |

## Stop

```bash
docker-compose down
```
