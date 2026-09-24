package com.example

import android.app.Application
import com.example.data.local.VybeDatabase
import com.example.data.remote.RetrofitClient
import com.example.data.repository.MusicRepositoryImpl
import com.example.domain.recommendation.RecommendationEngine
import com.example.domain.repository.MusicRepository
import com.example.player.PlayerManager

class VybeApp : Application() {

    lateinit var database: VybeDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var recommendationEngine: RecommendationEngine
        private set

    lateinit var playerManager: PlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = VybeDatabase.getInstance(this)
        repository = MusicRepositoryImpl(RetrofitClient.apiService, database.songDao())
        recommendationEngine = RecommendationEngine(repository, database.songDao())
        playerManager = PlayerManager.initialize(this, repository)
    }

    companion object {
        lateinit var instance: VybeApp
            private set
    }
}
