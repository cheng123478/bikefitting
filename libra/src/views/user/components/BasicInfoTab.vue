<template>
  <div class="panel-section fade-in">
    <div class="panel-header">
      <h3 class="panel-title">编辑个人资料</h3>
      <p class="sub">完善资料让骑友更容易认识你</p>
    </div>
    
    <form @submit.prevent="updateProfile">
      <!-- 头像上传区 (移动到资料内部以加强关联) -->
      <div class="avatar-edit-section mb-30">
        <div class="avatar-wrapper">
          <img :src="userForm.avatar || defaultAvatar" class="avatar-lg">
          <div class="edit-overlay">
            <i class="fas fa-camera"></i>
            <input type="file" accept="image/*" class="file-input" @change="handleAvatarChange">
          </div>
        </div>
        <div class="avatar-info">
          <span class="label">修改个人头像</span>
          <span class="tip">建议尺寸 200x200，支持 JPG/PNG</span>
        </div>
      </div>

      <div class="grid-2">
        <div class="input-wrapper">
          <label>昵称</label>
          <div class="input-group">
            <input type="text" v-model="userForm.name" class="zen-input" placeholder="起个响亮的名字">
          </div>
        </div>
        <div class="input-wrapper">
          <label>当前手机号</label>
          <div class="input-group disabled icon-input">
            <i class="fas fa-lock input-icon"></i>
            <input type="text" v-model="userForm.phone" class="zen-input" disabled>
          </div>
          <small class="helper-text" @click="$emit('switch-tab', 'security')">去“账号安全”修改 ></small>
        </div>
      </div>

      <div class="grid-2">
        <div class="input-wrapper">
          <label>性别</label>
          <div class="input-group">
            <select v-model="userForm.gender" class="zen-input select-arrow">
              <option :value="0">保密</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
            <i class="fas fa-chevron-down input-icon right-icon"></i>
          </div>
        </div>
        <div class="input-wrapper">
          <label>生日 ({{ getConstellation(userForm.birthday) }})</label>
          <div class="input-group icon-input">
            <i class="fas fa-birthday-cake input-icon"></i>
            <input
                type="date"
                v-model="userForm.birthday"
                class="zen-input"
                :min="'1900-01-01'"
                :max="today"
            >
          </div>
        </div>
      </div>

      <div class="input-wrapper">
        <label>所在城市</label>
        <div class="city-selector" :class="{ open: cityDropdownOpen }">
          <div class="city-trigger" @click.stop="cityDropdownOpen = !cityDropdownOpen">
            <i class="fas fa-map-marker-alt city-trigger-icon"></i>
            <span v-if="selectedProvinceName && userForm.city" class="city-value">{{ selectedProvinceName }} · {{ userForm.city }}</span>
            <span v-else class="city-placeholder">{{ userForm.city || '选择所在城市' }}</span>
            <i class="fas fa-chevron-down city-arrow" :class="{ rotated: cityDropdownOpen }"></i>
          </div>
          <Transition name="dropdown">
            <div v-if="cityDropdownOpen" class="city-dropdown" @click.stop>
              <template v-if="!selectedProvinceObj">
                <div class="city-search">
                  <i class="fas fa-search"></i>
                  <input v-model="citySearch" type="text" placeholder="搜索省份..." class="city-search-input" />
                </div>
                <div class="city-grid">
                  <button v-for="p in filteredProvinces" :key="p.name" type="button" class="city-card" @click="selectProvince(p)">
                    <span class="city-emoji">{{ p.emoji }}</span>
                    <span class="city-name">{{ p.name }}</span>
                  </button>
                </div>
                <p v-if="filteredProvinces.length === 0" class="city-empty"><i class="fas fa-map"></i> 未找到匹配省份</p>
              </template>
              <template v-else>
                <div class="city-breadcrumb">
                  <button type="button" class="breadcrumb-back" @click="selectedProvinceObj = null">
                    <i class="fas fa-arrow-left"></i> 返回
                  </button>
                  <span class="breadcrumb-name">{{ selectedProvinceObj.emoji }} {{ selectedProvinceObj.name }}</span>
                </div>
                <div class="city-grid">
                  <button v-for="c in selectedProvinceObj.cities" :key="c" type="button"
                    class="city-card" :class="{ active: userForm.city === c }" @click="selectCity(c)">
                    <span class="city-name">{{ c }}</span>
                  </button>
                </div>
              </template>
            </div>
          </Transition>
        </div>
      </div>

      <div class="input-wrapper">
        <label>个人简介</label>
        <textarea class="zen-textarea" v-model="userForm.introduction" rows="3" placeholder="写一句你的骑行座右铭..."></textarea>
      </div>

      <div class="form-footer">
        <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
          {{ isSubmitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { useUserStore } from '@/stores/user.js';
import { showToast } from '@/utils/toast.js';
import { updateUserProfile, uploadAvatar } from '@/api/profile.js';
import { STORAGE_KEYS } from '@/config/constants';

const emit = defineEmits(['switch-tab']);
const userStore = useUserStore();
const isSubmitting = ref(false);
const defaultAvatar = '/src/assets/image/default-avatar.png';

const cityDropdownOpen = ref(false);
const citySearch = ref('');
const selectedProvinceObj = ref(null);

const provinceList = [
  { name: '北京', emoji: '🏛', cities: ['北京'] },
  { name: '上海', emoji: '🌃', cities: ['上海'] },
  { name: '天津', emoji: '🎡', cities: ['天津'] },
  { name: '重庆', emoji: '⛰', cities: ['重庆'] },
  { name: '广东', emoji: '🌺', cities: ['广州','深圳','东莞','佛山','珠海','惠州','中山','汕头','江门','湛江','茂名','肇庆','揭阳','清远','梅州','韶关','河源','阳江','潮州','汕尾','云浮'] },
  { name: '江苏', emoji: '🏮', cities: ['南京','苏州','无锡','常州','南通','徐州','扬州','镇江','泰州','盐城','淮安','连云港','宿迁'] },
  { name: '浙江', emoji: '⚓', cities: ['杭州','宁波','温州','绍兴','嘉兴','金华','台州','湖州','衢州','丽水','舟山'] },
  { name: '四川', emoji: '🐼', cities: ['成都','绵阳','德阳','宜宾','南充','乐山','自贡','泸州','达州','内江','广元','遂宁','眉山','攀枝花','广安','资阳','雅安','巴中','凉山'] },
  { name: '湖北', emoji: '🌊', cities: ['武汉','宜昌','襄阳','荆州','黄石','十堰','荆门','鄂州','孝感','黄冈','咸宁','随州','恩施'] },
  { name: '湖南', emoji: '🌶', cities: ['长沙','株洲','湘潭','衡阳','岳阳','常德','郴州','邵阳','益阳','娄底','永州','怀化','张家界','湘西'] },
  { name: '山东', emoji: '🍺', cities: ['济南','青岛','烟台','潍坊','临沂','济宁','淄博','威海','泰安','德州','聊城','滨州','菏泽','枣庄','东营','日照'] },
  { name: '福建', emoji: '🍵', cities: ['福州','厦门','泉州','漳州','莆田','龙岩','三明','南平','宁德'] },
  { name: '河南', emoji: '🚂', cities: ['郑州','洛阳','开封','南阳','新乡','安阳','焦作','许昌','平顶山','商丘','信阳','驻马店','周口','漯河','濮阳','鹤壁','三门峡','济源'] },
  { name: '河北', emoji: '🏗', cities: ['石家庄','唐山','保定','廊坊','秦皇岛','邯郸','邢台','沧州','张家口','承德','衡水'] },
  { name: '辽宁', emoji: '🏭', cities: ['沈阳','大连','鞍山','抚顺','本溪','丹东','锦州','营口','阜新','辽阳','盘锦','铁岭','朝阳','葫芦岛'] },
  { name: '陕西', emoji: '🏺', cities: ['西安','咸阳','宝鸡','渭南','汉中','榆林','安康','延安','商洛','铜川'] },
  { name: '云南', emoji: '🌸', cities: ['昆明','大理','丽江','曲靖','玉溪','红河','楚雄','普洱','西双版纳','保山','昭通','临沧','文山','德宏','怒江','迪庆'] },
  { name: '安徽', emoji: '🔬', cities: ['合肥','芜湖','蚌埠','马鞍山','安庆','淮南','阜阳','亳州','黄山','滁州','宣城','六安','池州','铜陵','淮北','宿州'] },
  { name: '江西', emoji: '🚩', cities: ['南昌','赣州','九江','景德镇','上饶','吉安','宜春','抚州','萍乡','新余','鹰潭'] },
  { name: '广西', emoji: '🌴', cities: ['南宁','桂林','柳州','北海','玉林','百色','梧州','钦州','贵港','河池','崇左','来宾','贺州','防城港'] },
  { name: '贵州', emoji: '🌿', cities: ['贵阳','遵义','六盘水','安顺','毕节','铜仁','黔东南','黔南','黔西南'] },
  { name: '山西', emoji: '⛏', cities: ['太原','大同','运城','临汾','晋中','长治','忻州','吕梁','朔州','阳泉','晋城'] },
  { name: '海南', emoji: '🥥', cities: ['海口','三亚','儋州','琼海','文昌','万宁','五指山','陵水'] },
  { name: '吉林', emoji: '🌲', cities: ['长春','吉林','四平','通化','延边','白山','白城','松原','辽源'] },
  { name: '黑龙江', emoji: '❄', cities: ['哈尔滨','大庆','齐齐哈尔','牡丹江','佳木斯','鸡西','双鸭山','伊春','七台河','鹤岗','绥化','黑河','大兴安岭'] },
  { name: '甘肃', emoji: '🍜', cities: ['兰州','天水','白银','酒泉','张掖','武威','庆阳','平凉','定西','陇南','金昌','嘉峪关','临夏','甘南'] },
  { name: '内蒙古', emoji: '🐎', cities: ['呼和浩特','包头','鄂尔多斯','赤峰','通辽','呼伦贝尔','巴彦淖尔','乌兰察布','乌海','锡林郭勒','兴安盟','阿拉善'] },
  { name: '宁夏', emoji: '🏜', cities: ['银川','石嘴山','吴忠','固原','中卫'] },
  { name: '新疆', emoji: '🍇', cities: ['乌鲁木齐','克拉玛依','喀什','伊犁','哈密','阿克苏','吐鲁番','库尔勒','昌吉','石河子','塔城','阿勒泰','和田'] },
  { name: '青海', emoji: '🏔', cities: ['西宁','海东','海西','海南州','海北','黄南','玉树','果洛'] },
  { name: '西藏', emoji: '🗻', cities: ['拉萨','日喀则','昌都','林芝','山南','那曲','阿里'] },
  { name: '香港', emoji: '🌉', cities: ['香港'] },
  { name: '澳门', emoji: '🎰', cities: ['澳门'] },
  { name: '台湾', emoji: '🏝', cities: ['台北','高雄','台中','台南','新竹','基隆','桃园'] },
];

const filteredProvinces = computed(() => {
  const q = citySearch.value.trim().toLowerCase();
  if (!q) return provinceList;
  return provinceList.filter(p => p.name.toLowerCase().includes(q));
});

const selectedProvinceName = computed(() => selectedProvinceObj.value?.name || '');

const selectProvince = (p) => { selectedProvinceObj.value = p; citySearch.value = ''; userForm.city = ''; };
const selectCity = (city) => { userForm.city = city; cityDropdownOpen.value = false; selectedProvinceObj.value = null; };

const userForm = reactive({
  name: '',
  phone: '',
  city: '',
  introduction: '',
  avatar: '',
  gender: 0,
  birthday: ''
});

const today = computed(() => new Date().toISOString().split('T')[0]);

const getConstellation = (dateStr) => {
  if (!dateStr) return '星座未知';
  const date = new Date(dateStr);
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const s = "魔羯水瓶双鱼白羊金牛双子巨蟹狮子处女天秤天蝎射手魔羯";
  const arr = [20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22];
  return s.substr(month * 2 - (day < arr[month - 1] ? 2 : 0), 2) + "座";
};

const handleAvatarChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await uploadAvatar(formData);
    userForm.avatar = res;
    // 同步更新 Store
    userStore.userInfo.avatar = res;
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userStore.userInfo));
    showToast('头像更新成功', 'success');
  } catch (error) {
    showToast('头像上传失败', 'error');
  }
};

