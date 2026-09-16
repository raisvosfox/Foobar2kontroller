# Kontroller

A minimal, Syne Mono themed remote controller for the Beefweb player. DISCLAIMER: CODED USING AI (sorry...)

## Features
- **Auto-Update**: Displays current track, artist, and playback state in real-time.
- **Fullscreen Mode**: Automatically hides system bars in landscape.
- **Customizable UI**: Change background and text colors via hex codes in the settings.
- **Optional Controls**: Toggle playback controls (Skip, Play/Pause, Stop) on/off.
- **Keep Screen On**: Prevents the device from sleeping while the app is active.

## Setup
1. Install the `Kontroller.apk` from the [Releases](https://github.com/raisvosfox/Foobar2kontroller/releases) page.
2. Ensure your Foobar2000 Beefweb server is running and accessible on your local network. ([Link to download Beefweb component](https://www.foobar2000.org/components/view/foo_beefweb))
3. Enter the server URL (e.g., `192.168.x.xxx:8880`) on the initial setup screen.

## Settings
Press the **Back button** while on the player screen to access the settings menu. From there, you can:
- Toggle playback controls.
- Update colors using Hex codes.
- Update the Beefweb server URL.

## Development
This app is built with Jetpack Compose and uses Retrofit for Beefweb API communication.
