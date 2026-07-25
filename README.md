# Smart Milk Delivery System

A Spring Boot application for managing smart milk delivery operations.

## GitHub Actions Deployment Pipeline (Ubuntu VPS)

This repository includes an automated GitHub Actions workflow (`.github/workflows/deploy.yml`) that compiles the Spring Boot application and deploys it directly to your Ubuntu VPS via SSH.

### GitHub Repository Secrets List

In your GitHub repository, go to **Settings > Secrets and variables > Actions** and add the following repository secrets:

#### 1. VPS SSH Deployment Secrets (Required)

| Secret Name | Description | Example |
| :--- | :--- | :--- |
| `VPS_HOST` | IP address or domain name of your Ubuntu VPS server | `94.136.191.175` |
| `VPS_USERNAME` | SSH username to log in to the VPS | `savan` or `ubuntu` |
| `VPS_SSH_KEY` | Private SSH key matching `~/.ssh/authorized_keys` on your VPS | `-----BEGIN OPENSSH PRIVATE KEY----- ...` |
| `VPS_PORT` | *(Optional)* SSH port (default: `22`) | `22` |
| `VPS_APP_DIR` | *(Optional)* Target directory on VPS (default: `/opt/smart-milk-delivery`) | `/opt/smart-milk-delivery` |

#### 2. Application & Database Environment Secrets (Optional)

| Secret Name | Description | Default Fallback |
| :--- | :--- | :--- |
| `DB_HOST` | MySQL Database Host | `localhost` |
| `DB_PORT` | MySQL Database Port | `3306` |
| `DB_NAME` | MySQL Database Name | `smart_milk_delivery` |
| `DB_USER` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `root` |
| `JWT_SECRET` | Secret key used for signing JWT authentication tokens | *(Auto-generated default)* |
| `TELEGRAM_BOT_TOKEN` | Token for Telegram Bot integration | `mock_telegram_bot_token` |
| `TELEGRAM_BOT_USERNAME` | Username for Telegram Bot | `mock_telegram_bot_username` |
| `RAZORPAY_KEY_ID` | Razorpay Payment Gateway Key ID | `rzp_test_mock_key_id` |
| `RAZORPAY_KEY_SECRET` | Razorpay Payment Gateway Key Secret | `mock_razorpay_key_secret` |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay Webhook Signing Secret | `mock_webhook_secret` |

---

### Setting up the Systemd Service on Ubuntu VPS

To allow systemd to automatically load these secrets into Spring Boot:

Create or update `/etc/systemd/system/smart-milk-delivery.service`:

```ini
[Unit]
Description=Smart Milk Delivery Spring Boot Service
After=network.target mysql.service redis.service

[Service]
User=savan
WorkingDirectory=/opt/smart-milk-delivery
EnvironmentFile=/opt/smart-milk-delivery/app.env
ExecStart=/usr/bin/java -jar /opt/smart-milk-delivery/milkdelivery-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Reload and restart service:
```bash
sudo systemctl daemon-reload
sudo systemctl restart smart-milk-delivery
```