const updateProfile = async () => {
  if (!userForm.name) return showToast('昵称不能为空', 'warning');

  try {
    isSubmitting.value = true;
    await updateUserProfile(userForm);
    userStore.setUserInfo({ ...userStore.userInfo, ...userForm });
    localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userStore.userInfo));
    showToast('个人资料已保存', 'success');
  } catch (error) {
    // handled by request interceptor
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(() => {
  if (userStore.userInfo) {
    Object.assign(userForm, {
      name: userStore.userInfo.name || '',
      phone: userStore.userInfo.phone || '',
      avatar: userStore.userInfo.avatar || '',
      city: userStore.userInfo.city || '',
      introduction: userStore.userInfo.introduction || '',
      gender: userStore.userInfo.gender !== undefined ? userStore.userInfo.gender : 0,
      birthday: userStore.userInfo.birthday || ''
    });
  }
});
</script>

<style scoped>
.mb-30 { margin-bottom: 30px; }
.mb-0 { margin-bottom: 0; }

.avatar-edit-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: rgba(92, 110, 88, 0.03);
  border-radius: var(--radius-md);
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  cursor: pointer;
}

.avatar-lg {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: var(--shadow-sm);
}

.edit-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: 0.3s;
}

.avatar-wrapper:hover .edit-overlay {
  opacity: 1;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-info .label {
  display: block;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.avatar-info .tip {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.input-wrapper { margin-bottom: 25px; }
.input-wrapper label { display: block; margin-bottom: 8px; font-size: 0.9rem; color: var(--text-muted); }

.input-group { position: relative; }
.input-group.disabled .zen-input { background: #f8f8f8; cursor: not-allowed; opacity: 0.8; }
.input-group .right-icon { left: auto; right: 15px; pointer-events: none; font-size: 0.75rem; color: var(--text-muted); }

/* 隐藏 Select 默认箭头 */
.select-arrow {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 40px !important; /* 为右侧自定义图标留空 */
}

.helper-text { font-size: 0.8rem; color: var(--primary-color); cursor: pointer; margin-top: 5px; display: inline-block; }
.form-footer { margin-top: 30px; display: flex; justify-content: flex-end; }

/* 城市选择器 */
.city-selector { position: relative; }
.city-trigger {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border: 1.5px solid rgba(0,0,0,0.07);
  border-radius: 12px; background: rgba(0,0,0,0.01);
  cursor: pointer; transition: all 0.25s ease;
  font-size: 0.95rem;
}
.city-trigger:hover { border-color: var(--primary-color); }
.city-selector.open .city-trigger { border-color: var(--primary-color); background: #fff; box-shadow: 0 0 0 4px rgba(92,110,88,0.08); }
.city-trigger-icon { font-size: 0.85rem; color: var(--text-muted); opacity: 0.5; }
.city-value { color: var(--text-main); font-weight: 500; }
.city-placeholder { color: rgba(0,0,0,0.22); }
.city-arrow { font-size: 0.7rem; color: var(--text-muted); margin-left: auto; transition: transform 0.25s ease; }
.city-arrow.rotated { transform: rotate(180deg); }

.city-dropdown {
  position: absolute; top: calc(100% + 8px); left: 0; right: 0;
  z-index: 100; background: #fff; border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,0,0,0.05);
  padding: 16px; max-height: 380px; overflow-y: auto;
}
.city-search {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; margin-bottom: 12px;
  background: rgba(0,0,0,0.03); border-radius: 10px;
}
.city-search i { font-size: 0.78rem; color: var(--text-muted); }
.city-search-input {
  border: none; background: transparent; flex: 1;
  font-size: 0.88rem; color: var(--text-main); outline: none;
}
.city-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}
.city-card {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 14px; border-radius: 10px;
  border: 1px solid rgba(0,0,0,0.05); background: rgba(0,0,0,0.01);
  font-size: 0.85rem; color: var(--text-main); cursor: pointer;
  transition: all 0.2s ease;
}
.city-card:hover { background: rgba(92,110,88,0.06); border-color: var(--primary-color); color: var(--primary-color); }
.city-card.active { background: rgba(92,110,88,0.1); border-color: var(--primary-color); color: var(--primary-color); font-weight: 700; }
.city-emoji { font-size: 0.85rem; }
.city-breadcrumb { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,0.05); }
.breadcrumb-back {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.08);
  background: transparent; font-size: 0.8rem; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s;
}
.breadcrumb-back:hover { border-color: var(--primary-color); color: var(--primary-color); }
.breadcrumb-name { font-size: 0.88rem; font-weight: 600; color: var(--text-main); }
.city-empty { text-align: center; padding: 20px; color: var(--text-muted); font-size: 0.85rem; }
.dropdown-enter-active { transition: all 0.2s ease; }
.dropdown-leave-active { transition: all 0.15s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-6px); }

@media (max-width: 600px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
