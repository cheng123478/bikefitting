<template>
  <div class="create-team-page">
    <div class="container">
      <!-- 顶部返回 + 标题 -->
      <header class="create-header">
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <div class="header-text">
          <h1 class="page-title serif-title">组建你的车队</h1>
          <p class="header-sub">召集志同道合的骑手，一起破风前行</p>
        </div>
      </header>

      <form class="create-form" @submit.prevent="handleSubmit">
        <!-- 视觉预览区：封面 + 头像 + 名称实时预览 -->
        <div class="preview-banner">
          <div class="cover-upload" @click="triggerCover">
            <input
              ref="coverInput"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="e => handleImageUpload(e, 'cover')"
            />
            <template v-if="coverPreview">
              <img :src="coverPreview" class="cover-img" alt="封面预览" />
              <div class="cover-overlay">
                <i class="fas fa-camera"></i>
                <span>更换封面</span>
              </div>
            </template>
            <template v-else>
              <div class="cover-empty">
                <div class="cover-empty-icon">
                  <i class="fas fa-mountain-sun"></i>
                </div>
                <span>点击上传封面图</span>
              </div>
            </template>
            <div v-if="uploading.cover" class="upload-progress">
              <div class="progress-bar"><div class="progress-fill"></div></div>
            </div>
          </div>

          <!-- 头像浮层 -->
          <div class="avatar-float" @click="triggerAvatar">
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              class="file-input-hidden"
              @change="e => handleImageUpload(e, 'avatar')"
            />
            <div class="avatar-ring">
              <img v-if="avatarPreview" :src="avatarPreview" class="avatar-img" alt="头像预览" />
              <i v-else class="fas fa-camera avatar-icon"></i>
              <div v-if="uploading.avatar" class="avatar-loading">
                <i class="fas fa-spinner fa-spin"></i>
              </div>
            </div>
            <span class="avatar-edit-hint"><i class="fas fa-pen"></i></span>
          </div>
        </div>

        <!-- 表单主体 -->
        <div class="form-body">
          <!-- 基本信息卡片 -->
          <section class="form-card">
            <div class="card-label">
              <span class="label-dot"></span>
              基本信息
            </div>

            <div class="field-group">
              <label class="field-label">车队名称 <span class="required">*</span></label>
              <input
                v-model="form.name"
                type="text"
                class="field-input"
                placeholder="起个响亮的名字，让世界记住你们"
                maxlength="50"
              />
              <span class="field-counter">{{ form.name.length }}/50</span>
            </div>

            <div class="field-group">
              <label class="field-label">所在城市</label>
              <div class="city-selector" :class="{ open: cityDropdownOpen }">
                <div class="city-trigger" @click.stop="cityDropdownOpen = !cityDropdownOpen">
                  <i class="fas fa-map-marker-alt city-trigger-icon"></i>
                  <span v-if="form.location" class="city-value">{{ selectedProvinceName }} · {{ form.location }}</span>
                  <span v-else class="city-placeholder">选择车队所在城市</span>
                  <i class="fas fa-chevron-down city-arrow" :class="{ rotated: cityDropdownOpen }"></i>
                </div>
                <Transition name="dropdown">
                  <div v-if="cityDropdownOpen" class="city-dropdown" @click.stop>
                    <!-- 省份列表 -->
                    <template v-if="!selectedProvince">
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
                    <!-- 城市列表 -->
                    <template v-else>
                      <div class="city-breadcrumb">
                        <button type="button" class="breadcrumb-back" @click="selectedProvince = null">
                          <i class="fas fa-arrow-left"></i> 返回
                        </button>
                        <span class="breadcrumb-name">{{ selectedProvince.emoji }} {{ selectedProvince.name }}</span>
                      </div>
                      <div class="city-grid">
                        <button v-for="c in selectedProvince.cities" :key="c" type="button"
                          class="city-card" :class="{ active: form.location === c }" @click="selectCity(c)">
                          <span class="city-name">{{ c }}</span>
                        </button>
                      </div>
                    </template>
                  </div>
                </Transition>
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">车队宣言</label>
              <textarea
                v-model="form.description"
                class="field-textarea"
                placeholder="你们的骑行理念、目标或招新宣言..."
                rows="4"
                maxlength="500"
              ></textarea>
              <span class="field-counter">{{ form.description.length }}/500</span>
            </div>
          </section>

          <!-- 标签卡片 -->
          <section class="form-card">
            <div class="card-label">
              <span class="label-dot"></span>
              车队标签
              <span class="label-hint">选择最能代表车队的标签</span>
            </div>

            <div class="tag-grid">
              <button
                v-for="tag in presetTags"
                :key="tag.label"
                type="button"
                class="tag-chip"
                :class="{ active: form.tags.includes(tag.label) }"
                @click="toggleTag(tag)"
              >
                <i :class="tag.icon" class="chip-icon"></i>
                <span class="chip-text">{{ tag.label }}</span>
              </button>
            </div>
            <p v-if="form.tags.length >= 5" class="tag-limit-hint">
              <i class="fas fa-info-circle"></i> 最多选择 5 个标签
            </p>
          </section>

          <!-- 提交 -->
          <div class="form-actions">
            <button type="button" class="btn-ghost" @click="handleBack">取消</button>
            <button type="submit" class="btn-primary" :disabled="!isFormValid || submitting">
              <template v-if="submitting">
                <i class="fas fa-spinner fa-spin"></i> 创建中...
              </template>
              <template v-else>
                <i class="fas fa-paper-plane"></i> 创建车队
              </template>
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { createTeam, uploadTeamImage } from '@/api/team';
import { showToast } from '@/utils/toast';

