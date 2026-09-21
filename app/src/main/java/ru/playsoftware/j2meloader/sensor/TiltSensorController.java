/*
 * Copyright 2026 J2ME Play
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;
import android.view.WindowManager;

import javax.microedition.lcdui.Canvas;
import ru.playsoftware.j2meloader.gamepad.GamepadManager;

public class TiltSensorController implements SensorEventListener {

	private static final float DEFAULT_TILT_THRESHOLD = 2.4f; // m/s^2 (~14 degrees tilt)

	private final Context context;
	private final GamepadManager.KeyEventListener keyListener;
	private final SensorManager sensorManager;
	private final Sensor accelerometer;

	private boolean isEnabled = false;
	private float tiltThreshold = DEFAULT_TILT_THRESHOLD;

	private float baselineX = 0.0f;
	private float baselineY = 0.0f;

	private boolean isLeftPressed;
	private boolean isRightPressed;
	private boolean isUpPressed;
	private boolean isDownPressed;

	public TiltSensorController(Context context, GamepadManager.KeyEventListener keyListener) {
		this.context = context;
		this.keyListener = keyListener;
		this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = null;
		if (sensorManager != null) {
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
			if (sensor == null) {
				sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			}
		}
		this.accelerometer = sensor;
	}

	public boolean isSensorAvailable() {
		return accelerometer != null;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.isEnabled == enabled) return;
		this.isEnabled = enabled;
		if (enabled) {
			start();
		} else {
			stop();
		}
	}

	public void toggle() {
		setEnabled(!isEnabled);
	}

	public void calibrate() {
		baselineX = lastRawX;
		baselineY = lastRawY;
	}

	private float lastRawX = 0f;
	private float lastRawY = 0f;

	private void start() {
		if (sensorManager != null && accelerometer != null) {
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	private void stop() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		resetTiltKeys();
	}

	public void resetTiltKeys() {
		if (isLeftPressed) { isLeftPressed = false; keyListener.onSimulatedKeyUp(Canvas.KEY_LEFT); }
		if (isRightPressed) { isRightPressed = false; keyListener.onSimulatedKeyUp(Canvas.KEY_RIGHT); }
		if (isUpPressed) { isUpPressed = false; keyListener.onSimulatedKeyUp(Canvas.KEY_UP); }
		if (isDownPressed) { isDownPressed = false; keyListener.onSimulatedKeyUp(Canvas.KEY_DOWN); }
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (!isEnabled) return;

		float rawX = event.values[0];
		float rawY = event.values[1];
		lastRawX = rawX;
		lastRawY = rawY;

		int rotation = Surface.ROTATION_0;
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (wm != null && wm.getDefaultDisplay() != null) {
			rotation = wm.getDefaultDisplay().getRotation();
		}

		float screenX;
		float screenY;

		switch (rotation) {
			case Surface.ROTATION_90:
				screenX = -rawY;
				screenY = rawX;
				break;
			case Surface.ROTATION_180:
				screenX = -rawX;
				screenY = -rawY;
				break;
			case Surface.ROTATION_270:
				screenX = rawY;
				screenY = -rawX;
				break;
			case Surface.ROTATION_0:
			default:
				screenX = rawX;
				screenY = rawY;
				break;
		}

		// Adjust for baseline/calibration
		float diffX = screenX - baselineX;
		float diffY = screenY - baselineY;

		// Tilting phone right means diffX < -threshold, tilting left means diffX > threshold
		boolean wantLeft = diffX > tiltThreshold;
		boolean wantRight = diffX < -tiltThreshold;
		// Tilting top away/down
		boolean wantUp = diffY < (4.0f - tiltThreshold);
		boolean wantDown = diffY > (7.5f + tiltThreshold);

		if (wantLeft != isLeftPressed) {
			isLeftPressed = wantLeft;
			if (wantLeft) keyListener.onSimulatedKeyDown(Canvas.KEY_LEFT);
			else keyListener.onSimulatedKeyUp(Canvas.KEY_LEFT);
		}
		if (wantRight != isRightPressed) {
			isRightPressed = wantRight;
			if (wantRight) keyListener.onSimulatedKeyDown(Canvas.KEY_RIGHT);
			else keyListener.onSimulatedKeyUp(Canvas.KEY_RIGHT);
		}
		if (wantUp != isUpPressed) {
			isUpPressed = wantUp;
			if (wantUp) keyListener.onSimulatedKeyDown(Canvas.KEY_UP);
			else keyListener.onSimulatedKeyUp(Canvas.KEY_UP);
		}
		if (wantDown != isDownPressed) {
			isDownPressed = wantDown;
			if (wantDown) keyListener.onSimulatedKeyDown(Canvas.KEY_DOWN);
			else keyListener.onSimulatedKeyUp(Canvas.KEY_DOWN);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
