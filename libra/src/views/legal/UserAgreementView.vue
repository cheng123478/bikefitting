<template>
  <div class="legal-page" ref="pageContainer">
    <!-- 阅读进度条 -->
    <div class="progress-bar" :style="{ width: readingProgress + '%' }"></div>

    <div class="legal-layout">
      <!-- 侧边栏导航 (仅桌面端) -->
      <aside class="legal-sidebar">
        <nav class="toc">
          <div class="toc-title">目录</div>
          <ul>
            <li v-for="item in tocItems" :key="item.id" :class="{ active: activeSection === item.id }">
              <a :href="'#' + item.id" @click.prevent="scrollToSection(item.id)">{{ item.text }}</a>
            </li>
          </ul>
        </nav>
      </aside>

      <div class="legal-main">
        <div class="legal-container">
          <header class="legal-header">
            <h1 class="legal-title">虚境骑适用户协议</h1>
            <div class="legal-meta">
              <p>更新日期：2025年8月20日</p>
              <p>生效日期：2025年8月27日</p>
            </div>
          </header>

          <div class="legal-content">
            <section id="section-1" class="legal-section">
              <h3>1、导言</h3>
              <p>欢迎你使用“虚境骑适”软件及相关服务！</p>
              <p>“虚境骑适”软件及相关服务，系指石家庄佳诚网络技术有限公司（以下简称“公司”或“我们”）以应用程序（App）形态方式向你提供的产品与服务（以下简称“本软件”）。本软件是一款集单车骑行运动Fitting（骑行姿态与器材设定分析）、运动数据记录与指导、社交互动于一体的综合性平台。</p>
              <p>本用户协议是你与公司之间就注册、登录、使用（以下统称“使用”）本软件及相关服务所订立的协议（以下简称“本协议”）。</p>
              <p>为了更好地为你提供服务，请你在开始使用本软件及相关服务之前，认真阅读并充分理解本协议，特别是涉及免除或者限制责任的条款、权利许可的条款、法律适用和争议解决条款等。其中，<strong>免除或者限制责任条款（特别是关于运动安全、健康风险、Fitting建议局限性等内容）将以加粗形式提示你注意，你应重点阅读。</strong></p>
              <p>如你不同意本协议，这将导致我们无法为你提供完整的产品和服务，你也可以选择停止使用。如你自主选择同意或使用本软件及相关服务，则视为你已充分理解本协议，并同意作为本协议的一方当事人接受本协议以及其他与本软件及相关服务相关的协议和规则（包括但不限于《隐私政策》、《社区管理规范》）的约束。</p>
              <p>为保障你的合法权益，请你在开始使用本软件及相关服务前，仔细阅读并充分理解本协议全部内容，特别是以下重点条款（已加粗提示）：</p>
              <ul>
                <li><strong>免除或限制公司责任的条款（含运动安全、Fitting 建议局限性、数据准确性等）；</strong></li>
                <li><strong>用户权利许可与信息使用条款；</strong></li>
                <li><strong>账号注销、数据处理相关条款；</strong></li>
                <li><strong>未成年人使用限制条款；</strong></li>
                <li><strong>法律适用与争议解决条款。</strong></li>
              </ul>
              <p>你理解并同意：本协议的成立、生效、履行及解释均适用中华人民共和国法律。《隐私政策》《社区管理规范》等为本协议的组成部分，与本协议具有同等法律效力，你使用本软件即视为一并接受上述全部文件的约束。如你不同意本协议或相关附属文件，你将无法使用本软件的全部或部分核心功能，请你立即停止使用；如你点击 “同意” 或继续使用本软件及相关服务，即视为你已充分理解并接受本协议及附属文件的全部约定。</p>
            </section>

            <section id="section-2" class="legal-section">
              <h3>2、 本软件及相关服务</h3>
              <p>2.1 主要适用人群：本软件及相关服务的主要适用人群是成年人。如你未满18周岁，请在法定监护人陪同下仔细阅读并充分理解本协议，并征得法定监护人的同意后使用本软件及相关服务。</p>
              <p>本软件及相关服务的适用人群为年满 13 周岁的自然人。</p>
              <p>若你年满 13 周岁 but 未满 18 周岁（未成年人），必须在法定监护人陪同下阅读本协议，经监护人明确同意后，方可注册和使用本软件；监护人应监督未成年人的使用行为，对其使用后果承担相应责任。</p>
              <p>若你未满 13 周岁，请勿注册或使用本软件，我们不向未满 13 周岁的未成年人提供服务。</p>
              <p>未成年人使用本软件时，不得参与高强度骑行训练、车辆器材调整等存在安全风险的操作，必须有监护人在场监护。</p>
              <p><strong>2.2 Fitting（骑行设定与姿态分析）功能的特别说明（核心风险提示）：</strong></p>
              <p>你充分知晓并同意：本软件提供的 Fitting 服务（含车辆设定建议、姿态调整分析）仅为通用参考工具，不构成医疗建议、康复指导或专业运动指导，具体如下：</p>
              <p>2.2.1 本软件提供的Fitting服务基于图像识别算法、传感器数据及通用运动力学模型。你理解并同意，软件输出的车辆设定建议（如座垫高度、把立长度等）及姿态调整建议仅供参考，不能完全替代线下专业Fitter（设定师）的实地测量与医学建议。</p>
              <p>2.2.2 因用户拍摄角度、设备精度、光线环境或用户自身身体结构特殊性（如长短腿、脊柱侧弯等隐形特征），分析结果可能存在偏差。你应结合自身实际骑行感受进行微调，切勿盲目强行适应软件建议的数据。</p>
              <p>2.2.3 <strong>你承诺：使用 Fitting 建议前已评估自身健康状况，必要时咨询医生或专业 Fitter；调整车辆或骑行姿态时，应循序渐进，结合自身骑行感受微调，若出现身体不适、疼痛、麻木等情况，须立即停止使用该设定，并及时就医；</strong></p>
              <p>2.2.4 你应保证提供的身高、体重、肢体长度等基础数据真实有效，因数据虚假导致的分析结果偏差及相关风险，由你自行承担。</p>
              <p><strong>2.3 关于运动与健康服务的风险提示：</strong></p>
              <p>2.3.1 本软件包含类似“Keep”的跟练课程、训练计划及运动数据记录功能。你应知晓体育运动本身包含固有的身体受伤风险（包括但不限于肌肉拉伤、关节损伤、心力衰竭等）。在开始任何训练计划或高强度骑行前，建议你评估自身健康状况，必要时咨询医生。</p>
              <p>2.3.2 本软件及相关服务不提供医疗诊断或治疗建议。 软件中显示的卡路里消耗、功率估算、心率区间等数据仅供参考，不作为医疗数据的依据。</p>
              <p>2.3.3 <strong>你应对自己的运动安全负责。在使用本软件进行室内或室外运动时，请确保周围环境安全、器材（如骑行台、自行车）安装牢固且状态良好。因用户自身操作不当、器材故障或环境因素导致的任何人身伤害或财产损失，公司不承担责任。</strong></p>
              <p>2.4 关于社交功能的说明：</p>
              <p>本软件提供类似“微博”的社交功能（包括动态发布、评论、点赞、关注等）。你在社区内的言论及行为需遵守法律法规及本协议第5条之规定。</p>
              <p>你理解，社区内容系用户自发上传，不代表公司的观点。</p>
              <p>你同意：公司可对社区内的合规内容进行合理编辑（如错别字修正、格式优化）或整合推广（如精选优质骑行攻略），推广时将注明用户来源。</p>
              <p>2.5 第三方产品与服务：</p>
              <p>你在使用本服务中由第三方（比如通过链接跳转的电商平台、第三方硬件设备连接等）提供的产品或服务时，请遵守第三方公布的用户协议、隐私政策等，我们对第三方产品及任何内容不承担责任。</p>
              <p>2.6 服务变更：</p>
              <p>2.6.1 公司将不定期提供本软件及相关服务的部分或全部更新（包括但不限于软件升级、功能强化、新服务开发）。我们会在可行的情况下以适当的方式提示你。</p>
              <p>2.6.2 若服务变更影响核心功能使用或加重用户义务，公司将给予你合理的适应期（至少 7 个工作日），你可选择继续使用（视为接受变更）或注销账号；</p>
              <p>2.6.3 因不可抗力（如自然灾害、网络故障）、法律要求或技术升级等原因导致服务暂时中断的，公司将尽快恢复服务，并及时通知你，不承担违约责任，但因故意或重大过失导致的除外。</p>
            </section>

            <section id="section-3" class="legal-section">
              <h3>3、关于“账号”</h3>
              <p>3.1 账号注册：本软件的核心功能如发布动态、保存Fitting数据、记录运动历史，需你登录账号后方可使用。你承诺注册时使用的身份信息真实、准确、合法、有效。若你提供虚假信息或冒用他人身份注册，公司有权暂停或终止账号服务，由此产生的法律责任由你承担。</p>
              <p>3.2 账号注销：在需要终止使用本软件账号服务时，你可以申请注销你的账号。注销成功后，账号记录、Fitting档案、运动数据、社交资产等将无法恢复。原注册使用的手机号在15天后才可以用以注册新账号。</p>
              <p>3.3 账号安全：你有责任维护个人账号、密码的安全性与保密性。因你保管不当导致遭受盗号或密码丢失，你将自行承担相应责任。</p>
              <p>3.4 禁止转让：你注册的账号仅限于你本人使用，未经公司书面同意，禁止以任何形式赠与、借用、出租、转让、售卖。</p>
              <p>3.5 长期未登录：如你在注册后连续超过一个月未登录账号使用，公司有权收回你的账号，以免造成资源浪费。</p>
            </section>

            <section id="section-4" class="legal-section">
              <h3>4、用户个人信息保护</h3>
              <p>保护用户个人信息是公司的基本原则之一。在使用本软件（特别是Fitting功能涉及的身高、体重、肢体长度数据，以及运动功能涉及的轨迹、心率数据）时，我们将依法保护你的隐私。更多内容请参见《隐私政策》。</p>
            </section>

            <section id="section-5" class="legal-section">
              <h3>5、信息内容使用规范</h3>
              <p>5.1 禁止行为：你应当对你使用本软件及相关服务的行为负责，不得进行以下行为：</p>
              <ul>
                <li>使用外挂、插件干扰软件正常运行（例如篡改运动数据刷榜）；</li>
                <li>对软件进行反向工程、反向汇编；</li>
                <li>恶意注册账号；</li>
                <li>侵犯他人知识产权、肖像权、隐私权、名誉权等合法权益；</li>
                <li>发布、传播危害国家安全、违反公序良俗、教唆犯罪等违法内容（具体详见 5.2）；</li>
                <li>未经许可采集、爬取本软件的用户数据、算法模型、课程内容等信息；</li>
                <li>其他违反法律法规或本协议约定的行为。</li>
                <li>其他违反法律法规的行为。</li>
              </ul>
              
              <p>5.2 信息内容规范（社区规则）：</p>
              <p>你制作、上传、发布、传播的信息（包括骑行照片、动态文字、评论等）应自觉遵守法律法规及公序良俗，不得包含以下内容：</p>
              <ul>
                <li>反对宪法原则、危害国家安全的；</li>
                <li>宣扬淫秽、色情、赌博、暴力、凶杀、恐怖或者教唆犯罪的；</li>
                <li>侮辱或者诽谤他人，侵害他人合法权益的；</li>
                <li>发布虚假运动数据、误导性骑行知识或具有高风险且未做安全提示的危险动作视频；</li>
                <li>散布广告及垃圾信息；</li>
                <li>其他违反“七条底线”要求的内容。</li>
              </ul>
              <p>5.3 投诉举报：如你发现社区内存在违规内容，可通过软件内的举报入口向我们反馈，我们将及时处理。</p>
            </section>

            <section id="section-6" class="legal-section">
              <h3>6、信息内容使用规范</h3>
              <p>6.1 用户内容授权：</p>
              <p>你理解并同意，你使用本软件及相关服务过程中上传、发布的全部内容（如骑行照片、Fitting对比图、心得体会）归属于你。为了更好地在社区内分享和推广骑行文化，你授予公司一项免费的、全球范围内的、非独占的权利，允许公司在“虚境骑适”平台及官方宣传渠道（如官方公众号、合作媒体）中使用你的公开内容。</p>
              <p>6.2禁止非法抓取：</p>
              <p>未经公司书面许可，任何第三方不得以任何方式（包括但不限于爬虫、模拟下载）窃取本软件内的用户数据、Fitting算法模型数据或运动课程内容。</p>
            </section>

            <section id="section-7" class="legal-section">
              <h3>7、知识产权</h3>
              <p>7.1 公司权利：</p>
              <p>公司在本软件及相关服务中提供的内容（包括但不限于软件代码、Fitting算法、界面设计、官方课程视频、官方Logo“虚境骑适”）的知识产权归公司所有。</p>
              <p>7.2 用户权利：</p>
              <p>用户在软件中生成的个性化数据（如Fitting报告、运动日志）属于用户个人信息，用户有权查询、复制或删除。但公司享有以下权利：</p>
              <ul>
                <li>基于该数据提供服务（如数据分析、趋势统计）；</li>
                <li>对数据进行匿名化、聚合分析（如生成行业骑行报告），聚合数据不得识别到具体用户；</li>
              </ul>
              <p>7.3 任何侵犯知识产权的行为，公司将追究其法律责任。</p>
            </section>

            <section id="section-8" class="legal-section">
              <h3>8、违约处理</h3>
              <p>针对你违反本协议的行为（如发布违规内容、恶意刷数据、骚扰他人），公司有权独立判断并视情况采取：</p>
              <ul>
                <li>删除违规信息；</li>
                <li>短期或永久封禁账号；</li>
                <li>限制使用特定功能（如禁止发布动态）；</li>
                <li>配合相关部门调查并追究法律责任。</li>
              </ul>
            </section>

            <section id="section-9" class="legal-section">
              <h3>9、免责声明</h3>
              <p><strong>9.1 数据准确性免责：</strong></p>
              <p>本软件提供的Fitting建议及运动数据分析仅基于算法推演，受限于手机摄像头精度、传感器误差及环境因素，我们不保证所有数据的绝对准确性。用户据此进行的器材购买、车辆调整或训练安排，风险由用户自担。</p>
              <p><strong>9.2 人身安全免责：</strong></p>
              <p>用户在使用本软件进行运动时，应自行承担运动损伤、意外事故（如摔车、器械损坏）的风险。对于因用户自身原因、不可抗力或第三方原因造成的损害，公司不承担赔偿责任。</p>
              <p>9.3 不可抗力：</p>
              <p>因自然灾害、网络故障、黑客攻击等不可抗力因素导致的服务中断或数据丢失，公司在法律允许范围内免责。</p>
            </section>

            <section id="section-10" class="legal-section">
              <h3>10、未成年人使用条款</h3>
              <p>10.1 若你是未满18周岁的未成年人，应在监护人指导下使用本软件。</p>
              <p>10.2 特别提示：青少年在进行高强度骑行运动或调整车辆器材时，必须有成年人在场监护，避免发生运动伤害或机械操作意外。</p>
              <p>10.3 本软件及相关服务的适用人群为年满 13 周岁的自然人。</p>
              <p>若你年满 13 周岁但未满 18 周岁（未成年人），必须在法定监护人陪同下阅读本协议，经监护人明确同意后，方可注册和使用本软件；监护人应监督未成年人的使用行为，对其使用后果承担相应责任。</p>
              <p>若你未满 13 周岁，请勿注册或使用本软件，我们不向未满 13 周岁的未成年人提供服务。</p>
              <p>未成年人使用本软件时，不得参与高强度骑行训练、车辆器材调整等存在安全风险的操作，必须有监护人在场监护。</p>
            </section>

            <section id="section-10" class="legal-section">
              <h3>11、其他</h3>
              <p>11.1 法律适用：本协议的成立、生效、履行及争议解决均适用中华人民共和国法律。</p>
              <p>11.2 管辖法院：若你与公司发生争议，应协商解决；协商不成，你同意将争议提交至河北省石家庄市裕华区人民法院诉讼解决。</p>
              <p>11.3 如本协议中的任何条款因任何原因完全或部分无效或不具有执行力，均不影响本协议其他条款的效力。</p>
              <p>11.4 协议修改：公司有权根据业务发展对本协议进行修订，并在软件内公布。继续使用软件即视为你接受修改后的协议。</p>
              <p>11.5 本协议及本协议中的任何条款内容的最终解释权及修改权归平台方所有。</p>
            </section>
            
            <p class="signature">[虚境骑适]</p>
          </div>

          <footer class="legal-footer">
            <button class="btn btn-secondary" @click="goBack">返回首页</button>
          </footer>
        </div>
      </div>
    </div>

    <!-- 浮动回顶按钮 -->
    <transition name="fade">
      <button v-show="showScrollTop" class="scroll-top-btn" @click="scrollToTop">
        <i class="fas fa-chevron-up"></i>
      </button>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const pageContainer = ref(null);
