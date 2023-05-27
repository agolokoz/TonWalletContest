package org.ton.wallet.screen.pass.base

data class PassCodeScreenState(
    val title: String?,
    val subtitle: String,
    val optionsText: String?,
    val passCodeLength: Int,
    val filledDotsCount: Int,
    val isLoading: Boolean
)