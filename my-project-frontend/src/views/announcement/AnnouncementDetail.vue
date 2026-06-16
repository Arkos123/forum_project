<script setup>
import {useRoute} from "vue-router";
import {reactive} from "vue";
import {ArrowLeft, Bell, Clock} from "@element-plus/icons-vue";
import {QuillDeltaToHtmlConverter} from "quill-delta-to-html";
import Card from "@/components/Card.vue";
import router from "@/router";
import {apiAnnouncementDetail} from "@/net/api/announcement";

const route = useRoute()
const state = reactive({
    data: null
})

apiAnnouncementDetail(route.params.id, data => state.data = data)

function convertToHtml(content) {
    const ops = JSON.parse(content).ops
    const converter = new QuillDeltaToHtmlConverter(ops, {inlineStyles: true})
    return converter.convert()
}
</script>

<template>
    <div class="announcement-detail" v-if="state.data">
        <card class="detail-header">
            <el-button :icon="ArrowLeft" type="info" size="small" plain round
                       @click="router.push('/index/announcements')">返回公告</el-button>
            <div class="header-title">
                <el-tag v-if="state.data.top" size="small" type="danger">置顶</el-tag>
                <span>{{ state.data.title }}</span>
            </div>
        </card>
        <card class="detail-body">
            <div class="title">
                <el-icon><Bell/></el-icon>
                {{ state.data.title }}
            </div>
            <div class="meta">
                <el-icon><Clock/></el-icon>
                发布时间：{{ new Date(state.data.publishTime || state.data.createTime).toLocaleString() }}
            </div>
            <el-divider/>
            <div class="summary" v-if="state.data.summary">{{ state.data.summary }}</div>
            <div class="content" v-html="convertToHtml(state.data.content)"></div>
        </card>
    </div>
</template>

<style lang="less" scoped>
.announcement-detail {
    max-width: 900px;
    margin: 20px auto;
    padding: 0 20px;

    .detail-header {
        position: sticky;
        top: 0;
        z-index: 10;
        display: flex;
        align-items: center;
    }

    .header-title {
        flex: 1;
        text-align: center;
        font-weight: bold;

        span {
            margin-left: 8px;
        }
    }

    .detail-body {
        margin-top: 10px;
    }

    .title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 22px;
        font-weight: bold;
    }

    .meta {
        display: flex;
        align-items: center;
        gap: 4px;
        color: grey;
        font-size: 13px;
        margin-top: 10px;
    }

    .summary {
        background-color: rgba(94, 94, 94, 0.08);
        border-radius: 6px;
        color: grey;
        line-height: 1.7;
        margin-bottom: 20px;
        padding: 12px;
    }

    .content {
        line-height: 1.8;
        overflow-wrap: anywhere;

        :deep(img) {
            max-width: 100%;
        }
    }
}
</style>
