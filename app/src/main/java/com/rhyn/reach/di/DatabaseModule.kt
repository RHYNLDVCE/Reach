package com.rhyn.reach.di

import android.content.Context
import androidx.room.Room
import android.content.SharedPreferences
import com.rhyn.reach.data.local.Database
import com.rhyn.reach.data.local.dao.DeviceDao
import com.rhyn.reach.data.local.dao.MeshRelayDao
import com.rhyn.reach.data.local.dao.MessageDao
import com.rhyn.reach.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {


    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // "reach_auth" matches the file your ChatRepository uses
        return context.getSharedPreferences("reach_auth", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideReachDatabase(
        @ApplicationContext context: Context
    ): Database {
        return Room.databaseBuilder(
            context,
            Database::class.java,
            "reach_offline_db"
        )
            .fallbackToDestructiveMigration(false) // Useful during early development
            .build()
    }

    @Provides
    @Singleton
    fun provideDeviceDao(database: Database): DeviceDao {
        return database.deviceDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: Database): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideMeshRelayDao(database: Database): MeshRelayDao {
        return database.meshRelayDao()
    }

    @Provides
    fun provideUserDao(database: Database): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideMeshEdgeDao(database: Database): com.rhyn.reach.data.local.dao.MeshEdgeDao {
        return database.meshEdgeDao()
    }
}