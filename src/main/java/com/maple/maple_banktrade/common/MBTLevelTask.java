package com.maple.maple_banktrade.common;

import com.gto.registrylib.util.entry.AttachmentTypeEntry;
import com.mapleutillib.utils.task.LevelTaskData;
import com.mapleutillib.utils.task.TaskHandler;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;

public class MBTLevelTask {

    public static final AttachmentTypeEntry<LevelTaskData> LEVEL_TASK_DATA = REGISTRY
            .attachmentType("level_task_data", _ -> new LevelTaskData())
            .serialize(LevelTaskData.CODEC)
            .register();

    public static final TaskHandler.Tasks TASKS = TaskHandler.tasks(LEVEL_TASK_DATA);
}
