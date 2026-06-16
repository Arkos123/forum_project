<script setup>
import {computed, nextTick, reactive, ref, watchEffect} from "vue";
import {
    Bell,
    Check,
    Delete,
    Document,
    EditPen,
    Plus,
    Refresh,
    Search,
    Star
} from "@element-plus/icons-vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import ImageResize from "quill-image-resize-vue";
import {ImageExtend, QuillWatch} from "quill-image-super-solution-module";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import axios from "axios";
import {accessHeader} from "@/net";
import {ElMessage, ElMessageBox} from "element-plus";
import {
    apiAnnouncementAdminList,
    apiAnnouncementCreate,
    apiAnnouncementDelete,
    apiAnnouncementPublish,
    apiAnnouncementTop,
    apiAnnouncementUpdate
} from "@/net/api/announcement";

const announcements = reactive({
    list: [],
    total: 0,
    page: 1,
    size: 10,
    keyword: '',
    published: null,
    loading: false
})

const editor = reactive({
    show: false,
    id: null,
    title: '',
    summary: '',
    content: '',
    uploading: false
})

const refEditor = ref()

const editorTitle = computed(() => editor.id ? '编辑校园公告' : '发布新的校园公告')

function loadAnnouncements() {
    announcements.loading = true
    apiAnnouncementAdminList(
        announcements.page,
        announcements.size,
        announcements.keyword,
        announcements.published,
        data => {
            announcements.list = data.list
            announcements.total = data.total
            announcements.loading = false
        }
    )
}

function resetSearch() {
    announcements.keyword = ''
    announcements.published = null
    announcements.page = 1
    loadAnnouncements()
}

function openCreate() {
    editor.id = null
    editor.title = ''
    editor.summary = ''
    editor.content = ''
    editor.show = true
    nextTick(() => refEditor.value?.setContents('', 'user'))
}

function openEdit(row) {
    editor.id = row.id
    editor.title = row.title
    editor.summary = row.summary || ''
    editor.content = row.content ? new Delta(JSON.parse(row.content)) : ''
    editor.show = true
}

function deltaToText(delta) {
    if(!delta.ops) return ''
    let str = ''
    for (let op of delta.ops) {
        if(typeof op.insert === 'string')
            str += op.insert
    }
    return str.replace(/\s/g, '')
}

function submitAnnouncement() {
    const text = deltaToText(editor.content)
    if(!editor.title) {
        ElMessage.warning('请填写公告标题')
        return
    }
    if(!text) {
        ElMessage.warning('请填写公告内容')
        return
    }
    if(text.length > 20000) {
        ElMessage.warning('公告内容超过 20000 字，无法保存')
        return
    }
    const data = {
        title: editor.title,
        summary: editor.summary,
        content: editor.content
    }
    const success = () => {
        ElMessage.success(editor.id ? '公告更新成功' : '公告创建成功')
        editor.show = false
        loadAnnouncements()
    }
    if(editor.id)
        apiAnnouncementUpdate({...data, id: editor.id}, success)
    else
        apiAnnouncementCreate(data, success)
}

function changePublished(row) {
    apiAnnouncementPublish({
        id: row.id,
        published: row.published
    }, () => {
        ElMessage.success(row.published ? '公告已发布' : '公告已下线')
        loadAnnouncements()
    })
}

function changeTop(row) {
    apiAnnouncementTop({
        id: row.id,
        top: row.top
    }, () => {
        ElMessage.success(row.top ? '公告已置顶' : '已取消置顶')
        loadAnnouncements()
    })
}

function deleteAnnouncement(row) {
    ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除公告', {
        callback: value => {
            if(value === 'confirm') {
                apiAnnouncementDelete(row.id, () => {
                    ElMessage.success('公告删除成功')
                    loadAnnouncements()
                })
            }
        }
    })
}

watchEffect(() => {
    loadAnnouncements()
})

Quill.register('modules/imageResize', ImageResize)
Quill.register('modules/ImageExtend', ImageExtend)
const editorOption = {
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike", "clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                {header: [1, 2, 3, 4, 5, 6, false]},
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                {indent: '-1'}, {indent: '+1'}
            ],
            handlers: {
                image: function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize: {
            modules: ['Resize', 'DisplaySize']
        },
        ImageExtend: {
            action: axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept: 'image/png, image/jpeg',
            response: resp => resp.data ? axios.defaults.baseURL + '/images' + resp.data : null,
            methods: 'POST',
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization)
            },
            start: () => editor.uploading = true,
            success: () => {
                ElMessage.success('图片上传成功')
                editor.uploading = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员')
                editor.uploading = false
            }
        }
    }
}
</script>