const readingProgress = ref(0);
const showScrollTop = ref(false);
const activeSection = ref('section-1');

const tocItems = [
  { id: 'section-1', text: '1、导言' },
  { id: 'section-2', text: '2、本软件及服务' },
  { id: 'section-3', text: '3、账号管理' },
  { id: 'section-4', text: '4、隐私保护' },
  { id: 'section-5', text: '5、内容使用' },
  { id: 'section-6', text: '6、信息内容使用规范' },
  { id: 'section-7', text: '7、知识产权' },
  { id: 'section-8', text: '8、违约处理' },
  { id: 'section-9', text: '9、免责声明' },
  { id: 'section-10', text: '10、未成年保护' },
  { id: 'section-11', text: '11、其他条款' }
];

const handleScroll = () => {
  const scrollTop = window.scrollY;
  const docHeight = document.documentElement.scrollHeight - window.innerHeight;
  readingProgress.value = (scrollTop / docHeight) * 100;
  showScrollTop.value = scrollTop > 400;

  tocItems.forEach(item => {
    const el = document.getElementById(item.id);
    if (el) {
      const rect = el.getBoundingClientRect();
      if (rect.top < 150) {
        activeSection.value = item.id;
      }
    }
  });
};

const scrollToSection = (id) => {
  const el = document.getElementById(id);
  if (el) {
    const top = el.offsetTop - 100;
    window.scrollTo({ top, behavior: 'smooth' });
  }
};

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const goBack = () => {
  router.push('/');
};

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.legal-page {
  min-height: 100vh;
  background: transparent;
  color: var(--text-main);
  font-family: var(--font-sans);
}

