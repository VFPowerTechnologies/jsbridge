package com.vfpowertech.jsbridge.ios;

import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.*;

public class Main extends UIApplicationDelegateAdapter {

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        UIWindow window = new UIWindow(UIScreen.getMainScreen().getBounds());
        window.setBackgroundColor(UIColor.black());
        setWindow(window);

        WebViewController webViewController = new WebViewController();
        UINavigationController navigationController = new UINavigationController(webViewController);

        window.setRootViewController(navigationController);
        window.makeKeyAndVisible();
        return true;
    }

    public static void main(String[] args) {
        Foundation.log("Main");
        try (NSAutoreleasePool pool = new NSAutoreleasePool()) {
            UIApplication.main(args, null, Main.class);
        }
    }
}
