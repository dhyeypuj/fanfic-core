# Fanfic Reader 📚

Fanfic Reader is a modern, feature-rich Android application designed for reading, organizing, and tracking fanfictions from popular sites like **Archive of Our Own (AO3)** and **FanFiction.net (FFN)**. Built entirely with Jetpack Compose, it offers a seamless and highly customizable offline reading experience.

## ✨ Features

- **Offline Reading:** Download your favorite fics and read them anytime, anywhere—no internet connection required.
- **Cross-Device Cloud Sync:** Powered by Firebase, automatically sync your entire library, reading progress, and settings across all your devices.
- **Smart Progress Tracking:** Automatically remembers exactly where you left off in a chapter and tracks your total reading time.
- **Library Management:** Easily search, filter (by status, source), and sort (by word count, date added, alphabetical, etc.) your extensive library.
- **Modern UI:** A clean, edge-to-edge Material 3 design built with Jetpack Compose, featuring smooth animations and an intuitive reading interface.

## 🛠️ Tech Stack

This project is built using modern Android development practices and tools:

- **UI:** Jetpack Compose, Material 3
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Asynchrony:** Coroutines & StateFlow / SharedFlow
- **Dependency Injection:** Dagger Hilt
- **Local Storage:** Room Database (SQLite)
- **Cloud Backend:** Firebase Auth & Firestore
- **Network:** OkHttp / Jsoup (for parsing)

## 🚀 Getting Started

### Prerequisites
- Android Studio (Iguana or newer recommended)
- Java JDK 17+
- Android SDK API 34+

### Installation & Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/fanfic-reader.git
   ```
2. **Open the project in Android Studio.**
3. **Firebase Setup:**
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App to your Firebase project using the package name `com.dhyey.fanfic`.
   - Enable **Authentication (Email/Password)** and **Firestore Database**.
   - Download the `google-services.json` file and place it in the `app/` directory.
4. **Build and Run:** Sync your Gradle files and run the app on an emulator or physical device.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](../../issues) if you want to contribute.

## 📝 License

This project is open-source and available under the [MIT License](LICENSE).
