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

package ru.playsoftware.j2meloader.gamepad;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Set;

import javax.microedition.lcdui.Canvas;

public class GamepadManager implements InputManager.InputDeviceListener {

	public interface GamepadListener {
		void onControllerConnected(String controllerName);
		void onControllerDisconnected(String controllerName);
	}

	public interface KeyEventListener {
		void onSimulatedKeyDown(int midpKeyCode);
		void onSimulatedKeyUp(int midpKeyCode);
	}

	private static final float DEADZONE = 0.35f;

	private final Context context;
	private final KeyEventListener keyEventListener;
	private GamepadListener gamepadListener;
	private final Set<Integer> knownGamepadIds = new HashSet<>();

	private boolean isUpPressed;
	private boolean isDownPressed;
	private boolean isLeftPressed;
	private boolean isRightPressed;

	public GamepadManager(Context context, KeyEventListener keyEventListener) {
		this.context = context;
		this.keyEventListener = keyEventListener;
	}

	public void setGamepadListener(GamepadListener listener) {
		this.gamepadListener = listener;
	}

	public void register() {
		InputManager im = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
		if (im != null) {
			im.registerInputDeviceListener(this, null);
			checkConnectedControllers();
		}
	}

	public void unregister() {
		InputManager im = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
		if (im != null) {
			im.unregisterInputDeviceListener(this);
		}
		resetAxes();
	}

	public void checkConnectedControllers() {
		int[] deviceIds = InputDevice.getDeviceIds();
		for (int id : deviceIds) {
			InputDevice device = InputDevice.getDevice(id);
			if (isGamepad(device)) {
				if (!knownGamepadIds.contains(id)) {
					knownGamepadIds.add(id);
					notifyConnected(device);
				}
			}
		}
	}

	public static boolean isGamepad(InputDevice device) {
		if (device == null || device.isVirtual()) {
			return false;
		}
		int sources = device.getSources();
		return ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
				|| ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK);
	}

	public static String formatControllerName(InputDevice device) {
		if (device == null) return "Gamepad";
		String name = device.getName();
		if (name == null || name.trim().isEmpty()) {
			return "Bluetooth/USB Gamepad";
		}
		String lower = name.toLowerCase();
		if (lower.contains("xbox") || lower.contains("microsoft")) {
			return "Xbox Wireless Controller";
		} else if (lower.contains("dualsense") || lower.contains("wireless controller") || lower.contains("playstation") || lower.contains("sony")) {
			return "PlayStation Controller";
		} else if (lower.contains("8bitdo")) {
			return "8BitDo Gamepad (" + name + ")";
		} else if (lower.contains("switch") || lower.contains("joy-con") || lower.contains("nintendo")) {
			return "Nintendo Switch Controller";
		}
		return name;
	}

	private void notifyConnected(InputDevice device) {
		if (gamepadListener != null) {
			gamepadListener.onControllerConnected(formatControllerName(device));
		}
	}

	public boolean handleGenericMotionEvent(MotionEvent event) {
		if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK
				&& (event.getSource() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) {
			return false;
		}

		float x = event.getAxisValue(MotionEvent.AXIS_X);
		float y = event.getAxisValue(MotionEvent.AXIS_Y);
		float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
		float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

		float effectiveX = Math.abs(hatX) > 0.1f ? hatX : x;
		float effectiveY = Math.abs(hatY) > 0.1f ? hatY : y;

		boolean wantLeft = effectiveX < -DEADZONE;
		boolean wantRight = effectiveX > DEADZONE;
		boolean wantUp = effectiveY < -DEADZONE;
		boolean wantDown = effectiveY > DEADZONE;

		processAxisDirection(wantLeft, isLeftPressed, Canvas.KEY_LEFT, pressed -> isLeftPressed = pressed);
		processAxisDirection(wantRight, isRightPressed, Canvas.KEY_RIGHT, pressed -> isRightPressed = pressed);
		processAxisDirection(wantUp, isUpPressed, Canvas.KEY_UP, pressed -> isUpPressed = pressed);
		processAxisDirection(wantDown, isDownPressed, Canvas.KEY_DOWN, pressed -> isDownPressed = pressed);

		return true;
	}

	private interface StateSetter {
		void set(boolean state);
	}

	private void processAxisDirection(boolean want, boolean current, int midpKey, StateSetter setter) {
		if (want != current) {
			setter.set(want);
			if (want) {
				keyEventListener.onSimulatedKeyDown(midpKey);
			} else {
				keyEventListener.onSimulatedKeyUp(midpKey);
			}
		}
	}

	public void resetAxes() {
		if (isLeftPressed) { isLeftPressed = false; keyEventListener.onSimulatedKeyUp(Canvas.KEY_LEFT); }
		if (isRightPressed) { isRightPressed = false; keyEventListener.onSimulatedKeyUp(Canvas.KEY_RIGHT); }
		if (isUpPressed) { isUpPressed = false; keyEventListener.onSimulatedKeyUp(Canvas.KEY_UP); }
		if (isDownPressed) { isDownPressed = false; keyEventListener.onSimulatedKeyUp(Canvas.KEY_DOWN); }
	}

	@Override
	public void onInputDeviceAdded(int deviceId) {
		InputDevice device = InputDevice.getDevice(deviceId);
		if (isGamepad(device)) {
			knownGamepadIds.add(deviceId);
			notifyConnected(device);
		}
	}

	@Override
	public void onInputDeviceRemoved(int deviceId) {
		if (knownGamepadIds.remove(deviceId)) {
			if (gamepadListener != null) {
				gamepadListener.onControllerDisconnected("Gamepad (ID " + deviceId + ")");
			}
		}
	}

	@Override
	public void onInputDeviceChanged(int deviceId) {
	}
}
