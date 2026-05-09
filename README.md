# ✦ Glassbox

**The first native Android implementation of A2UI v0.9.**

Glassbox is an agent that paints its own reasoning as native Android UI in real time. Built for the [Generative UI Global Hackathon (Durango, May 2026)](https://durango-mx.aitinkerers.org/p/generative-ui-global-hackathon-agentic-interfaces-durango-mx).

While most teams built A2UI clients for the web, Glassbox demonstrates the true promise of A2UI: **framework-agnostic by design**. Same agent, same protocol, native Android UI rendered in Jetpack Compose.

---

## 🎯 What it does

You speak or type a request in Spanish or English. The agent (Gemini 2.5 Flash) generates an A2UI v0.9 declarative UI as JSON. The Android client parses it and renders native Material 3 components — cards, sliders, buttons, images — with animations and dynamic theming.

**Bidirectional state sync**: move a slider, the agent re-reasons with the new state and re-renders the UI. STATE_DELTA via AG-UI patterns.

### Demo prompts that work today

- `"Quiero un viaje a Mazatlán para 4 personas con 8000 pesos"`
- `"Compara MacBook Air vs Dell XPS 13 para programar"`
- `"Recomiéndame qué hacer este fin de semana en Durango"`

The agent generates a hero image, title, slider, comparison cards, and action buttons — all as native Compose components.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────┐
│  Native Android Client (Jetpack Compose)         │
│  - Voice input (SpeechRecognizer)                │
│  - Live JSON A2UI overlay (debug view)           │
│  - Native Material 3 rendering                   │
│  - Bidirectional state sync                      │
└──────────────────────────────────────────────────┘
                     ↑↓ SSE (AG-UI events)
┌──────────────────────────────────────────────────┐
│  Backend (Kotlin + Ktor 3.x)                     │
│  - SSE streaming                                 │
│  - Agent orchestration                           │
│  - A2UI v0.9 envelope generation                 │
│  - Gemini 2.5 Flash integration                  │
│  - Fallback UI for resilient demos               │
└──────────────────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────┐
│  Google Gemini 2.5 Flash                         │
│  System prompt teaches A2UI v0.9 spec            │
└──────────────────────────────────────────────────┘
```

### Sponsor protocols used

- **A2UI v0.9** (Google DeepMind) — declarative UI envelope: `surfaceUpdate`, `dataModelUpdate`
- **Gemini 2.5 Flash** (Google) — LLM that generates A2UI JSON
- **AG-UI patterns** — SSE event streaming for agent → frontend communication

---

## 📦 Project structure

This is the **backend** repo. The native Android client lives at:
[github.com/Joecosta6/glassbox-android](https://github.com/Joecosta6/glassbox-android)

```
glassbox-agent/
├── src/main/kotlin/
│   ├── main.kt                 # Ktor entrypoint
│   ├── Routing.kt              # /agent/stream SSE endpoint
│   ├── AgentService.kt         # Gemini call + A2UI streaming logic
│   ├── A2UISystemPrompt.kt     # The prompt that teaches A2UI to the LLM
│   ├── Sse.kt, Http.kt, Serialization.kt
│   └── ...
├── build.gradle.kts
└── gradle/...
```

---

## 🚀 Run locally

### Requirements

- JDK 21
- A Gemini API key from [aistudio.google.com](https://aistudio.google.com/apikey)

### Setup

```bash
git clone https://github.com/Joecosta6/glassbox-agent.git
cd glassbox-agent

# Set your Gemini API key
export GEMINI_API_KEY="AIza..."

# Run the server
./gradlew run
```

The server will start on `http://0.0.0.0:8080`.

### Test it

```bash
curl http://localhost:8080/
# → Glassbox agent is alive 👁️

curl -N "http://localhost:8080/agent/stream?prompt=viaje%20a%20Mazatlan%20para%204%20personas"
# → SSE stream of A2UI v0.9 events
```

You should see events like:
```
event: thinking
data: {"message":"Pensando..."}

event: surface_update
data: {"components":{...}}

event: data_update
data: {"data":{"budget":8000}}

event: begin_render
data: {"ready":true}
```

---

## 🎤 Why native Android matters

A2UI v0.9 was designed to be framework-agnostic. The spec promises that the same agent JSON can render on web, mobile, and desktop. Most demos prove this on web. Glassbox proves it on **mobile native**.

This matters because:

1. **It validates the spec.** A protocol is only as good as its multiple implementations.
2. **It opens up real verticals.** POS systems, field-service apps, kiosks, in-car interfaces — all places where web doesn't fit.
3. **It demonstrates true portability.** Same backend, different clients, zero duplicated code.

---

## 🛠️ Tech stack

| Layer | Technology |
|---|---|
| Backend | Kotlin 2.0, Ktor 3.x, kotlinx.serialization |
| Streaming | Server-Sent Events (SSE) |
| LLM | Google Gemini 2.5 Flash |
| Protocol | A2UI v0.9, AG-UI event patterns |
| Client | Jetpack Compose, Material 3 |
| Voice | Android SpeechRecognizer (native) |

---

## 📜 License

MIT.

---

Built in 24 hours for the Generative UI Global Hackathon · Durango, México · May 2026.

> *"The agent doesn't hide its reasoning. It paints it."*
