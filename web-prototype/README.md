# Life Analytics — Web Clone Prototype

This directory contains an isolated, interactive web prototype of the **Life Analytics** application.

It is designed for rapid design exploration, interactive UI reviews, and fast feature testing without rebuilding APKs.

---

## Complete Isolation Guarantee

- This entire prototype is strictly contained inside the `web-prototype/` folder.
- **Zero impact on Android app:** The Android application (`app/`, `gradle/`, `build.gradle.kts`) does not reference or depend on anything inside this folder.
- **Safe deletion:** When you are satisfied with your MVP and ready to remove this prototype, simply delete this directory:
  ```bash
  rm -rf web-prototype
  ```

---

## How to Run Locally on Your Laptop

You have two very easy ways to run this:

### Option A: Open directly in your browser (Zero setup, no Node needed!)
Simply double-click `index.html` or open it directly in Chrome, Brave, Safari, or Edge:
```bash
# On macOS:
open index.html

# On Linux:
xdg-open index.html

# On Windows:
start index.html
```

### Option B: Run with local live dev server
From this `web-prototype` folder:
```bash
npm install
npm run dev
```
Open `http://localhost:5173` in your browser. Any edits you make to `index.html` or code will hot-reload instantly.
