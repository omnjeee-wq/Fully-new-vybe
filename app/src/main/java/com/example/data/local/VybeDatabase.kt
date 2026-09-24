package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongHistoryEntity::class,
        FavoriteSongEntity::class,
        AppPreferenceEntity::class,
        SearchHistoryEntity::class,
        FavoriteAlbumEntity::class,
        DownloadedSongEntity::class,
        UserPlaylistEntity::class,
        UserPlaylistSongEntity::class,
        LikedPlaylistEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VybeDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: VybeDatabase? = null

        fun getInstance(context: Context): VybeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VybeDatabase::class.java,
                    "vybe_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
