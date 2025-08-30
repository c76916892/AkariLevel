/**
 * AkariLevel
 *
 * 自动加入等级组脚本。
 *
 * @author 季楠
 * @since 2025/8/18 17:28
 */

function onPluginEnable() {
    onPlayerJoin();
}

// 设置等级组列表。
var levelGroupNames = ["Example"];

function onPlayerJoin() {

    new Listener(PlayerJoinEvent.class)
        .setExecutor(
            function (event) {
                // 获取事件参数
                var player = event.player;
                var uniqueId = player.getUniqueId().toString();

                // 异步延迟2秒执行操作（非阻塞方式）
                Bukkit.getScheduler().runTaskLaterAsynchronously(
                    AkariLevel,  // 当前插件实例
                    function() {
                        // 注意：如果需要操作 Bukkit API 中的游戏对象，需切换回主线程
                        Bukkit.getScheduler().runTask(
                            AkariLevel,
                            function() {
                                // 自动加入等级组
                                levelGroupNames.forEach(
                                    function (name) {
                                        var levelGroup = LevelGroup.getLevelGroups()[name];
                                        if (levelGroup != null && !levelGroup.hasMember(uniqueId)) {
                                            levelGroup.addMember(uniqueId, "AUTO_JOIN");
                                            //player.sendMessage("已自动加入等级组: " + name);
                                        }
                                    }
                                );

                            }
                        );
                    },60); //因为进服延时加载等级(防掉级) 所以这里也得延时执行
            }
        ).register();
}