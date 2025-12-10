package com.swent.mapin.model.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutineDispatcherProvider {
  val io: CoroutineDispatcher
  val default: CoroutineDispatcher
  val main: CoroutineDispatcher
}
