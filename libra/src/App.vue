<template>
  <AppHeader @open-auth="userStore.openAuthModal()"/>

  <router-view/>

  <AppFooter/>

  <AuthModal :isOpen="userStore.showAuthModal" @close="userStore.closeAuthModal()"/>
  <AiAgent />
  <ToastManager/>
</template>

<script setup>
import {onMounted} from 'vue';
import AppHeader from './components/AppHeader.vue';
import AppFooter from './components/AppFooter.vue';
import AuthModal from './components/AuthModal.vue';
import ToastManager from './components/ToastManager.vue';
import AiAgent from './components/AiAgent.vue';
import {useUserStore} from "@/stores/user";

const userStore = useUserStore();

onMounted(() => {
  userStore.initUser();
});
</script>