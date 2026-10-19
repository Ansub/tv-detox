# Optional wallpaper

The default build keeps the original gradient. To bundle a static image in your own
build, place it at `assets/wallpaper.jpg`, then run `sh build.sh`. This path is ignored
by Git so a personal image is not included in a contribution by accident. No image
is downloaded, and the app does not request any additional permissions.

The wallpaper stays fixed behind the clock and tiles, including when the app list
scrolls. It is cropped to fill the screen without stretching. A dark gradient over
the lower part of the image keeps labels readable.

## Resolution and memory

A 3840 × 2160 image is decoded at its original resolution. Larger images are sampled
down before decoding to at most 8,294,400 pixels, with neither edge exceeding 4096
pixels. The source file is not rewritten. The decoded image uses about 32 MiB at UHD
resolution; the display compositor also allocates surface buffers, so total memory
use is higher. There is no continuous redraw, background service or animation.

The image is drawn on a separate `SurfaceView`. This allows a UHD buffer even on
Android TVs that run their regular app interface at 1920 × 1080. The buffer follows
the window's aspect ratio and the visible source detail; it does not enlarge a
smaller source image just to create a UHD buffer.

Actual output resolution depends on the TV's compositor and firmware. A UHD image
or buffer alone does not prove that a particular TV presents it at native resolution.
This feature does not change the TV's global display resolution, density, refresh
rate or input settings. See Android's [surface guidance](https://developer.android.com/media/media3/ui/surface#choose-a-surface-type-for-playerview)
for the distinction between the UI layer and the display's full resolution.

If the asset is missing, unreadable or cannot be allocated, the original gradient
remains available. A drawing failure hides the wallpaper surface and reveals that
gradient. Surface contents are redrawn when Android recreates the surface.

## Change or remove the image

Replace `assets/wallpaper.jpg` and rebuild to change the wallpaper. Remove it and
rebuild to return to the original gradient. Both builds keep the same application
identity and settings. As with any update, use the same signing key.

## Verification

After building the app, run the wallpaper checks on an Android emulator:

```sh
sh ci/test-wallpaper.sh emulator-5554
```

The test script refuses physical devices. It installs the build and a small test
APK, checks UHD decoding and surface size, center cropping, missing and invalid
images, and drawing after a surface is recreated. Fixtures are generated images
with no connection to a personal wallpaper or app list.

For a real TV, inspect `adb shell dumpsys SurfaceFlinger` after launching the build.
Check the wallpaper layer's buffer size, source crop, destination and composition
type. A 3840 × 2160 source that is composited into a 1920 × 1080 client framebuffer
still loses detail before the TV scales it up. ADB screenshots can also reflect the
UI capture resolution rather than the panel's native resolution.
