# 🧮 Material You Calculator

An elegant, Android-native calculator and currency converter app, built with **Jetpack Compose** and styled using **Material You** (Monet) theming. The app adapts dynamically to your system colors and offers a clean, modern experience for daily calculations and conversions.

---

## ✨ Features

- 🧮 **Basic Calculator**  
  Perform quick and simple arithmetic operations with a responsive interface.

- 💱 **Currency Converter**  
  Converts between currencies using real-time exchange rates (data fetched from a free public API).

- 🎨 **Material You Support**  
  Adapts to your system’s dynamic color palette (Monet), including light/dark themes and accent coloring.

- 💾 **Calculation History**  
  View and clear previous operations in a neat list, persisted across app restarts via Room.

- ☁️ **Offline Caching**  
  Exchange rates and history are cached locally (Room/DataStore) for offline use—fresh data is fetched only when older than 24 hours.

- ⌨️ **Custom Keypads**  
  Separate numeric/math keypad for the calculator and currency converter for optimized input.

- 📱 **Edge-to-Edge UI**  
  Seamlessly blends with Android's status and gesture navigation bars using the `EdgeToEdge` API.

---

## 🚧 To-Do

- 🔧 **Unit Converter Tab**  
  Add support for converting between units (length, weight, temperature, etc.).

- 🌍 **Localization & Formatting**  
  Provide translations and locale-specific number/date formatting.

---

## 🛠️ Tech Stack

- **Kotlin**  
- **Jetpack Compose** for UI  
- **Hilt** for Dependency Injection  
- **MVVM** architecture  
- **Room** for local database  
- **Ktor** HTTP client  
- **Gson** for JSON parsing  
- **Material Design 3 (Material You)**

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Electric/Eiger or newer  
- Android SDK 33+  
- Kotlin 1.9+  

### Clone the repo

```bash
git https://github.com/metzger100/calculator.git
cd calculator
```

### Run the app

Open the project in Android Studio, let Gradle sync, then click **Run**.

---

## 🗂️ Project Structure

```
com.metzger100.calculator
├── data
│   ├── local                 # Room entities & DAOs
│   │   ├── CalculationDao.kt
│   │   ├── CalculationEntity.kt
│   │   ├── CurrencyRateDao.kt
│   │   ├── CurrencyRateEntity.kt
│   │   ├── CurrencyListDao.kt
│   │   ├── CurrencyListEntity.kt
│   │   └── CalculatorDatabase.kt
│   ├── CurrencyRepository.kt # fetch & cache logic for exchange rates & list
│   └── CalculatorRepository.kt
│
├── di                        # Hilt modules & qualifiers
│   ├── AppModule.kt          # provides Room, HttpClient, dispatchers
│   └── Qualifiers.kt
│
├── features
│   ├── calculator             # Calculator feature
|   |   ├── model
|   |   │   └── CalculatorMode.kt
│   │   ├── ui
│   │   │   ├── CalculatorScreen.kt
│   │   │   ├── StandardKeyboard.kt
│   │   │   └── ScientificKeyboard.kt
│   │   └──.viewmodel
│   │       └── CalculatorViewModel.kt
│   └── currency               # Currency converter feature
│       ├── ui
|       |   ├── Constants.kt
│       │   ├── CurrencyConverterScreen.kt
│       │   └── CurrencyConverterKeyboard.kt
│       └── viewmodel
│           └── CurrencyViewModel.kt
│
├── ui
│   ├── navigation            # NavGraph, AppTopBar, BottomNavBar & NavItem classes
│   └── theme                 # Material You theme setup
│
└── MainActivity.kt           # Entry point
```

---

## 📄 License

--- Not Licensed yet ---

---

## 🙌 Credits
 
- Exchange rate data provided by [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api)

Feel free to contribute by submitting issues or pull requests!
