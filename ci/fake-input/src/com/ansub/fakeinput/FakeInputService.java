package com.ansub.fakeinput;

import android.content.Context;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.view.Surface;
import android.view.View;

// For testing only: the smallest TV input service that will register. It has no hardware behind
// it, so tuning does nothing; it exists so a TV without HDMI inputs still has an input to show.
public class FakeInputService extends TvInputService {
    @Override
    public Session onCreateSession(String inputId) {
        return new FakeSession(this);
    }

    private static final class FakeSession extends Session {
        FakeSession(Context context) {
            super(context);
        }

        @Override public View onCreateOverlayView() { return null; }
        @Override public void onRelease() {}
        @Override public boolean onSetSurface(Surface surface) { return true; }
        @Override public void onSurfaceChanged(int format, int width, int height) {}
        @Override public void onSetStreamVolume(float volume) {}
        @Override public void onSetCaptionEnabled(boolean enabled) {}
        @Override public boolean onTune(Uri channelUri) { return true; }
    }
}