const router = useRouter();
const submitting = ref(false);

const coverInput = ref(null);
const avatarInput = ref(null);
const coverPreview = ref('');
const avatarPreview = ref('');
const uploading = reactive({ cover: false, avatar: false });

const cityDropdownOpen = ref(false);
const citySearch = ref('');
const selectedProvince = ref(null);

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
  const q = citySearch.value.trim();
  if (!q) return provinceList;
  return provinceList.filter(p => p.name.includes(q));
});

const selectedProvinceName = computed(() => selectedProvince.value?.name || '');

const selectProvince = (p) => {
  selectedProvince.value = p;
  if (p.cities.length === 1) {
    form.location = p.cities[0];
    cityDropdownOpen.value = false;
    selectedProvince.value = null;
    citySearch.value = '';
  }
};

const selectCity = (name) => {
  form.location = name;
  cityDropdownOpen.value = false;
  selectedProvince.value = null;
  citySearch.value = '';
};

const closeCityDropdown = (e) => {
  const el = document.querySelector('.city-selector');
  if (el && !el.contains(e.target)) {
    cityDropdownOpen.value = false;
    selectedProvince.value = null;
    citySearch.value = '';
  }
};

const presetTags = [
  { label: '公路', icon: 'fas fa-road' },
  { label: '山地', icon: 'fas fa-mountain' },
  { label: '砾石', icon: 'fas fa-grip-lines' },
  { label: '爬坡', icon: 'fas fa-arrow-up-right-dots' },
  { label: '冲刺', icon: 'fas fa-bolt' },
  { label: '长途', icon: 'fas fa-route' },
  { label: '通勤', icon: 'fas fa-city' },
  { label: '休闲骑', icon: 'fas fa-mug-hot' },
  { label: '竞赛', icon: 'fas fa-trophy' },
  { label: '训练', icon: 'fas fa-dumbbell' },
  { label: '新手友好', icon: 'fas fa-seedling' },
  { label: '夜骑', icon: 'fas fa-moon' },
  { label: '周末', icon: 'fas fa-calendar-day' }
];

const form = reactive({
  name: '',
  location: '',
  description: '',
  tags: [],
  avatar: null,
  coverImage: null
});

