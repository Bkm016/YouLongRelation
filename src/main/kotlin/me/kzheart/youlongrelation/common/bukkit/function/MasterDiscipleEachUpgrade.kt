package me.kzheart.youlongrelation.common.bukkit.function

import com.sucy.skill.SkillAPI
import com.sucy.skill.api.enums.ExpSource
import me.kzheart.youlongrelation.api.YouLongRelationBukkitApi
import me.kzheart.youlongrelation.api.event.bukkit.*
import me.kzheart.youlongrelation.common.bukkit.conf.BukkitMasterDiscipleConfManager
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.platform.util.sendLang
import javax.script.ScriptEngineManager

/**
 * @author kzheart
 * @date 2021/11/7 14:49
 */
object MasterDiscipleEachUpgrade {
    private val engine = ScriptEngineManager().getEngineByName("javascript")
    private val time = BukkitMasterDiscipleConfManager.time
    private val discipleexp = BukkitMasterDiscipleConfManager.discipleexp
    private val masterexp = BukkitMasterDiscipleConfManager.masterexp
    fun start(player: Player, disciples: List<Player>) {
        if (StatusMap.playerIsInStatus(player)) {
            return
        }
        disciples.forEach {
            if (StatusMap.playerIsInStatus(it)) return
        }

        StatusMap.setPlayerMasterUpgrading(player, disciples)


        submit(async = true, period = 20) {
            val masterRemainTime = YouLongRelationBukkitApi.getLoverUpgradeRemainTime(player)
            if (StatusMap.getPlayerStatus(player) == Status.MASTER_UPGRADING) {
                disciples.forEach {
                    if (StatusMap.getPlayerStatus(it) != Status.DISCIPLE_UPGRADING) {
                        cancel()
                    }
                }
                if (masterRemainTime > 0) {
                    val masterCurrentExp = SkillAPI.getPlayerData(player).mainClass.exp
                    var disciplesExp = 0.0

                    disciples.forEach {
                        val discipleExpString = discipleexp.replace("{master_exp}", masterCurrentExp.toString())
                        val discipleAddExp = engine.eval(discipleExpString).toString().toDouble()
                        disciplesExp += SkillAPI.getPlayerData(it).mainClass.exp
                        it.sendLang("lover-upgrade-get-exp", discipleAddExp, masterRemainTime - 1)
                        SkillAPI.getPlayerData(it).giveExp(discipleAddExp, ExpSource.SPECIAL)
                    }

                    val masterExpString = masterexp.replace("{disciple_exp}", disciplesExp.toString())
                    val masterAddExp = engine.eval(masterExpString).toString().toDouble()

                    player.sendLang("master-upgrade-get-exp", masterAddExp, masterRemainTime - 1)
                    SkillAPI.getPlayerData(player).giveExp(masterAddExp, ExpSource.SPECIAL)

                    YouLongRelationBukkitApi.setMasterUpgradeRemainTime(player, masterRemainTime - 1)
                    PlayerMasterUpgradeEvent(player, disciples).call()

                    YouLongRelationBukkitApi.updateMasterUpgradeDate(player)
                } else {
                    disciples.forEach {
                        PlayerDisturbedEvent(
                            player.name,
                            disciples.map { it.name },
                            DisturbedCause.TIME_OVER,
                            Status.MASTER_UPGRADING
                        ).call()
                    }
                    StatusMap.removePlayerFromStatus(player)
                }
                cancel()
            } else cancel()
        }
    }
}

