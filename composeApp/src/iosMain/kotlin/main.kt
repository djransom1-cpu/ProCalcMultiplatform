import androidx.compose.ui.window.ComposeUIViewController
import com.djran.constructioncalculator.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController { App() }
}
