# ChatGPT UI Designer — Developer Guide

## Overview

The **AI UI Designer** module lets you generate production-ready Jetpack Compose code by chatting with ChatGPT directly inside the Player-ID app. ChatGPT is automatically primed with the full Spotr design system so every generated component follows the correct colors, typography, and component patterns.

---

## Quick Start

### 1. Obtain an OpenAI API Key

1. Sign up at [platform.openai.com](https://platform.openai.com).
2. Navigate to **API Keys** and create a new secret key.
3. Copy the key — you will not be able to view it again.

### 2. Configure the App

Open the app, navigate to **Settings → Developer Tools → AI UI Designer**, and paste your API key when prompted. The key is stored in `EncryptedSharedPreferences` on your device and is **never committed to source control**.

### 3. Start Designing

1. Tap **Settings → AI UI Designer**.
2. Type a UI requirement in natural language.
3. ChatGPT returns a `kotlin` code block with the Compose implementation.
4. Tap **Copy code** to copy the generated code to your clipboard.
5. Paste it into your feature branch and iterate.

---

## Example Prompts

| What you type | What you get |
|---------------|-------------|
| `Create a player stats card with jersey number, position, and team color` | `PlayerStatsCard` composable with dynamic team color tinting |
| `Build a team roster list screen with search and filter by position` | Full `RosterScreen` with `LazyColumn`, `SearchBar`, and chips |
| `Design a score banner overlay for the camera screen` | Semi-transparent overlay composable using `AROverlay` color |
| `Add a confirmation dialog for deleting a player` | `DeletePlayerDialog` following Material 3 dialog spec |

---

## Architecture

```
ui/
  ai/
    OpenAIClient.kt          ← Secure HTTP client (OkHttp + EncryptedSharedPreferences)
    DesignSystemAnalyzer.kt  ← Generates system prompt with design tokens
    UIDesignGenerator.kt     ← High-level façade: requirement → code
  screens/
    DesignSystemExplorerScreen.kt  ← Chat UI with history, code rendering, copy button
```

### Data Flow

```
User types requirement
        ↓
UIDesignGenerator.generate(requirement, history)
        ↓
DesignSystemAnalyzer.buildSystemPrompt()  ←  injects design reference
        ↓
OpenAIClient.chatCompletion(messages, systemPrompt)
        ↓
ChatGPT (gpt-4-turbo) returns Compose code
        ↓
DesignSystemExplorerScreen renders code block + Copy button
```

---

## Security Notes

- The OpenAI API key is stored via `androidx.security:security-crypto` (`EncryptedSharedPreferences`).
- The key is **not** stored in `local.properties`, `BuildConfig`, or any file tracked by Git.
- If you need to rotate the key, tap the **key icon** in the AI UI Designer top bar and save a new value.
- The `OpenAIClient` builds the `Authorization` header at runtime from the encrypted store — no secrets appear in compiled bytecode.

---

## Adding the Security Crypto Dependency

The module requires:

```gradle
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

This is already added to `app/build.gradle`.

---

## Multi-Turn Conversations

The screen maintains full conversation history for the session. You can refine a design across multiple turns:

1. **First prompt:** `Create a player card with jersey number`
2. **Follow-up:** `Add a team color stripe on the left edge`
3. **Follow-up:** `Make the jersey number use displayLarge typography`

Each follow-up sends the entire history to ChatGPT so it can iterate on the previous result.

---

## Clearing History

Tap the **trash icon** in the top bar to clear the chat history for the current session. History is in-memory only and does not persist across app restarts.
