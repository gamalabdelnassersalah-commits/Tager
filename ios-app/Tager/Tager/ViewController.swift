import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, WKUIDelegate {
    private var webView: WKWebView!
    private var progressBar: UIProgressView!
    private var loadingView: UIView!
    private var loadingLabel: UILabel!
    private var offlineView: UIView!
    private var navBar: UIView!
    private var navItems: [(String, String)] = []
    private var navButtons: [UIButton] = []
    private let homeURL = URL(string: "https://tager-new.vercel.app/?tager_app=ios&app_version=1.0.0")!
    private var filePickerCallback: WKUIDelegate?
    private var documentPicker: UIDocumentPickerViewController?

    // Navigation pages
    private let navPages: [(title: String, page: String, icon: String)] = [
        ("الرئيسية", "home", "🏠"),
        ("المنتجات", "products", "📦"),
        ("الموردون", "suppliers", "🏬"),
        ("تتبع", "track", "📍"),
        ("السلة", "cart", "🛒")
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadPage("home")
    }

    // MARK: - UI Setup
    private func setupUI() {
        view.backgroundColor = UIColor.systemBackground

        // Navigation Bar
        navBar = UIView()
        navBar.backgroundColor = UIColor { traitCollection in
            return traitCollection.userInterfaceStyle == .dark ? UIColor.black : UIColor.white
        }
        navBar.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(navBar)

        // WebView
        let config = WKWebViewConfiguration()
        config.preferences.javaScriptEnabled = true
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        view.addSubview(webView)

        // Progress Bar
        progressBar = UIProgressView(progressViewStyle: .bar)
        progressBar.translatesAutoresizingMaskIntoConstraints = false
        progressBar.tintColor = UIColor(red: 0.0, green: 0.353, blue: 0.310, alpha: 1.0)
        view.addSubview(progressBar)

        // Loading View
        loadingView = UIView()
        loadingView.backgroundColor = UIColor.systemBackground
        loadingView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(loadingView)

        loadingLabel = UILabel()
        loadingLabel.text = "جاري التحميل..."
        loadingLabel.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        loadingLabel.textColor = UIColor.darkGray
        loadingLabel.textAlignment = .center
        loadingLabel.translatesAutoresizingMaskIntoConstraints = false
        loadingView.addSubview(loadingLabel)

        // Offline View
        setupOfflineView()

        // Navigation buttons
        setupNavButtons()

        // Constraints
        NSLayoutConstraint.activate([
            // Nav bar at bottom
            navBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            navBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            navBar.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            navBar.heightAnchor.constraint(equalToConstant: 56),

            // WebView
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: navBar.topAnchor),

            // Progress bar
            progressBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressBar.topAnchor.constraint(equalTo: view.topAnchor),
            progressBar.heightAnchor.constraint(equalToConstant: 3),

            // Loading view
            loadingView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            loadingView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            loadingView.topAnchor.constraint(equalTo: view.topAnchor),
            loadingView.bottomAnchor.constraint(equalTo: navBar.topAnchor),

            loadingLabel.centerXAnchor.constraint(equalTo: loadingView.centerXAnchor),
            loadingLabel.centerYAnchor.constraint(equalTo: loadingView.centerYAnchor),
        ])

        // Progress observer
        webView.addObserver(self, forKeyPath: "estimatedProgress", options: .new, context: nil)

        // Network monitoring
        setupNetworkMonitoring()
    }

    private func setupNavButtons() {
        for pageInfo in navPages {
            let button = UIButton(type: .system)
            button.tag = navButtons.count
            button.setTitle("\(pageInfo.icon)\n\(pageInfo.title)", for: .normal)
            button.titleLabel?.font = UIFont.systemFont(ofSize: 11, weight: .medium)
            button.titleLabel?.numberOfLines = 2
            button.titleLabel?.textAlignment = .center
            button.setTitleColor(UIColor.darkGray, for: .normal)
            button.setTitleColor(UIColor(red: 0.0, green: 0.353, blue: 0.310, alpha: 1.0), for: .selected)
            button.addTarget(self, action: #selector(navButtonTapped(_:)), for: .touchUpInside)
            button.translatesAutoresizingMaskIntoConstraints = false
            navBar.addSubview(button)
            navButtons.append(button)
        }

        // Layout nav buttons
        let stackView = UIStackView(arrangedSubviews: navButtons)
        stackView.axis = .horizontal
        stackView.distribution = .fillEqually
        stackView.translatesAutoresizingMaskIntoConstraints = false
        navBar.addSubview(stackView)

        NSLayoutConstraint.activate([
            stackView.leadingAnchor.constraint(equalTo: navBar.leadingAnchor, constant: 8),
            stackView.trailingAnchor.constraint(equalTo: navBar.trailingAnchor, constant: -8),
            stackView.topAnchor.constraint(equalTo: navBar.topAnchor, constant: 4),
            stackView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -4),
        ])
    }

    @objc private func navButtonTapped(_ sender: UIButton) {
        let page = navPages[sender.tag].page
        navigateTo(page)
    }

    private func setupOfflineView() {
        offlineView = UIView()
        offlineView.backgroundColor = UIColor.systemBackground
        offlineView.translatesAutoresizingMaskIntoConstraints = false
        offlineView.isHidden = true
        view.addSubview(offlineView)

        let titleLabel = UILabel()
        titleLabel.text = "لا يوجد اتصال بالإنترنت"
        titleLabel.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        offlineView.addSubview(titleLabel)

        let subtitleLabel = UILabel()
        subtitleLabel.text = "تحقق من اتصالك بالإنترنت وحاول مرة أخرى"
        subtitleLabel.font = UIFont.systemFont(ofSize: 16)
        subtitleLabel.textColor = UIColor.gray
        subtitleLabel.textAlignment = .center
        subtitleLabel.numberOfLines = 0
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        offlineView.addSubview(subtitleLabel)

        let retryButton = UIButton(type: .system)
        retryButton.setTitle("إعادة المحاولة", for: .normal)
        retryButton.titleLabel?.font = UIFont.systemFont(ofSize: 18, weight: .bold)
        retryButton.setTitleColor(.white, for: .normal)
        retryButton.backgroundColor = UIColor(red: 0.0, green: 0.353, blue: 0.310, alpha: 1.0)
        retryButton.layer.cornerRadius = 12
        retryButton.addTarget(self, action: #selector(retryTapped), for: .touchUpInside)
        retryButton.translatesAutoresizingMaskIntoConstraints = false
        offlineView.addSubview(retryButton)

        NSLayoutConstraint.activate([
            offlineView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            offlineView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            offlineView.topAnchor.constraint(equalTo: view.topAnchor),
            offlineView.bottomAnchor.constraint(equalTo: navBar.topAnchor),

            titleLabel.centerXAnchor.constraint(equalTo: offlineView.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: offlineView.centerYAnchor, constant: -40),

            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8),
            subtitleLabel.centerXAnchor.constraint(equalTo: offlineView.centerXAnchor),

            retryButton.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 24),
            retryButton.centerXAnchor.constraint(equalTo: offlineView.centerXAnchor),
            retryButton.widthAnchor.constraint(equalToConstant: 200),
            retryButton.heightAnchor.constraint(equalToConstant: 50),
        ])
    }

    @objc private func retryTapped() {
        offlineView.isHidden = true
        loadPage(currentPage())
    }

    private func setupNetworkMonitoring() {
        // Basic network check
    }

    // MARK: - Navigation
    private func loadPage(_ page: String) {
        let url = URL(string: "https://tager-new.vercel.app/?tager_app=ios&app_version=1.0.0#\(page)")!
        var request = URLRequest(url: url)
        request.cachePolicy = .reloadRevalidatingCacheData
        webView.load(request)
        loadingView.isHidden = false
        updateNavSelection(page)
    }

    private func navigateTo(_ page: String) {
        let js = "if(typeof window.go==='function'){window.go('\(page)');}else{location.hash='\(page)';}"
        webView.evaluateJavaScript(js) { [weak self] _, _ in
            self?.updateNavSelection(page)
        }
    }

    private func currentPage() -> String {
        guard let url = webView.url, let fragment = url.fragment else { return "home" }
        return fragment.isEmpty ? "home" : fragment
    }

    private func updateNavSelection(_ page: String) {
        for (i, pageInfo) in navPages.enumerated() {
            navButtons[i].isSelected = pageInfo.page == page
        }
    }

    // MARK: - WKNavigationDelegate
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        loadingView.isHidden = false
        progressBar.isHidden = false
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        loadingView.isHidden = true
        progressBar.isHidden = true
        injectAppPresentation()
        updateNavSelection(currentPage())
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        loadingView.isHidden = true
        progressBar.isHidden = true
        if let urlError = error as? URLError, urlError.code == .notConnectedToInternet {
            offlineView.isHidden = false
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url {
            let scheme = url.scheme ?? ""
            if scheme == "http" || scheme == "https" {
                if url.host == "tager-new.vercel.app" || url.host?.hasSuffix(".tager-new.vercel.app") == true {
                    decisionHandler(.allow)
                    return
                }
                // External link - open in Safari
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                    decisionHandler(.cancel)
                    return
                }
            } else if scheme == "tel" || scheme == "mailto" || scheme == "whatsapp" {
                if UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url)
                    decisionHandler(.cancel)
                    return
                }
            }
        }
        decisionHandler(.allow)
    }

    // MARK: - Image Optimization Injection
    private func injectAppPresentation() {
        let script = """
        (function(){
            document.documentElement.classList.add('tager-native-ios');
            document.body.classList.add('tager-native-ios');

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

    // MARK: - Progress Observer
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey: Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "estimatedProgress" {
            DispatchQueue.main.async {
                self.progressBar.progress = Float(self.webView.estimatedProgress)
                if self.webView.estimatedProgress >= 1.0 {
                    self.progressBar.isHidden = true
                }
            }
        }
    }
}
