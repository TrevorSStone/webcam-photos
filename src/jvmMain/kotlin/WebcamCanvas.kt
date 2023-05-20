import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.github.sarxos.webcam.*

@Composable
fun WebcamCanvas(webcam: Webcam) {
  var image: ImageBitmap? by remember(webcam.name) { mutableStateOf(null) }
  DisposableEffect(webcam.name) {
    var active = true
    val listener =
      object : WebcamListener {
        override fun webcamOpen(we: WebcamEvent?) {}

        override fun webcamClosed(we: WebcamEvent?) {}

        override fun webcamDisposed(we: WebcamEvent?) {}

        override fun webcamImageObtained(we: WebcamEvent?) {
          if (active) {
            image = we?.image?.toComposeImageBitmap()
          }
        }
      }

    webcam.viewSize = webcam.viewSizes[1]
    webcam.addWebcamListener(listener)
    webcam.open(true)

    onDispose {
      active = false
      webcam.webcamListeners.forEach { webcam.removeWebcamListener(it) }

      webcam.removeWebcamListener(listener)

      webcam.close()
    }
  }
  image?.let { image ->
    Image(
      bitmap = image,
      contentDescription = "",
      contentScale = ContentScale.Fit,
      modifier =
        Modifier.fillMaxSize(
          //          with(LocalDensity.current) { image.width.toDp() },
          //          with(LocalDensity.current) { image.height.toDp() },
          )
    )
  }
}
