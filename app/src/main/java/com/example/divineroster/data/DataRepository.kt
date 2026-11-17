package com.example.divineroster.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface for the data layer.
 */
interface DataRepository {
    val data: Flow<List<String>>
}
