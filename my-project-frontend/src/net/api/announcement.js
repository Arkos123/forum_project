import {get, post} from "@/net";

export const apiAnnouncementLatest = (limit, success) =>
    get(`/api/announcement/latest?limit=${limit}`, success)

export const apiAnnouncementList = (page, size, success) =>
    get(`/api/announcement/list?page=${page}&size=${size}`, success)

export const apiAnnouncementDetail = (id, success) =>
    get(`/api/announcement/detail?id=${id}`, success)

export const apiAnnouncementAdminList = (page, size, keyword, published, success) => {
    const params = new URLSearchParams({
        page,
        size
    })
    if(keyword)
        params.set('keyword', keyword)
    if(published === true || published === false)
        params.set('published', published)
    get(`/api/admin/announcement/list?${params.toString()}`, success)
}

export const apiAnnouncementCreate = (data, success) =>
    post('/api/admin/announcement/create', data, success)

export const apiAnnouncementUpdate = (data, success) =>
    post('/api/admin/announcement/update', data, success)

export const apiAnnouncementPublish = (data, success) =>
    post('/api/admin/announcement/publish', data, success)

export const apiAnnouncementTop = (data, success) =>
    post('/api/admin/announcement/top', data, success)

export const apiAnnouncementDelete = (id, success) =>
    get(`/api/admin/announcement/delete?id=${id}`, success)
