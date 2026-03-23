import { reactive } from 'vue';

// 1. 定义响应式状态：存储当前显示的消息队列
export const toastState = reactive({
    messages: [],
    dialog: {
        visible: false,
        title: '',
        content: '',
        confirmText: '确定',
        cancelText: '取消',
        resolve: null, // 用于存储 Promise 的 resolve 函数
    }
});

/**
 * 2. 导出显示函数
 * @param {string} message - 提示文字
 * @param {string} type - 类型：'success'(成功) | 'error'(错误) | 'info'(普通)
 * @param {number} duration - 停留时间(ms)，默认3000
 */
export const showToast = (message, type = 'success', duration = 3000) => {
    // 生成唯一 ID，防止列表渲染出错
    const id = Date.now() + Math.random();
    const toastItem = { id, message, type };
    // 将消息推入队列（显示）
    toastState.messages.push(toastItem);
    // 设置定时器自动移除
    setTimeout(() => {
        removeToast(id);
    }, duration);
};

// 3. 移除函数
const removeToast = (id) => {
    const index = toastState.messages.findIndex(item => item.id === id);
    if (index !== -1) {
        toastState.messages.splice(index, 1);
    }
};

/**
 * 显示询问对话框
 * @param {string} content - 询问内容
 * @param {string} title - 标题 (默认: 提示)
 * @param {string} confirmText - 确认按钮文字
 * @param {string} cancelText - 取消按钮文字
 * @returns {Promise<boolean>} - 点击确定返回 true，点击取消返回 false
 */
export const showConfirm = (content, title = '提示', confirmText = '确定', cancelText = '取消') => {
    return new Promise((resolve) => {
        // 设置状态
        toastState.dialog.content = content;
        toastState.dialog.title = title;
        toastState.dialog.confirmText = confirmText;
        toastState.dialog.cancelText = cancelText;
        toastState.dialog.visible = true;
        // 存储 resolve，以便在点击按钮时调用
        toastState.dialog.resolve = resolve;
    });
};

/**
 * 供组件调用的内部方法：处理用户点击
 */
export const closeDialog = (result) => {
    toastState.dialog.visible = false;
    if (toastState.dialog.resolve) {
        toastState.dialog.resolve(result); // 返回 true 或 false
        toastState.dialog.resolve = null;  // 清理
    }
};