<script setup>
import {reactive, ref, watchEffect} from "vue";
import {apiForumTopicAllList, apiForumTypes} from "@/net/api/forum";
import {User} from "@element-plus/icons-vue";
import {useStore} from "@/store";

const store = useStore()

const topicList = reactive({
    list: [],
    page: 1,
    size: 10,
    total: 0
})

const types = ref([])
const findType = type => types.value.find(item => item.id === type)

watchEffect(() =>{
    apiForumTopicAllList(topicList.page, topicList.size, data => {
        topicList.list = data.list;
        topicList.total = data.total;
    })
})

apiForumTypes(data => types.value = data)
</script>

<template>
    <div class="forum-admin">
        <div class="title">
            <el-icon><User/></el-icon>
            论坛帖子列表
        </div>
        <div class="desc">
            在这里管理论坛所有的帖子，并对帖子进行各种操作和管理
        </div>
        <el-table :data="topicList.list" height="400">
            <el-table-column prop="id" label="帖子ID" width="80" align="center"/>
            <el-table-column prop="title" label="帖子标题" width="300" show-overflow-tooltip/>
            <el-table-column label="帖子类型" width="120">
                <template #default="{ row }">
                    <div class="topic-type">
                        <div class="type-dot" :style="{ backgroundColor: findType(row.type)?.color ?? '#bababa' }"></div>
                        <div>{{ findType(row.type)?.name ?? '未知类型' }}</div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="帖子作者" width="150">
                <template #default="{ row }">
                    <div class="topic-username">
                        <el-avatar :size="20" :src="store.avatarUserUrl(row.avatar)"/>
                        <div>{{ row.username }}</div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="time" label="发表时间" width="180" align="center"
                             :formatter="row => new Date(row.time).toLocaleString()"/>
            <el-table-column label="操作" width="200" fixed="right" align="center">
                <template #default="{ row }">
                    <el-button size="small" type="info" plain>屏蔽</el-button>
                    <el-button size="small" type="warning" plain>锁定</el-button>
                    <el-button size="small" type="danger" plain>删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <div class="pagination">
            <el-pagination :total="topicList.total"
                           v-model:current-page="topicList.page"
                           v-model:page-size="topicList.size"
                           layout="total, sizes, prev, pager, next, jumper"/>
        </div>
    </div>
</template>

<style lang="less" scoped>
.forum-admin {
    .title {
        font-weight: bold;
    }

    .desc {
        color: #bababa;
        font-size: 13px;
        margin-bottom: 20px;
    }

    .pagination {
        margin-top: 20px;
        display: flex;
        justify-content: right;
    }

    .topic-username {
        gap: 10px;
        display: flex;
        align-items: center;
    }

    .topic-type {
        gap: 10px;
        display: flex;
        align-items: center;

        .type-dot {
            height: 7px;
            width: 7px;
            border-radius: 50%;
        }
    }
}
</style>
