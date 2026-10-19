# Optional wallpaper

The default build keeps the original gradient. To bundle a static image in your own
build, place it at `assets/wallpaper.jpg`, then run `sh build.sh`. This path is ignored
by Git so a personal image is not included in a contribution by accident. No image
is downloaded, and the app does not request any additional permissions.

The wallpaper stays fixed behind the clock and tiles, including when the app list
scrolls. It is cropped to fill the screen without stretching. A dark gradient over
the lower part of the image keeps labels readable.

## Automatic resolution and memory

The image is sampled before decoding to fit the UI dimensions Android reports for
the app. The source file is never rewritten. For a 3840 × 2160 source:

| Android UI resolution | Decoded image | Bitmap memory |
| --- | --- | --- |
| 1920 × 1080 | 1920 × 1080 | About 8 MiB |
| 3840 × 2160 | 3840 × 2160 | About 32 MiB |

Sampling uses powers of two, so other image sizes or aspect ratios can produce a
bitmap smaller than the reported UI bounds. Smaller source images are not enlarged
during decoding. As an additional memory guard, decoded images cannot exceed
8,294,400 pixels or 4096 pixels on either edge. Compositor buffers use additional
memory beyond the decoded bitmap.

The wallpaper's `SurfaceView` follows its layout size. It never requests a larger
fixed buffer based on the source image. A 1080p interface therefore uses a 1080p
wallpaper surface even when both the source image and the TV panel are 4K.

The image is decoded once when the activity is created, and the surface follows
subsequent layout size changes. Restart the launcher after changing Android's
display resolution to decode for the new UI size. There is no continuous redraw,
background service or wallpaper animation.

This feature follows the existing UI resolution; it does not change the TV's
global resolution, density, refresh rate or input settings. Some 4K TVs expose a
1080p UI and upscale it for their panel. See Android's [surface guidance](https://developer.android.com/media/media3/ui/surface#choose-a-surface-type-for-playerview)
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
APK, checks automatic sampling for a 1080p UI, UHD decoding for a UHD UI, the default
loader's use of Android UI metrics, center cropping, missing and invalid images,
layout-sized surface buffers and drawing after a surface is recreated. Fixtures
are generated images with no connection to a personal wallpaper or app list.
