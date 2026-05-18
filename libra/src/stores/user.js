/**
 * @file user.js
 * @description State management for user authentication, profile, and session tokens.
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { STORAGE_KEYS } from '@/config/constants';

export const useUserStore = defineStore('user', () => {
    // --- State ---
    const userInfo = ref(null);
    const accessToken = ref('');
    const refreshToken = ref('');
    const showAuthModal = ref(false);

    // --- Getters ---
    const isLoggedIn = computed(() => !!userInfo.value && !!accessToken.value);

    /**
     * Internal helper to synchronize state with local storage.
     */
    const syncStorage = (data = {}) => {
        const { user, access, refresh } = data;

        if (user !== undefined) {
            userInfo.value = user;
            user && user !== 'undefined' && user !== 'null'
                ? localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(user))
                : localStorage.removeItem(STORAGE_KEYS.USER_INFO);
        }

        if (access !== undefined) {
            accessToken.value = access;
            access && access !== 'undefined' && access !== 'null'
                ? localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, access)
                : localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
        }

        if (refresh !== undefined) {
            refreshToken.value = refresh;
            refresh && refresh !== 'undefined' && refresh !== 'null'
                ? localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refresh)
                : localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
        }
    };

    /**
     * Initializes store state from local storage.
     * Called on application mount.
     */
    const initUser = () => {
        try {
            const storedUser = localStorage.getItem(STORAGE_KEYS.USER_INFO);
            const storedAccess = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
            const storedRefresh = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);

            const isValid = (val) => val && val !== 'undefined' && val !== 'null';

            if (isValid(storedUser)) userInfo.value = JSON.parse(storedUser);
            if (isValid(storedAccess)) accessToken.value = storedAccess;
            if (isValid(storedRefresh)) refreshToken.value = storedRefresh;
        } catch (e) {
            console.error('[UserStore] Failed to hydrate state from storage', e);
            logout(); // Reset on corruption
        }
    };

    /**
     * Handles successful login response processing.
     */
    const login = (apiResponse) => {
        if (!apiResponse) return;

        const data = apiResponse.data || apiResponse;
        const user = data.user || data;

        // Normalize avatar URL
        if (user?.avatar) {
            user.avatar = `${user.avatar}`;
        }

        syncStorage({
            user,
            access: data.accessToken,
            refresh: data.refreshToken
        });
    };

    const refreshSession = (newAccessToken, newRefreshToken, newUserProfile) => {
        syncStorage({
            access: newAccessToken,  // 对应 accessToken
            refresh: newRefreshToken, // 对应 refreshToken
            user: newUserProfile      // 对应 userInfo (可选，如果后端返回了 profile 就更新)
        });
    };

    /**
     * Clears all user-related state and storage.
     */
    const logout = () => {
        syncStorage({ user: null, access: '', refresh: '' });
    };

    /**
     * Updates user info specifically (often used during profile updates or token refresh).
     */
    const setUserInfo = (info) => {
        syncStorage({ user: info });
    };

    /**
     * UI Control: Open/Close Auth Modal
     */
    const openAuthModal = () => { showAuthModal.value = true; };
    const closeAuthModal = () => { showAuthModal.value = false; };

    // Immediately hydrate state upon store creation to avoid race conditions with router guards.
    initUser();

    return {
        userInfo,
        accessToken,
        refreshToken,
        showAuthModal,
        isLoggedIn,
        initUser,
        login,
        logout,
        setUserInfo,
        refreshSession,
        openAuthModal,
        closeAuthModal
    };
});