const isFormValid = computed(() => form.name.trim().length >= 2);

const toggleTag = (tag) => {
  const idx = form.tags.indexOf(tag.label);
  if (idx >= 0) {
    form.tags.splice(idx, 1);
  } else if (form.tags.length < 5) {
    form.tags.push(tag.label);
  } else {
    showToast('最多选择5个标签', 'warning');
  }
};

const triggerCover = () => coverInput.value?.click();
const triggerAvatar = () => avatarInput.value?.click();

const handleImageUpload = async (e, type) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    if (type === 'cover') coverPreview.value = reader.result;
    else avatarPreview.value = reader.result;
  };
  reader.readAsDataURL(file);

  uploading[type] = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    const url = await uploadTeamImage(fd);
    if (type === 'cover') form.coverImage = url;
    else form.avatar = url;
  } catch {
    showToast(`${type === 'cover' ? '封面' : '头像'}上传失败，请重试`, 'error');
  } finally {
    uploading[type] = false;
  }
};

const handleBack = () => router.push('/peloton');

onMounted(() => document.addEventListener('click', closeCityDropdown));
onBeforeUnmount(() => document.removeEventListener('click', closeCityDropdown));

const handleSubmit = async () => {
  if (!isFormValid.value) {
    showToast('请至少填写车队名称（2字以上）', 'warning');
    return;
  }
  submitting.value = true;
  try {
    const result = await createTeam({
      name: form.name.trim(),
      location: form.location.trim() || null,
      description: form.description.trim() || null,
      tags: form.tags.length > 0 ? form.tags : null,
      avatar: form.avatar,
      coverImage: form.coverImage
    });
    showToast('车队创建成功！', 'success');
    router.push(`/peloton/${result.id}`);
  } catch (err) {
    showToast(err?.message || '创建失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.create-team-page {
  padding: 120px 0 100px;
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f7f5 0%, #fff 40%);
}

/* ── Header ── */
.create-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  max-width: 680px;
  margin-left: auto;
  margin-right: auto;
}
.back-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.08);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  color: var(--text-body);
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.back-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(92,110,88,0.12);
}
.header-text { flex: 1 }
.page-title {
  font-family: var(--font-serif);
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
  line-height: 1.3;
}
.header-sub {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin: 4px 0 0;
}

/* ── Form Layout ── */
.create-form {
  max-width: 680px;
  margin: 0 auto;
}

/* ── Preview Banner ── */
.preview-banner {
  position: relative;
  margin-bottom: 32px;
}
.cover-upload {
  position: relative;
  width: 100%;
  height: 220px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(135deg, #e8ebe6 0%, #d4d9d1 100%);
  transition: box-shadow 0.3s ease;
}
.cover-upload:hover {
  box-shadow: 0 8px 30px rgba(92,110,88,0.15);
}
.file-input-hidden { display: none }
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
}
.cover-overlay i { font-size: 1.1rem }
.cover-upload:hover .cover-overlay { opacity: 1 }

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
}
.cover-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.cover-empty-icon i { font-size: 1.4rem; color: var(--primary-color); opacity: 0.6 }
.cover-empty span { font-size: 0.85rem; font-weight: 500 }

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px 16px;
  background: linear-gradient(transparent, rgba(0,0,0,0.3));
}
.progress-bar {
  height: 3px;
  background: rgba(255,255,255,0.3);
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  width: 40%;
  background: #fff;
  border-radius: 2px;
  animation: progress-indeterminate 1.2s ease-in-out infinite;
}
@keyframes progress-indeterminate {
  0% { transform: translateX(-100%); width: 40% }
  100% { transform: translateX(350%); width: 40% }
}

