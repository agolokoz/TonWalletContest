package org.ton.wallet.domain

import org.ton.wallet.data.model.AddressType
import org.ton.wallet.data.repo.AccountsRepository
import org.ton.wallet.data.ton.TonApiException

interface GetAddressTypeUseCase {

    suspend fun getAddressType(input: String): AddressType?

    fun guessAddressType(input: String): AddressType?
}

class GetAddressTypeUseCaseImpl(
    private val accountsRepository: AccountsRepository
) : GetAddressTypeUseCase {

    private val dnsRegex = Regex("^([a-zA-Z\\d]+(-[a-zA-Z\\d]+)*\\.)+[a-z]{2,}\$")
    private val rawAddressRegex = Regex("^-?\\d+:[\\da-fA-F]{64}\$")
    private val ufAddressRegex = Regex("^[\\w/+_\\-]{48}\$")

    override suspend fun getAddressType(input: String): AddressType? {
        return if (ufAddressRegex.matches(input)) {
            val isValidAddress = try {
                accountsRepository.isValidUfAddress(input)
            } catch (e: TonApiException) {
                false
            }
            if (isValidAddress) {
                AddressType.UserFriendlyAddress(input)
            } else {
                null
            }
        } else if (rawAddressRegex.matches(input)) {
            val ufAddress = try {
                accountsRepository.getUfAddress(input)
            } catch (e: Exception) {
                null
            }
            if (ufAddress == null) {
                null
            } else {
                AddressType.RawAddress(input, ufAddress)
            }
        } else if (dnsRegex.matches(input)) {
            val accountAddress = try {
                accountsRepository.resolveDnsName(input)
            } catch (e: TonApiException) {
                null
            }
            if (accountAddress == null) {
                null
            } else {
                AddressType.DnsAddress(input, accountAddress)
            }
        } else {
            null
        }
    }

    override fun guessAddressType(input: String): AddressType? {
        return when {
            ufAddressRegex.matches(input) -> AddressType.UserFriendlyAddress(input)
            rawAddressRegex.matches(input) -> AddressType.RawAddress(input, null)
            dnsRegex.matches(input) -> AddressType.DnsAddress(input, null)
            else -> null
        }
    }
}