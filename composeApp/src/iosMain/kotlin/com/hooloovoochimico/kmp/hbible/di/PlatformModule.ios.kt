package com.hooloovoochimico.kmp.hbible.di

import org.koin.core.module.Module
import org.koin.dsl.module

// Su iOS gli actual (db builder, settings) non richiedono dipendenze iniettate.
actual val platformModule: Module = module { }
