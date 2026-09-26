# Monkeypox Detection System — Group 22C

An Android application that screens skin-lesion photos for monkeypox using an on-device deep learning model, paired with the Jupyter notebook used to train that model. Built as a final-year project (Group 22C) for the ENR Tech Fair 2026.

## Overview

The system combines a **MobileNetV2‑LSTM** hybrid deep learning model with a **native Android (Kotlin)** app so that a user can take or upload a photo and get an instant, offline prediction of whether it shows monkeypox, does not show monkepox, or is not a valid skin-lesion image at all. Results can be saved with location data, viewed on a map, exported as a PDF report, and discussed with an AI assistant for further explanation.

## Repository Contents

| Path | Description |
|---|---|
| `monkey-pox-detection-with-mobilenet-lstm.ipynb` | Model training notebook (PyTorch). Fine-tunes MobileNetV2 and feeds pooled features into a bidirectional LSTM classifier. |
| `pothole_android_kotlin-main/` | Android Studio project for the mobile app (package `com.example.yoporth`). |
| `app-debug.apk` | Pre-built debug APK for quick installation/testing on an Android device. |

> Note: the Android project folder is named `pothole_android_kotlin-main` for historical reasons (it started from an earlier pothole-detection template) — it now contains the monkeypox detection app.

## Model Details

- **Architecture:** MobileNetV2 (partial fine-tuning of the last 3 blocks) → pooled feature vector → 2‑layer bidirectional LSTM → classification head
- **Input size:** 224 × 224
- **Optimizer / loss:** AdamW, cross-entropy loss
- **Training:** 20 epochs, CPU, no early stopping
- **Dataset:** Kaggle Human Monkeypox Skin Dataset (HMSD)
- **Reported performance:** ~95.63% test accuracy, 0.9928 AUC
- **Deployment format:** converted to TensorFlow Lite (`Monkeypoxmodel.tflite`) for on-device inference
- **Classes** (`labels.txt`): `Monkey Pox`, `Not Monkey Pox`, `Wrong Image`

## App Features

- **Camera & gallery analysis** — capture a photo with CameraX or pick one from the gallery for classification
- **On-device inference** — runs fully offline via TensorFlow Lite (with optional GPU delegate)
- **AI Insights panel** — sends the result to a DeepSeek-powered chat endpoint for a plain-language explanation of the prediction
- **Chatbot** — a dedicated chat screen for follow-up questions
- **Saved reports & sessions** — past detections are stored locally (with timestamps and, where available, location) and can be revisited
- **Map view** — plots saved detections on a Google Map
- **PDF export** — generates a shareable PDF report of a detection/session
- **Disclaimer & About screens** — clarifies that the app is a screening aid, not a medical diagnosis

## Tech Stack

- **Language:** Kotlin
- **Build system:** Gradle (Kotlin DSL)
- **ML runtime:** TensorFlow Lite 2.17 (+ GPU delegate), model trained in PyTorch
- **Camera:** CameraX
- **Maps/Location:** Google Play Services (Maps, Location)
- **Networking:** OkHttp (DeepSeek AI Insights API calls)
- **Image loading:** Glide
- **Serialization:** Gson
- **Min SDK:** 24 · **Target/Compile SDK:** 36

## Getting Started

### Run the pre-built APK
1. Download `app-debug.apk` from this repo.
2. Enable "Install unknown apps" for your file manager/browser on your Android device.
3. Install the APK and grant camera/storage/location permissions when prompted.

### Build from source
1. Open the `pothole_android_kotlin-main` folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Connect a device or start an emulator (API 24+).
4. Run the `app` configuration.

### Explore/retrain the model
1. Open `monkey-pox-detection-with-mobilenet-lstm.ipynb` in Jupyter or Kaggle/Colab.
2. Provide the HMSD dataset (or your own labeled dataset) in the expected input path.
3. Run all cells to retrain; export the resulting model to TensorFlow Lite and replace `app/src/main/assets/Monkeypoxmodel.tflite` (and `labels.txt` if classes change) to update the app's model.

## Disclaimer

This application is a final-year academic project intended for research and demonstration purposes only. It is **not** a certified medical device and should not be used as a substitute for professional medical diagnosis. Any suspected monkeypox case should be evaluated by a qualified healthcare provider.

## Team

Developed by **Group 22C**, supervised by **Dr. Anokye Acheampong Amponsah**, University of Energy and Natural Resources (UENR).
