# Spotify Sync

A full-stack web application that allows users to easily synchronize their **Liked Songs**, **Playlists**, and **Saved Albums** between two different Spotify accounts. 

## ✨ Features

- **Dual Account Authentication**: Seamlessly log in to a Source account and a Destination account simultaneously using Spotify OAuth.
- **Selective Sync**: Choose exactly what you want to sync. Check or uncheck individual songs, playlists, or albums, or use the "Select All" feature.
- **High Performance**:
  - **Backend**: Implements multi-threading (`CompletableFuture`) for blazing-fast synchronization, along with intelligent rate-limit handling (429 Too Many Requests) that respects Spotify's `Retry-After` headers per thread.
  - **Frontend**: Utilizes Virtual Lists (`@tanstack/react-virtual`) to render thousands of tracks or playlists without lagging the browser.
- **Real-time Tracking**: Watch the synchronization progress in real-time via WebSocket integration.
- **Beautiful UI**: Modern, dark-mode first design built with Next.js, Tailwind CSS, and Shadcn UI.

## 🛠️ Tech Stack

### Frontend
- **Framework**: Next.js (React 19)
- **Styling**: Tailwind CSS
- **Components**: Shadcn UI (Base UI)
- **Performance**: TanStack Virtual (React Virtual) for rendering massive lists.

### Backend
- **Framework**: Spring Boot 3 (Java)
- **Communication**: Spring WebSocket / STOMP for real-time progress updates.
- **API**: Spotify Web API.

## 🚀 Getting Started (Local Development)

### Prerequisites
- Node.js (v18+)
- Java 17+
- Maven
- A Spotify Developer App (Client ID and Client Secret)

### 1. Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Configure your application properties. Set up your Spotify Client ID and Secret in `application.yml` or via environment variables.
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   The backend will start on `http://localhost:8080`.

### 2. Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```
   The frontend will start on `http://localhost:3000`.

## ⚙️ Environment Variables

### Backend (`backend/src/main/resources/application.yml` or OS Env Vars)
You can configure the application's behavior using the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | The port the Spring Boot server runs on | `8080` |
| `FRONTEND_URL` | URL of the frontend application | `http://localhost:3000` |
| `SENTRY_DSN` | Your Sentry DSN for error tracking | *(empty)* |
| `SYNC_CORE_POOL_SIZE` | Core number of threads for the sync executor | `2` |
| `SYNC_MAX_POOL_SIZE` | Maximum number of threads for the sync executor | `5` |
| `SYNC_QUEUE_CAPACITY` | Max queued tasks before thread pool rejection | `50` |

### Frontend (`frontend/.env` or `.env.local`)
| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | URL of the backend API | `http://localhost:8080` |

## 📄 License
This project is for educational and personal use.
