package expo.modules.battery;

import android.content.Context;

import org.unimodules.core.ExportedModule;
import org.unimodules.core.ModuleRegistry;
import org.unimodules.core.Promise;
import org.unimodules.core.interfaces.ExpoMethod;
import org.unimodules.core.interfaces.RegistryLifecycleListener;
import org.unimodules.core.interfaces.services.EventEmitter;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class BatteryModule extends ExportedModule implements RegistryLifecycleListener {
  private static final String NAME = "ExpoBattery";
  private static final String TAG = BatteryModule.class.getSimpleName();
  private static final String BATTERY_LEVEL_EVENT_NAME = "Expo.batteryLevelDidChange";
  private static final String BATTERY_CHARGED_EVENT_NAME = "Expo.batteryStateDidChange";
  private static final String POWERMODE_EVENT_NAME = "Expo.powerModeDidChange";

  private ModuleRegistry mModuleRegistry;
  static protected Context mContext;
  static private EventEmitter mEventEmitter;

  public BatteryModule(Context context) {
    super(context);
    mContext = context;
  }

  public enum BatteryState {
    UNKNOWN(0),
    UNPLUGGED(1),
    CHARGING(2),
    FULL(3);

    private final int value;

    BatteryState(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }


  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mModuleRegistry = moduleRegistry;
    mEventEmitter = moduleRegistry.getModule(EventEmitter.class);
    mContext.registerReceiver(new BatteryStateReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    mContext.registerReceiver(new PowerSaverReceiver(), new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED"));
    IntentFilter ifilter = new IntentFilter();
    ifilter.addAction(Intent.ACTION_BATTERY_LOW);
    ifilter.addAction(Intent.ACTION_BATTERY_OKAY);
    mContext.registerReceiver(new BatteryLevelReceiver(), ifilter);
  }

  static protected void onBatteryStateChange(BatteryState batteryState) {
    Bundle result = new Bundle();
    result.putInt("batteryState", batteryState.getValue());
    mEventEmitter.emit(BATTERY_CHARGED_EVENT_NAME, result);
  }

  static protected void onLowPowerModeChange(boolean lowPowerMode) {
    Bundle result = new Bundle();
    result.putBoolean("lowPowerMode", lowPowerMode);
    mEventEmitter.emit(POWERMODE_EVENT_NAME, result);
  }

  static protected void onBatteryLevelChange(float BatteryLevel) {
    Bundle result = new Bundle();
    result.putFloat("batteryLevel", BatteryLevel);
    mEventEmitter.emit(BATTERY_LEVEL_EVENT_NAME, result);
  }

  static protected BatteryState batteryStatusNativeToJS(int status) {
    if (status == BatteryManager.BATTERY_STATUS_FULL) {
      return BatteryState.FULL;
    } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
      return BatteryState.CHARGING;
    } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING || status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
      return BatteryState.UNPLUGGED;
    } else {
      return BatteryState.UNKNOWN;
    }
  }

  @ExpoMethod
  public void getBatteryLevelAsync(Promise promise) {
    Intent batteryIntent = this.mContext.getApplicationContext().registerReceiver(
      null,
      new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (batteryIntent == null) {
      promise.resolve(-1);
      return;
    }

    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    float batteryLevel = (level != -1 && scale != -1) ? level / (float) scale : -1;
    promise.resolve(batteryLevel);
  }

  @ExpoMethod
  public void getBatteryStateAsync(Promise promise) {
    Intent batteryIntent = this.mContext.getApplicationContext().registerReceiver(
      null,
      new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (batteryIntent == null) {
      promise.resolve(BatteryState.UNKNOWN.getValue());
      return;
    }

    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    promise.resolve(batteryStatusNativeToJS(status).getValue());
  }

  @ExpoMethod
  public void isLowPowerModeEnabledAsync(Promise promise) {
    PowerManager powerManager = (PowerManager) mContext.getApplicationContext().getSystemService(Context.POWER_SERVICE);
    if (powerManager == null) {
      promise.reject("ERR_BATTERY_LOW_POWER_UNREADABLE", "Could not get low-power mode");
      return;
    }

    boolean lowPowerMode = powerManager.isPowerSaveMode();
    promise.resolve(lowPowerMode);
  }

  @ExpoMethod
  public void getPowerStateAsync(Promise promise) {
    Bundle result = new Bundle();
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryIntent = this.mContext.getApplicationContext().registerReceiver(null, ifilter);

    if (batteryIntent == null) {
      result.putFloat("batteryLevel", -1);
      result.putInt("batteryState", BatteryState.UNKNOWN.getValue());  
    } else {
      int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
      float batteryLevel = (level != -1 && scale != -1) ? level / (float) scale : -1;
      result.putFloat("batteryLevel", batteryLevel);

      int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      result.putInt("batteryState", batteryStatusNativeToJS(status).getValue());  
    }

    PowerManager powerManager = (PowerManager) mContext.getApplicationContext().getSystemService(Context.POWER_SERVICE);
    if (powerManager == null) {
      promise.reject("ERR_BATTERY_LOW_POWER_UNREADABLE", "Could not get low-power mode");
      return;
    } else {
      boolean lowPowerMode = powerManager.isPowerSaveMode();
      result.putBoolean("lowPowerMode", lowPowerMode);
    }

    promise.resolve(result);
  }
}
