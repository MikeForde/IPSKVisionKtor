package com.example

import dev.kilua.rpc.annotations.RpcService
import kotlinx.serialization.Serializable

@Serializable
enum class Sort {
    FN, LN, E, F
}

@RpcService
interface IIPSService {
    suspend fun getIPSList(): List<IPSModel>
}

