package com.matrix.multigpt.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.matrix.multigpt.R
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun BedrockCredentialsField(
    value: String,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse existing JSON or use defaults
    val credentials = remember(value) {
        try {
            if (value.isNotBlank()) {
                Json.decodeFromString<BedrockCredentials>(value)
            } else {
                BedrockCredentials()
            }
        } catch (e: Exception) {
            BedrockCredentials()
        }
    }

    var accessKeyId by remember(value) { mutableStateOf(credentials.accessKeyId) }
    var secretAccessKey by remember(value) { mutableStateOf(credentials.secretAccessKey) }
    var region by remember(value) { mutableStateOf(credentials.region) }
    var sessionToken by remember(value) { mutableStateOf(credentials.sessionToken ?: "") }

    // Update parent when any field changes
    fun updateCredentials() {
        val newCredentials = BedrockCredentials(
            accessKeyId = accessKeyId,
            secretAccessKey = secretAccessKey,
            region = region,
            sessionToken = sessionToken.ifBlank { null }
        )
        val json = Json.encodeToString(newCredentials)
        onValueChange(json)
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.bedrock_credentials_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = accessKeyId,
            onValueChange = { 
                accessKeyId = it
                updateCredentials()
            },
            label = { Text(stringResource(R.string.aws_access_key_id)) },
            placeholder = { Text("AKIAIOSFODNN7EXAMPLE") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = secretAccessKey,
            onValueChange = { 
                secretAccessKey = it
                updateCredentials()
            },
            label = { Text(stringResource(R.string.aws_secret_access_key)) },
            placeholder = { Text("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = region,
            onValueChange = { 
                region = it
                updateCredentials()
            },
            label = { Text(stringResource(R.string.aws_region)) },
            placeholder = { Text("us-east-1") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            supportingText = {
                Text(
                    text = stringResource(R.string.aws_region_help),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = sessionToken,
            onValueChange = { 
                sessionToken = it
                updateCredentials()
            },
            label = { Text(stringResource(R.string.aws_session_token)) },
            placeholder = { Text(stringResource(R.string.aws_session_token_optional)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            supportingText = { 
                Text(
                    text = stringResource(R.string.aws_session_token_help),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

        if (accessKeyId.isNotBlank() || secretAccessKey.isNotBlank() || region != "us-east-1" || sessionToken.isNotBlank()) {
            TextButton(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    accessKeyId = ""
                    secretAccessKey = ""
                    region = "us-east-1"
                    sessionToken = ""
                    onClearClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.clear_token))
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class BedrockCredentials(
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val region: String = "us-east-1",
    val sessionToken: String? = null
)