.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 4px;
  background: linear-gradient(to right, var(--primary-color), var(--primary-light));
  z-index: 1001;
}

.legal-layout {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  gap: 40px;
}

.legal-sidebar {
  width: 240px;
  position: sticky;
  top: 40px;
  align-self: flex-start;
}

@media (max-width: 1024px) {
  .legal-sidebar {
    display: none;
  }
}

.toc {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.toc-title {
  font-weight: 700;
  margin-bottom: 16px;
  font-size: 1.1rem;
  color: var(--text-main);
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.toc ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc li {
  margin-bottom: 8px;
  font-size: 0.9rem;
}

.toc a {
  color: var(--text-muted);
  text-decoration: none;
  transition: 0.2s;
  display: block;
  padding: 4px 0;
}

.toc li.active a {
  color: var(--primary-color);
  font-weight: 600;
  transform: translateX(4px);
}

.legal-main {
  flex: 1;
}

.legal-container {
  background: #fff;
  padding: 60px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.04);
}

.legal-header {
  border-bottom: 2px solid #f1f1f1;
  margin-bottom: 50px;
  padding-bottom: 30px;
  text-align: center;
}

.legal-title {
  font-size: 2.4rem;
  font-family: var(--font-serif);
  margin-bottom: 16px;
  color: var(--primary-color);
  font-weight: 800;
}

.legal-meta {
  color: var(--text-muted);
  font-size: 0.95rem;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.legal-content {
  line-height: 1.9;
  color: #3f4449;
  font-size: 1.05rem;
}

.legal-section {
  margin-bottom: 60px;
  scroll-margin-top: 120px;
}

.legal-section h3 {
  font-size: 1.6rem;
  font-weight: 700;
  margin-bottom: 24px;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 12px;
}

.legal-section h3::before {
  content: '';
  width: 4px;
  height: 24px;
  background: var(--primary-color);
  border-radius: 2px;
}

.signature {
  text-align: right;
  margin-top: 80px;
  font-weight: 800;
  font-size: 1.2rem;
  color: var(--primary-color);
}

.scroll-top-btn {
  position: fixed;
  right: 40px;
  bottom: 40px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.3s;
}

.scroll-top-btn:hover {
  transform: translateY(-5px);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.legal-footer {
  margin-top: 60px;
  padding-top: 30px;
  border-top: 1px solid #eee;
  text-align: center;
}

@media (max-width: 768px) {
  .legal-container {
    padding: 35px 20px;
  }
}
</style>
