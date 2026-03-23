/**
 * mockService.js
 * 模拟 API 服务层，封装了对 mockData 的异步访问
 */
import { homeData, teamData, activityData, shareData } from './mockData';

// 模拟网络延迟
const sleep = (ms = 300) => new Promise(resolve => setTimeout(resolve, ms));

export const homeService = {
    async getHomeData() {
        await sleep();
        return { ...homeData };
    }
};

export const teamService = {
    async getAllTeams() {
        await sleep();
        return [...teamData.allTeams];
    }
};

export const activityService = {
    async getAllActivities() {
        await sleep();
        return [...activityData.allActivities];
    }
};

export const shareService = {
    async getAllPosts() {
        await sleep();
        return [...shareData.allPosts];
    }
};
