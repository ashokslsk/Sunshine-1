/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final String TAG = MyWatchFace.class.getSimpleName();
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    public static final String KEY_MIN_TEMP = "min_temp";
    public static final String KEY_MAX_TEMP = "max_temp";
    public static final String KEY_WEATHER_CONDITION_ID = "weather_id";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine
            implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        private static final String DEGREE_SYMBOL = "\u00b0";

        Paint mBackgroundPaint;
        Paint mTextWhitePaint;
        Paint mTextLightPaint;
        Paint mTextMaxTempPaint;
        Paint mTextMinTempPaint;

        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //TODO Time classes used in this example are different than the watch face sample.  Which impelemntation is better?
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        private Integer mMinTemp;
        private Integer mMaxTemp;
        private Integer mWeatherId;

        private float mTimeWidth;
        private float mDateWidth;
        private float mTimeHeight;
        private float mDateHeight;
        private float mMaxTempWidth;
        private float mMinTempWidth;
        private float mMaxTempHeight;

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.LEFT | Gravity.TOP)
                    //.setHotwordIndicatorGravity(Gravity.RIGHT | Gravity.TOP)
                    .build());
            Resources resources = MyWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.primary));

            mTextWhitePaint = new Paint();
            mTextWhitePaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTextLightPaint = new Paint();
            mTextLightPaint = createTextPaint(resources.getColor(R.color.primary_light));

            mTextMaxTempPaint = new Paint();
            mTextMaxTempPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTextMinTempPaint = new Paint();
            mTextMinTempPaint = createTextPaint(resources.getColor(R.color.primary_light));

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d(TAG, "onVisibilityChanged");
            if (visible) {
                registerReceiver();
                mGoogleApiClient.connect();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                    Log.d(TAG, "disconnecting GoogleApiClient");
                }
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextWhitePaint.setTextSize(textSize);
            mTextLightPaint.setTextSize(resources.getDimension(R.dimen.date_text_size));
            mTextMinTempPaint.setTextSize(resources.getDimension(R.dimen.min_temp_text_size));
            mTextMaxTempPaint.setTextSize(resources.getDimension(R.dimen.max_temp_text_size));

            mTimeHeight = mTextWhitePaint.getTextSize();
            mDateHeight = mTextLightPaint.getTextSize();
            mMaxTempHeight = mTextMaxTempPaint.getTextSize();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            // method is called giving very special display support
            super.onPropertiesChanged(properties);
            Log.d(TAG, "onPropertiesChanged run..");
            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            //TODO set typeface based on burnInProtection
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            // this method is called in ambient mode once/minute
            super.onTimeTick();
            // tell framework canvas needs to be redrawn (onDraw() method)
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            // called whenever a switch has been made between modes
            Log.d(TAG, "onAmbientModeChanged");
            super.onAmbientModeChanged(inAmbientMode);

            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextWhitePaint.setAntiAlias(!inAmbientMode);
                    mTextLightPaint.setAntiAlias(!inAmbientMode);
                    mTextMaxTempPaint.setAntiAlias(!inAmbientMode);
                    mTextMinTempPaint.setAntiAlias(!inAmbientMode);
                }
                // force onDraw refresh after these changes
                if (!mAmbient) {

                }
                else {

                }
                invalidate();
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // onDraw is called alot, so should be efficient
            // it is called in both ambient and interactive mode

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, width, height, mBackgroundPaint);

            }
            double centerX = width / 2f;
            double centerY = height / 2f;

            // Draw Line Separator
            int lineLength = width/4;
            canvas.drawLine((float) centerX-lineLength/2, (float) centerY, (float) centerX+lineLength/2, (float) centerY, mTextLightPaint);

            float topTextBaselineOffset = height/2/5;

            // Draw H:MM in ambient and interactive modes
            mTime.setToNow();
            String time = String.format("%d:%02d", mTime.hour, mTime.minute);
            // Day of the week
            Integer weekDay = Utility.getDay(mTime.weekDay);
            String weekDayAbbrev;
            if (weekDay != null) {
                weekDayAbbrev = getResources().getString(weekDay);
            }
            else {
                weekDayAbbrev = "";
            }
            // Month of the year
            Integer yearMonth = Utility.getMonth(mTime.month);
            String yearMonthAbbrev;

            // Calculate the positioning of time and date strings based on available space
            float topMargin = 30;   // in pixels
            float dateMargin = (height/2 -mDateHeight - mTimeHeight - topMargin)/2;
            if (yearMonth != null) {
                yearMonthAbbrev = getResources().getString(yearMonth);
            }
            else {
                yearMonthAbbrev = "";
            }
            String formattedDate = weekDayAbbrev + ", " + yearMonthAbbrev + " " + mTime.monthDay;

            // Measure the text to be drawn on the screen

            mTimeWidth = mTextWhitePaint.measureText(time);
            mDateWidth = mTextLightPaint.measureText(formattedDate);

            if (mMaxTemp != null && mMinTemp != null) {
                mMaxTempWidth = mTextMaxTempPaint.measureText(Integer.toString(mMaxTemp) + DEGREE_SYMBOL);
                mMinTempWidth = mTextMinTempPaint.measureText(Integer.toString(mMinTemp)+ DEGREE_SYMBOL);
            }

            // convert DP to pixels

            // Draw the top half of screen
            canvas.drawText(time, (float) centerX - mTimeWidth / 2, (float) centerY - dateMargin * 2 - mDateHeight, mTextWhitePaint);
            canvas.drawText(formattedDate, (float)centerX-mDateWidth/2, (float) centerY-dateMargin, mTextLightPaint);

            // Draw the bottom half of the screen, if not in ambient mode
            if (!isInAmbientMode()) {
                // Canvas uses px - need to convert from dp and do math in px
                Resources r = getResources();
                int artMargin = (int) getResources().getDimension(R.dimen.art_margin_top);
                float artMarginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, artMargin, r.getDisplayMetrics());
                int art = (int) getResources().getDimension(R.dimen.weather_art_size);
                float artPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, art, r.getDisplayMetrics());

                float offsetTextPx = (artPx - mMaxTempHeight) / 2f;
                float textBaselinePx = (float) centerY + artMarginPx + artPx - offsetTextPx;
                // equal spacing between art,
                int weatherInfoSpacing = (int) ((width - artPx - mMinTempWidth - mMaxTempWidth) / 4);
                if (mWeatherId != null) {
                    int weatherImage = Utility.getArtResourceForWeatherCondition(mWeatherId);
                    Drawable weatherArt = ResourcesCompat.getDrawable(getResources(), weatherImage, null);
                    weatherArt.setBounds((int) (weatherInfoSpacing),
                            (int) (centerY + artMarginPx), (int) (weatherInfoSpacing + artPx), (int) (centerY + artPx + artMarginPx));
                    weatherArt.draw(canvas);
                }
                if (mMaxTemp != null) {
                    canvas.drawText(Integer.toString(mMaxTemp) + DEGREE_SYMBOL, (float) 2 * weatherInfoSpacing + artPx, textBaselinePx, mTextMaxTempPaint);
                }
                if (mMinTemp != null) {
                    canvas.drawText(Integer.toString(mMinTemp) + DEGREE_SYMBOL, (float) 3 * weatherInfoSpacing + artPx + mMaxTempWidth, textBaselinePx, mTextMinTempPaint);
                }
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            // called when connection to Google Play services is established
            Log.d(TAG, "onConnected running...");
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + bundle);
            }
            // notify Google Play services that we are interested in listening for data layer events
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended running...");
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {

                DataItem dataItem = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                for (String configKey : config.keySet()) {
                    if (configKey.equals(KEY_MIN_TEMP)) {
                        mMinTemp = config.getInt(KEY_MIN_TEMP);
                    }
                    if (configKey.equals(KEY_MAX_TEMP)) {
                        mMaxTemp = config.getInt(KEY_MAX_TEMP);
                    }
                    if (configKey.equals(KEY_WEATHER_CONDITION_ID)) {
                        mWeatherId = config.getInt(KEY_WEATHER_CONDITION_ID);
                    }
                }
            }
        }
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "onConnectionFailed running...");

        }
    }
}
