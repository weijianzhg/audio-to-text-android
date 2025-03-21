# Audio Transcriber Android App

A simple Android application that converts audio to text using OpenAI's Whisper API. This app allows users to either record audio directly or select an audio file from their device for transcription.

## Features

- Record audio directly within the app
- Select existing audio files (MP3 format)
- Transcribe audio to text using OpenAI's Whisper API
- Simple and intuitive Material Design interface
- Support for both recorded and uploaded audio files
- Save transcriptions to device storage

## Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 24 or higher
- OpenAI API key
- Kotlin 1.9.22 or newer

## Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/audio-transcriber-android.git
```

2. Open the project in Android Studio

3. Set up sensitive data:
   - Copy `gradle.properties.example` to `gradle.properties`
   - Update `gradle.properties` with your actual values:
     - Add your OpenAI API key
     - Configure your keystore settings if you plan to build release versions

4. Sync the project with Gradle files

5. Build and run the application

## Security Notes

This project contains sensitive configuration that should never be committed to version control:
- `gradle.properties` containing keystore passwords and API keys
- Keystore files (`.jks` or `.keystore`)

These files are listed in `.gitignore` and should be kept private. Example files (`gradle.properties.example`) are provided as templates.

## Usage

1. Launch the app
2. Choose one of the following options:
   - Tap "Record Audio" to record new audio
   - Tap "Select Audio File" to choose an existing MP3 file
3. After recording or selecting a file, tap "Transcribe"
4. Wait for the transcription process to complete
5. View the transcribed text in the app
6. Tap "Save" to save the transcription to your device

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO`: For recording audio
- `INTERNET`: For API communication
- `READ_EXTERNAL_STORAGE`: For accessing audio files
- `WRITE_EXTERNAL_STORAGE`: For saving transcriptions

## Technical Details

- Built with Kotlin
- Uses ViewBinding for view access
- Implements coroutines for asynchronous operations
- Uses OkHttp for network requests
- Material Design components for UI
- Handles runtime permissions for audio recording
- Supports MP3 audio format

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Add your license information here]

## Acknowledgments

- OpenAI for the Whisper API
- Material Design components
- OkHttp library
