package co.rainx.loop.broadcaster.di

import android.content.Context
import android.net.nsd.NsdManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BroadcasterModule {

    @Provides
    fun provideNsdManager(@ApplicationContext context: Context): NsdManager {
        return context.getSystemService(NsdManager::class.java)
    }
}
