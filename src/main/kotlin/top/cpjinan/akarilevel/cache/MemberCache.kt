package top.cpjinan.akarilevel.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import taboolib.platform.util.bukkitPlugin
import top.cpjinan.akarilevel.config.SettingsConfig
import top.cpjinan.akarilevel.database.Database
import top.cpjinan.akarilevel.entity.MemberData
import top.cpjinan.akarilevel.entity.MemberLevelData
import java.util.concurrent.ConcurrentHashMap

/**
 * AkariLevel
 * top.cpjinan.akarilevel.cache
 *
 * 成员数据缓存。
 *
 * @author 季楠, QwQ-dev
 * @since 2025/8/12 04:43
 */
object MemberCache {
    val gson = Gson()
    private val dirtyMembers = ConcurrentHashMap.newKeySet<String>()
    private var autoSaveTask: BukkitTask? = null

    val memberCache = Caffeine.newBuilder()
        .build<String, MemberData> { key ->
            try {
                Database.instance.get(Database.instance.memberTable, key)?.takeUnless { it.isBlank() }
                    ?.let { json ->
                        try {
                            gson.fromJson(json, MemberData::class.java)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            MemberData()
                        }
                    } ?: MemberData()
            } catch (e: Exception) {
                e.printStackTrace()
                MemberData()
            }
        }

    fun startAutoSave() {
        if (autoSaveTask != null) return
        val period = (SettingsConfig.autoSaveInterval * 20L).coerceAtLeast(20L)
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            bukkitPlugin,
            Runnable { flushDirtyMembers() },
            period,
            period
        )
    }

    fun restartAutoSave() {
        stopAutoSave()
        startAutoSave()
    }

    fun stopAutoSave() {
        autoSaveTask?.cancel()
        autoSaveTask = null
    }

    fun saveLater(member: String) {
        dirtyMembers.add(member)
    }

    fun saveNow(member: String): Boolean {
        dirtyMembers.remove(member)
        return saveMember(member)
    }

    fun flushDirtyMembers() {
        var count = 0
        dirtyMembers.toList().forEach {
            if (dirtyMembers.remove(it)) {
                saveMember(it)
                count++
            }
        }
        bukkitPlugin.logger.info("[自动保存] 本轮共成功保存 $count 位玩家数据")
    }

    private fun saveMember(member: String): Boolean {
        try {
            val data = memberCache.getIfPresent(member) ?: return true
            Database.instance.set(Database.instance.memberTable, member, gson.toJson(data.snapshot()))
            return true
        } catch (e: Exception) {
            dirtyMembers.add(member)
            e.printStackTrace()
            return false
        }
    }

    private fun MemberData.snapshot(): MemberData {
        return MemberData(
            ConcurrentHashMap(levelGroups.mapValues { (_, data) ->
                MemberLevelData(data.level, data.exp)
            })
        )
    }
}
