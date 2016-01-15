package com.vfpowertech.jsbridge.ios

import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.uikit.*

class Main : UIApplicationDelegateAdapter() {

    override fun didFinishLaunching(application: UIApplication, launchOptions: UIApplicationLaunchOptions?): Boolean {
        val window = UIWindow(UIScreen.getMainScreen().bounds)
        window.backgroundColor = UIColor.black()
        setWindow(window)

        val webViewController = WebViewController()
        val navigationController = UINavigationController(webViewController)

        window.rootViewController = navigationController
        window.makeKeyAndVisible()
        return true
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val pool = NSAutoreleasePool()
            try {
                UIApplication.main<UIApplication, Main>(args, null, Main::class.java)
            }
            finally {
                pool.close()
            }
        }
    }
}