/* ── Avatar Float ── */
.avatar-float {
  position: absolute;
  bottom: -32px;
  left: 32px;
  cursor: pointer;
  z-index: 2;
}
.avatar-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8ebe6, #d4d9d1);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  position: relative;
}
.avatar-float:hover .avatar-ring {
  box-shadow: 0 4px 20px rgba(92,110,88,0.25);
  transform: scale(1.05);
}
.avatar-img { width: 100%; height: 100%; object-fit: cover }
.avatar-icon { font-size: 1.2rem; color: var(--text-muted); opacity: 0.35 }
.avatar-loading {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  border-radius: 50%;
}
.avatar-edit-hint {
  position: absolute;
  bottom: -2px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(92,110,88,0.3);
}
.avatar-edit-hint i { font-size: 0.55rem; color: #fff }

/* ── Form Body ── */
.form-body {
  padding-top: 20px;
}

/* ── Form Card ── */
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 28px 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(0,0,0,0.04);
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
}
.card-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 24px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.label-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  flex-shrink: 0;
}
.label-hint {
  font-weight: 400;
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: none;
  letter-spacing: 0;
  margin-left: auto;
}

/* ── Fields ── */
.field-group { margin-bottom: 22px }
.field-group:last-child { margin-bottom: 0 }
.field-label {
  display: block;
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--text-body);
  margin-bottom: 8px;
}
.required { color: #d9534f; font-weight: 400 }

.input-with-icon { position: relative }
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.82rem;
  color: var(--primary-color);
  opacity: 0.5;
  pointer-events: none;
}
.field-input, .field-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid rgba(0,0,0,0.06);
  border-radius: 12px;
  font-size: 0.9rem;
  color: var(--text-main);
  background: #fafbf9;
  outline: none;
  transition: all 0.25s ease;
  font-family: inherit;
  box-sizing: border-box;
}
.field-input-icon { padding-left: 40px }
.field-input:focus, .field-textarea:focus {
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(92,110,88,0.06);
}
.field-input::placeholder, .field-textarea::placeholder {
  color: var(--text-muted);
  opacity: 0.4;
}
.field-textarea {
  resize: vertical;
  min-height: 100px;
  line-height: 1.7;
}
.field-counter {
  display: block;
  text-align: right;
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-top: 5px;
  opacity: 0.6;
}

/* ── Tag Grid ── */
.tag-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.tag-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid rgba(0,0,0,0.06);
  background: linear-gradient(135deg, #fafbf9 0%, #f3f5f1 100%);
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03), inset 0 1px 0 rgba(255,255,255,0.7);
  position: relative;
}
.tag-chip:hover {
  border-color: rgba(92,110,88,0.25);
  color: var(--primary-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(92,110,88,0.1), inset 0 1px 0 rgba(255,255,255,0.7);
}
.tag-chip.active {
  border-color: transparent;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6d816a 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(92,110,88,0.3), inset 0 1px 0 rgba(255,255,255,0.15);
  transform: translateY(-1px);
}
.chip-icon {
  font-size: 0.75rem;
  opacity: 0.55;
  transition: opacity 0.25s ease;
}
.tag-chip:hover .chip-icon { opacity: 0.8 }
.tag-chip.active .chip-icon { opacity: 0.9 }
.tag-limit-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin: 12px 0 0;
}
.tag-limit-hint i { margin-right: 4px; color: var(--primary-color); opacity: 0.5 }

/* ── Actions ── */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-bottom: 20px;
}
.btn-ghost {
  padding: 13px 32px;
  border-radius: 12px;
  border: 1.5px solid rgba(0,0,0,0.08);
  background: transparent;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.25s ease;
}
.btn-ghost:hover {
  border-color: rgba(0,0,0,0.15);
  color: var(--text-body);
}
.btn-primary {
  padding: 13px 36px;
  border-radius: 12px;
  border: none;
  background: var(--primary-color);
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  box-shadow: 0 4px 16px rgba(92,110,88,0.25);
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(92,110,88,0.3);
}
.btn-primary:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn-primary i { font-size: 0.78rem }

