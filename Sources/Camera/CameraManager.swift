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
    private var wantsSessionRunning = false // Accessed only on sessionQueue.
    private var pendingCaptureAspectRatio: CGFloat = 1.0
    private var videoDeviceInput: AVCaptureDeviceInput?
    private let photoOutput = AVCapturePhotoOutput()
    let videoDataOutput = AVCaptureVideoDataOutput()

    private let frameQueue = DispatchQueue(label: "camera.frame.queue", qos: .userInitiated)

    var frameDelegate: AVCaptureVideoDataOutputSampleBufferDelegate? {
        didSet {
            let delegate = frameDelegate
            sessionQueue.async { [weak self] in
                guard let self = self else { return }
                if let delegate {
                    self.videoDataOutput.setSampleBufferDelegate(
                        delegate,
                        queue: self.frameQueue
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

    // MARK: - 启停会话
    func startSession() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.wantsSessionRunning = true
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized:
                self.configureAndRunSession()
            case .notDetermined:
                AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                    self?.sessionQueue.async { [weak self] in
                        guard let self, self.wantsSessionRunning else { return }
                        if granted {
                            self.configureAndRunSession()
                        } else {
                            self.publishCameraError(.denied)
                        }
                    }
                }
            default:
                self.publishCameraError(.denied)
            }
        }
    }

    func stopSession() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.wantsSessionRunning = false
            if self.session.isRunning { self.session.stopRunning() }
            DispatchQueue.main.async { self.isSessionRunning = false }
        }
    }

    private func configureAndRunSession() {
        guard wantsSessionRunning else { return }
        if videoDeviceInput == nil {
            configureSession()
        }
        guard videoDeviceInput != nil, !session.isRunning else { return }
        session.startRunning()
        let isRunning = session.isRunning
        DispatchQueue.main.async { self.isSessionRunning = isRunning }
    }

    private func configureSession() {
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .photo

        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            publishCameraError(.cameraUnavailable)
            return
        }

        do {
            let input = try AVCaptureDeviceInput(device: camera)
            guard session.canAddInput(input) else {
                publishCameraError(.cannotAddInput)
                return
            }
            session.addInput(input)
            videoDeviceInput = input
        } catch {
            publishCameraError(.cannotAddInput)
            return
        }

        if session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
        }

        videoDataOutput.alwaysDiscardsLateVideoFrames = true
        if session.canAddOutput(videoDataOutput) {
            session.addOutput(videoDataOutput)
            videoDataOutput.connection(with: .video)?.videoOrientation = .portrait
        }
    }

    private func publishCameraError(_ error: CameraError) {
        DispatchQueue.main.async { self.error = error }
    }

    // MARK: - 拍照
    func capturePhoto(aspectRatio: CGFloat = 1.0) {
        pendingCaptureAspectRatio = max(0.1, aspectRatio)
        let settings = AVCapturePhotoSettings()
        photoOutput.capturePhoto(with: settings, delegate: self)
    }
}

// MARK: - 拍照代理
extension CameraManager: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }

        let targetAspectRatio = pendingCaptureAspectRatio
        let cropped = Self.cropToAspect(image, aspectRatio: targetAspectRatio)
        DispatchQueue.main.async {
            self.capturedPhoto = cropped
        }
    }

    /// 将拍摄的照片裁剪为与当前有效取景框一致的画幅比例。
    static func cropToAspect(_ image: UIImage, aspectRatio: CGFloat) -> UIImage {
        let imgSize = image.size
        guard imgSize.width > 0, imgSize.height > 0, aspectRatio > 0 else { return image }

        let imageAspectRatio = imgSize.width / imgSize.height
        let cropSize: CGSize
        if imageAspectRatio > aspectRatio {
            let cropHeight = imgSize.height
            cropSize = CGSize(width: cropHeight * aspectRatio, height: cropHeight)
        } else {
            let cropWidth = imgSize.width
            cropSize = CGSize(width: cropWidth, height: cropWidth / aspectRatio)
        }

        let cropOrigin = CGPoint(
            x: (imgSize.width - cropSize.width) / 2,
            y: (imgSize.height - cropSize.height) / 2
        )
        let renderer = UIGraphicsImageRenderer(size: cropSize)
        return renderer.image { _ in
            image.draw(at: CGPoint(x: -cropOrigin.x, y: -cropOrigin.y))
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
