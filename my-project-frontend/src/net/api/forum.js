import {get, post} from "@/net";
import {ElMessage} from "element-plus";

export const apiForumTypes = (success) =>
    get('/api/forum/types', success)

export const apiForumTopic = (tid, success) =>
    get(`api/forum/topic?tid=${tid}`, success)

export const apiForumInteract = (tid, type, topic, message) => {
    get(`/api/forum/interact?tid=${tid}&type=${type}&state=${!topic[type]}`, () => {
        topic[type] = !topic[type]
        if(topic[type])
            ElMessage.success(`${message}成功！`)
        else
            ElMessage.success(`已取消${message}！`)
    })
}

export const apiForumUpdateTopic = (data, success) =>
    post('/api/forum/update-topic', data, success)

export const apiForumComments = (tid, page, success) =>
    get(`/api/forum/comments?tid=${tid}&page=${page}`, success)

export const apiForumCommentDelete = (id, success) =>
    get(`/api/forum/delete-comment?id=${id}`, success)

export const apiForumCommentSubmit = (data, success) =>
    post('/api/forum/add-comment', data, success)

export const apiForumTopicCreate = (data, success) =>
    post('/api/forum/create-topic', data, success)

export const apiForumTopTopics = (success) =>
    get('/api/forum/top-topic', success)

export const apiForumTopicList = (page, type, success) =>
    get(`/api/forum/list-topic?page=${page}&type=${type}`, success)

export const apiForumWeather = (longitude, latitude, success) =>
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, success)

export const apiForumCollect = (success) =>
    get('/api/forum/collects', success)

export const apiForumCollectDelete = (tid, success) =>
    get(`/api/forum/interact?tid=${tid}&type=collect&state=false`, success)

export const apiForumTopicAllList = (page, size, success) =>
    get(`/api/admin/forum/list?page=${page}&size=${size}`, success)

export const apiForumTopicDelete = (tid, success) =>
    get(`/api/admin/forum/delete?tid=${tid}`, success)

export const apiForumTopicTop = (data, success) =>
    post('api/admin/forum/top', data, success)

export const apiForumTopicLocked = (data, success) =>
    post('api/admin/forum/locked', data, success)

export const apiForumTopicInvisible = (data, success) =>
    post('api/admin/forum/invisible', data, success)

export const apiForumProhibitedList = (success) =>
    get('/api/admin/forum/prohibited-list', success)

export const apiForumProhibit = (data, success) =>
    post(`/api/admin/forum/prohibited-save`, data, success)
