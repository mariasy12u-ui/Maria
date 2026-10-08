package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RdpMode
import com.example.data.model.RdpProfile
import com.example.data.model.RdpProtocol

@Composable
fun EditRdpProfileDialog(
    initialProfile: RdpProfile?,
    onSave: (RdpProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "My RDP Server") }
    var host by remember { mutableStateOf(initialProfile?.host ?: "192.168.1.100") }
    var portStr by remember { mutableStateOf(initialProfile?.port?.toString() ?: "8080") }
    var selectedMode by remember { mutableStateOf(initialProfile?.mode ?: RdpMode.PROXY_TUNNEL) }
    var selectedProtocol by remember { mutableStateOf(initialProfile?.protocol ?: RdpProtocol.HTTP) }
    var webRdpUrl by remember { mutableStateOf(initialProfile?.webRdpUrl ?: "") }
    var username by remember { mutableStateOf(initialProfile?.username ?: "") }
    var password by remember { mutableStateOf(initialProfile?.password ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile == null) "Add RDP Server" else "Edit RDP Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Connection Mode", fontSize = 13.sp)
                RdpMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .padding(vertical = 3.dp)
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(mode.displayName, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedMode == RdpMode.PROXY_TUNNEL) {
                    Text("Proxy Protocol", fontSize = 13.sp)
                    RdpProtocol.values().filter { it != RdpProtocol.WEB_RDP }.forEach { proto ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProtocol = proto }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedProtocol == proto,
                                onClick = { selectedProtocol = proto }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(proto.displayName, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("RDP IP / Host Address") },
                        placeholder = { Text("e.g. 192.168.1.100 or vps.net") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = webRdpUrl,
                        onValueChange = { webRdpUrl = it },
                        label = { Text("Web RDP Portal URL") },
                        placeholder = { Text("http://192.168.1.100:8080/guacamole") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portStr.toIntOrNull() ?: selectedProtocol.defaultPort
                    val profile = (initialProfile ?: RdpProfile(
                        name = name,
                        host = host,
                        port = port
                    )).copy(
                        name = name.ifBlank { "RDP Server" },
                        mode = selectedMode,
                        host = host,
                        port = port,
                        protocol = if (selectedMode == RdpMode.WEB_DESKTOP) RdpProtocol.WEB_RDP else selectedProtocol,
                        webRdpUrl = webRdpUrl,
                        username = username,
                        password = password,
                        isEnabled = true
                    )
                    onSave(profile)
                    onDismiss()
                },
                enabled = host.isNotBlank() || (selectedMode == RdpMode.WEB_DESKTOP && webRdpUrl.isNotBlank())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
