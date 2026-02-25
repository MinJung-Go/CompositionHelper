import AVFoundation
import UIKit
import SwiftUI

// MARK: - 相机管理器
class CameraManager: NSObject, ObservableObject {
    @Published var isSessionRunning = false
    @Published var capturedPhoto: UIImage?
    @Published var error: CameraError?

    let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "camera.session.queue")
    private var videoDeviceInput: AVCaptureDeviceInput?
    private let photoOutput = AVCapturePhotoOutput()
    let videoDataOutput = AVCaptureVideoDataOutput()

    var frameDelegate: AVCaptureVideoDataOutputSampleBufferDelegate? {
        didSet {
            sessionQueue.async { [weak self] in
                guard let self = self else { return }
                if let delegate = self.frameDelegate {
                    self.videoDataOutput.setSampleBufferDelegate(
                        delegate,
                        queue: DispatchQueue(label: "camera.frame.queue", qos: .userInitiated)
                    )
                } else {
                    self.videoDataOutput.setSampleBufferDelegate(nil, queue: nil)
                }
            }
        }
    }

    enum CameraError: Error, LocalizedError {
        case cameraUnavailable
        case cannotAddInput
        case cannotAddOutput
        case denied

        var errorDescription: String? {
            switch self {
            case .cameraUnavailable: return "相机不可用"
            case .cannotAddInput: return "无法添加相机输入"
            case .cannotAddOutput: return "无法添加相机输出"
            case .denied: return "相机权限被拒绝"
            }
        }
    }

    // MARK: - 权限检查
    func checkPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted {
                    self?.configureSession()
                } else {
                    DispatchQueue.main.async {
                        self?.error = .denied
                    }
                }
            }
        default:
            DispatchQueue.main.async {
                self.error = .denied
            }
        }
    }

    // MARK: - 配置会话
    private func configureSession() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }

            self.session.beginConfiguration()
            self.session.sessionPreset = .photo

            // 添加视频输入
            guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                DispatchQueue.main.async { self.error = .cameraUnavailable }
                self.session.commitConfiguration()
                return
            }

            do {
                let input = try AVCaptureDeviceInput(device: camera)
                if self.session.canAddInput(input) {
                    self.session.addInput(input)
                    self.videoDeviceInput = input
                } else {
                    DispatchQueue.main.async { self.error = .cannotAddInput }
                    self.session.commitConfiguration()
                    return
                }
            } catch {
                DispatchQueue.main.async { self.error = .cannotAddInput }
                self.session.commitConfiguration()
                return
            }

            // 添加拍照输出
            if self.session.canAddOutput(self.photoOutput) {
                self.session.addOutput(self.photoOutput)
            }

            // 添加视频帧输出（用于实时分析）
            self.videoDataOutput.alwaysDiscardsLateVideoFrames = true
            if self.session.canAddOutput(self.videoDataOutput) {
                self.session.addOutput(self.videoDataOutput)
                if let connection = self.videoDataOutput.connection(with: .video) {
                    connection.videoOrientation = .portrait
                }
            }

            self.session.commitConfiguration()
            self.startSession()
        }
    }

    // MARK: - 启停会话
    func startSession() {
        sessionQueue.async { [weak self] in
            guard let self = self, !self.session.isRunning else { return }
            self.session.startRunning()
            DispatchQueue.main.async {
                self.isSessionRunning = self.session.isRunning
            }
        }
    }

    func stopSession() {
        sessionQueue.async { [weak self] in
            guard let self = self, self.session.isRunning else { return }
            self.session.stopRunning()
            DispatchQueue.main.async {
                self.isSessionRunning = false
            }
        }
    }

    // MARK: - 拍照
    func capturePhoto() {
        let settings = AVCapturePhotoSettings()
        photoOutput.capturePhoto(with: settings, delegate: self)
    }
}

// MARK: - 拍照代理
extension CameraManager: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }

        DispatchQueue.main.async {
            self.capturedPhoto = image
        }
    }
}

// MARK: - 相机预览层（UIViewRepresentable）
struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> CameraPreviewUIView {
        let view = CameraPreviewUIView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: CameraPreviewUIView, context: Context) {}
}

class CameraPreviewUIView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }

    var previewLayer: AVCaptureVideoPreviewLayer {
        // swiftlint:disable:next force_cast
        layer as! AVCaptureVideoPreviewLayer
    }
}
