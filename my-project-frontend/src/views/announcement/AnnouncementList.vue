<script setup>
import {Bell, Clock, Search} from "@element-plus/icons-vue";
import {reactive, watchEffect} from "vue";
import LightCard from "@/components/LightCard.vue";
import router from "@/router";
import {apiAnnouncementList} from "@/net/api/announcement";

const announcements = reactive({
    list: [],
    page: 1,
    size: 10,
    total: 0
})

watchEffect(() => {
    apiAnnouncementList(announcements.page - 1, announcements.size, data => {
        announcements.list = data.list
        announcements.total = data.total
    })
})
</script>

<template>
    <div class="announcement-page">
        <div class="page-title">
            <div>
                <div class="title">
                    <el-icon><Bell/></el-icon>
                    校园公告
                </div>
                <div class="desc">查看学校发布的最新通知和重要事项。</div>
            </div>
        </div>
        <el-empty v-if="!announcements.list.length" description="暂无公告"/>
        <div v-else class="announcement-list">
            <light-card v-for="item in announcements.list"
                        :key="item.id"
                        class="announcement-item"
                        @click="router.push(`/index/announcement/${item.id}`)">
                <div class="item-header">
                    <div>
                        <el-tag v-if="item.top" size="small" type="danger">置顶</el-tag>
                        <span class="item-title">{{ item.title }}</span>
                    </div>
                    <div class="item-time">
                        <el-icon><Clock/></el-icon>
                        {{ new Date(item.publishTime || item.createTime).toLocaleString() }}
                    </div>
                </div>
                <div class="item-summary">{{ item.summary || '暂无摘要' }}</div>
                <div class="item-more">
                    <el-icon><Search/></el-icon>
                    查看详情
                </div>
            </light-card>
        </div>
        <div class="pagination" v-if="announcements.total > announcements.size">
            <el-pagination background
                           layout="prev, pager, next"
                           v-model:current-page="announcements.page"
                           :page-size="announcements.size"
                           :total="announcements.total"/>
        </div>
    </div>
</template>

<style lang="less" scoped>
.announcement-page {
    max-width: 900px;
    margin: 20px auto;
    padding: 0 20px;

    .page-title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 15px;

        .title {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 20px;
            font-weight: bold;
        }

        .desc {
            color: grey;
            font-size: 13px;
            margin-top: 5px;
        }
    }

    .announcement-list {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .announcement-item {
        transition: .3s;

        &:hover {
            cursor: pointer;
            opacity: 0.75;
        }
    }

    .item-header {
        display: flex;
        justify-content: space-between;
        gap: 20px;
        align-items: center;
    }

    .item-title {
        font-size: 16px;
        font-weight: bold;
        margin-left: 8px;
    }

    .item-time {
        display: flex;
        align-items: center;
        gap: 4px;
        color: grey;
        font-size: 13px;
        white-space: nowrap;
    }

    .item-summary {
        color: grey;
        font-size: 14px;
        margin-top: 10px;
        line-height: 1.6;
    }

    .item-more {
        display: flex;
        align-items: center;
        gap: 4px;
        color: var(--el-color-primary);
        font-size: 13px;
        margin-top: 10px;
    }

    .pagination {
        display: flex;
        justify-content: center;
        margin-top: 20px;
    }
}
</style>
