# AI‑IoT Device Visualization & Basic Insight (Android, Jetpack Compose)

A simple, dependency‑light Android app that visualizes streaming sensor data from a local CSV, highlights anomalies using basic AI logic, and includes a small rule‑based chatbot for quick insights.

## Demo
- Watch the working demo: [Google Drive video](https://drive.google.com/file/d/1j5pst8M-Di8RQz1HmGrkGJyfIPKKS1MZ/view?usp=sharing)

## Features
- Live, interval‑streamed data from `assets` CSV (no network, no extra libs)
- Region selection; chart and stats reflect the active region
- Custom Canvas line chart (Compose), rolling numeric history
- Basic AI insights: rolling average and anomaly detection
- Rule‑based chatbot answering three common questions

## Quick Start

### 1) Place the CSV
Place the provided file at:
```
app/src/main/assets/annual_conc_by_monitor_2021.csv
```

### 2) Build
From the project root:
```bash
# macOS/Linux
./gradlew clean assembleDebug

# Windows
gradlew.bat clean assembleDebug
```

### 3) Install/Run
- Android Studio: Run the "app" configuration on a device/emulator.
- Or install the built APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Performance knobs (optional)
Adjust in `DataSimulator`:
- `intervalMs`: lower for faster updates (e.g., 200)
- `sampleStride`: lower for denser data (e.g., 1 to emit every row)

## How It Works

### Data flow
- CSV is read sequentially from `assets` on a background thread.
- Records are emitted at a fixed interval to simulate real‑time updates.
- UI observes state and updates chart, stats, and history as data arrives.

Key files:
- `app/src/main/java/com/example/alappassignment/data/CsvIo.kt` – CSV column detection + streaming parser
- `app/src/main/java/com/example/alappassignment/data/DataSimulator.kt` – sequential emitter with delay and sampling
- `app/src/main/java/com/example/alappassignment/SensorViewModel.kt` – state, insights, chatbot answers
- `app/src/main/java/com/example/alappassignment/ui/Dashboard.kt` – region selector, chart (Canvas), history, chatbot
- `app/src/main/java/com/example/alappassignment/MainActivity.kt` – sets content and starts streaming

### AI logic (brief)
- Rolling average: mean of the last 30 points for the selected region
- Anomaly detection: compute mean and standard deviation over the last 50 points; any value outside ±2.5σ is considered anomalous
- Visual cue: chart line turns red when any anomaly is detected in the recent window; stats show the anomaly count

### Chatbot workflow (rule‑based)
Under the current region context, the chatbot parses keywords and responds from in‑memory data:
- "peak"/"max": reports the maximum reading
- "anomal"/"alert": reports whether anomalies are present and their count
- "average"/"avg"/"mean": reports the rolling average over the last 30 points

## Screenshots:
<img width="196" height="441" alt="image" src="https://github.com/user-attachments/assets/afa09d44-e8c9-4487-a6a3-90215d19e947" hspace="20" />
<img width="197" height="444" alt="image" src="https://github.com/user-attachments/assets/a2facf03-9666-4eec-8ce3-8c19a82835eb" hspace="20" />
<img width="188" height="442" alt="image" src="https://github.com/user-attachments/assets/ed394dcf-4807-48f1-a772-04b02e761efa" /></br>
<h3>Also the demo link of the whole app is given above.</h3>



