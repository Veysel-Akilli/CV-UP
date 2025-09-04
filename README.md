# 🚀 CV-UP – AI Powered ATS-Friendly Document Assistant

<div align="center">
  <h1>
    <img src="docs/assets/icon.png" alt="Document Assistant Icon" width="48"/>
    Document<em>Assistant</em>
  </h1>

  <h3>🌟 AI Destekli Belge Üretimi: CV ve daha fazlası</h3>

[![FastAPI](https://img.shields.io/badge/FastAPI-0.104.1-009688?style=for-the-badge&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)](LICENSE)

</div>

<p align="center">
  <a href="https://github.com/Veysel-Akilli/cursor/stargazers">
    <img src="https://img.shields.io/github/stars/Veysel-Akilli/cursor?style=social" alt="Stars">
  </a>
  <a href="https://github.com/Veysel-Akilli/cursor/network/members">
    <img src="https://img.shields.io/github/forks/Veysel-Akilli/cursor?style=social" alt="Forks">
  </a>
  <a href="https://github.com/Veysel-Akilli/cursor/issues">
    <img src="https://img.shields.io/github/issues/Veysel-Akilli/cursor" alt="Issues">
  </a>
  <a href="https://github.com/Veysel-Akilli/cursor/pulls">
    <img src="https://img.shields.io/github/issues-pr/Veysel-Akilli/cursor" alt="PRs">
  </a>
</p>

<p align="center">
  <img src="docs/assets/demo.gif" width="300" alt="Project Demo GIF">
</p>

---

## 📋 Contents

- [About the Project](#-about-the-project)
- [Why Document Assistant?](#-why-document-assistant)
- [Features](#-features)
- [Technologies](#-technologies)
- [System Architecture](#-system-architecture)
- [Project Structure](#-project-structure)
- [Installation](#️-installation)
- [API Reference](#-api-reference)
- [Mobile Application](#-mobile-application)
- [Security Notes](#-security-notes)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 About the Project

**Document Assistant (CV-UP)** is an AI-powered backend + mobile application designed to help users generate and manage professional documents such as **CVs**.

Unlike traditional CV generators, this project focuses on producing documents that are **ATS (Applicant Tracking System) compliant**.  
The aim is to support job seekers in preparing CVs and application documents that can successfully pass through the automated filtering systems used by companies to handle a large volume of applications.

- **Backend:** FastAPI + PostgreSQL + Docker + Gemini API
- **Mobile App:** Kotlin + Jetpack Compose (MVVM)

This combination enables **secure, scalable, and intelligent document generation**, fully integrated with a modern mobile app for seamless user experience.

---

## 🌟 Why Document Assistant?

- **🤖 Smart Document Generation** – AI-powered CVs with Gemini API.
- **⚡ Fast & Simple** – Easy integration with mobile clients via REST APIs.
- **🔒 Secure** – JWT-based authentication and role-based access control.
- **🐳 Portable** – Dockerized setup for fast deployment.

---

## 💡 Features

### 👤 For Users

- Register & login (JWT authentication)
- Create, update, list, and delete document requests
- Upload & download files with type/size validation
- Generate professional documents using Gemini API
- View personal document statistics

### 📱 For Mobile

- Kotlin + Jetpack Compose (MVVM architecture)
- Secure login & signup
- Form-based document input (CV, letters, contracts)
- Integration with backend API
- View & manage generated documents

---

## 🛠 Technologies

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://fastapi.tiangolo.com/img/logo-margin/logo-teal.png" width="40"><br>FastAPI</td>
      <td align="center"><img src="https://www.postgresql.org/media/img/about/press/elephant.png" width="40"><br>PostgreSQL</td>
      <td align="center"><img src="https://www.docker.com/wp-content/uploads/2022/03/vertical-logo-monochromatic.png" width="40"><br>Docker</td>
      <td align="center"><img src="https://kotlinlang.org/assets/images/favicon.ico" width="40"><br>Kotlin/Compose</td>
      <td align="center"><img src="https://raw.githubusercontent.com/github/explore/master/topics/python/python.png" width="40"><br>Python</td>
      <td align="center"><img src="https://www.globaltechmagazine.com/wp-content/uploads/2024/06/gemini-google-globaltechmagazine.jpg" width="40"><br>Gemini API</td>
      <td align="center"><img src="https://cdn-icons-png.flaticon.com/512/25/25231.png" width="40"><br>GitHub</td>
    </tr>
  </table>
</div>

---

## 🏗 System Architecture

```
Mobile App (Kotlin + Compose + MVVM)
        │
        ▼
   FastAPI Backend ──► PostgreSQL
        │
        └── Gemini API (AI-powered text generation)
```

---

## 📂 Project Structure

```
project-root/
├── app/
│   ├── main.py                # FastAPI entry
│   ├── core/                  # Config, DB, security, middleware
│   ├── models/                # SQLAlchemy models
│   ├── schemas/               # Pydantic schemas
│   ├── routes/                # API endpoints
│   └── services/              # Business logic + Gemini
├── uploads/                   # Uploaded/generated files
├── alembic/                   # Migration files
├── docker-compose.yml         # Docker services
├── requirements.txt           # Python dependencies
├── env.example                # Example environment file
└── README.md                  # This file
```

---

## ⚙️ Installation

### 1. Clone

```bash
git clone https://github.com/Veysel-Akilli/cursor.git
cd cursor
```

### 2. Environment

```bash
cp env.example .env
# Fill in .env with your secrets
```

### 3. Run with Docker

```bash
docker-compose up --build -d
docker-compose exec app alembic upgrade head
```

- Swagger → [http://localhost:8000/docs](http://localhost:8000/docs)
- ReDoc → [http://localhost:8000/redoc](http://localhost:8000/redoc)

---

## 📘 API Reference

The **Document Assistant API** enables secure integration with external and mobile applications for ATS-compliant CV generation.

### Core Endpoints

| Endpoint                             | Method | Description                   | Access |
| ------------------------------------ | ------ | ----------------------------- | ------ |
| `/api/v1/auth/register`              | POST   | Create a new user             | Public |
| `/api/v1/auth/login`                 | POST   | User login (returns JWT)      | Public |
| `/api/v1/auth/me`                    | GET    | Get current user profile      | User   |
| `/api/v1/document-requests`          | GET    | List all document requests    | User   |
| `/api/v1/document-requests`          | POST   | Create a new request          | User   |
| `/api/v1/document-requests/{id}`     | GET    | Get a specific request        | User   |
| `/api/v1/document-requests/{id}`     | PUT    | Update a specific request     | User   |
| `/api/v1/document-requests/{id}`     | DELETE | Delete a specific request     | User   |
| `/api/v1/document-requests/generate` | POST   | Generate document (Gemini AI) | User   |
| `/api/v1/documents`                  | GET    | List user documents           | User   |
| `/api/v1/documents/upload`           | POST   | Upload a document             | User   |
| `/api/v1/documents/{id}`             | GET    | Document details              | User   |
| `/api/v1/documents/{id}/download`    | GET    | Download a document           | User   |
| `/api/v1/documents/{id}`             | PUT    | Update a document             | User   |
| `/api/v1/documents/{id}`             | DELETE | Delete a document             | User   |

For detailed API documentation, visit the **[Swagger UI](http://localhost:8000/docs)** or **[ReDoc](http://localhost:8000/redoc)** endpoints.

---

## 📱 Mobile Application

- **Stack:** Kotlin, Jetpack Compose, MVVM, Retrofit
- **Flow:** Login → Form → API `/generate` → Result
- **Screenshots:**

<p align="center">
  <img src="docs/screenshots/result.png" alt="Result Screen" width="300"/>
</p>

---

## 🔐 Security Notes

- `.env` must **never** be committed, only `env.example`.
- Set `DEBUG=False` and restrict CORS in production.
- Secrets like `SECRET_KEY`, `POSTGRES_PASSWORD`, and `GEMINI_API_KEY` must stay private.
  -All endpoints except register/login require JWT authentication.

---

## 🤝 Contributing

1. Fork
2. Create branch (`feature/xyz`)
3. Commit
4. Push
5. Open PR

---

## 📄 License

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

This project is licensed under the MIT License. For details, see the [LICENSE](LICENSE) file.

```text
MIT License

Copyright (c) 2025 Veysel Akıllı

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
...
```
