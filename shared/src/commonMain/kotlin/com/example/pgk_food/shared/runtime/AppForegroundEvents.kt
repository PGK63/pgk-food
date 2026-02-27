package com.example.pgk_food.shared.runtime

import kotlinx.coroutines.flow.Flow

expect fun appForegroundEvents(): Flow<Unit>
