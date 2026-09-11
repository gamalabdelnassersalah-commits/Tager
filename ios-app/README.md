# Tager iOS App

## Overview
iOS WebView wrapper for the Tager marketplace platform (https://tager-new.vercel.app/)

## Requirements
- macOS (latest version recommended)
- Xcode 15.0+
- iOS 15.0+ deployment target
- Apple Developer account (for TestFlight)

## Setup Instructions

### 1. Open the Project
```bash
cd ios-app/Tager
open Tager.xcodeproj
```

### 2. Configure Code Signing
1. In Xcode, select the "Tager" target
2. Go to "Signing & Capabilities" tab
3. Check "Automatically manage signing"
4. Select your Development Team (requires Apple Developer Program membership)
5. Set Bundle Identifier to `com.tager.marketplace`

### 3. Build and Run
- Select your device or simulator
- Press Cmd+R to build and run

### 4. TestFlight Deployment

#### Step 1: Archive the App
1. Select "Any iOS Device (arm64)" as the build target
2. Go to Product > Archive (or Cmd+Shift+B for Build, then Archive)
3. Wait for the archive to complete

#### Step 2: Upload to App Store Connect
1. In the Organizer window, select your archive
2. Click "Distribute App"
3. Select "App Store Connect"
4. Select "Upload"
5. Follow the prompts to upload

#### Step 3: Configure in App Store Connect
1. Go to https://appstoreconnect.apple.com
2. Navigate to "My Apps" > "Tager"
3. In the "TestFlight" tab, add a new build
4. Add test information and select testers
5. Submit for beta review

### 5. Important Notes

- **App Privacy**: Since this app loads a web view, you must declare data collection in App Store Connect
- **App Review**: Apple may require justification for the WebView approach. Include a note that the app provides native navigation, offline handling, and camera integration
- **Encryption**: The app uses standard HTTPS, so set `ITSAppUsesNonExemptEncryption` to `false` in Info.plist (already configured)
- **Deep Links**: The app supports `tager://` URL scheme

## Features
- Native bottom navigation bar (Home, Products, Suppliers, Track, Cart)
- Offline detection and retry
- Image optimization (Supabase Storage transformations)
- Camera and file picker support
- Download manager
- Pull-to-refresh
- Deep link support (`tager://` scheme)
- RTL Arabic interface support
- Dark mode support
