import { defineStore } from 'pinia';
import { ref } from 'vue';
import { submitDynamicSession } from '@/api/dynamicFitting.js';
import { showToast } from '@/utils/toast';

export const useDynamicSessionStore = defineStore('dynamicSession', () => {
    // 核心状态：存放三个视角的数据
    const sessionData = ref({
        bikeId: null,
        side: null,  // { summary: {}, motionData: [], snapshots: {} }
        front: null,
        back: null,
        painLevels: [], // 痛感标签列表
        sessionId: null,
        isSubmitting: false
    });

    // 初始化会话
    const initSession = (bikeId) => {
        sessionData.value = {
            bikeId,
            side: null,
            front: null,
            back: null,
            painLevels: [],
            id: null,
            isSubmitting: false
        };
    };

    // 保存单次采集结果
    const saveCapture = (viewType, data) => {
        // viewType: 'side' | 'front' | 'back'
        sessionData.value[viewType] = data;
    };

    const isEmpty = () => {
        return !sessionData.value.side && !sessionData.value.front && !sessionData.value.back;
    };

    // 提交全量数据
    const submit = async () => {
        sessionData.value.isSubmitting = true;
        try {
            const payload = {
                bikeId: sessionData.value.bikeId,
                sessionTime: new Date().toISOString(),
                painLevels: sessionData.value.painLevels || [],
                views: []
            };

            if (sessionData.value.side) payload.views.push({ viewType: 'SIDE', ...sessionData.value.side });
            if (sessionData.value.front) payload.views.push({ viewType: 'FRONT', ...sessionData.value.front });
            if (sessionData.value.back) payload.views.push({ viewType: 'BACK', ...sessionData.value.back });

            console.log(payload);
            const res = await submitDynamicSession(payload);
            const data = res.data || res;
            sessionData.value.id = data.id;
            showToast('分析提交成功，云端已同步', 'success');
            return data;
        } catch (error) {
            console.error('Submit dynamic session failed:', error);
            showToast('提交失败，请检查网络', 'error');
            return null;
        } finally {
            sessionData.value.isSubmitting = false;
        }
    };

    // 清空
    const clearSession = () => {
        const bikeId = sessionData.value.bikeId;
        sessionData.value = { bikeId, side: null, front: null, back: null, painLevels: [], id: null, isSubmitting: false };
    };

    return { sessionData, initSession, saveCapture, isEmpty, clearSession, submit };
});
