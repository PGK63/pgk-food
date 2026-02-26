import SwiftUI
import UIKit
import Shared

struct SharedRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Platform_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
