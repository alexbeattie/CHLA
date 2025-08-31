# 🔐 GitHub Secrets Setup for Notifications

## How to Add Secrets to GitHub

1. **Go to your repository on GitHub**: https://github.com/[your-username]/CHLAProj
2. Click **Settings** (in the repository, not your profile)
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret**

## Required Secrets for Each Notification Type

### 📧 Option 1: Email Notifications (Gmail)

Add these secrets:
- **Name**: `EMAIL`
  **Value**: Your email address (e.g., `your-email@gmail.com`)

- **Name**: `EMAIL_USERNAME`
  **Value**: Your Gmail address (same as EMAIL)

- **Name**: `EMAIL_PASSWORD`
  **Value**: Your Gmail App Password (NOT your regular password!)
  
  **To get Gmail App Password:**
  1. Go to https://myaccount.google.com/security
  2. Enable 2-factor authentication if not already enabled
  3. Search for "App passwords"
  4. Generate a new app password for "Mail"
  5. Copy the 16-character password (no spaces)

### 💬 Option 2: Slack Notifications

Add this secret:
- **Name**: `SLACK_WEBHOOK`
  **Value**: Your Slack webhook URL
  
  **To get Slack Webhook:**
  1. Go to https://api.slack.com/apps
  2. Create a new app or use existing
  3. Add "Incoming Webhooks" feature
  4. Create webhook for your channel
  5. Copy the webhook URL

### 🐙 Option 3: GitHub Issue Comments (Already Working!)

No secrets needed! This uses the built-in `GITHUB_TOKEN`.

## 🚀 Quick Copy-Paste Secrets

If you want email notifications, add these 3 secrets:
```
EMAIL = your-email@gmail.com
EMAIL_USERNAME = your-email@gmail.com
EMAIL_PASSWORD = xxxx xxxx xxxx xxxx (16-char app password, no spaces)
```

## ✅ After Adding Secrets

1. Your next deployment will automatically send notifications
2. Email/Slack will only trigger if those secrets exist
3. GitHub comments will always work (they're already set up)

## 🔧 Testing Notifications

To test if your secrets are working:
1. Make a small change to any file
2. Commit and push
3. Check your email/Slack/GitHub for notifications

## 📝 Notes

- Secrets are encrypted and never shown in logs
- Use Gmail App Passwords, not your regular password
- If email fails, check spam folder first
- GitHub Issue comments work without any setup!
