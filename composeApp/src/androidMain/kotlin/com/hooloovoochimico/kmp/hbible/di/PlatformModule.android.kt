package com.hooloovoochimico.kmp.hbible.di

import android.content.Context
import com.hooloovoochimico.kmp.hbible.platform.AndroidAppContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // Context per eventuali definizioni che lo richiedono; gli actual platform
    // leggono direttamente AndroidAppContext, il Context non entra in commonMain.
    single<Context> { AndroidAppContext.appContext }
}
