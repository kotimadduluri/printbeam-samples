package com.labelmate.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberManualSaveAction(vm: AppViewModel): () -> Unit = { vm.commitManual() }
