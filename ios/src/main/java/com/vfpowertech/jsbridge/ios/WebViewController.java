package com.vfpowertech.jsbridge.ios;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.apple.webkit.WKUserContentController;
import org.robovm.apple.webkit.WKWebView;
import org.robovm.apple.webkit.WKWebViewConfiguration;

import java.io.File;

public class WebViewController extends UIViewController {
    @Override
    public void loadView() {
        CGRect bounds = UIScreen.getMainScreen().getBounds();
        UIView contentView = new UIView(bounds);
        contentView.setBackgroundColor(UIColor.darkGray());

        setView(contentView);

        WKWebViewConfiguration configuration = new WKWebViewConfiguration();
        WKUserContentController userContentController = new WKUserContentController();
        userContentController.addScriptMessageHandler(new MessageHandler(), "native");
        configuration.setUserContentController(userContentController);

        WKWebView webView = new WKWebView(contentView.getFrame(), configuration);

        webView.setNavigationDelegate(new NavDelegate());

        String path = NSBundle.getMainBundle().findResourcePath("index", "html", "www");
        File f = new File(path);
        webView.loadRequest(new NSURLRequest(new NSURL(f)));

        //NSURL url = new NSURL("http://www.google.com");
        //webView.loadRequest(new NSURLRequest(url));

        contentView.addSubview(webView);

        Foundation.log("Here");
    }
}
