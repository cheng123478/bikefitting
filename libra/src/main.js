import { createApp } from 'vue';
import { createPinia } from "pinia";
import App from './App.vue';
import router from './router';

// 引入全局样式
import './assets/css/main.css';

const app = createApp(App);

/* =========================================
   注册全局动画指令: v-scroll-reveal
   ========================================= */
app.directive('scroll-reveal', {
    mounted(el, binding) {
        // 1. 添加初始隐藏类
        // 注意：由于 CSS 中加了媒体查询，这个类在手机端虽然会被加上，但不会产生任何视觉效果
        el.classList.add('reveal-hidden');

        // 2. 添加延迟类
        if (binding.value !== undefined) {
            const delayIndex = (binding.value % 4) + 1;
            el.classList.add(`stagger-${delayIndex}`);
        }

        // 3. 创建观察者
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    el.classList.add('reveal-visible');
                    observer.unobserve(el);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -30px 0px'
        });

        observer.observe(el);
    }
});

app.use(router);
app.use(createPinia());

/* =========================================
   注册全局错误处理机制
   ========================================= */
import { showToast } from '@/utils/toast';
app.config.errorHandler = (err, instance, info) => {
    console.error('[Global Error Info]:', info);
    console.error('[Global Error]:', err);
    showToast(err.message || '应用发生未知错误，请刷新重试', 'error');
};

app.mount('#app');