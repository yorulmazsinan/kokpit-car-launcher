package io.github.yorulmazsinan.kokpit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.yorulmazsinan.kokpit.system.ActivityExplorerScreen
import io.github.yorulmazsinan.kokpit.system.DiagnosticsScreen
import io.github.yorulmazsinan.kokpit.system.PackageActivitiesScreen
import io.github.yorulmazsinan.kokpit.system.SystemToolsScreen
import io.github.yorulmazsinan.kokpit.ui.theme.KokpitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KokpitTheme {
                KokpitNavHost()
            }
        }
    }
}

object Routes {
    const val SYSTEM_TOOLS = "system-tools"
    const val EXPLORER = "explorer"
    const val PACKAGE_ACTIVITIES = "explorer/{pkg}"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
private fun KokpitNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.SYSTEM_TOOLS) {
        composable(Routes.SYSTEM_TOOLS) {
            SystemToolsScreen(
                onBack = null,
                onOpenExplorer = { nav.navigate(Routes.EXPLORER) },
                onOpenDiagnostics = { nav.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(Routes.EXPLORER) {
            ActivityExplorerScreen(onBack = { nav.popBackStack() }, onPickPackage = { nav.navigate("explorer/$it") })
        }
        composable(Routes.PACKAGE_ACTIVITIES) { entry ->
            PackageActivitiesScreen(entry.arguments?.getString("pkg").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { nav.popBackStack() }) }
    }
}
