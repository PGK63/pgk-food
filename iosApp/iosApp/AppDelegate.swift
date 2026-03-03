import BackgroundTasks
import Foundation
import Shared
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    private let taskIdentifier = "com.example.pgkfood.ios.background.keys.sync"
    private let repeatIntervalSeconds: TimeInterval = 6 * 60 * 60

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        registerBackgroundTasks()
        scheduleBackgroundRefresh()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        scheduleBackgroundRefresh()
    }

    private func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let appRefreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: true)
                return
            }
            self.handleAppRefresh(task: appRefreshTask)
        }
    }

    private func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: repeatIntervalSeconds)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // The OS may reject duplicate/too-frequent requests; ignore and reschedule later.
        }
    }

    private func handleAppRefresh(task: BGAppRefreshTask) {
        scheduleBackgroundRefresh()
        var isExpired = false
        task.expirationHandler = {
            isExpired = true
        }

        DispatchQueue.global(qos: .utility).async {
            if isExpired {
                task.setTaskCompleted(success: false)
                return
            }
            let needsRetry = BackgroundSyncBridge.shared.runOnceBlockingNeedsRetry()
            task.setTaskCompleted(success: !needsRetry)
        }
    }
}
