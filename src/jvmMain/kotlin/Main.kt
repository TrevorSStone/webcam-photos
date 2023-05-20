import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamDiscoveryEvent
import com.github.sarxos.webcam.WebcamDiscoveryListener
import com.github.sarxos.webcam.WebcamUtils
import com.github.sarxos.webcam.util.ImageUtils
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

fun cameraFlow() =
  callbackFlow<WebcamDiscoveryEvent> {
      val discoveryService = Webcam.getDiscoveryService()

      val discoveryListener =
        object : WebcamDiscoveryListener {
          override fun webcamFound(event: WebcamDiscoveryEvent) {
            trySendBlocking(event)
          }

          override fun webcamGone(event: WebcamDiscoveryEvent) {
            trySendBlocking(event)
          }
        }
      Webcam.addDiscoveryListener(discoveryListener)
      discoveryService.start()

      awaitClose {
        discoveryService.stop()
        Webcam.removeDiscoveryListener(discoveryListener)
      }
    }
    .scan(Webcam.getWebcams().toList()) { acc, value ->
      when (value.type) {
        WebcamDiscoveryEvent.ADDED -> {
          acc + value.webcam
        }
        WebcamDiscoveryEvent.REMOVED -> {
          acc - value.webcam
        }
        else -> TODO()
      }
    }

@Composable
@Preview
fun App() {
  var webcams: List<Webcam> by remember { mutableStateOf(Webcam.getWebcams(Long.MAX_VALUE)) }
  var selectedWebcam: Webcam? by remember { mutableStateOf(null) }
  val coroutine = rememberCoroutineScope()
  var path: File? by remember { mutableStateOf(null) }

  LaunchedEffect(Unit) {
    cameraFlow()
      .onStart { println("JO") }
      .collect {
        println("Yo ${it.joinToString { it.name }}")
        webcams = it
      }
  }

  MaterialTheme {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
      Row(Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(.5f), verticalAlignment = Alignment.CenterVertically) {
//          Button(
//            onClick = {
//              coroutine.launch(Dispatchers.IO) { webcams = Webcam.getWebcams(Long.MAX_VALUE) }
//            }
//          ) {
//            Text("Refresh")
//          }

          TextButton(
            modifier = Modifier.width(200.dp),
            onClick = {
              coroutine.launch {
                delay(200)
                expanded = true
              }
            }
          ) {
            selectedWebcam?.let { Text(it.name) } ?: run { Text("Select Camera") }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
              webcams.forEach { webcam ->
                val web = remember(webcam.name) { webcam }
                DropdownMenuItem(
                  onClick = {
                    selectedWebcam = web
                    expanded = false
                  }
                ) {
                  Text(web.name)
                }
              }
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End
        ) {
          var showDirPicker by remember { mutableStateOf(false) }

          if (showDirPicker) {
            FileDialog(
              onCloseRequest = { savepath ->
                savepath?.let { path = savepath }
                showDirPicker = false
                println("Result $path")
              }
            )
          }

          Text(
            text = path?.path ?: "Select a file",
            modifier = Modifier.fillMaxWidth(.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Button(
            modifier =
              Modifier.align(Alignment.CenterVertically).size(width = 100.dp, height = 50.dp),
            onClick = { showDirPicker = true }
          ) {
            Text("Location")
          }
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        selectedWebcam?.let { webcam -> WebcamCanvas(webcam) }
      }
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        selectedWebcam?.let { Text(it.viewSizes.map { it.size.toString() }.joinToString()) }
        Button(
          onClick = {
            selectedWebcam?.let { WebcamUtils.capture(it, path?.path, ImageUtils.FORMAT_PNG) }
          }
        ) {
          Text("Hello")
        }
      }
    }
  }
}

@Composable
private fun FileDialog(parent: Frame? = null, onCloseRequest: (result: File?) -> Unit) =
  AwtWindow(
    create = {
      object : FileDialog(parent, "Choose a file", SAVE) {
        override fun setVisible(value: Boolean) {
          super.setVisible(value)
          if (value) {
            onCloseRequest(files.firstOrNull())
          }
        }
      }
    },
    dispose = FileDialog::dispose
  )

fun main() = application {
  Webcam.setDriver(NativeDriver())
  Window(
    onCloseRequest = ::exitApplication,
    state =
      WindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = getPreferredWindowSize(1920, 1200)
      ),
  ) {
    App()
  }
}

private fun getPreferredWindowSize(desiredWidth: Int, desiredHeight: Int): DpSize {
  val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
  val preferredWidth: Int = (screenSize.width * 0.8f).toInt()
  val preferredHeight: Int = (screenSize.height * 0.8f).toInt()
  val width: Int = if (desiredWidth < preferredWidth) desiredWidth else preferredWidth
  val height: Int = if (desiredHeight < preferredHeight) desiredHeight else preferredHeight
  return DpSize(width.dp, height.dp)
}
