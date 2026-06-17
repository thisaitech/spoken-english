package com.example.masterenglishfluency.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DataRepository {
    fun getData(): Flow<List<String>>
}

class DefaultDataRepository : DataRepository {
    override fun getData(): Flow<List<String>> = flow {
        emit(listOf("Android"))
    }
}
