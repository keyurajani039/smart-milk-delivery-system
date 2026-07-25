# Smart Milk Delivery System

A Spring Boot application for managing smart milk delivery operations.

## GitHub Actions Deployment Pipeline (Ubuntu VPS)

This repository includes an automated GitHub Actions workflow (`.github/workflows/deploy.yml`) that compiles the Spring Boot application and deploys it directly to your Ubuntu VPS via SSH.

### GitHub Repository Secrets List

In your GitHub repository, go to **Settings > Secrets and variables > Actions** and add the following repository secrets:

#### 1. VPS SSH Server Secrets (Required)

| Secret Name | Description | Example Value |
| :--- | :--- | :--- |
| `VPS_HOST` | Server IP or Domain | `94.136.191.175` |
| `VPS_USERNAME` | SSH User | `savan` |
| `VPS_SSH_KEY` | Private SSH Key | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `VPS_PORT` | *(Optional)* SSH Port | `22` |
| `VPS_APP_DIR` | *(Optional)* Target Directory | `/opt/smart-milk-delivery` |

---

#### 2. Application & Database Secrets (Optional)

| Secret Name | Description | Default Fallback |
| :--- | :--- | :--- |
| `DB_HOST` | MySQL Host | `localhost` |
| `DB_PORT` | MySQL Port | `3306` |
| `DB_NAME` | MySQL Database Name | `smart_milk_delivery` |
| `DB_USER` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `root` |
| `JWT_SECRET` | Secret key for signing JWT tokens | *(Auto-generated default)* |
| `TELEGRAM_BOT_TOKEN` | Telegram Bot API Token | `mock_telegram_bot_token` |
| `TELEGRAM_BOT_USERNAME` | Telegram Bot Username | `mock_telegram_bot_username` |

---

### Setting up the Systemd Service on Ubuntu VPS

To allow systemd to automatically load these secrets into Spring Boot:

Create or update `/etc/systemd/system/smart-milk-delivery.service`:

```ini
[Unit]
Description=Smart Milk Delivery Application
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