/* ── Responsive ── */
@media (max-width: 768px) {
  .create-team-page { padding: 100px 0 60px }
  .page-title { font-size: 1.3rem }
  .cover-upload { height: 170px; border-radius: 12px }
  .avatar-float { bottom: -28px; left: 20px }
  .avatar-ring { width: 60px; height: 60px }
  .form-card { padding: 22px 20px 16px; border-radius: 12px }
  .tag-chip { padding: 8px 14px; font-size: 0.78rem }
  .form-actions { flex-direction: column }
  .form-actions button { width: 100%; justify-content: center }
  .city-grid { grid-template-columns: repeat(3, 1fr) }
  .city-dropdown { max-height: 280px }
}

/* ── City Selector ── */
.city-selector { position: relative }
.city-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid rgba(0,0,0,0.06);
  border-radius: 12px;
  font-size: 0.9rem;
  color: var(--text-main);
  background: #fafbf9;
  cursor: pointer;
  transition: all 0.25s ease;
  box-sizing: border-box;
}
.city-trigger:hover { border-color: rgba(92,110,88,0.2) }
.city-selector.open .city-trigger {
  border-color: var(--primary-color);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(92,110,88,0.06);
}
.city-trigger-icon { font-size: 0.82rem; color: var(--primary-color); opacity: 0.5; flex-shrink: 0 }
.city-value { flex: 1; font-weight: 600 }
.city-placeholder { flex: 1; color: var(--text-muted); opacity: 0.4 }
.city-arrow {
  font-size: 0.65rem; color: var(--text-muted); opacity: 0.4;
  transition: transform 0.25s ease; flex-shrink: 0;
}
.city-arrow.rotated { transform: rotate(180deg) }

.city-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0; right: 0;
  background: #fff;
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 12px 40px rgba(0,0,0,0.1), 0 2px 8px rgba(0,0,0,0.04);
  z-index: 50;
  padding: 12px;
  max-height: 340px;
  overflow-y: auto;
}

.city-search {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; margin-bottom: 10px;
  border-radius: 10px; background: #f6f7f5;
}
.city-search i { font-size: 0.75rem; color: var(--text-muted); opacity: 0.4 }
.city-search-input {
  flex: 1; border: none; background: transparent;
  font-size: 0.82rem; color: var(--text-main); outline: none; font-family: inherit;
}
.city-search-input::placeholder { color: var(--text-muted); opacity: 0.35 }

.city-breadcrumb {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 4px 12px; margin-bottom: 6px;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.breadcrumb-back {
  display: flex; align-items: center; gap: 5px;
  padding: 4px 10px; border-radius: 8px;
  border: 1px solid rgba(0,0,0,0.06); background: #fafbf9;
  font-size: 0.72rem; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s ease; font-family: inherit;
}
.breadcrumb-back:hover { border-color: var(--primary-color); color: var(--primary-color) }
.breadcrumb-back i { font-size: 0.6rem }
.breadcrumb-name { font-size: 0.84rem; font-weight: 700; color: var(--text-main) }

.city-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px }
.city-card {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 6px; border-radius: 10px;
  border: 1.5px solid transparent; background: #fafbf9;
  cursor: pointer; transition: all 0.2s ease; font-family: inherit;
}
.city-card:hover {
  background: #f0f3ee; border-color: rgba(92,110,88,0.15);
  transform: translateY(-1px); box-shadow: 0 3px 10px rgba(92,110,88,0.08);
}
.city-card.active {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6d816a 100%);
  border-color: transparent; box-shadow: 0 4px 14px rgba(92,110,88,0.25);
}
.city-emoji { font-size: 1.2rem; line-height: 1 }
.city-name { font-size: 0.78rem; font-weight: 600; color: var(--text-body) }
.city-card.active .city-name { color: #fff }

.city-empty { text-align: center; font-size: 0.8rem; color: var(--text-muted); padding: 20px 0; opacity: 0.5 }
.city-empty i { margin-right: 6px }

.dropdown-enter-active, .dropdown-leave-active { transition: all 0.2s ease }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-8px) }
</style>
