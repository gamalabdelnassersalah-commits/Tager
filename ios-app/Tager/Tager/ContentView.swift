import SwiftUI
import WebKit

struct WebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.preferences.javaScriptEnabled = true
        config.allowsInlineMediaPlayback = true

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true

        var request = URLRequest(url: url)
        request.cachePolicy = .reloadRevalidatingCacheData
        webView.load(request)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator: NSObject, WKNavigationDelegate {
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            // Inject image optimization
            let script = """
            (function(){
                if(!window.__tagerImageOpt){
                    window.__tagerImageOpt = true;
                    var isSupabaseStorage = function(u){
                        try {
                            var url = new URL(u, location.origin);
                            return /supabase.co$/.test(url.hostname) && /storage\\/v1\\/object\\/public\\//.test(url.pathname);
                        } catch(_) { return false; }
                    };
                    var addThumbParams = function(u, size){
                        try {
                            var url = new URL(u, location.origin);
                            if(!isSupabaseStorage(url.href)) return u;
                            url.searchParams.set('width', String(size));
                            url.searchParams.set('height', String(size));
                            url.searchParams.set('resize', 'cover');
                            url.searchParams.set('quality', '75');
                            return url.href;
                        } catch(_) { return u; }
                    };
                    var observer = new MutationObserver(function(mutations){
                        mutations.forEach(function(m){
                            m.addedNodes.forEach(function(node){
                                if(node.nodeType === 1){
                                    if(node.tagName === 'IMG'){
                                        var src = node.getAttribute('src') || '';
                                        if(src && !node.dataset.tagerOpt && isSupabaseStorage(src)){
                                            var w = node.clientWidth || node.offsetWidth || 200;
                                            var size = Math.min(Math.max(w * 2, 200), 600);
                                            node.src = addThumbParams(src, size);
                                            node.dataset.tagerOpt = '1';
                                            node.decoding = 'async';
                                        }
                                    } else if(node.querySelectorAll){
                                        node.querySelectorAll('img').forEach(function(img){
                                            var src = img.getAttribute('src') || '';
                                            if(src && !img.dataset.tagerOpt && isSupabaseStorage(src)){
                                                var w = img.clientWidth || img.offsetWidth || 200;
                                                var size = Math.min(Math.max(w * 2, 200), 600);
                                                img.src = addThumbParams(src, size);
                                                img.dataset.tagerOpt = '1';
                                                img.decoding = 'async';
                                            }
                                        });
                                    }
                                }
                            });
                        });
                    });
                    observer.observe(document.body, {childList: true, subtree: true});
                    document.querySelectorAll('img').forEach(function(img){
                        var src = img.getAttribute('src') || '';
                        if(src && !img.dataset.tagerOpt && isSupabaseStorage(src)){
                            var w = img.clientWidth || img.offsetWidth || 200;
                            var size = Math.min(Math.max(w * 2, 200), 600);
                            img.src = addThumbParams(src, size);
                            img.dataset.tagerOpt = '1';
                            img.decoding = 'async';
                        }
                    });
                }
            })();
            """
            webView.evaluateJavaScript(script, completionHandler: nil)
        }

        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            if let url = navigationAction.request.url {
                let scheme = url.scheme ?? ""
                if scheme == "http" || scheme == "https" {
                    if url.host == "tager-new.vercel.app" || url.host?.hasSuffix(".tager-new.vercel.app") == true {
                        decisionHandler(.allow)
                        return
                    }
                    if UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                        decisionHandler(.cancel)
                        return
                    }
                }
            }
            decisionHandler(.allow)
        }
    }
}

struct ContentView: View {
    private let homeURL = URL(string: "https://tager-new.vercel.app/?tager_app=ios&app_version=1.0.0")!

    var body: some View {
        VStack(spacing: 0) {
            WebView(url: homeURL)
            // Native bottom navigation bar
            HStack(spacing: 0) {
                NavButton(title: "الرئيسية", icon: "house", page: "home")
                NavButton(title: "المنتجات", icon: "cube", page: "products")
                NavButton(title: "الموردون", icon: "storefront", page: "suppliers")
                NavButton(title: "تتبع", icon: "location", page: "track")
                NavButton(title: "السلة", icon: "cart", page: "cart")
            }
            .frame(height: 56)
            .background(Color(.systemBackground))
            .shadow(color: Color.black.opacity(0.1), radius: 4, y: -2)
        }
        .ignoresSafeArea(edges: .bottom)
    }
}

struct NavButton: View {
    let title: String
    let icon: String
    let page: String

    var body: some View {
        Button(action: {
            // Navigate via JavaScript
            if let webView = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first?.windows
                .first?.rootViewController?.view.subviews
                .compactMap({ $0 as? WKWebView })
                .first {
                let js = "if(typeof window.go==='function'){window.go('\(page)');}else{location.hash='\(page)';}"
                webView.evaluateJavaScript(js)
            }
        }) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                Text(title)
                    .font(.system(size: 11, weight: .medium))
            }
            .foregroundColor(Color(red: 0.0, green: 0.353, blue: 0.310))
            .frame(maxWidth: .infinity)
        }
    }
}
