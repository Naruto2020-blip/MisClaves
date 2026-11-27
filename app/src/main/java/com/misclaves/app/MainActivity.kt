package com.misclaves.app

import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultItem(val id: Long, val title: String, val username: String, val password: String, val website: String, val notes: String)

class VaultStore(private val file: File) {
    private val alias = "misclaves_vault_key"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }
    fun load(): List<VaultItem> {
        if (!file.exists()) return emptyList()
        return try {
            val raw = file.readBytes()
            require(raw.isNotEmpty())
            val ivSize = raw[0].toInt() and 0xff
            require(ivSize in 12..16 && raw.size > 1 + ivSize)
            val iv = raw.copyOfRange(1, 1 + ivSize)
            val encrypted = raw.copyOfRange(1 + ivSize, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val arr = JSONArray(String(cipher.doFinal(encrypted), Charsets.UTF_8))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                VaultItem(o.getLong("id"), o.getString("title"), o.getString("username"),
                    o.getString("password"), o.optString("website"), o.optString("notes"))
            }
        } catch (_: Exception) { emptyList() }
    }
    fun save(items: List<VaultItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().put("id", item.id).put("title", item.title).put("username", item.username)
                .put("password", item.password).put("website", item.website).put("notes", item.notes))
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(arr.toString().toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val out = byteArrayOf(iv.size.toByte()) + iv + encrypted
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeBytes(out)
        if (!temp.renameTo(file)) {
            file.writeBytes(out)
            temp.delete()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = VaultStore(File(filesDir, "vault.enc"))
        setContent { MisClavesApp(store) }
    }
}

@Composable
fun MisClavesApp(store: VaultStore) {
    var dark by remember { mutableStateOf(false) }
    val colors = if (dark) darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFFB7A8FF), surface = androidx.compose.ui.graphics.Color(0xFF191B2B), background = androidx.compose.ui.graphics.Color(0xFF111321))
        else lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF5546B8), surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF), background = androidx.compose.ui.graphics.Color(0xFFF5F6FC))
    MaterialTheme(colorScheme = colors) {
        var entries by remember { mutableStateOf(store.load()) }
        var query by remember { mutableStateOf("") }
        var showEditor by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<VaultItem?>(null) }
        val clipboard = LocalClipboardManager.current
        Scaffold(
            topBar = { TopAppBarCompat(dark = dark, onTheme = { dark = !dark }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir cuenta")
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Text("Tu bóveda privada", style = MaterialTheme.typography.headlineSmall)
                Text("${entries.size} cuentas guardadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar aplicaciones o sitios") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(12.dp))
                if (entries.isEmpty()) {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tu bóveda está vacía", style = MaterialTheme.typography.titleMedium)
                            Text("Pulsa + para guardar tu primera cuenta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(entries.filter { it.title.contains(query, true) || it.username.contains(query, true) || it.website.contains(query, true) }, key = { it.id }) { item ->
                            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                                            Text(item.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { editing = item; showEditor = true }) { Icon(Icons.Default.Edit, "Editar") }
                                        IconButton(onClick = { entries = entries.filterNot { it.id == item.id }; store.save(entries) }) { Icon(Icons.Default.Delete, "Eliminar") }
                                    }
                                    if (item.website.isNotBlank()) Text(item.website, color = MaterialTheme.colorScheme.primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(item.username)) }) {
                                            Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(5.dp)); Text("Copiar usuario")
                                        }
                                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(item.password)) }) {
                                            Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(5.dp)); Text("Copiar clave")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showEditor) {
            ItemEditor(initial = editing, onDismiss = { showEditor = false }, onSave = { item ->
                entries = if (editing == null) entries + item else entries.map { if (it.id == item.id) item else it }
                store.save(entries); showEditor = false
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarCompat(dark: Boolean, onTheme: () -> Unit) {
    TopAppBar(title = { Text("Mis Claves") }, actions = {
        TextButton(onClick = onTheme) { Text(if (dark) "Claro" else "Oscuro") }
    })
}

@Composable
fun ItemEditor(initial: VaultItem?, onDismiss: () -> Unit, onSave: (VaultItem) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var website by remember { mutableStateOf(initial?.website ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var reveal by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva cuenta" else "Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nombre (ej. Correo)") }, singleLine = true)
                OutlinedTextField(username, { username = it }, label = { Text("Usuario o correo") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { reveal = !reveal }) { Icon(if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                OutlinedTextField(website, { website = it }, label = { Text("Página web o aplicación") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && password.isNotBlank(), onClick = {
            onSave(VaultItem(initial?.id ?: System.currentTimeMillis(), title.trim(), username, password, website, notes))
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
