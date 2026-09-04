<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/4b752101-4f51-4b43-a062-26a8d6c22047

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio and choose **Open**, then select this project folder.
2. Let Android Studio finish the Gradle sync.
3. No `google-services.json`, Firebase project, or `.env` file is required for the Android APK in this version.
4. Run the app on an emulator or physical device, or use the GitHub Actions workflow to build a debug APK.

## Build a debug APK on GitHub

Push the project to GitHub, then open **Actions → Build Android APK → Run workflow**. The workflow builds `app/build/outputs/apk/debug/app-debug.apk` and uploads it as the `neiro-debug-apk` artifact.
