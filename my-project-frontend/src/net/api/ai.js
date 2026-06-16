import {fetchPost, get, post} from "@/net";
import {accessHeader} from '@/net/index.js'

// 旧的 SSE 聊天函数 - 保留给浮动 AiChatWindow 使用
export const apiChatWithAI = async (context, onMessage, onError, onComplete) => {
    const response = await fetchPost('/api/ai/chat', context)
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value)
            .replaceAll("data:", "")
            .replaceAll("\n", "")
        onMessage(text)
    }
    onComplete()
}

// ===== 对话管理 API =====

// 获取对话列表
export const apiConversationList = (success) =>
    get('/api/ai/conversations', success)

// 创建新对话
export const apiConversationCreate = (title, success) =>
    post('/api/ai/conversations', {title}, success)

// 删除对话
export const apiConversationDelete = (id, success) =>
    fetch(`/api/ai/conversations/${id}`, {
        method: 'DELETE',
        headers: accessHeader()
    }).then(r => r.json()).then(data => success && success(data))

// 获取对话消息历史
export const apiConversationMessages = (id, success) =>
    get(`/api/ai/conversations/${id}/messages`, success)

// SSE 流式对话（按会话ID）- 新版
export const apiChatWithConversation = async (conversationId, body, onMessage, onError, onComplete) => {
    try {
        const response = await fetch(`/api/ai/chat/${conversationId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...accessHeader()
            },
            body: JSON.stringify(body)
        })
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, {stream: true});
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
                if (line.startsWith('event: ')) {
                    const eventType = line.slice(7).trim();
                    if (eventType === 'done') {
                        onComplete && onComplete();
                        return;
                    }
                } else if (line.startsWith('data: ')) {
                    try {
                        const data = JSON.parse(line.slice(6));
                        if (data.type === 'text') {
                            onMessage && onMessage(data.content);
                        } else if (data.type === 'error') {
                            onError && onError(data.content || '未知错误');
                            return;
                        }
                    } catch (e) {
                        onMessage && onMessage(line.slice(6));
                    }
                }
            }
        }
        onComplete && onComplete();
    } catch (e) {
        onError && onError(e);
    }
}
