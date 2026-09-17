# Fix Volume Slider and Seek Bar Responsiveness

The current implementation of the volume slider and seek bar in `MainActivity.kt` suffers from "state fighting." The sliders are directly bound to the polling state updated every second. When a user drags a slider, the `onValueChange` event triggers a network request followed by a `refresh()` call. If the `refresh()` call completes before the user finishes dragging, it updates the state with the server's current (older) value, causing the slider to "snap back."

## Proposed Changes

### [MainActivity.kt](file:///D:/Downloads/Project/app/src/main/java/com/foxings/foobarthingy/MainActivity.kt)

I will refactor the `PlayerScreen` and `InfoAndControls` components to decouple the user's interaction from the background polling state.

#### [MODIFY] [MainActivity.kt](file:///D:/Downloads/Project/app/src/main/java/com/foxings/foobarthingy/MainActivity.kt)

1.  **Introduce Local Interaction State**:
    *   Add `localSeekPosition` and `localVolume` states to `PlayerScreen`.
    *   Add `isSeeking` and `isChangingVolume` flags to track user interaction.
2.  **Update `InfoAndControls` to use local state**:
    *   While the user is dragging (detected via `onValueChange`), the slider will display the local value.
    *   Updates from the background polling (`refresh()`) will be ignored for a specific field while it's being interacted with.
3.  **Optimize Network Requests**:
    *   Use `onValueChangeFinished` for the Seek Bar to trigger a single `seek` request when the user releases the slider.
    *   For the Volume Slider, we'll keep `onValueChange` but ensure it doesn't trigger a `refresh()` that overwrites the local state until the interaction is stable, or use a debounced approach.

## Verification Plan

### Manual Verification
1.  **Seek Bar**: Drag the seek bar and verify it moves smoothly without jumping back. Release it and verify the playback position changes to the selected time.
2.  **Volume Slider**: Drag the volume slider and verify it moves smoothly. Verify the volume on the server (foobar2000) updates accordingly.
3.  **Background Polling**: Verify that when the user is NOT interacting with the sliders, they still update correctly to reflect changes made on the server or natural playback progress.
