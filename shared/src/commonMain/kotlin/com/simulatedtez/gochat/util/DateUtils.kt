package com.simulatedtez.gochat.util

import kotlinx.datetime.Clock

fun nowAsISOString(): String = Clock.System.now().toString()
