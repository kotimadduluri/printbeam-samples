package com.freshcart.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberManualSaveAction(vm: SettingsViewModel): () -> Unit = { vm.commitManual() }
