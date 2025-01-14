## How to Contribute

### Adding a New Detector
1. Implement the `TextDetector` interface.
2. Override methods for detection, configuration, and actions.
3. Register the detector in `VisionGuard` via `ServiceLoader`.

### Extending Actions
1. Add a new action to the `Action` enum in `TextDetector`.
2. Implement the logic in each detector's `applyAction` method.
3. Update `VisionGuard` to handle the new action in its workflow.
