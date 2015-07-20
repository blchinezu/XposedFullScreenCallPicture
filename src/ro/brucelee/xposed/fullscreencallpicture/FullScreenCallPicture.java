package ro.brucelee.xposed.fullscreencallpicture;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


public class FullScreenCallPicture implements IXposedHookLoadPackage {

    private static final String TAG = "FullScreenCallPicture";
    public static final String PACKAGE_NAME = "com.android.phone";
    private static final String CLASS_CALLCARD = "com.android.phone.CallCard";
    private static final String CLASS_PHONE_CONSTANTS_STATE = "com.android.internal.telephony.PhoneConstants$State";
    private static final String CLASS_CALL = "com.android.internal.telephony.Call";
    private static final String CLASS_IN_CALL_TOUCH_UI = "com.android.phone.InCallTouchUi";

    private static XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals(PACKAGE_NAME))
            return;

        try {
            XposedBridge.log(TAG + ": init");

            prefs = new XSharedPreferences(PACKAGE_NAME);

            Class<?> phoneConstStateClass = XposedHelpers.findClass(CLASS_PHONE_CONSTANTS_STATE, null);
            Class<?> callClass = XposedHelpers.findClass(CLASS_CALL, null);

            Class<?> callCardClass = XposedHelpers.findClass(CLASS_CALLCARD, lpparam.classLoader);
            Class<?> inCallTouchUiClass = XposedHelpers.findClass(CLASS_IN_CALL_TOUCH_UI, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(callCardClass, "updateCallInfoLayout", phoneConstStateClass,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            prefs.reload();

                            XposedBridge.log(TAG + ": CallCard: after updateCallInfoLayout");

                            LinearLayout layout = (LinearLayout) param.thisObject;
                            ViewGroup.MarginLayoutParams mlParams =
                                    (ViewGroup.MarginLayoutParams) layout.getLayoutParams();
                            if (mlParams != null) {
                                mlParams.bottomMargin = 0;
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(callCardClass, "updateCallBannerBackground",
                    callClass, ViewGroup.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    prefs.reload();

                    XposedBridge.log(TAG + ": CallCard: after updateCallBannerBackground");

                    TextView simIndicator =
                            (TextView) XposedHelpers.getObjectField(param.thisObject, "mSimIndicator");
                    if (simIndicator != null) {
                        simIndicator.setBackgroundResource(0);
                    }

                    ViewGroup secondaryInfoContainer =
                            (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSecondaryInfoContainer");
                    if (secondaryInfoContainer != null) {
                        secondaryInfoContainer.setBackgroundResource(0);
                    }

                    ViewGroup secondaryCallBanner =
                            (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mSecondaryCallBanner");
                    if (secondaryCallBanner != null) {
                        secondaryCallBanner.setBackgroundResource(0);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(inCallTouchUiClass, "showIncomingCallWidget",
                    callClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": InCallTouchUi: after showIncomingCallWidget");
                    prefs.reload();

                    View incomingCallWidget =
                            (View) XposedHelpers.getObjectField(param.thisObject, "mIncomingCallWidget");
                    if (incomingCallWidget != null) {
                        incomingCallWidget.setBackgroundColor(Color.TRANSPARENT/*Color.BLACK*/);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }
}