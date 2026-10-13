package com.hybridfitnesscoach.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/** A normal full-screen Compose surface; only confirmations use a modal window. */
@Composable
internal fun FlowScreen(
    onDismissRequest:()->Unit,
    title:@Composable ()->Unit,
    text:@Composable ()->Unit,
    confirmButton:@Composable ()->Unit,
    dismissButton:@Composable ()->Unit = {}
) {
    BackHandler(onBack=onDismissRequest)
    Column(Modifier.fillMaxSize().zIndex(2f).background(MaterialTheme.colorScheme.background).safeDrawingPadding()
        .pointerInput(Unit){awaitPointerEventScope{while(true)awaitPointerEvent()}}) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            TextButton(onClick=onDismissRequest){Text("INDIETRO")}
            Box(Modifier.weight(1f).padding(12.dp)){title()}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal=16.dp)){text()}
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            confirmButton()
            dismissButton()
        }
    }
}
