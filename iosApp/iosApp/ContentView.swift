import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        // MainViewController() è definito in composeApp/src/iosMain/…/MainViewController.kt
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
