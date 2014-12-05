/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.deskclock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews.RemoteView;
import com.android.deskclock.obfuscated.R;
import java.util.TimeZone;

/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 */
public class AnalogClock extends View {
    public Time mCalendar;

    public final Drawable mHourHand;
    public final Drawable mMinuteHand;
    public final Drawable mSecondHand;
    public final Drawable mDial;

    public final int mDialWidth;
    public final int mDialHeight;

    public boolean mAttached;

    public final Handler mHandler = new Handler();
    public float mSeconds;
    public float mMinutes;
    public float mHour;
    public boolean mChanged;
    public final Context mContext;
    public String mTimeZoneId;
    public boolean mNoSeconds = false;

    public final float mDotRadius;
    public final float mDotOffset;
    public Paint mDotPaint;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        final Resources r = mContext.getResources();

        mDial = r.getDrawable(R.drawable.clock_analog_dial_mipmap);
        mHourHand = r.getDrawable(R.drawable.clock_analog_hour_mipmap);
        mMinuteHand = r.getDrawable(R.drawable.clock_analog_minute_mipmap);
        mSecondHand = r.getDrawable(R.drawable.clock_analog_second_mipmap);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock);
        mDotRadius = a.getDimension(R.styleable.AnalogClock_jewelRadius, 0);
        mDotOffset = a.getDimension(R.styleable.AnalogClock_jewelOffset, 0);
        final int dotColor = a.getColor(R.styleable.AnalogClock_jewelColor, Color.WHITE);
        if (dotColor != 0) {
            mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mDotPaint.setColor(dotColor);
        }

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            final IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();

        // tick the seconds
        post(mClockTick);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            removeCallbacks(mClockTick);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    	final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    	final int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
    	final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    	final int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        final float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        final int availableWidth = getWidth();
        final int availableHeight = getHeight();

        final int x = availableWidth / 2;
        final int y = availableHeight / 2;

        final Drawable dial = mDial;
        final int w = dial.getIntrinsicWidth();
        final int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            final float scale = Math.min((float) availableWidth / (float) w,
                                   (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);

        if (mDotRadius > 0f && mDotPaint != null) {
            canvas.drawCircle(x, y - (h / 2) + mDotOffset, mDotRadius, mDotPaint);
        }

        drawHand(canvas, mHourHand, x, y, mHour / 12.0f * 360.0f, changed);
        drawHand(canvas, mMinuteHand, x, y, mMinutes / 60.0f * 360.0f, changed);
        if (!mNoSeconds) {
            drawHand(canvas, mSecondHand, x, y, mSeconds / 60.0f * 360.0f, changed);
        }

        if (scaled) {
            canvas.restore();
        }
    }

    public void drawHand(Canvas canvas, Drawable hand, int x, int y, float angle,
          boolean changed) {
      canvas.save();
      canvas.rotate(angle, x, y);
      if (changed) {
          final int w = hand.getIntrinsicWidth();
          final int h = hand.getIntrinsicHeight();
          hand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
      }
      hand.draw(canvas);
      canvas.restore();
    }

    public void onTimeChanged() {
        mCalendar.setToNow();

        if (mTimeZoneId != null) {
            mCalendar.switchTimezone(mTimeZoneId);
        }

        final int hour = mCalendar.hour;
        final int minute = mCalendar.minute;
        final int second = mCalendar.second;
  //      long millis = System.currentTimeMillis() % 1000;

        mSeconds = second;//(float) ((second * 1000 + millis) / 166.666);
        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;

        updateContentDescription(mCalendar);
    }

    public final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            	final String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            onTimeChanged();
            invalidate();
        }
    };

    public final Runnable mClockTick = new Runnable () {

        @Override
        public void run() {
            onTimeChanged();
            invalidate();
            AnalogClock.this.postDelayed(mClockTick, 1000);
        }
    };

    public void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        final String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }

    public void setTimeZone(String id) {
        mTimeZoneId = id;
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        mNoSeconds = !enable;
    }

}

