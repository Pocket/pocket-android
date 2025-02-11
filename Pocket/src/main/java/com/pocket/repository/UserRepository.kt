package com.pocket.repository

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.LoginInfo
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun isLoggedIn(): Flow<Boolean>
    fun isPremiumUpgradeAvailable(): Flow<Boolean>
    fun hasPremiumDisplaySettings(): Flow<Boolean>
    fun getLoginInfoAsFlow(): Flow<LoginInfo>
}

@Singleton
class SyncEngineUserRepository @Inject constructor(
    private val pocket: Pocket
) : UserRepository {
    override fun isLoggedIn(): Flow<Boolean> {
        return getLoginInfoAsFlow()
            .map { it.isLoggedIn() }
            .distinctUntilChanged()
    }

    override fun isPremiumUpgradeAvailable(): Flow<Boolean> {
        return getLoginInfoAsFlow()
            .map { it.isPremiumUpgradeAvailable() }
            .distinctUntilChanged()
    }

    override fun hasPremiumDisplaySettings(): Flow<Boolean> {
        return getLoginInfoAsFlow()
            .map { it.hasPremiumDisplaySettings() }
            .distinctUntilChanged()
    }

    override fun getLoginInfoAsFlow(): Flow<LoginInfo> =
        pocket.bindLocalAsFlow(
            pocket.spec().things().loginInfo().build()
        )
}

fun LoginInfo.isLoggedIn() = access_token != null
fun LoginInfo.hasPremium() = account?.premium_status ?: false
fun LoginInfo.hasPremiumDisplaySettings(): Boolean {
    // There is no [PremiumFeature] for this.
    // Let's still keep this separate method to encode the knowledge or in case we add it in the future.
    return hasPremium()
}
fun LoginInfo.isFree() = !hasPremium()
fun LoginInfo.isPremiumUpgradeAvailable() = isLoggedIn() && isFree()
