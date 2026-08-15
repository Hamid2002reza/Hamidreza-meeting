# Hamidreza Meeting — V11 Release Candidate Foundation

نسخه V11 پروژه را از Prototype به Release Candidate Foundation نزدیک می‌کند.

## امکانات
- مدیریت جلسات فنی/مدیریتی
- Agenda، حاضرین، حوزه، محرمانگی
- Topics / Decisions / Action Items
- ضبط صدا
- Speech-to-Text فارسی و On-Device preference
- Transcript رمزنگاری‌شده با Android Keystore + AES-256-GCM
- AI Backend امن با HTTPS و Bearer token
- تحلیل ساختاریافته: Executive Summary, Decisions, Action Items, Owner, Deadline, Priority, Risks, Technical Issues, Management Attention, Follow-ups, Unresolved Items
- گزارش Word
- Reminder / Notification
- Biometric foundation
- FileProvider
- Cleartext HTTP disabled
- Backup/data extraction exclusions برای داده‌های حساس
- Release minification/shrink resources
- CI build workflow

## امنیت
Android توصیه می‌کند برای حفاظت بیشتر از کلیدها از Android Keystore استفاده شود و AES/GCM از الگوریتم‌های توصیه‌شده است. API Key سرویس AI فقط روی Backend است.

## Build
از Android Studio پروژه را باز کنید و Sync/Build را اجرا کنید. برای Release باید keystore شخصی خودتان را بسازید؛ کلید یا password داخل پروژه قرار داده نشده است.

جزئیات تست در `RELEASE_CHECKLIST.md` است.
