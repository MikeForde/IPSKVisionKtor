package com.example.serviceHelpers

import com.example.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper to upsert an IPSModel: if a record with the same packageUUID exists, merges into it;
 * otherwise adds a new record.
 */
fun upsertIPSRecord(model: IPSModel): IPSModel {
  // Check existence of a record with this packageUUID
  val exists = transaction {
    IPSModelDao.select { IPSModelDao.packageUUID eq model.packageUUID }.any()
  }
  return if (exists) {
    mergeIPSRecord(model)
  } else {
    addIPSRecord(model)
  }
}
