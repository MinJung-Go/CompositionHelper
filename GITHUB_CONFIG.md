# GitHub Configuration

**更新时间**: 2026-02-19

## 仓库信息

### Repository
- **URL**: https://github.com/MinJung-Go/CompositionHelper
- **Branch**: android-apk
- **Actions**: https://github.com/MinJung-Go/CompositionHelper/actions

### Authentication
- **Token**: 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
- **Type**: Personal Access Token (Classic)
- **Permissions**: repo (full control)

### Git Remote
```bash
git remote add origin https://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@github.com/MinJung-Go/CompositionHelper.git
```

## 当前项目

### CompositionHelper (Android)
- **Tech Stack**: Kotlin + Jetpack Compose
- **Features**: 18 composition types, ML Kit, CameraX
- **CI Workflow**: `.github/workflows/android-ci.yml`
- **Latest Commit**: ef0af83 (fix: Add Gradle cache)

### AI PPT Pro (Flutter)
- **Location**: `/tmp/clawd-backup/AI-PPT-APK`
- **Tech Stack**: Flutter 3.0+, Riverpod, GLM API
- **Build Script**: `build_apk.sh`

## Cron Jobs

### Android CI Monitor
- **ID**: aed1b324-1103-44e6-9f77-0dc106ce94db
- **Schedule**: Every 5 minutes
- **Purpose**: Monitor GitHub Actions CI build status
- **Status**: Active

### Year-End Work Reminder
- **ID**: def7b3a0-c971-4113-ae8c-b1db39e08608
- **Schedule**: Feb 25, 9:00 AM (Asia/Shanghai)
- **Purpose**: Work reminder for post-holiday tasks

## API Commands

### Check CI Status
```bash
curl -H "Authorization: token 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" \
  "https://api.github.com/repos/MinJung-Go/CompositionHelper/actions/runs?branch=android-apk&per_page=3"
```

### List Workflow Runs
```bash
curl -H "Authorization: token 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" \
  "https://api.github.com/repos/MinJung-Go/CompositionHelper/actions/runs"
```

### Get Job Logs
```bash
curl -H "Authorization: token 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" \
  "https://api.github.com/repos/MinJung-Go/CompositionHelper/actions/runs/{run_id}/logs" -o logs.zip
```

## Important Notes

1. **Token Security**: Never share this token publicly
2. **Token Rotation**: Consider rotating this token regularly
3. **Branch Strategy**: Use `android-apk` for Android development, `master` for stable releases
4. **CI Monitoring**: The cron job automatically checks build status every 5 minutes

## Troubleshooting

### Git Push Fails
```bash
# Update remote URL with token
git remote set-url origin https://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@github.com/MinJung-Go/CompositionHelper.git

# Force push (use carefully)
git push -f origin android-apk
```

### CI Status Check
```bash
# Use Moltbot cron tool
moltbot cron list
moltbot cron runs <job-id>
```

---

**Last Updated**: 2026-02-19 11:30 (GMT+8)
