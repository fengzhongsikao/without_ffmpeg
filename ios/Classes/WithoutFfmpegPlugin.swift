import Flutter
import UIKit

public class WithoutFfmpegPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "without_ffmpeg", binaryMessenger: registrar.messenger())
    let instance = WithoutFfmpegPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "test1":
      result("flutter 真好啊")
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