<template>
    <div class="announcement-admin">
        <div class="title">
            <el-icon><Bell/></el-icon>
            校园公告管理
        </div>
        <div class="desc">
            在这里创建、编辑、发布和下线面向全体用户的校园公告。
        </div>
        <div class="toolbar">
            <el-input v-model="announcements.keyword"
                      :prefix-icon="Search"
                      clearable
                      placeholder="搜索标题或摘要..."
                      style="width: 260px"
                      @keyup.enter="announcements.page = 1; loadAnnouncements()"/>
            <el-select v-model="announcements.published" placeholder="发布状态" clearable style="width: 140px">
                <el-option label="已发布" :value="true"/>
                <el-option label="未发布" :value="false"/>
            </el-select>
            <el-button :icon="Search" type="primary" plain @click="announcements.page = 1; loadAnnouncements()">搜索</el-button>
            <el-button :icon="Refresh" plain @click="resetSearch">重置</el-button>
            <div style="flex: 1"/>
            <el-button :icon="Plus" type="success" @click="openCreate">新增公告</el-button>
        </div>
        <el-table :data="announcements.list" height="430" v-loading="announcements.loading">
            <el-table-column prop="id" label="ID" width="80" align="center"/>
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip/>
            <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip/>
            <el-table-column prop="username" label="创建人" width="120" align="center"/>
            <el-table-column label="发布" width="90" align="center">
                <template #default="{ row }">
                    <el-switch v-model="row.published" @change="changePublished(row)"/>
                </template>
            </el-table-column>
            <el-table-column label="置顶" width="90" align="center">
                <template #default="{ row }">
                    <el-switch v-model="row.top" :active-icon="Star" @change="changeTop(row)"/>
                </template>
            </el-table-column>
            <el-table-column label="发布时间" width="180" align="center">
                <template #default="{ row }">
                    {{ row.publishTime ? new Date(row.publishTime).toLocaleString() : '未发布' }}
                </template>
            </el-table-column>
            <el-table-column label="更新时间" width="180" align="center">
                <template #default="{ row }">
                    {{ new Date(row.updateTime || row.createTime).toLocaleString() }}
                </template>
            </el-table-column>
            <el-table-column width="170" label="操作" fixed="right" align="center">
                <template #default="{ row }">
                    <el-button size="small" type="primary" :icon="EditPen" @click="openEdit(row)">编辑</el-button>
                    <el-button size="small" type="danger" :icon="Delete" @click="deleteAnnouncement(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>
        <div class="pagination">
            <el-pagination :total="announcements.total"
                           v-model:current-page="announcements.page"
                           v-model:page-size="announcements.size"
                           layout="total, sizes, prev, pager, next, jumper"/>
        </div>
        <el-drawer :model-value="editor.show"
                   direction="btt"
                   :close-on-click-modal="false"
                   :size="650"
                   @close="editor.show = false">
            <template #header>
                <div>
                    <div style="font-weight: bold">{{ editorTitle }}</div>
                    <div style="font-size: 13px">公告发布后将对所有登录用户可见，请确认内容准确。</div>
                </div>
            </template>
            <div class="editor-form">
                <el-input v-model="editor.title"
                          :prefix-icon="Document"
                          maxlength="100"
                          show-word-limit
                          placeholder="请输入公告标题..."/>
                <el-input v-model="editor.summary"
                          maxlength="255"
                          show-word-limit
                          placeholder="请输入公告摘要..."
                          type="textarea"
                          :rows="2"/>
                <div class="editor-box" v-loading="editor.uploading" element-loading-text="正在上传图片，请稍后...">
                    <quill-editor v-model:content="editor.content"
                                  content-type="delta"
                                  ref="refEditor"
                                  style="height: calc(100% - 45px)"
                                  placeholder="请输入公告正文..."
                                  :options="editorOption"/>
                </div>
                <div class="editor-actions">
                    <div class="counter">正文纯文本字数 {{ deltaToText(editor.content).length }} / 20000</div>
                    <el-button type="success" :icon="Check" @click="submitAnnouncement">保存公告</el-button>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style lang="less" scoped>
.announcement-admin {
    .title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-weight: bold;
    }

    .desc {
        color: #bababa;
        font-size: 13px;
        margin-bottom: 20px;
    }

    .toolbar {
        display: flex;
        gap: 10px;
        margin-bottom: 15px;
    }

    .pagination {
        margin-top: 20px;
        display: flex;
        justify-content: right;
    }

    .editor-form {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .editor-box {
        border-radius: 5px;
        height: 430px;
        overflow: hidden;
    }

    .editor-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .counter {
        color: grey;
        font-size: 13px;
    }

    :deep(.el-drawer) {
        width: 850px;
        margin: auto;
        border-radius: 10px 10px 0 0;
    }

    :deep(.el-drawer__header) {
        margin: 0;
    }
}
</style>
