import { defineStore } from 'pinia';
import { getLabDashboard } from '@/api/lab.js';

export const useLabStore = defineStore('lab', {
  state: () => ({
    dashboardData: {
      hasBodyProfile: false,
      flexibilityScore: null,
      stabilityScore: null,
      bikeCount: 0,
      bikes: [],
      defaultBikeId: null,
      hasReport: false,
      completeness: 0
    },
    isLoaded: false,
    isLoading: false,
    lastFetched: null
  }),

  getters: {
    allBikes: (state) => state.dashboardData?.bikes || [],
    hasBodyProfile: (state) => state.dashboardData?.hasBodyProfile || false,
    bikeCount: (state) => state.dashboardData?.bikeCount || 0
  },

  actions: {
    /**
     * 获取实验室首页数据
     * @param {boolean} force 是否强制从后端刷新
     */
    async fetchDashboard(force = false) {
      // 如果数据已加载且不是强制刷新，则直接返回
      if (this.isLoaded && !force && this.dashboardData.bikes.length > 0) {
        return this.dashboardData;
      }

      this.isLoading = true;
      try {
        const res = await getLabDashboard();
        this.dashboardData = res.data || res;
        this.isLoaded = true;
        this.lastFetched = new Date().getTime();
        return this.dashboardData;
      } catch (error) {
        console.error('Failed to fetch lab dashboard:', error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 标记数据过时，下次进入页面将重新获取
     */
    setStale() {
      this.isLoaded = false;
    }
  }
});
