# Hamidreza Meeting AI Backend — V11

## Production rules
- فقط HTTPS.
- `BACKEND_TOKEN` اجباری است و باید طولانی و تصادفی باشد.
- `AI_API_KEY` فقط روی سرور نگهداری می‌شود و هرگز داخل APK قرار نمی‌گیرد.
- لاگ کردن Transcript خام ممنوع است مگر با سیاست سازمانی صریح.
- برای Production، Reverse Proxy با TLS، rate limiting، firewall و audit logging لازم است.

## Run
```bash
docker build -t hamidreza-meeting-ai .
docker run --env-file .env -p 8000:8000 hamidreza-meeting-ai
```

Endpoint: `POST /v1/meeting/analyze`
Health: `GET /health`
