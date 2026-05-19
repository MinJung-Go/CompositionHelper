package com.example.compositionhelper

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compositionhelper.camera.CameraCompositionScreen
import com.example.compositionhelper.ui.theme.CompositionHelperTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式：让内容延伸到系统栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CompositionHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionHelperNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompositionHelperNavigation() {
    val navController = rememberNavController()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val galleryPermissionState = rememberPermissionState(galleryPermission)
    val hasCameraPermission = cameraPermissionState.status == PermissionStatus.Granted
    val hasGalleryPermission = galleryPermissionState.status == PermissionStatus.Granted

    NavHost(navController = navController, startDestination = "camera") {
        // 实时相机模式（默认启动）
        composable("camera") {
            CameraCompositionScreen(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onOpenGallery = {
                    navController.navigate("gallery")
                }
            )
        }

        // 相册分析模式
        composable("gallery") {
            CompositionHelperApp(
                hasPermissions = hasGalleryPermission,
                onRequestPermissions = { galleryPermissionState.launchPermissionRequest() },
                onOpenCamera = { navController.navigate("camera") }
            )
        }
    }
}
