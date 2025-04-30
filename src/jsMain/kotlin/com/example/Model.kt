package com.example

import dev.kilua.rpc.getService
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.syncWithList
import kotlinx.coroutines.launch

object Model {
    private val ipsService = getService<IIPSService>()

    //suspend fun getIPSList(): List<IPSModel> = ipsService.getIPSList()
    // new observable list for IPS
    val ipsRecords: ObservableList<IPSModel> = observableListOf()
    val selectedIps: ObservableValue<IPSModel?> = ObservableValue(null)

    suspend fun getIPSList() {
        val list = ipsService.getIPSList()
        //console.log("IPS list: $list")
        ipsRecords.syncWithList(list)
        selectedIps.value = null
    }

}