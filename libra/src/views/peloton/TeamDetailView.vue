<template>
  <div class="team-detail-page">
    <div class="container">
      <header class="detail-header" v-scroll-reveal>
        <button class="back-btn" @click="handleBack">
          <i class="fas fa-arrow-left"></i> 返回破风社
        </button>
      </header>

      <div v-if="loading" class="loading-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载中...</p>
      </div>

      <template v-else-if="team">
        <section class="team-hero" v-scroll-reveal>
          <div class="hero-cover">
            <img :src="team.coverImage" :alt="team.name" class="cover-img" />
            <div class="cover-gradient"></div>
            <!-- 非队长：申请加入按钮 -->
            <button v-if="!isCreator" class="hero-join-btn" @click="handleJoin()">
              <i class="fas fa-plus"></i> 申请加入
            </button>
            <!-- 队长：操作菜单 -->
            <div v-else class="hero-actions">
              <div class="captain-menu-wrap" @click.stop>
                <div v-if="showCaptainMenu" class="captain-menu-backdrop" @click="showCaptainMenu = false"></div>
                <button class="hero-more-btn" @click="showCaptainMenu = !showCaptainMenu" title="车队管理">
                  <i class="fas fa-ellipsis-v"></i>
                </button>
                <Transition name="menu-fade">
                  <div v-if="showCaptainMenu" class="captain-dropdown">
                    <button class="captain-dropdown-item edit" @click="showCaptainMenu = false; handleEdit()">
                      <i class="fas fa-pen"></i> 编辑车队
                    </button>
                    <button v-if="otherMembers.length > 0" class="captain-dropdown-item leave" @click="showCaptainMenu = false; showLeaveModal = true">
                      <i class="fas fa-sign-out-alt"></i> 退出车队
                    </button>
                    <button class="captain-dropdown-item disband" @click="showCaptainMenu = false; showDisbandModal = true">
                      <i class="fas fa-trash-alt"></i> 解散车队
                    </button>
                  </div>
                </Transition>
              </div>
            </div>
          </div>

          <div class="hero-body">
            <div class="hero-avatar-ring">
              <img :src="team.avatar" :alt="team.name" class="hero-avatar" />
            </div>
            <div class="hero-text">
              <h1 class="team-name serif-title">{{ team.name }}</h1>
              <div class="hero-meta">
                <span class="meta-item"><i class="fas fa-map-marker-alt"></i> {{ team.location }}</span>
                <span class="meta-dot"></span>
                <span class="meta-item"><i class="far fa-calendar-alt"></i> {{ formatDate(team.createdAt) }} 创建</span>
              </div>
              <p class="team-description">{{ team.description }}</p>
            </div>
            <div class="hero-stats-side">
              <span class="side-stat"><i class="fas fa-user-friends"></i> {{ team.memberCount }} 成员</span>
              <span class="side-stat-sep">·</span>
              <span class="side-stat"><i class="fas fa-flag-checkered"></i> {{ team.activities ? team.activities.length : 5 }} 活动</span>
              <span class="side-stat-sep">·</span>
              <span class="side-stat"><i class="fas fa-road"></i> {{ formatDistance(totalDistance) }}</span>
            </div>
          </div>
        </section>

        <section class="tab-section" v-scroll-reveal.delay="100">
          <div class="tab-bar">
            <div class="capsule-nav">
              <button v-for="tab in visibleTabs" :key="tab.key" class="capsule-btn"
                :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
                <i :class="tab.icon"></i> {{ tab.label }}
              </button>
            </div>
            <button v-if="activeTab === 'events' && isCaptain" class="publish-btn"
              @click="handlePublishActivity">
              <i class="fas fa-plus-circle"></i> 发布活动
            </button>
            <!-- 成员搜索栏（显示在 tab-bar 右侧） -->
            <div v-if="activeTab === 'members'" class="tab-bar-search">
              <div class="search-input-wrap">
                <i class="fas fa-search search-icon"></i>
                <input
                  v-model="memberSearchInput"
                  type="text"
                  class="member-search-input"
                  placeholder="搜索昵称或标签…"
                  @keyup.enter="handleMemberSearch"
                />
                <button v-if="memberSearchInput" class="search-clear-btn" @click="clearMemberSearch" title="清空">
                  <i class="fas fa-times"></i>
                </button>
              </div>
              <button class="search-submit-btn" @click="handleMemberSearch">搜索</button>
            </div>
          </div>

          <!-- 车队活动 -->
          <div v-if="activeTab === 'events'" class="tab-content">
            <div v-if="teamActivities.length > 0" class="events-grid">
              <div v-for="item in teamActivities" :key="item.id" class="event-card">
                <div class="event-card-header">
                  <div class="activity-icon activity"><i class="fas fa-calendar-check"></i></div>
                  <div class="event-meta">
                    <span class="activity-date">{{ item.date }}</span>
                    <span v-if="item.location" class="event-location">
                      <i class="fas fa-map-marker-alt"></i> {{ item.location }}
                    </span>
                  </div>
                </div>
                <h4 class="event-title">{{ item.title }}</h4>
                <p v-if="item.desc" class="event-desc">{{ item.desc }}</p>
                <p v-if="item.content" class="event-desc">{{ item.content }}</p>
                <img v-if="item.image" :src="item.image" class="activity-image" alt="活动照片" />
                <span v-if="item.participants" class="activity-participants">
                  <i class="fas fa-user"></i> {{ item.participants }} 人已报名
                </span>
                <div v-if="isManager" class="event-card-actions">
                  <button type="button" class="event-act-btn" title="编辑" @click.stop="handleEditActivity(item.id)">
                    <i class="fas fa-pen"></i>
                  </button>
                  <button type="button" class="event-act-btn del" title="删除" @click.stop="handleDeleteActivity($event, item)">
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="empty-tab">
              <i class="fas fa-calendar-times"></i>
              <p>暂无活动</p>
            </div>
          </div>

          <!-- 成员列表 -->
          <div v-if="activeTab === 'members'" class="tab-content">
            <!-- 搜索结果统计 -->
            <div v-if="memberKeyword" class="member-search-info">
              <span>搜索 "<strong>{{ memberKeyword }}</strong>" 共找到 <strong>{{ memberTotal }}</strong> 名成员</span>
            </div>

            <!-- 待审核申请（管理员可见） -->
            <div v-if="isManager && pendingApplications.length > 0" class="apply-requests">
              <div class="apply-requests-header">
                <i class="fas fa-user-clock"></i>
                <span>待审核申请</span>
                <span class="apply-count">{{ pendingApplications.length }}</span>
              </div>
              <div class="apply-list">
                <div v-for="app in pendingApplications" :key="app.id" class="apply-card">
                  <img :src="app.avatar" :alt="app.name" class="apply-avatar" />
                  <div class="apply-info">
                    <span class="apply-name">{{ app.name }}</span>
                    <span class="apply-reason">{{ app.reason }}</span>
                    <span class="apply-time">{{ formatApplyTime(app.joinDate) }}</span>
                  </div>
                  <div class="apply-actions">
                    <button class="apply-btn approve" title="同意" @click="handleApprove(app)">
                      <i class="fas fa-check"></i>
                    </button>
                    <button class="apply-btn reject" title="拒绝" @click="handleReject(app)">
                      <i class="fas fa-times"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 成员加载中 -->
            <div v-if="memberLoading" class="member-loading">
              <i class="fas fa-spinner fa-spin"></i>
            </div>

            <div v-if="members.length > 0" class="members-grid">
              <div v-for="(member, idx) in members" :key="member.id"
                class="glass-member-card"
                :class="{ 'card-tag-open': editingTagMember?.id === member.id }"
                :style="{ animationDelay: idx * 0.06 + 's' }">
                <div class="glass-member-bg"></div>
                <div class="glass-member-content">
                  <div class="glass-avatar-wrap">
                    <img :src="member.avatar" :alt="member.name" class="glass-avatar" />
                    <span v-if="member.role === '队长'" class="glass-crown"><i class="fas fa-crown"></i></span>
                  </div>
                  <div class="glass-member-info">
                    <span class="glass-name">{{ member.name }}</span>
                    <span class="glass-role" :class="{ 'is-leader': member.role === '队长', 'is-admin': member.role === '管理员' }">
                      {{ member.role }}
                    </span>
                    <span v-if="member.tag" class="glass-tag" :class="[getTagClass(member.tag), { 'tag-editable': canEditTag(member) }]" @click.stop="handleTagClick(member)">
                      <span class="tag-label"><i :class="getTagIcon(member.tag)"></i> {{ member.tag }}</span>
                      <span v-if="canEditTag(member)" class="tag-hover-edit"><i class="fas fa-pen"></i></span>
                    </span>
                    <!-- 标签选择下拉 -->
                    <div v-if="editingTagMember?.id === member.id" class="tag-dropdown" @click.stop>
                      <div v-for="t in allTags" :key="t.name" class="tag-option" :class="[getTagClass(t.name), { 'is-current': t.name === member.tag }]" @click.stop="changeMemberTag(member, t.name)">
                        <i :class="getTagIcon(t.name)"></i> {{ t.name }}
                      </div>
                    </div>
                  </div>
                  <div class="glass-join">
                    <i class="far fa-calendar-check"></i>
                    <span>{{ formatDate(member.joinDate) }}</span>
                  </div>
                  <!-- 队长管理操作 -->
                  <div v-if="isCaptain && member.role !== '队长'" class="glass-actions">
                    <button v-if="member.role === '成员'" class="glass-act-btn promote" title="设为管理员" @click.stop="handlePromote(member)">
                      <i class="fas fa-arrow-up"></i>
                    </button>
                    <button v-if="member.role === '管理员'" class="glass-act-btn demote" title="撤销管理员" @click.stop="handleDemote(member)">
                      <i class="fas fa-arrow-down"></i>
                    </button>
                    <button class="glass-act-btn kick" title="请离车队" @click.stop="handleKick(member)">
                      <i class="fas fa-user-minus"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tab"><i class="fas fa-user-plus"></i><p>{{ memberKeyword ? '未找到匹配的成员' : '暂无成员' }}</p></div>

            <!-- 分页控件 -->
            <div v-if="memberPages > 1" class="member-pagination">
              <button
                class="page-btn"
                :disabled="memberPage <= 1"
                @click="handleMemberPageChange(memberPage - 1)"
              >
                <i class="fas fa-chevron-left"></i> 上一页
              </button>
              <div class="page-numbers">
                <button
                  v-for="p in visibleMemberPages"
                  :key="p"
                  class="page-num-btn"
                  :class="{ active: p === memberPage, ellipsis: p === '...' }"
                  :disabled="p === '...'"
                  @click="p !== '...' && handleMemberPageChange(p)"
                >
                  {{ p }}
                </button>
              </div>
              <button
                class="page-btn"
                :disabled="memberPage >= memberPages"
                @click="handleMemberPageChange(memberPage + 1)"
              >
                下一页 <i class="fas fa-chevron-right"></i>
              </button>
            </div>
          </div>

          <!-- 车队车库 -->
          <div v-if="activeTab === 'garage'" class="tab-content garage-tab">
            <!-- 车队装备总览栏 -->
            <div class="garage-overview">
              <div class="overview-stat">
                <span class="stat-num">{{ garageBikes.length }}</span>
                <span class="stat-label">总计</span>
              </div>
              <div class="overview-divider"></div>
              <div v-for="st in garageStats" :key="st.type" class="overview-stat">
                <span class="stat-num">{{ st.count }}</span>
                <span class="stat-label">{{ st.type }}</span>
              </div>
              <!-- 添加我的单车（仅车队成员可见） -->
              <button v-if="isMember" class="garage-add-btn" @click="openAddBikeModal">
                <i class="fas fa-plus"></i> 添加我的单车
              </button>
            </div>
            <!-- 筛选胶囊 -->
            <div class="garage-filters">
              <button v-for="f in garageFilters" :key="f.key" class="filter-capsule"
                :class="{ active: garageFilter === f.key }" @click="garageFilter = f.key">
                <i :class="f.icon"></i> {{ f.label }}
              </button>
            </div>
            <!-- 车辆网格 -->
            <div v-if="filteredGarageBikes.length > 0" class="garage-grid">
              <div v-for="bike in filteredGarageBikes" :key="bike.id" class="garage-bike-card"
                :style="{ '--bike-color': bike.color }" @click="openBikeShowcase(bike)">
                <!-- 背景色晕染 -->
                <div class="bike-card-glow"></div>
                <!-- 移除按钮（车主本人或管理员可见） -->
                <button v-if="isBikeOwner(bike) || isManager"
                  class="bike-remove-btn"
                  :disabled="removingBikeId === bike.bikeId"
                  title="从车队车库移除"
                  @click.stop="handleRemoveBikeFromGarage(bike)">
                  <i class="fas" :class="removingBikeId === bike.bikeId ? 'fa-circle-notch fa-spin' : 'fa-times'"></i>
                </button>
                <!-- 车主行 -->
                <div class="bike-card-body">
                  <div class="bike-owner-row">
                    <div class="bike-avatar-ring" :style="{ borderColor: bike.color }">
                      <img :src="bike.ownerAvatar" :alt="bike.ownerName" class="bike-avatar" />
                    </div>
                    <span class="bike-owner-name">{{ bike.ownerName }}</span>
                    <span class="bike-type-tag" :class="'type-' + bike.type">{{ bike.type }}</span>
                  </div>
                  <!-- 车辆信息 -->
                  <div class="bike-name-row">
                    <span class="bike-brand">{{ bike.brand }}</span>
                    <span class="bike-dot">·</span>
                    <span class="bike-model">{{ bike.model }}</span>
                  </div>
                  <div class="bike-bottom-row">
                    <div class="bike-size-row">
                      <i class="fas fa-ruler-horizontal"></i>
                      <span>{{ bike.frameSize }} 码</span>
                    </div>
                    <!-- 装饰单车剪影 -->
                    <i class="fas fa-bicycle bike-ghost-icon"></i>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-tab"><i class="fas fa-bicycle"></i><p>暂无符合筛选条件的车辆</p></div>
          </div>

          <!-- 车队动态 -->
          <div v-if="activeTab === 'activity'" class="tab-content">
            <div v-if="teamNotices.length > 0 || isManager" class="feed-section">
              <div class="feed-section-header notice-header">
                <i class="fas fa-bullhorn"></i> 公告
                <button v-if="isManager" class="notice-publish-btn" @click="handlePublishNotice">
                  <i class="fas fa-plus"></i> 发布公告
                </button>
              </div>
              <div v-if="teamNotices.length > 0" class="notice-list">
                <div v-for="item in teamNotices" :key="item.id" class="notice-item"
                  :class="{ expanded: expandedNotice === item.id }" @click="toggleNotice(item.id)">
                  <div class="notice-row">
                    <span class="notice-dot"></span>
                    <span class="notice-title">{{ item.title }}</span>
                    <span class="notice-date">{{ item.date }}</span>
                    <i class="fas fa-chevron-down notice-arrow"></i>
                    <!-- 管理员操作按钮 -->
                    <div v-if="isManager" class="notice-actions" @click.stop>
                      <button type="button" class="notice-act-btn" title="编辑" @click="handleEditNotice(item)">
                        <i class="fas fa-pen"></i>
                      </button>
                      <button type="button" class="notice-act-btn del" title="删除" @click="handleDeleteNotice(item)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </div>
                  </div>
                  <div v-if="expandedNotice === item.id" class="notice-body">
                    <p v-if="item.content">{{ item.content }}</p>
                  </div>
                </div>
              </div>
              <p v-else class="notice-empty">暂无公告</p>
            </div>

            <!-- 发布动态区 -->
            <div v-if="isMember" class="feed-section">
              <div class="feed-section-header notice-header post-section-header">
                <i class="fas fa-stream"></i> 动态
              </div>
              <div class="post-compose">
                <textarea v-model="publishPostText" class="post-compose-input"
                  placeholder="分享车队动态…" maxlength="500"></textarea>
                <!-- 图片预览网格 -->
                <div v-if="publishPostImages.length > 0" class="compose-image-grid">
                  <div v-for="(img, idx) in publishPostImages" :key="idx" class="compose-img-wrap">
                    <img :src="img" class="compose-preview-img" alt="" />
                    <button type="button" class="compose-img-remove" @click="removePostImage(idx)">
                      <i class="fas fa-times"></i>
                    </button>
                  </div>
                </div>
                <div class="post-compose-footer">
                  <div class="compose-footer-left">
                    <button type="button" class="compose-img-btn" :disabled="publishPostImages.length >= 9 || uploadingPostImg"
                      @click="triggerPostImage">
                      <i class="fas fa-image"></i>
                      <span v-if="uploadingPostImg">上传中…</span>
                      <span v-else>{{ publishPostImages.length > 0 ? `${publishPostImages.length}/9` : '图片' }}</span>
                    </button>
                    <input ref="postFileInput" type="file" accept="image/*" multiple hidden @change="handlePostImageSelect" />
                    <span class="post-compose-count">{{ publishPostText.length }}/500</span>
                  </div>
                  <button class="post-compose-btn" :disabled="(!publishPostText.trim() && publishPostImages.length === 0) || publishingPost"
                    @click="handlePublishPost">
                    <i class="fas fa-paper-plane"></i> {{ publishingPost ? '发布中…' : '发布' }}
                  </button>
                </div>
              </div>
            </div>

            <div v-if="teamPosts.length > 0" class="feed-section">
              <div class="activity-list">
                <div v-for="item in teamPosts" :key="item.id" class="activity-item">
                  <img :src="item.authorAvatar || 'https://i.pravatar.cc/100?img=0'" class="post-avatar" alt="" />
                  <div class="activity-body">
                    <div class="activity-header">
                      <span class="activity-user">{{ item.authorName }}</span>
                      <button v-if="isMember && userStore.userInfo && String(item.authorId) === String(userStore.userInfo.id)"
                        class="post-del-btn" title="删除动态" @click.stop="handleDeletePost(item)">
                        <i class="fas fa-trash-alt"></i>
                      </button>
                    </div>
                    <p v-if="item.content" class="activity-desc">{{ item.content }}</p>
                    <div v-if="item.images && item.images.length > 0" class="post-image-grid">
                      <img v-for="(img, idx) in item.images.slice(0, 9)" :key="idx" :src="img" class="post-grid-img" alt="" />
                    </div>
                    <div class="post-actions">
                      <button type="button" class="post-action-btn" :class="{ liked: getPostInter(item).liked }"
                        @click.stop="toggleLike(item)">
                        <i :class="getPostInter(item).liked ? 'fas fa-heart' : 'far fa-heart'"></i>
                        <span v-if="getPostInter(item).likeCount > 0">{{ getPostInter(item).likeCount }}</span>
                      </button>
                      <button type="button" class="post-action-btn" :class="{ active: getPostInter(item).showComment }"
                        @click.stop="toggleCommentInput(item)">
                        <i class="far fa-comment"></i>
                        <span v-if="getPostInter(item).comments.length > 0">{{ getPostInter(item).comments.length }}</span>
                      </button>
                      <span class="post-time">{{ formatDateTime(item.createdAt) }}</span>
                    </div>
                    <div v-if="getPostInter(item).comments.length > 0" class="post-comments">
                      <div v-for="(c, ci) in getPostInter(item).comments" :key="ci" class="comment-item">
                        <span class="comment-user">{{ c.user }}</span>
                        <span class="comment-text">{{ c.text }}</span>
                        <button type="button"
                          v-if="isMember && userStore.userInfo && c.userId === String(userStore.userInfo.id)"
                          class="comment-del" title="删除" @click.stop="deleteComment(item, ci)">
                          <i class="fas fa-times"></i>
                        </button>
                      </div>
                    </div>
                    <div v-if="getPostInter(item).showComment" class="comment-input-row">
                      <input v-model="getPostInter(item).commentText" type="text" class="comment-input"
                        placeholder="写评论…" @keyup.enter="submitComment(item)" />
                      <button type="button" class="comment-submit" @click.stop="submitComment(item)">
                        <i class="fas fa-paper-plane"></i>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="postsLoading" class="empty-tab">
              <i class="fas fa-spinner fa-spin"></i><p>加载动态中…</p>
            </div>
            <div v-else-if="teamNotices.length === 0 && teamPosts.length === 0" class="empty-tab">
              <i class="fas fa-inbox"></i><p>暂无动态</p>
            </div>
          </div>
        </section>
      </template>

      <div v-else-if="!loading" class="empty-state">
        <i class="fas fa-exclamation-circle"></i>
        <p>车队不存在或已被解散</p>
        <button class="back-btn-secondary" @click="handleBack">返回破风社</button>
      </div>
    </div>

    <!-- 删除确认弹窗（使用独立类名，避免与全局 modal-overlay 冲突） -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="del-confirm-overlay" @click.self="cancelDelete">
        <div class="del-confirm-dialog">
          <div class="del-confirm-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h3 class="del-confirm-title">确认删除</h3>
          <p class="del-confirm-body">您确定删除活动「{{ deletingActivity?.title }}」吗？</p>
          <div class="del-confirm-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelDelete">取消</button>
            <button type="button" class="del-confirm-btn del-confirm-btn-confirm" @click.stop="confirmDelete">确定删除</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 请离车队确认弹窗 -->
    <Teleport to="body">
      <div v-if="showKickModal" class="del-confirm-overlay" @click.self="cancelKick">
        <div class="del-confirm-dialog">
          <div class="del-confirm-icon kick-icon">
            <i class="fas fa-user-minus"></i>
          </div>
          <h3 class="del-confirm-title">请离车队</h3>
          <p class="del-confirm-body">您确定将 <strong>「{{ kickingMember?.name }}」</strong> 请离车队吗？该操作需要成员重新申请才能归队。</p>
          <div class="del-confirm-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelKick">再想想</button>
            <button type="button" class="del-confirm-btn del-confirm-btn-confirm" @click.stop="confirmKick">确认请离</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 删除公告确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDeleteNoticeModal" class="del-confirm-overlay" @click.self="cancelDeleteNotice">
        <div class="del-confirm-dialog">
          <div class="del-confirm-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h3 class="del-confirm-title">确认删除</h3>
          <p class="del-confirm-body">您确定删除公告「{{ deletingNotice?.title }}」吗？</p>
          <div class="del-confirm-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelDeleteNotice">取消</button>
            <button type="button" class="del-confirm-btn del-confirm-btn-confirm" @click.stop="confirmDeleteNotice">确定删除</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 删除动态确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDeletePostModal" class="del-confirm-overlay" @click.self="cancelDeletePost">
        <div class="del-confirm-dialog">
          <div class="del-confirm-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h3 class="del-confirm-title">确认删除</h3>
          <p class="del-confirm-body">您确定删除该动态吗？删除后相关内容将不再显示。</p>
          <div class="del-confirm-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelDeletePost">取消</button>
            <button type="button" class="del-confirm-btn del-confirm-btn-confirm" @click.stop="confirmDeletePost">确定删除</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 申请加入车队弹窗 -->
    <Teleport to="body">
      <div v-if="showJoinModal" class="del-confirm-overlay" @click.self="cancelJoin">
        <div class="join-modal-dialog">
          <div class="join-modal-icon"><i class="fas fa-paper-plane"></i></div>
          <h3 class="join-modal-title">申请加入「{{ team?.name }}」</h3>
          <p class="join-modal-subtitle">请填写申请理由，车队管理员将审核您的申请</p>
          <textarea v-model="joinReason" class="join-reason-input" placeholder="例如：我是一名骑行爱好者，希望能和车队一起训练和参加活动..." rows="4" maxlength="200"></textarea>
          <span class="join-char-count">{{ joinReason.length }}/200</span>
          <div class="join-modal-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelJoin">取消</button>
            <button type="button" class="join-submit-btn" :disabled="!joinReason.trim()" @click.stop="submitJoin">
              <i class="fas fa-paper-plane"></i> 提交申请
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 退出车队 - 选择新队长弹窗 -->
    <Teleport to="body">
      <div v-if="showLeaveModal" class="del-confirm-overlay" @click.self="cancelLeave">
        <div class="leave-team-dialog">
          <div class="leave-dialog-header">
            <i class="fas fa-crown" style="color: #ff9800; font-size: 1.6rem;"></i>
            <h3 class="leave-dialog-title">退出车队</h3>
          </div>
          <p class="leave-dialog-desc">您确定要退出 <strong>「{{ team?.name }}」</strong> 吗？请选择一位成员作为新队长。</p>
          <div class="new-captain-list">
            <div
              v-for="m in otherMembers"
              :key="m.userId || m.id"
              class="new-captain-item"
              :class="{ 'is-selected': selectedNewCaptain?.id === m.id }"
              @click="selectedNewCaptain = (selectedNewCaptain?.id === m.id) ? null : m"
            >
              <img :src="m.avatar" :alt="m.name" class="new-captain-avatar" />
              <div class="new-captain-info">
                <span class="new-captain-name">{{ m.name }}</span>
                <span class="new-captain-role">{{ m.role }}</span>
              </div>
              <i v-if="selectedNewCaptain?.id === m.id" class="fas fa-check-circle new-captain-check"></i>
            </div>
          </div>
          <div class="leave-dialog-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="cancelLeave">取消</button>
            <button
              type="button"
              class="leave-confirm-btn"
              :disabled="!selectedNewCaptain || leavingTeam"
              @click.stop="confirmLeave"
            >
              <template v-if="leavingTeam">
                <i class="fas fa-spinner fa-spin"></i> 处理中...
              </template>
              <template v-else>
                <i class="fas fa-check"></i> 确认退出
              </template>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 添加我的单车至车队车库弹窗 -->
    <Teleport to="body">
      <Transition name="showcase-zoom">
        <div v-if="showAddBikeModal" class="add-bike-modal-overlay" @click.self="closeAddBikeModal">
          <div class="add-bike-modal">
            <div class="add-bike-modal-header">
              <h3><i class="fas fa-bicycle"></i> 添加我的单车</h3>
              <p>选择您个人车库中的单车，同步至车队车库</p>
              <button class="add-bike-modal-close" @click="closeAddBikeModal">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="add-bike-modal-body">
              <!-- 加载中 -->
              <div v-if="myBikesLoading" class="add-bike-loading">
                <i class="fas fa-circle-notch fa-spin"></i> 正在打开车库...
              </div>
              <!-- 无可录入的单车 -->
              <div v-else-if="availableMyBikes.length === 0" class="add-bike-empty">
                <i class="fas fa-info-circle"></i>
                <p v-if="myBikes.length === 0">您的个人车库暂无单车，<br/>可前往「单车展厅」或「我的车库」先录入</p>
                <p v-else>您的单车已全部录入车队车库</p>
              </div>
              <!-- 可录入的单车列表 -->
              <div v-else class="add-bike-list">
                <div v-for="bike in availableMyBikes" :key="bike.id" class="add-bike-item">
                  <div class="add-bike-info">
                    <div class="add-bike-img-wrap" v-if="bike.imageUrl">
                      <img :src="bike.imageUrl" class="add-bike-img" />
                    </div>
                    <div class="add-bike-img-wrap add-bike-img-placeholder" v-else>
                      <i class="fas fa-bicycle"></i>
                    </div>
                    <div class="add-bike-text">
                      <span class="add-bike-name">
                        {{ bike.brandModel || `${bike.brand || ''} ${bike.model || ''}`.trim() || '未知车型' }}
                      </span>
                      <span class="add-bike-meta">
                        <span v-if="bike.frameSize">{{ bike.frameSize }} 码</span>
                        <span v-if="bike.remark"> · {{ bike.remark }}</span>
                      </span>
                    </div>
                  </div>
                  <button class="add-bike-action-btn"
                    :disabled="addingBikeId === bike.id"
                    @click="handleAddBikeToGarage(bike)">
                    <i class="fas" :class="addingBikeId === bike.id ? 'fa-circle-notch fa-spin' : 'fa-plus'"></i>
                    {{ addingBikeId === bike.id ? '录入中' : '录入' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 解散车队确认弹窗 -->
    <Teleport to="body">
      <div v-if="showDisbandModal" class="del-confirm-overlay" @click.self="showDisbandModal = false">
        <div class="del-confirm-dialog">
          <div class="del-confirm-icon" style="background: rgba(220,53,69,0.1); color: #dc3545;">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h3 class="del-confirm-title">确认解散车队</h3>
          <p class="del-confirm-body">您确定解散 <strong>「{{ team?.name }}」</strong> 吗？所有成员记录和活动数据将被永久删除，此操作不可恢复。</p>
          <div class="del-confirm-actions">
            <button type="button" class="del-confirm-btn del-confirm-btn-cancel" @click.stop="showDisbandModal = false">再想想</button>
            <button type="button" class="del-confirm-btn del-confirm-btn-confirm" @click.stop="confirmDisband">确认解散</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>

  <!-- 解散车队告别页 -->
  <Teleport to="body">
    <Transition name="farewell">
      <div v-if="showFarewell" class="farewell-page">
        <div class="farewell-content">
          <div class="farewell-icon">
            <i class="fas fa-heart"></i>
          </div>
          <h2 class="farewell-title">车队已解散</h2>
          <p class="farewell-quote">感恩相遇，有缘再会</p>
          <p class="farewell-sub">每一段并肩破风的旅程，都值得被记住</p>
          <button class="farewell-btn" @click="goHome">
            <i class="fas fa-arrow-left"></i> 返回破风社
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTeamDetail, getTeamMembers, getTeamGarage, addBikeToTeamGarage, removeBikeFromTeamGarage, disbandTeam, joinTeam, getPendingApplications, approveJoin, rejectJoin, updateMemberTag, promoteAdmin, revokeAdmin, kickMember, captainLeaveTeam, getNoticeBoard, deleteNotice, getPosts, publishPost, deletePost, toggleLike as toggleLikeApi, addComment as addCommentApi, deleteComment as deleteCommentApi, uploadTeamImage } from '@/api/team'
import { getBikeByModel } from '@/api/bikeShowroom'
import { getMyGarage } from '@/api/userBike'
import { showToast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const team = ref(null)
const members = ref([])
const loading = ref(true)

// 成员列表分页与搜索状态
const MEMBER_PAGE_SIZE = 20
const memberPage = ref(1)
const memberTotal = ref(0)
const memberPages = ref(0)
const memberKeyword = ref('')
const memberSearchInput = ref('')
const memberLoading = ref(false)
const activeTab = ref('events')
const totalDistance = ref(12860)
const expandedNotice = ref(null)
const isMock = ref(false)
const showDeleteModal = ref(false)
const deletingActivity = ref(null)
const showKickModal = ref(false)
const kickingMember = ref(null)
const editingTagMember = ref(null)
const allTags = [
  { name: '全能' }, { name: '破风手' }, { name: '爬坡手' },
  { name: '冲刺手' }, { name: '计时赛专家' }, { name: '铁三选手' },
  { name: '耐力王' }, { name: '山地达人' }, { name: '夜骑达人' },
  { name: '通勤专家' }, { name: '新秀' }
]
const showDeleteNoticeModal = ref(false)
const deletingNotice = ref(null)
const showJoinModal = ref(false)
const joinReason = ref('')
const pendingApplications = ref([])
const garageBikes = ref([])
const garageFilter = ref('all')
const showAddBikeModal = ref(false)
const myBikes = ref([])
const myBikesLoading = ref(false)
const addingBikeId = ref(null)
const removingBikeId = ref(null)

const postInteractions = reactive({})
const getPostInter = (post) => {
  if (!postInteractions[post.id]) {
    postInteractions[post.id] = {
      liked: post.liked || false,
      likeCount: post.likeCount || 0,
      showComment: false,
      commentText: '',
      comments: (post.comments || []).map(c => ({
        id: c.id,
        userId: String(c.userId),
        user: c.userName || '未知用户',
        text: c.content || ''
      }))
    }
  }
  return postInteractions[post.id]
}
const toggleLike = async (post) => {
  const p = getPostInter(post)
  try {
    const res = await toggleLikeApi(team.value.id, post.id)
    p.liked = res.liked
    p.likeCount = res.liked ? p.likeCount + 1 : Math.max(0, p.likeCount - 1)
  } catch (err) {
    showToast(err.message || '操作失败', 'error')
  }
}
const toggleCommentInput = (post) => { const p = getPostInter(post); p.showComment = !p.showComment }
const submitComment = async (post) => {
  const p = getPostInter(post)
  const text = p.commentText.trim()
  if (!text) return
  try {
    const c = await addCommentApi(team.value.id, post.id, { content: text })
    p.comments.push({ id: c.id, userId: String(c.userId), user: c.userName || '我', text: c.content })
    p.commentText = ''
    post.commentCount = (post.commentCount || 0) + 1
  } catch (err) {
    showToast(err.message || '评论失败', 'error')
  }
}
const deleteComment = async (post, ci) => {
  const p = getPostInter(post)
  const c = p.comments[ci]
  if (!c?.id) return
  try {
    await deleteCommentApi(team.value.id, post.id, c.id)
    p.comments.splice(ci, 1)
    post.commentCount = Math.max(0, (post.commentCount || 0) - 1)
  } catch (err) {
    showToast(err.message || '删除失败', 'error')
  }
}
const showDeletePostModal = ref(false)
const deletingPost = ref(null)
const handleDeletePost = (post) => {
  deletingPost.value = post
  showDeletePostModal.value = true
}
const cancelDeletePost = () => {
  showDeletePostModal.value = false
  deletingPost.value = null
}
const confirmDeletePost = async () => {
  const post = deletingPost.value
  showDeletePostModal.value = false
  deletingPost.value = null
  if (!post) return
  try {
    await deletePost(team.value.id, post.id)
    teamPosts.value = teamPosts.value.filter(p => p.id !== post.id)
    showToast('动态已删除', 'success')
  } catch (err) {
    showToast(err.message || '删除失败', 'error')
  }
}
const handlePublishPost = async () => {
  const text = publishPostText.value.trim()
  if (!text && publishPostImages.value.length === 0) return
  publishingPost.value = true
  try {
    const payload = { content: text || '' }
    if (publishPostImages.value.length > 0) payload.images = publishPostImages.value
    const newPost = await publishPost(team.value.id, payload)
    teamPosts.value.unshift(newPost)
    publishPostText.value = ''
    publishPostImages.value = []
    showToast('动态发布成功', 'success')
  } catch (err) {
    showToast(err.message || '发布失败', 'error')
  } finally {
    publishingPost.value = false
  }
}
const triggerPostImage = () => { postFileInput.value?.click() }
const handlePostImageSelect = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  const remaining = 9 - publishPostImages.value.length
  const toUpload = files.slice(0, remaining)
  uploadingPostImg.value = true
  for (const file of toUpload) {
    try {
      const fd = new FormData()
      fd.append('file', file)
      const url = await uploadTeamImage(fd)
      publishPostImages.value.push(url)
    } catch {
      showToast('图片上传失败，请重试', 'error')
    }
  }
  uploadingPostImg.value = false
  // 重置 input 以允许再次选择同一文件
  if (postFileInput.value) postFileInput.value.value = ''
}
const removePostImage = (idx) => { publishPostImages.value.splice(idx, 1) }
const fetchPosts = async () => {
  if (!team.value?.id || !isMember.value) return
  postsLoading.value = true
  try {
    const res = await getPosts(team.value.id, { page: 1, size: 30 })
    teamPosts.value = res.records || []
  } catch { teamPosts.value = [] }
  finally { postsLoading.value = false }
}

const tabs = [
  { key: 'events', label: '车队活动', icon: 'fas fa-calendar-alt' },
  { key: 'members', label: '成员列表', icon: 'fas fa-users' },
  { key: 'garage', label: '车队车库', icon: 'fas fa-bicycle' },
  { key: 'activity', label: '车队动态', icon: 'fas fa-bullhorn', memberOnly: true }
]
const visibleTabs = computed(() => tabs.filter(t => !t.memberOnly || isMember.value))

const teamActivities = computed(() => (team.value?.activities || []).filter(a => a.type === 'activity'))
const teamNotices = ref([])
const teamPosts = ref([])
const postsLoading = ref(false)
const publishPostText = ref('')
const publishingPost = ref(false)
const publishPostImages = ref([])
const uploadingPostImg = ref(false)
const postFileInput = ref(null)

const isCreator = computed(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  return String(team.value?.creatorId) === String(userStore.userInfo.id)
})

const isCaptain = computed(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  const uid = String(userStore.userInfo.id)
  // 创建者即为队长
  return String(team.value?.creatorId) === uid
})
const isManager = computed(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  const uid = String(userStore.userInfo.id)
  if (String(team.value?.creatorId) === uid) return true
  return members.value.some(m =>
    (m.role === '队长' || m.role === '管理员') && String(m.userId || m.id) === uid
  )
})
const isMember = computed(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  const uid = String(userStore.userInfo.id)
  return members.value.some(m => String(m.userId || m.id) === uid)
})

// 除队长外的其他成员（用于队长退出车队时选择新队长）
const otherMembers = computed(() =>
  members.value.filter(m => m.role !== '队长')
)

/**
 * 分页页码数组（包含省略号）
 * 例如：[1, '...', 4, 5, 6, '...', 10]
 */
const visibleMemberPages = computed(() => {
  const total = memberPages.value
  const current = memberPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  if (current <= 3) {
    pages.push(1, 2, 3, 4, '...', total)
  } else if (current >= total - 2) {
    pages.push(1, '...', total - 3, total - 2, total - 1, total)
  } else {
    pages.push(1, '...', current - 1, current, current + 1, '...', total)
  }
  return pages
})

/**
 * 判断当前用户是否可编辑某成员的标签
 * - 队长：可编辑所有人（含自己）
 * - 管理员：可编辑除队长外所有人
 * - 普通成员：仅可编辑自己
 * - 未登录 / 非队内成员：不可编辑
 */
const canEditTag = (member) => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  if (!isMember.value) return false
  const uid = String(userStore.userInfo.id)
  if (isCaptain.value) return true
  if (isManager.value) return member.role !== '队长'
  return String(member.userId) === uid
}

const toggleNotice = (id) => { expandedNotice.value = expandedNotice.value === id ? null : id }
const formatDistance = (km) => {
  if (km >= 10000) return (km / 10000).toFixed(1) + '万 km'
  return km.toLocaleString() + ' km'
}
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  // 兼容 "2025-03-15 00:00:00" 或 "2025-03-15T00:00:00" 或 "2025-03-15" 格式
  return String(dateStr).split(/[T ]/)[0]
}
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(String(dateStr).replace(' ', 'T'))
  if (isNaN(d.getTime())) return String(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
const formatApplyTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  const hours = Math.floor(diff / 3600000)
  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}
const handleBack = () => router.push('/peloton')

const handleJoin = () => {
  if (!userStore.isLoggedIn) { showToast('请先登录', 'warning'); userStore.openAuthModal(); return }
  joinReason.value = ''
  showJoinModal.value = true
}
const handleEdit = () => {
  router.push(`/peloton/create?id=${team.value.id}`)
}
const cancelJoin = () => {
  showJoinModal.value = false
  joinReason.value = ''
}
const submitJoin = async () => {
  if (!joinReason.value.trim()) return
  try {
    await joinTeam(team.value.id, joinReason.value.trim())
    showToast('申请已提交，等待管理员审核', 'success')
  } catch (err) {
    showToast(err.message || '提交申请失败', 'error')
    return
  }
  showJoinModal.value = false
  joinReason.value = ''
}
const handleApprove = async (app) => {
  try {
    await approveJoin(app.id)
  } catch (err) {
    showToast(err.message || '操作失败', 'error')
    return
  }
  const idx = pendingApplications.value.findIndex(a => a.id === app.id)
  if (idx !== -1) pendingApplications.value.splice(idx, 1)
  // 重新加载成员列表以反映新增
  try {
    await fetchMembers()
    if (team.value) team.value.memberCount = memberTotal.value
  } catch { /* 忽略刷新失败 */ }
  showToast(`已同意 ${app.name} 加入车队`, 'success')
}
const handleReject = async (app) => {
  try {
    await rejectJoin(app.id)
  } catch (err) {
    showToast(err.message || '操作失败', 'error')
    return
  }
  const idx = pendingApplications.value.findIndex(a => a.id === app.id)
  if (idx !== -1) pendingApplications.value.splice(idx, 1)
  showToast(`已拒绝 ${app.name} 的入队申请`, 'info')
}
const handlePublishActivity = () => { router.push(`/peloton/${route.params.id}/activity/create`) }
const handleEditActivity = (id) => { router.push(`/peloton/${route.params.id}/activity/edit/${id}`) }

const handleDeleteActivity = (e, item) => {
  e.stopPropagation()
  deletingActivity.value = { id: item.id, title: item.title }
  showDeleteModal.value = true
}

const cancelDelete = () => {
  showDeleteModal.value = false
  deletingActivity.value = null
}

const confirmDelete = async () => {
  const id = deletingActivity.value?.id
  const title = deletingActivity.value?.title
  showDeleteModal.value = false
  deletingActivity.value = null
  if (!id) return
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    showToast(`活动「${title}」已删除`, 'success')
  } catch (err) {
    showToast('删除失败，请稍后重试', 'error')
  }
}

const getTagClass = (tag) => {
  const map = {
    '全能': 'tag-allround', '破风手': 'tag-leadout', '爬坡手': 'tag-climber',
    '冲刺手': 'tag-sprinter', '计时赛专家': 'tag-tt', '铁三选手': 'tag-tri',
    '耐力王': 'tag-endurance', '山地达人': 'tag-mtb', '夜骑达人': 'tag-night',
    '通勤专家': 'tag-commute', '新秀': 'tag-rookie'
  }
  return map[tag] || 'tag-default'
}
const getTagIcon = (tag) => {
  const map = {
    '全能': 'fas fa-star', '破风手': 'fas fa-wind', '爬坡手': 'fas fa-mountain',
    '冲刺手': 'fas fa-bolt', '计时赛专家': 'fas fa-stopwatch', '铁三选手': 'fas fa-medal',
    '耐力王': 'fas fa-heartbeat', '山地达人': 'fas fa-tree', '夜骑达人': 'fas fa-moon',
    '通勤专家': 'fas fa-city', '新秀': 'fas fa-seedling'
  }
  return map[tag] || 'fas fa-tag'
}

const handleKick = (member) => {
  kickingMember.value = member
  showKickModal.value = true
}
const cancelKick = () => {
  showKickModal.value = false
  kickingMember.value = null
}
const confirmKick = async () => {
  const member = kickingMember.value
  showKickModal.value = false
  kickingMember.value = null
  if (!member) return
  try {
    await kickMember(team.value.id, member.userId || member.id)
    members.value = members.value.filter(m => (m.userId || m.id) !== (member.userId || member.id))
    memberTotal.value = Math.max(0, memberTotal.value - 1)
    if (team.value) team.value.memberCount = memberTotal.value
    showToast(`已将 ${member.name} 请离车队`, 'success')
  } catch (err) {
    showToast(err?.message || '操作失败，请稍后重试', 'error')
  }
}
const handlePromote = async (member) => {
  try {
    await promoteAdmin(team.value.id, member.userId || member.id)
    member.role = '管理员'
    showToast(`已将 ${member.name} 设为管理员`, 'success')
  } catch (err) {
    showToast(err?.message || '操作失败，请稍后重试', 'error')
  }
}
const handleDemote = async (member) => {
  try {
    await revokeAdmin(team.value.id, member.userId || member.id)
    member.role = '成员'
    showToast(`已撤销 ${member.name} 的管理员权限`, 'info')
  } catch (err) {
    showToast(err?.message || '操作失败，请稍后重试', 'error')
  }
}

// ── 退出车队（队长转让身份后退出） ──
const showLeaveModal = ref(false)
const selectedNewCaptain = ref(null)
const leavingTeam = ref(false)
const cancelLeave = () => {
  showLeaveModal.value = false
  selectedNewCaptain.value = null
}
const confirmLeave = async () => {
  if (!selectedNewCaptain.value) return
  leavingTeam.value = true
  try {
    await captainLeaveTeam(team.value.id, selectedNewCaptain.value.userId || selectedNewCaptain.value.id)
    showLeaveModal.value = false
    selectedNewCaptain.value = null
    showToast('已成功退出车队，队长身份已转让', 'success')
    setTimeout(() => router.push('/peloton'), 1500)
  } catch (err) {
    showToast(err?.message || '退出失败，请稍后重试', 'error')
  } finally {
    leavingTeam.value = false
  }
}

// ── 解散车队 ──
const showCaptainMenu = ref(false)
const showDisbandModal = ref(false)
const showFarewell = ref(false)
const disbanding = ref(false)
const confirmDisband = async () => {
  disbanding.value = true
  try {
    await disbandTeam(team.value.id)
    showDisbandModal.value = false
    showFarewell.value = true
    setTimeout(() => {
      if (showFarewell.value) goHome()
    }, 6000)
  } catch (err) {
    showToast(err?.message || '解散失败，请稍后重试', 'error')
  } finally {
    disbanding.value = false
  }
}
const goHome = () => {
  showFarewell.value = false
  router.push('/peloton')
}
const handleTagClick = (member) => {
  if (canEditTag(member)) {
    toggleTagEdit(member)
  } else {
    if (!userStore.isLoggedIn) {
      showToast('请先登录后再修改标签', 'warning')
    } else if (!isMember.value) {
      showToast('仅车队成员可修改标签', 'warning')
    } else {
      showToast('无权修改该成员的标签', 'warning')
    }
  }
}
const toggleTagEdit = (member) => {
  editingTagMember.value = editingTagMember.value?.id === member.id ? null : member
}
const changeMemberTag = async (member, newTag) => {
  if (member.tag === newTag) {
    editingTagMember.value = null
    return
  }
  try {
    await updateMemberTag(team.value.id, member.userId, newTag)
    member.tag = newTag
    showToast(`已将 ${member.name} 的标签改为「${newTag}」`, 'success')
  } catch (err) {
    showToast(err.message || '修改标签失败', 'error')
  } finally {
    editingTagMember.value = null
  }
}

const garageFilters = computed(() => {
  const types = [...new Set(garageBikes.value.map(b => b.type))]
  return [
    { key: 'all', label: '全部', icon: 'fas fa-th-large' },
    ...types.map(t => ({
      key: t, label: t,
      icon: t === '公路车' ? 'fas fa-road' : t === '山地车' ? 'fas fa-mountain' : t === 'TT车' ? 'fas fa-stopwatch' : t === '砾石车' ? 'fas fa-route' : 'fas fa-bicycle'
    }))
  ]
})
const filteredGarageBikes = computed(() => {
  if (garageFilter.value === 'all') return garageBikes.value
  return garageBikes.value.filter(b => b.type === garageFilter.value)
})
const garageStats = computed(() => {
  const map = {}
  garageBikes.value.forEach(b => { map[b.type] = (map[b.type] || 0) + 1 })
  return Object.entries(map).map(([type, count]) => ({ type, count }))
})

/** 个人车库中尚未录入车队车库的单车 */
const availableMyBikes = computed(() => {
  const existingBikeIds = new Set(garageBikes.value.map(b => b.bikeId))
  return myBikes.value.filter(b => !existingBikeIds.has(b.id))
})

/** 打开“添加我的单车”弹窗 */
const openAddBikeModal = async () => {
  showAddBikeModal.value = true
  myBikesLoading.value = true
  try {
    const res = await getMyGarage()
    myBikes.value = res.data || res || []
  } catch (err) {
    console.error('获取个人车库失败', err)
    showToast('获取个人车库失败', 'error')
    myBikes.value = []
  } finally {
    myBikesLoading.value = false
  }
}

const closeAddBikeModal = () => {
  showAddBikeModal.value = false
}

/** 将单车录入车队车库 */
const handleAddBikeToGarage = async (bike) => {
  addingBikeId.value = bike.id
  try {
    await addBikeToTeamGarage(team.value.id, bike.id)
    showToast('录入车队车库成功', 'success')
    // 刷新车队车库列表
    const garageData = await getTeamGarage(team.value.id)
    garageBikes.value = garageData.records || []
  } catch (err) {
    showToast(err.message || '录入失败', 'error')
  } finally {
    addingBikeId.value = null
  }
}

/** 从车队车库移除单车 */
const handleRemoveBikeFromGarage = async (bike) => {
  removingBikeId.value = bike.bikeId
  try {
    await removeBikeFromTeamGarage(team.value.id, bike.bikeId)
    showToast('已从车队车库移除', 'success')
    garageBikes.value = garageBikes.value.filter(b => b.id !== bike.id)
  } catch (err) {
    showToast(err.message || '移除失败', 'error')
  } finally {
    removingBikeId.value = null
  }
}

/** 当前用户是否为车库卡片车主 */
const isBikeOwner = (bike) => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return false
  return String(bike.ownerId) === String(userStore.userInfo.id)
}

const openBikeShowcase = async (bike) => {
  try {
    const res = await getBikeByModel(bike.model, bike.brand)
    const bikes = res.data || res || []
    const list = Array.isArray(bikes) ? bikes : (bikes.records || bikes.list || [])
    if (list.length > 0) {
      const matched = list[0]
      router.push({ name: 'bike-detail', params: { id: matched.id } })
    } else {
      showToast(`展厅暂未收录「${bike.brand} ${bike.model}」，敬请期待`, 'info')
    }
  } catch (err) {
    console.error('查询展厅车辆失败', err)
    showToast(`展厅暂未收录「${bike.brand} ${bike.model}」，敬请期待`, 'info')
  }
}

const handlePublishNotice = () => {
  router.push({ name: 'notice-create', params: { id: route.params.id } })
}
const handleEditNotice = (item) => {
  router.push({ name: 'notice-edit', params: { id: route.params.id, noticeId: item.id } })
}
const handleDeleteNotice = (item) => {
  deletingNotice.value = item
  showDeleteNoticeModal.value = true
}
const cancelDeleteNotice = () => {
  showDeleteNoticeModal.value = false
  deletingNotice.value = null
}
const confirmDeleteNotice = async () => {
  const notice = deletingNotice.value
  showDeleteNoticeModal.value = false
  deletingNotice.value = null
  if (!notice) return
  try {
    await deleteNotice(route.params.id, notice.id)
    teamNotices.value = teamNotices.value.filter(n => n.id !== notice.id)
    showToast(`已删除公告「${notice.title}」`, 'success')
  } catch (err) {
    showToast('删除失败，请重试', 'error')
  }
}

/**
 * 加载成员列表（支持分页 + 关键词搜索）
 * @param {Object} [opts] - 可选参数
 * @param {Number} [opts.page] - 页码（默认使用当前页）
 * @param {String} [opts.keyword] - 搜索关键词（默认使用当前关键词）
 */
const fetchMembers = async (opts = {}) => {
  if (!team.value?.id) return
  memberLoading.value = true
  try {
    const page = opts.page ?? memberPage.value
    const keyword = opts.keyword ?? memberKeyword.value
    const data = await getTeamMembers(team.value.id, { page, size: MEMBER_PAGE_SIZE, keyword })
    members.value = Array.isArray(data) ? data : (data.records || [])
    memberTotal.value = data.total ?? members.value.length
    memberPages.value = data.pages ?? 1
    memberPage.value = page
  } catch { members.value = [] }
  finally { memberLoading.value = false }
}

/** 成员搜索（回车或点击按钮触发） */
const handleMemberSearch = () => {
  memberKeyword.value = memberSearchInput.value.trim()
  memberPage.value = 1
  fetchMembers({ page: 1, keyword: memberKeyword.value })
}

/** 清空搜索 */
const clearMemberSearch = () => {
  memberSearchInput.value = ''
  memberKeyword.value = ''
  memberPage.value = 1
  fetchMembers({ page: 1, keyword: '' })
}

/** 成员分页切换 */
const handleMemberPageChange = (page) => {
  fetchMembers({ page })
}

const fetchData = async () => {
  try {
    const id = route.params.id
    const [teamData, garageData] = await Promise.all([
      getTeamDetail(id), getTeamGarage(id)
    ])
    team.value = teamData
    garageBikes.value = garageData.records || []
    // 加载公告栏
    try {
      const noticeRes = await getNoticeBoard(id, { page: 1, size: 20 })
      teamNotices.value = (noticeRes.records || []).map(n => ({
        ...n,
        date: n.createdAt ? n.createdAt.substring(0, 10) : ''
      }))
    } catch { teamNotices.value = [] }
    // 单独加载成员列表（分页）
    await fetchMembers()
    // 加载车队动态（仅队内成员）
    if (isMember.value) { await fetchPosts() }
    // 不再使用 isMock 判断权限，权限始终基于真实数据校验

    // 仅登录的队长/管理员才加载待审核申请
    if (userStore.isLoggedIn && isManager.value) {
      try {
        pendingApplications.value = await getPendingApplications(id)
      } catch { pendingApplications.value = [] }
    }
  } catch (err) { console.error('获取车队详情失败', err); team.value = null }
  finally { loading.value = false }
}
onMounted(() => fetchData())
</script>

<style scoped>
.team-detail-page { padding: 120px 0 100px; min-height: 100vh }
.detail-header { margin-bottom: 40px }
.back-btn { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; background: #fff; border: 1px solid rgba(0,0,0,0.08); border-radius: 8px; font-size: 0.9rem; font-weight: 600; color: var(--text-body); cursor: pointer; transition: all 0.3s }
.back-btn:hover { border-color: var(--primary-color); color: var(--primary-color); background: rgba(92,110,88,0.05) }
.loading-state { text-align: center; padding: 80px 0; color: var(--text-muted) }
.loading-state i { font-size: 2rem; margin-bottom: 12px; color: var(--primary-color) }

.team-hero { border-radius: 18px; overflow: hidden; background: #fff; box-shadow: var(--shadow-sm); margin-bottom: 48px }
.hero-cover { position: relative; height: 300px; overflow: hidden }
.cover-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.8s cubic-bezier(0.165,0.84,0.44,1) }
.team-hero:hover .cover-img { transform: scale(1.04) }
.cover-gradient { position: absolute; inset: 0; background: linear-gradient(to bottom, transparent 40%, rgba(0,0,0,0.12) 72%, rgba(0,0,0,0.42) 100%) }
.hero-join-btn { position: absolute; top: 24px; right: 24px; display: flex; align-items: center; gap: 7px; padding: 11px 24px; border-radius: 30px; border: none; background: rgba(255,255,255,0.92); backdrop-filter: blur(8px); color: var(--primary-color); font-size: 0.9rem; font-weight: 700; cursor: pointer; transition: all 0.35s cubic-bezier(0.165,0.84,0.44,1); box-shadow: 0 4px 20px rgba(0,0,0,0.12) }
.hero-join-btn:hover { background: var(--primary-color); color: #fff; transform: translateY(-2px); box-shadow: 0 8px 28px rgba(92,110,88,0.4) }
.hero-join-btn.is-edit:hover { background: rgba(255,255,255,0.92); color: var(--primary-color); transform: translateY(-2px) }
.hero-join-btn i { font-size: 0.8rem; transition: transform 0.35s }
.hero-join-btn:hover i { transform: rotate(90deg) }
.hero-join-btn.is-edit:hover i { transform: none }

/* ── 队长操作下拉 ── */
.hero-actions { position: absolute; top: 24px; right: 24px; display: flex; align-items: center; gap: 8px; z-index: 10; }
.hero-actions .hero-join-btn { position: static; }
.hero-more-btn { width: 40px; height: 40px; border-radius: 50%; border: none; background: rgba(255,255,255,0.92); backdrop-filter: blur(8px); color: var(--primary-color); font-size: 0.95rem; cursor: pointer; transition: all 0.25s ease; box-shadow: 0 4px 20px rgba(0,0,0,0.12); display: flex; align-items: center; justify-content: center; }
.hero-more-btn:hover { background: var(--primary-color); color: #fff; transform: translateY(-2px); box-shadow: 0 8px 28px rgba(92,110,88,0.4); }
.captain-menu-wrap { position: relative; }
.captain-menu-backdrop { position: fixed; inset: 0; z-index: 199; }
.captain-dropdown { position: absolute; top: calc(100% + 8px); right: 0; min-width: 160px; background: #fff; border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.14), 0 0 0 1px rgba(0,0,0,0.04); padding: 6px; z-index: 200; }
.captain-dropdown-item { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 14px; border-radius: 8px; border: none; background: transparent; font-size: 0.86rem; font-weight: 500; cursor: pointer; transition: all 0.2s ease; color: var(--text-body); white-space: nowrap; }
.captain-dropdown-item i { font-size: 0.82rem; width: 18px; text-align: center; }
.captain-dropdown-item.edit:hover { background: rgba(92,110,88,0.08); color: var(--primary-color); }
.captain-dropdown-item.leave:hover { background: rgba(255,152,0,0.08); color: #e68900; }
.captain-dropdown-item.disband:hover { background: rgba(220,53,69,0.06); color: #dc3545; }
.menu-fade-enter-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.menu-fade-leave-active { transition: opacity 0.12s ease, transform 0.12s ease; }
.menu-fade-enter-from, .menu-fade-leave-to { opacity: 0; transform: translateY(-6px); }
.hero-body { display: flex; gap: 32px; padding: 32px 40px 36px; position: relative; align-items: flex-start }

.hero-avatar-ring { position: relative; flex-shrink: 0; margin-top: -56px }
.hero-avatar-ring::before { content: ''; position: absolute; inset: -8px; border-radius: 50%; background: linear-gradient(135deg, rgba(92,110,88,0.22) 0%, rgba(92,110,88,0.04) 50%, rgba(92,110,88,0.16) 100%); opacity: 0; transition: opacity 0.6s ease }
.hero-avatar-ring::after { content: ''; position: absolute; inset: -4px; border-radius: 50%; border: 2px dashed rgba(92,110,88,0.18); animation: ring-spin 24s linear infinite; opacity: 0; transition: opacity 0.6s ease }
.team-hero:hover .hero-avatar-ring::before, .team-hero:hover .hero-avatar-ring::after { opacity: 1 }
@keyframes ring-spin { from { transform: rotate(0deg) } to { transform: rotate(360deg) } }

.hero-avatar { width: 108px; height: 108px; border-radius: 50%; object-fit: cover; border: 5px solid #fff; box-shadow: 0 4px 20px rgba(0,0,0,0.12), 0 0 0 1px rgba(92,110,88,0.16); background: #fff; display: block; position: relative; z-index: 1; transition: transform 0.45s cubic-bezier(0.165,0.84,0.44,1) }
.hero-avatar:hover { transform: scale(1.06) }
.hero-text { flex: 1; min-width: 0; padding-top: 4px }
.team-name { font-family: var(--font-serif); font-size: 2rem; font-weight: 800; color: var(--text-main); margin: 0 0 10px 0; line-height: 1.2; letter-spacing: -0.01em }
.hero-meta { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 14px }
.meta-item { font-size: 0.85rem; color: var(--text-muted); display: flex; align-items: center; gap: 6px; white-space: nowrap }
.meta-item i { font-size: 0.78rem; opacity: 0.6 }
.meta-dot { width: 3px; height: 3px; border-radius: 50%; background: rgba(0,0,0,0.15); flex-shrink: 0 }
.team-description { font-size: 0.93rem; color: var(--text-body); line-height: 1.7; margin: 0 }

.hero-stats-side { display: flex; align-items: center; gap: 10px; flex-shrink: 0; padding-top: 6px }
.side-stat { font-size: 0.82rem; color: var(--text-muted); font-weight: 500; white-space: nowrap; display: flex; align-items: center; gap: 5px }
.side-stat i { font-size: 0.72rem; opacity: 0.55 }
.side-stat-sep { color: rgba(0,0,0,0.12); font-weight: 300; user-select: none; font-size: 0.9rem }

.tab-section { margin-top: 8px }
.tab-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 36px }
.capsule-nav { display: flex; gap: 6px; padding: 6px; background: rgba(0,0,0,0.035); border-radius: 30px; width: fit-content }
.capsule-btn { padding: 10px 24px; border-radius: 24px; border: none; background: transparent; font-size: 0.9rem; font-weight: 600; color: var(--text-muted); cursor: pointer; transition: all 0.3s ease; white-space: nowrap }
.capsule-btn:hover { color: var(--text-main) }
.capsule-btn.active { background: #fff; color: var(--primary-color); box-shadow: var(--shadow-sm) }
.capsule-btn i { margin-right: 6px }

.publish-btn { display: inline-flex; align-items: center; gap: 7px; padding: 10px 22px; border-radius: 24px; border: none; background: var(--primary-color); color: #fff; font-size: 0.88rem; font-weight: 600; cursor: pointer; transition: all 0.3s ease; white-space: nowrap; box-shadow: 0 2px 8px rgba(92,110,88,0.25) }
.publish-btn:hover { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 4px 16px rgba(92,110,88,0.35) }
.publish-btn i { font-size: 0.85rem }

.events-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px }
.event-card { position: relative; padding: 22px 24px; border-radius: 14px; background: #fff; border: 1px solid rgba(0,0,0,0.04); box-shadow: var(--shadow-sm); transition: all 0.35s ease }
.event-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md) }
.event-card-header { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 14px }
.event-card-header .activity-icon { width: 40px; height: 40px; border-radius: 10px; font-size: 0.9rem; flex-shrink: 0 }
.event-meta { display: flex; flex-direction: column; gap: 3px }
.event-title { font-size: 0.92rem; font-weight: 700; color: var(--text-main); margin: 0 0 8px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden }
.event-location { font-size: 0.74rem; color: var(--text-muted); margin: 0; display: flex; align-items: center; gap: 4px }
.event-location i { font-size: 0.65rem; opacity: 0.55 }
.event-desc { font-size: 0.82rem; color: var(--text-muted); line-height: 1.6; margin: 0 0 10px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden }

.event-card-actions { position: absolute; bottom: 16px; right: 16px; display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s ease; pointer-events: none }
.event-card:hover .event-card-actions { opacity: 1; pointer-events: auto }
.event-act-btn { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 6px; border: none; background: rgba(0,0,0,0.04); color: var(--text-muted); font-size: 0.7rem; cursor: pointer; transition: all 0.15s ease }
.event-act-btn:hover { background: rgba(92,110,88,0.12); color: var(--primary-color) }
.event-act-btn.del:hover { background: rgba(231,76,60,0.12); color: #e74c3c }

.feed-section { margin-bottom: 28px }
.feed-section:last-child { margin-bottom: 0 }
.feed-section-header { font-size: 0.88rem; font-weight: 700; color: var(--text-main); margin: 0 0 16px 0; display: flex; align-items: center; gap: 8px }
.feed-section-header.mt-none { margin-top: 0 }
.feed-section-header i { font-size: 0.82rem; color: var(--primary-color) }

.notice-header { padding-bottom: 14px; margin-bottom: 14px !important; border-bottom: 1px solid rgba(0,0,0,0.06); display: flex; align-items: center; gap: 8px }
.notice-header i { color: var(--primary-color) !important }
.post-section-header { margin-top: 24px; padding-top: 18px; border-top: 1px solid rgba(0,0,0,0.06) }
.post-section-header i { color: var(--primary-color) !important }
.notice-empty { font-size: 0.82rem; color: var(--text-muted); opacity: 0.55; text-align: center; padding: 16px 0 8px; margin: 0 }
.notice-publish-btn {
  margin-left: auto; display: inline-flex; align-items: center; gap: 7px;
  padding: 8px 20px; border-radius: 24px;
  border: 1px solid rgba(92,110,88,0.2); background: transparent;
  color: var(--primary-color); font-size: 0.82rem; font-weight: 600;
  cursor: pointer; transition: all 0.35s cubic-bezier(0.165,0.84,0.44,1);
}
.notice-publish-btn:hover {
  background: var(--primary-color); border-color: var(--primary-color);
  color: #fff; transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(92,110,88,0.3);
}
.notice-publish-btn i {
  font-size: 0.72rem; color: inherit;
  transition: transform 0.35s cubic-bezier(0.165,0.84,0.44,1);
}
.notice-publish-btn:hover i { transform: rotate(90deg) }
.notice-list { display: flex; flex-direction: column; gap: 2px }
.notice-item { border-radius: 8px; transition: background 0.2s ease; cursor: pointer }
.notice-item:hover { background: rgba(0,0,0,0.02) }
.notice-item.expanded { background: rgba(0,0,0,0.02) }
.notice-row { display: flex; align-items: center; gap: 12px; padding: 10px 14px }
.notice-dot { width: 6px; height: 6px; border-radius: 50%; background: #0d6efd; flex-shrink: 0; opacity: 0.6 }
.notice-title { flex: 1; min-width: 0; font-size: 0.88rem; font-weight: 500; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis }
.notice-date { font-size: 0.76rem; color: var(--text-muted); flex-shrink: 0 }
.notice-arrow { font-size: 0.65rem; color: var(--text-muted); flex-shrink: 0; transition: transform 0.25s ease; opacity: 0.4 }
.notice-item.expanded .notice-arrow { transform: rotate(180deg); opacity: 0.7 }
.notice-actions {
  display: flex; gap: 4px; margin-left: 4px;
  opacity: 0; transition: opacity 0.2s ease;
}
.notice-item:hover .notice-actions { opacity: 1 }
.notice-act-btn {
  width: 24px; height: 24px; border-radius: 6px; border: none;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.62rem; cursor: pointer; transition: all 0.2s ease;
  background: rgba(0,0,0,0.04); color: var(--text-muted);
}
.notice-act-btn:hover { background: rgba(13,110,253,0.12); color: #0d6efd; transform: scale(1.1) }
.notice-act-btn.del:hover { background: rgba(231,76,60,0.1); color: #e74c3c }
.notice-body { padding: 0 32px 14px 32px; font-size: 0.84rem; color: var(--text-muted); line-height: 1.7 }
.notice-body p { margin: 0 }
.notice-body p + p { margin-top: 6px }

.activity-list { display: flex; flex-direction: column; gap: 14px }
.activity-item { display: flex; gap: 20px; padding: 24px 28px; border-radius: 14px; background: #fff; border: 1px solid rgba(0,0,0,0.04); box-shadow: var(--shadow-sm); transition: all 0.35s ease }
.activity-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-md) }
.activity-icon { width: 48px; height: 48px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 1.05rem; flex-shrink: 0 }
.activity-icon.activity { background: rgba(92,110,88,0.1); color: var(--primary-color) }
.activity-icon.checkin { background: rgba(255,193,7,0.15); color: #c89510 }
.activity-icon.notice { background: rgba(13,110,253,0.1); color: #0d6efd }
.activity-body { flex: 1; min-width: 0 }
.activity-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 8px }
.activity-user { font-size: 0.9rem; font-weight: 700; color: var(--text-main) }
.activity-title { font-size: 0.95rem; font-weight: 700; color: var(--text-main); margin: 0 }
.activity-date { font-size: 0.78rem; color: var(--text-muted); margin-left: auto; flex-shrink: 0 }
.activity-desc { font-size: 0.88rem; color: var(--text-muted); line-height: 1.65; margin: 0 0 10px 0 }
.activity-image { width: 100%; max-width: 320px; border-radius: 8px; margin-top: 8px }
.activity-participants { display: inline-flex; align-items: center; gap: 4px; font-size: 0.78rem; color: var(--primary-color); padding: 4px 10px; background: rgba(92,110,88,0.06); border-radius: 6px; margin-top: 8px }

.post-actions { display: flex; align-items: center; gap: 4px; margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(0,0,0,0.04) }
.post-action-btn { display: inline-flex; align-items: center; gap: 5px; padding: 6px 14px; border: none; border-radius: 18px; background: transparent; font-size: 0.8rem; color: var(--text-muted); cursor: pointer; transition: all 0.2s ease }
.post-action-btn:hover { background: rgba(0,0,0,0.04); color: var(--text-body) }
.post-action-btn i { font-size: 0.82rem; transition: transform 0.15s ease }
.post-action-btn:active i { transform: scale(1.25) }
.post-action-btn.liked { color: #e74c3c }
.post-action-btn.liked i { animation: heart-pop 0.35s ease }
@keyframes heart-pop { 0%{transform:scale(1)} 30%{transform:scale(1.35)} 60%{transform:scale(0.9)} 100%{transform:scale(1)} }
.post-action-btn.active { color: var(--primary-color); background: rgba(92,110,88,0.06) }
.post-comments { margin-top: 10px; padding: 10px 14px; background: rgba(0,0,0,0.015); border-radius: 10px }
.comment-item { display: flex; align-items: baseline; gap: 0; font-size: 0.8rem; line-height: 1.7; color: var(--text-muted) }
.comment-item + .comment-item { margin-top: 2px }
.comment-user { font-weight: 600; color: var(--text-body); margin-right: 6px }
.comment-text { color: var(--text-muted); flex: 1 }
.comment-del { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 50%; border: none; background: transparent; color: var(--text-muted); cursor: pointer; font-size: 0.6rem; opacity: 0; transition: all 0.15s ease; flex-shrink: 0; margin-left: 4px }
.comment-item:hover .comment-del { opacity: 0.5 }
.comment-del:hover { opacity: 1 !important; background: rgba(231,76,60,0.1); color: #e74c3c }
.comment-input-row { display: flex; align-items: center; gap: 8px; margin-top: 10px }
.comment-input { flex: 1; padding: 8px 14px; border: 1px solid rgba(0,0,0,0.08); border-radius: 20px; font-size: 0.8rem; color: var(--text-main); background: rgba(0,0,0,0.02); outline: none; transition: border-color 0.25s ease }
.comment-input:focus { border-color: var(--primary-color); background: #fff }
.comment-input::placeholder { color: var(--text-muted); opacity: 0.5 }
.comment-submit { width: 36px; height: 36px; border-radius: 50%; border: none; background: var(--primary-color); color: #fff; font-size: 0.82rem; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.2s ease }
.comment-submit:hover { background: var(--primary-dark); transform: scale(1.05) }
.comment-submit:active { transform: scale(0.95) }

/* ── 动态发布区 ── */
.post-compose { background: #fff; border: 1px solid rgba(0,0,0,0.06); border-radius: 14px; padding: 16px 20px 12px; margin-bottom: 0 }
.post-compose-input { width: 100%; min-height: 70px; border: none; font-size: 0.88rem; color: var(--text-main); resize: vertical; outline: none; font-family: inherit; line-height: 1.6 }
.post-compose-input::placeholder { color: var(--text-muted); opacity: 0.5 }
.post-compose-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px }
.post-compose-count { font-size: 0.75rem; color: var(--text-muted) }
.post-compose-btn { padding: 7px 20px; border: none; border-radius: 20px; background: var(--primary-color); color: #fff; font-size: 0.82rem; font-weight: 600; cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px }
.post-compose-btn:hover:not(:disabled) { background: var(--primary-dark); transform: translateY(-1px) }
.post-compose-btn:disabled { opacity: 0.4; cursor: not-allowed }
.compose-footer-left { display: flex; align-items: center; gap: 12px }
.compose-img-btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border: 1px solid rgba(0,0,0,0.1); border-radius: 18px; background: transparent; font-size: 0.8rem; color: var(--text-muted); cursor: pointer; transition: all 0.2s }
.compose-img-btn:hover:not(:disabled) { border-color: var(--primary-color); color: var(--primary-color) }
.compose-img-btn:disabled { opacity: 0.4; cursor: not-allowed }
.compose-img-btn i { font-size: 0.85rem }
.compose-image-grid { display: flex; flex-wrap: wrap; gap: 10px; margin: 10px 0 0 }
.compose-img-wrap { position: relative; width: 90px; height: 90px; border-radius: 10px; overflow: hidden; border: 1px solid rgba(0,0,0,0.06) }
.compose-preview-img { width: 100%; height: 100%; object-fit: cover; display: block }
.compose-img-remove { position: absolute; top: 3px; right: 3px; width: 22px; height: 22px; border-radius: 50%; border: none; background: rgba(0,0,0,0.55); color: #fff; font-size: 0.65rem; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background 0.2s }
.compose-img-remove:hover { background: #e74c3c }

/* ── 动态头像与图片网格 ── */
.post-avatar { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; flex-shrink: 0; border: 2px solid rgba(0,0,0,0.05) }
.post-del-btn { margin-left: auto; border: none; background: transparent; color: var(--text-muted); cursor: pointer; font-size: 0.78rem; padding: 4px 8px; border-radius: 6px; transition: all 0.15s }
.post-del-btn:hover { color: #e74c3c; background: rgba(231,76,60,0.08) }
.post-image-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px }
.post-grid-img { width: 140px; height: 140px; object-fit: cover; border-radius: 10px; border: 1px solid rgba(0,0,0,0.04); cursor: pointer; transition: transform 0.2s }
.post-grid-img:hover { transform: scale(1.03) }
.post-time { margin-left: auto; font-size: 0.75rem; color: var(--text-muted); opacity: 0.75; flex-shrink: 0; padding-left: 8px }

/* ── 成员搜索栏（tab-bar 右侧） ── */
.tab-bar-search { display: flex; align-items: center; gap: 8px; margin-left: auto }
.tab-bar-search .search-input-wrap { position: relative; width: 220px }
.tab-bar-search .search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); font-size: 0.8rem; pointer-events: none }
.tab-bar-search .member-search-input { width: 100%; padding: 8px 34px 8px 34px; border: 1px solid rgba(0,0,0,0.1); border-radius: 20px; font-size: 0.84rem; color: var(--text-main); background: #fff; outline: none; transition: border-color 0.25s, box-shadow 0.25s }
.tab-bar-search .member-search-input:focus { border-color: var(--primary-color); box-shadow: 0 0 0 3px rgba(92,110,88,0.1) }
.tab-bar-search .member-search-input::placeholder { color: var(--text-muted); opacity: 0.5 }
.tab-bar-search .search-clear-btn { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; border-radius: 50%; border: none; background: rgba(0,0,0,0.06); color: var(--text-muted); font-size: 0.68rem; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s }
.tab-bar-search .search-clear-btn:hover { background: rgba(0,0,0,0.12); color: var(--text-main) }
.tab-bar-search .search-submit-btn { padding: 8px 16px; border-radius: 20px; border: none; background: var(--primary-color); color: #fff; font-size: 0.84rem; font-weight: 600; cursor: pointer; transition: all 0.25s; white-space: nowrap }
.tab-bar-search .search-submit-btn:hover { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 4px 14px rgba(92,110,88,0.3) }

/* ── 搜索结果信息 ── */
.member-search-info { margin-bottom: 16px; font-size: 0.86rem; color: var(--text-muted) }
.member-search-info strong { color: var(--text-main); font-weight: 600 }

/* ── 成员加载中 ── */
.member-loading { text-align: center; padding: 24px 0; color: var(--primary-color); font-size: 1.2rem }

/* ── 成员分页控件 ── */
.member-pagination { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 28px; padding: 16px 0 }
.page-btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(0,0,0,0.1); background: #fff; color: var(--text-body); font-size: 0.85rem; font-weight: 500; cursor: pointer; transition: all 0.2s }
.page-btn:hover:not(:disabled) { border-color: var(--primary-color); color: var(--primary-color); background: rgba(92,110,88,0.04) }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed }
.page-numbers { display: flex; align-items: center; gap: 4px }
.page-num-btn { min-width: 36px; height: 36px; border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--text-body); font-size: 0.88rem; font-weight: 500; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center }
.page-num-btn:hover:not(:disabled):not(.active):not(.ellipsis) { background: rgba(92,110,88,0.08); color: var(--primary-color) }
.page-num-btn.active { background: var(--primary-color); color: #fff; border-color: var(--primary-color); font-weight: 700 }
.page-num-btn.ellipsis { cursor: default; color: var(--text-muted) }
.page-num-btn:disabled { cursor: not-allowed }

.members-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px }

.glass-member-card {
  position: relative; border-radius: 20px; overflow: visible;
  transition: all 0.45s cubic-bezier(0.165, 0.84, 0.44, 1);
  animation: glass-card-in 0.5s ease both;
}
@keyframes glass-card-in {
  from { opacity: 0; transform: translateY(20px) scale(0.96) }
  to { opacity: 1; transform: translateY(0) scale(1) }
}
.glass-member-card:hover { transform: translateY(-6px) scale(1.02); z-index: 10 }
.glass-member-card.card-tag-open { z-index: 20 }

.glass-member-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.55) 0%, rgba(255,255,255,0.22) 100%);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255,255,255,0.45);
  border-radius: 20px;
  transition: all 0.4s ease;
}
.glass-member-card:hover .glass-member-bg {
  background: linear-gradient(135deg, rgba(255,255,255,0.65) 0%, rgba(255,255,255,0.32) 100%);
  border-color: rgba(92,110,88,0.18);
  box-shadow: 0 16px 40px rgba(92,110,88,0.12), 0 0 0 1px rgba(92,110,88,0.06);
}

.glass-member-content {
  position: relative; z-index: 1; padding: 28px 24px 22px;
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 14px;
}

.glass-avatar-wrap {
  position: relative; width: 72px; height: 72px;
}
.glass-avatar {
  width: 72px; height: 72px; border-radius: 50%; object-fit: cover;
  border: 3px solid rgba(255,255,255,0.8);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1), 0 0 0 1px rgba(92,110,88,0.08);
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
}
.glass-member-card:hover .glass-avatar {
  transform: scale(1.08);
  box-shadow: 0 8px 24px rgba(92,110,88,0.18), 0 0 0 2px rgba(92,110,88,0.12);
}

.glass-crown {
  position: absolute; top: -6px; right: -4px;
  width: 24px; height: 24px; border-radius: 50%;
  background: linear-gradient(135deg, #f6d365, #fda085);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(253,160,133,0.4);
  animation: crown-float 3s ease-in-out infinite;
}
.glass-crown i { font-size: 0.6rem; color: #fff }
@keyframes crown-float {
  0%, 100% { transform: translateY(0) rotate(-8deg) }
  50% { transform: translateY(-3px) rotate(4deg) }
}

.glass-member-info { display: flex; flex-direction: column; align-items: center; gap: 4px; position: relative }
.glass-name {
  font-size: 0.95rem; font-weight: 700; color: var(--text-main);
  letter-spacing: 0.02em;
}
.glass-role {
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.04em;
  padding: 3px 12px; border-radius: 20px;
  background: rgba(0,0,0,0.04); color: var(--text-muted);
  transition: all 0.3s ease;
}
.glass-role.is-leader {
  background: linear-gradient(135deg, rgba(246,211,101,0.2), rgba(253,160,133,0.15));
  color: #c89510;
}
.glass-role.is-admin {
  background: rgba(92,110,88,0.08);
  color: var(--primary-color);
}

.glass-tag {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 0.68rem; font-weight: 600; letter-spacing: 0.03em;
  padding: 2px 10px; border-radius: 12px;
  transition: all 0.3s ease;
}
.glass-tag i { font-size: 0.58rem }
.glass-member-card:hover .glass-tag { transform: scale(1.05) }
.glass-tag.tag-editable { cursor: pointer; position: relative; overflow: hidden }
.glass-tag.tag-editable:hover { filter: brightness(0.95); transform: scale(1.08) }
.tag-label { display: inline-flex; align-items: center; gap: 4px; transition: opacity 0.2s ease }
.tag-hover-edit {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s ease;
}
.tag-hover-edit i { font-size: 0.62rem !important; opacity: 0.8 }
.glass-tag.tag-editable:hover .tag-label { opacity: 0 }
.glass-tag.tag-editable:hover .tag-hover-edit { opacity: 1 }

.tag-dropdown {
  position: absolute; z-index: 200; top: calc(100% + 8px); left: 50%;
  transform: translateX(-50%);
  padding: 10px;
  background: #fff; border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06);
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px;
  width: max-content; min-width: 220px;
  animation: tag-drop-in 0.22s cubic-bezier(0.34,1.56,0.64,1) both;
  border: 1px solid rgba(92,110,88,0.08);
}
.tag-dropdown::before {
  content: ''; position: absolute; top: -6px; left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px; height: 12px;
  background: #fff; border-left: 1px solid rgba(92,110,88,0.08);
  border-top: 1px solid rgba(92,110,88,0.08);
}
@keyframes tag-drop-in { from { opacity: 0; transform: translateX(-50%) translateY(-8px) scale(0.95) } to { opacity: 1; transform: translateX(-50%) translateY(0) scale(1) } }
.tag-option {
  display: inline-flex; align-items: center; justify-content: center; gap: 4px;
  font-size: 0.7rem; font-weight: 600; white-space: nowrap;
  padding: 6px 12px; border-radius: 8px;
  cursor: pointer; transition: all 0.2s ease;
}
.tag-option:hover { transform: scale(1.05); filter: brightness(0.92) }
.tag-option.is-current {
  outline: 2px solid var(--primary-color); outline-offset: 1px;
  box-shadow: 0 0 0 4px rgba(92,110,88,0.08);
}

.tag-allround { background: linear-gradient(135deg, rgba(255,215,0,0.22), rgba(255,180,50,0.12)); color: #b8860b }
.tag-leadout { background: linear-gradient(135deg, rgba(0,180,255,0.18), rgba(80,200,255,0.1)); color: #1575b8 }
.tag-climber { background: linear-gradient(135deg, rgba(180,90,40,0.18), rgba(210,140,80,0.1)); color: #7a4a20 }
.tag-sprinter { background: linear-gradient(135deg, rgba(255,60,0,0.18), rgba(255,120,60,0.1)); color: #c43a00 }
.tag-tt { background: linear-gradient(135deg, rgba(90,90,90,0.18), rgba(150,150,150,0.1)); color: #444 }
.tag-tri { background: linear-gradient(135deg, rgba(255,130,0,0.2), rgba(255,180,50,0.1)); color: #b36b00 }
.tag-endurance { background: linear-gradient(135deg, rgba(40,200,110,0.2), rgba(80,220,140,0.1)); color: #188a45 }
.tag-mtb { background: linear-gradient(135deg, rgba(30,130,30,0.18), rgba(80,180,80,0.1)); color: #186a18 }
.tag-night { background: linear-gradient(135deg, rgba(100,80,200,0.18), rgba(140,120,230,0.1)); color: #4e3fa8 }
.tag-commute { background: linear-gradient(135deg, rgba(0,120,120,0.18), rgba(40,170,170,0.1)); color: #006060 }
.tag-rookie { background: linear-gradient(135deg, rgba(50,170,100,0.22), rgba(100,210,150,0.12)); color: #258a50 }
.tag-default { background: rgba(0,0,0,0.05); color: var(--text-muted) }

.glass-join {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.7rem; color: var(--text-muted); opacity: 0.7;
  transition: opacity 0.3s ease;
}
.glass-join i { font-size: 0.62rem }
.glass-member-card:hover .glass-join { opacity: 1 }

.glass-actions {
  display: flex; gap: 6px; margin-top: 4px;
  opacity: 0; transition: opacity 0.25s ease;
}
.glass-member-card:hover .glass-actions { opacity: 1 }
.glass-act-btn {
  width: 26px; height: 26px; border-radius: 50%; border: none;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.62rem; cursor: pointer; transition: all 0.2s ease;
  background: rgba(0,0,0,0.05); color: var(--text-muted);
}
.glass-act-btn:hover { transform: scale(1.12) }
.glass-act-btn.promote:hover { background: rgba(92,110,88,0.15); color: var(--primary-color) }
.glass-act-btn.demote:hover { background: rgba(255,165,0,0.15); color: #e67e22 }
.glass-act-btn.kick:hover { background: rgba(231,76,60,0.12); color: #e74c3c }

/* === 车队车库 === */
.garage-tab { display: flex; flex-direction: column; gap: 20px }
.garage-overview {
  display: flex; align-items: center; gap: 16px;
  padding: 18px 24px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(255,255,255,0.6), rgba(255,255,255,0.3));
  backdrop-filter: blur(16px) saturate(160%); -webkit-backdrop-filter: blur(16px) saturate(160%);
  border: 1px solid rgba(255,255,255,0.5);
}
.overview-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 54px }
.stat-num { font-size: 1.5rem; font-weight: 800; color: var(--primary-color); line-height: 1 }
.stat-label { font-size: 0.72rem; color: var(--text-muted); letter-spacing: 0.05em }
.overview-divider { width: 1px; height: 32px; background: rgba(0,0,0,0.07); flex-shrink: 0 }

/* 添加我的单车按钮 */
.garage-add-btn {
  margin-left: auto; padding: 8px 18px; border-radius: 12px;
  background: var(--primary-color); color: #fff; border: none;
  font-size: 0.82rem; font-weight: 700; cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
  transition: all 0.3s ease; flex-shrink: 0;
}
.garage-add-btn:hover { background: #4a5e47; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(92,110,88,0.25) }
.garage-add-btn:active { transform: translateY(0) }

/* 车库卡片移除按钮 */
.bike-remove-btn {
  position: absolute; top: 10px; right: 10px; z-index: 10;
  width: 28px; height: 28px; border-radius: 50%;
  background: rgba(0,0,0,0.04); border: none;
  color: var(--text-muted); font-size: 0.75rem;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.25s ease;
}
.bike-remove-btn:hover { background: rgba(231,76,60,0.12); color: #e74c3c }
.bike-remove-btn:disabled { cursor: not-allowed; opacity: 0.5 }

/* 添加单车弹窗 */
.add-bike-modal-overlay {
  position: fixed; inset: 0; z-index: 9000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.add-bike-modal {
  background: #fff; border-radius: 20px; width: 100%; max-width: 480px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.18); overflow: hidden;
}
.add-bike-modal-header {
  padding: 24px 28px 16px; border-bottom: 1px solid rgba(0,0,0,0.05);
  position: relative;
}
.add-bike-modal-header h3 {
  font-family: var(--font-serif); font-size: 1.25rem; margin: 0 0 4px;
  display: flex; align-items: center; gap: 8px; color: var(--primary-color);
}
.add-bike-modal-header p { font-size: 0.82rem; color: var(--text-muted); margin: 0 }
.add-bike-modal-close {
  position: absolute; top: 20px; right: 20px;
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(0,0,0,0.04); border: none;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.add-bike-modal-close:hover { background: rgba(0,0,0,0.08); color: var(--text-main) }
.add-bike-modal-body { padding: 16px 28px 28px; max-height: 60vh; overflow-y: auto }
.add-bike-loading, .add-bike-empty {
  text-align: center; padding: 40px 20px; color: var(--text-muted);
}
.add-bike-loading i, .add-bike-empty i { font-size: 1.5rem; margin-bottom: 12px; display: block; color: var(--primary-color); opacity: 0.5 }
.add-bike-empty p { font-size: 0.85rem; line-height: 1.6 }
.add-bike-list { display: flex; flex-direction: column; gap: 8px }
.add-bike-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 16px; border-radius: 14px;
  background: rgba(0,0,0,0.02); border: 1px solid rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.add-bike-item:hover { background: rgba(92,110,88,0.04); border-color: rgba(92,110,88,0.12) }
.add-bike-info { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0 }
.add-bike-img-wrap {
  width: 44px; height: 44px; border-radius: 12px; overflow: hidden; flex-shrink: 0;
  background: rgba(0,0,0,0.03);
}
.add-bike-img { width: 100%; height: 100%; object-fit: cover }
.add-bike-img-placeholder {
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); font-size: 1rem;
}
.add-bike-text { display: flex; flex-direction: column; gap: 2px; min-width: 0 }
.add-bike-name {
  font-size: 0.9rem; font-weight: 700; color: var(--text-main);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.add-bike-meta { font-size: 0.75rem; color: var(--text-muted) }
.add-bike-action-btn {
  padding: 6px 14px; border-radius: 10px; border: none;
  background: var(--primary-color); color: #fff;
  font-size: 0.78rem; font-weight: 700; cursor: pointer;
  display: inline-flex; align-items: center; gap: 5px;
  transition: all 0.2s; flex-shrink: 0;
}
.add-bike-action-btn:hover { background: #4a5e47 }
.add-bike-action-btn:disabled { opacity: 0.6; cursor: not-allowed }

.garage-filters {
  display: flex; gap: 6px; padding: 5px;
  background: rgba(0,0,0,0.03); border-radius: 28px; width: fit-content;
  flex-wrap: wrap;
}
.filter-capsule {
  padding: 8px 18px; border-radius: 22px; border: none;
  background: transparent; color: var(--text-muted);
  font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.3s ease; display: inline-flex; align-items: center; gap: 6px;
}
.filter-capsule i { font-size: 0.7rem }
.filter-capsule:hover { color: var(--primary-color) }
.filter-capsule.active {
  background: #fff; color: var(--primary-color);
  box-shadow: 0 2px 10px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.04);
}

.garage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.garage-bike-card {
  position: relative; border-radius: 16px; overflow: hidden; cursor: pointer;
  background: #fff;
  border: 1px solid rgba(0,0,0,0.06);
  transition: all 0.35s cubic-bezier(0.34,1.56,0.64,1);
}
.garage-bike-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 14px 36px rgba(0,0,0,0.1), 0 0 0 1px rgba(92,110,88,0.1);
  border-color: rgba(92,110,88,0.15);
}
/* 背景色晕染：用车辆颜色生成右上角微渐变 */
.bike-card-glow {
  position: absolute; top: -30px; right: -30px;
  width: 140px; height: 140px; border-radius: 50%;
  background: var(--bike-color); opacity: 0.07;
  filter: blur(28px); pointer-events: none;
  transition: opacity 0.35s ease, width 0.35s ease, height 0.35s ease;
}
.garage-bike-card:hover .bike-card-glow {
  opacity: 0.14; width: 180px; height: 180px;
}
.bike-card-body {
  position: relative; padding: 18px 20px 16px;
  display: flex; flex-direction: column; gap: 12px;
}
.bike-owner-row {
  display: flex; align-items: center; gap: 10px;
}
.bike-avatar-ring {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  border: 2.5px solid; padding: 2px; box-sizing: border-box;
  transition: transform 0.3s ease;
}
.garage-bike-card:hover .bike-avatar-ring { transform: scale(1.1) rotate(-4deg) }
.bike-avatar {
  width: 100%; height: 100%; border-radius: 50%;
  object-fit: cover; display: block;
}
.bike-owner-name {
  font-size: 0.8rem; font-weight: 600; color: var(--text-body); flex: 1;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.bike-type-tag {
  font-size: 0.65rem; font-weight: 700;
  padding: 3px 9px; border-radius: 8px;
  letter-spacing: 0.03em; flex-shrink: 0;
}
.type-公路车 { background: rgba(30,144,255,0.12); color: #1e90ff }
.type-山地车 { background: rgba(34,139,34,0.12); color: #228b22 }
.type-TT车 { background: rgba(100,100,100,0.12); color: #555 }
.type-砾石车 { background: rgba(22,160,133,0.12); color: #16a085 }
.type-折叠车 { background: rgba(46,204,113,0.12); color: #27ae60 }
.bike-name-row {
  display: flex; align-items: baseline; gap: 6px;
}
.bike-brand {
  font-size: 0.67rem; color: var(--text-muted); font-weight: 600;
  letter-spacing: 0.06em; text-transform: uppercase;
}
.bike-dot { color: var(--text-muted); opacity: 0.4; font-size: 0.8rem }
.bike-model {
  font-size: 1.02rem; font-weight: 800; color: var(--text-main); line-height: 1.2;
}
.bike-bottom-row {
  display: flex; align-items: center; justify-content: space-between;
}
.bike-size-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 0.72rem; color: var(--text-muted);
}
.bike-size-row i { font-size: 0.62rem; opacity: 0.5 }
/* 装饰单车剪影：右下角半透明图标 */
.bike-ghost-icon {
  font-size: 1.5rem; color: var(--bike-color);
  opacity: 0.12; transition: opacity 0.3s ease, transform 0.3s ease;
}
.garage-bike-card:hover .bike-ghost-icon {
  opacity: 0.25; transform: translateX(-4px);
}

.empty-tab { text-align: center; padding: 64px 0; color: var(--text-muted) }
.empty-tab i { font-size: 2.5rem; margin-bottom: 12px; opacity: 0.12; display: block }
.empty-tab p { font-size: 0.9rem; max-width: 360px; margin: 0 auto }

.empty-state { text-align: center; padding: 100px 0; color: var(--text-muted) }
.empty-state i { font-size: 3rem; margin-bottom: 16px; opacity: 0.3 }
.empty-state p { font-size: 0.95rem; margin-bottom: 20px }
.back-btn-secondary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 24px; background: var(--primary-color); color: #fff; border: none; border-radius: 8px; font-size: 0.9rem; font-weight: 600; cursor: pointer; transition: all 0.3s }
.back-btn-secondary:hover { background: var(--primary-dark) }

@media (max-width: 900px) { .hero-cover { height: 240px } .hero-body { gap: 24px } .hero-stats-side { gap: 8px } .side-stat { font-size: 0.78rem } }
@media (max-width: 768px) {
  .team-detail-page { padding: 100px 0 60px } .hero-cover { height: 200px }
  .hero-actions { top: 14px; right: 14px; }
  .hero-join-btn { padding: 8px 16px; font-size: 0.82rem }
  .hero-body { flex-direction: column; align-items: center; text-align: center; padding: 24px 24px 30px }
  .hero-avatar-ring { margin-top: -42px } .hero-avatar { width: 88px; height: 88px } .hero-text { padding-top: 0 }
  .team-name { font-size: 1.45rem } .hero-meta { justify-content: center; gap: 10px }
  .hero-stats-side { flex-direction: row; gap: 10px; padding-top: 8px } .side-stat { font-size: 0.78rem }
  .events-grid { grid-template-columns: 1fr } .tab-bar { flex-direction: column; align-items: stretch; gap: 12px }
  .capsule-nav { width: 100%; overflow-x: auto } .capsule-nav::-webkit-scrollbar { display: none }
  .capsule-btn { padding: 8px 18px; font-size: 0.82rem } .publish-btn { justify-content: center }
  .tab-bar-search { margin-left: 0; width: 100% } .tab-bar-search .search-input-wrap { flex: 1; width: auto }
  .members-grid { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px }
  .garage-grid { grid-template-columns: 1fr 1fr; gap: 12px }
  .garage-overview { flex-wrap: wrap; gap: 12px; padding: 14px 18px }
    .garage-add-btn { width: 100%; justify-content: center }
  .garage-filters { width: 100%; overflow-x: auto; flex-wrap: nowrap }
  .garage-filters::-webkit-scrollbar { display: none }
  .event-card-actions { opacity: 1; pointer-events: auto }
}
@media (max-width: 480px) {
  .hero-cover { height: 170px } .hero-actions { top: 10px; right: 10px; gap: 5px; } .hero-join-btn { padding: 7px 12px; font-size: 0.78rem } .hero-more-btn { width: 34px; height: 34px; font-size: 0.82rem; }
  .hero-body { padding: 20px 18px 24px } .team-name { font-size: 1.25rem }
  .hero-stats-side { gap: 6px } .side-stat { font-size: 0.72rem } .side-stat i { font-size: 0.65rem }
  .activity-item { padding: 18px 20px; gap: 14px } .event-card { padding: 16px 18px }
  .garage-grid { grid-template-columns: 1fr }
  .notice-row { padding: 8px 12px; gap: 10px } .notice-body { padding: 0 28px 12px 28px; font-size: 0.8rem }
  .notice-title { font-size: 0.82rem }
}

/* 待审核申请 */
.apply-requests { margin-bottom: 28px }
.apply-requests-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 0.88rem; font-weight: 700; color: var(--text-main);
  margin-bottom: 14px;
}
.apply-requests-header i { color: #e67e22; font-size: 0.82rem }
.apply-count {
  background: rgba(230,126,34,0.12); color: #e67e22;
  font-size: 0.7rem; font-weight: 700;
  padding: 2px 8px; border-radius: 10px;
}
.apply-list { display: flex; flex-direction: column; gap: 10px }
.apply-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-radius: 14px;
  background: linear-gradient(135deg, rgba(230,126,34,0.04), rgba(255,165,0,0.02));
  border: 1px solid rgba(230,126,34,0.1);
  transition: all 0.3s ease;
}
.apply-card:hover { border-color: rgba(230,126,34,0.2); box-shadow: 0 4px 16px rgba(230,126,34,0.08) }
.apply-avatar {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  border: 2px solid rgba(230,126,34,0.2);
}
.apply-info {
  flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px;
}
.apply-name { font-size: 0.88rem; font-weight: 700; color: var(--text-main) }
.apply-reason {
  font-size: 0.8rem; color: var(--text-muted); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.apply-time { font-size: 0.68rem; color: var(--text-muted); opacity: 0.6 }
.apply-actions { display: flex; gap: 8px; flex-shrink: 0 }
.apply-btn {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 0.72rem; cursor: pointer; transition: all 0.25s ease;
}
.apply-btn.approve { background: rgba(92,110,88,0.08); color: var(--primary-color) }
.apply-btn.approve:hover { background: var(--primary-color); color: #fff; transform: scale(1.1); box-shadow: 0 4px 12px rgba(92,110,88,0.3) }
.apply-btn.reject { background: rgba(0,0,0,0.04); color: var(--text-muted) }
.apply-btn.reject:hover { background: rgba(231,76,60,0.1); color: #e74c3c; transform: scale(1.1) }

/* ── 危险区域 ── */
.danger-zone {
  margin-top: 72px;
  position: relative;
  padding-top: 32px;
}
.danger-zone::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(0,0,0,0.12) 20%, rgba(0,0,0,0.12) 80%, transparent);
}
.danger-zone-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border: 1.5px solid rgba(220,53,69,0.25);
  border-radius: 14px;
  background: rgba(220,53,69,0.02);
  padding: 24px 28px;
}
.danger-zone-info {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.danger-zone-icon {
  font-size: 1.3rem;
  color: #dc3545;
  flex-shrink: 0;
  margin-top: 2px;
}
.danger-zone-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #dc3545;
  margin: 0 0 4px;
}
.danger-zone-desc {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin: 0;
  line-height: 1.5;
}
.danger-zone-btn {
  flex-shrink: 0;
  padding: 9px 20px;
  border-radius: 8px;
  border: 1.5px solid rgba(220,53,69,0.4);
  background: transparent;
  color: #dc3545;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.danger-zone-btn:hover {
  background: #dc3545;
  border-color: #dc3545;
  color: #fff;
  box-shadow: 0 4px 16px rgba(220,53,69,0.3);
}
@media (max-width: 600px) {
  .danger-zone-inner { flex-direction: column; align-items: flex-start; }
  .danger-zone-btn { width: 100%; justify-content: center; }
}
</style>

<style>
/* Teleport 到 body 的删除确认弹窗样式（不能 scoped，使用独立类名避免与 AuthModal 冲突） */
.del-confirm-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
  animation: del-confirm-fade-in 0.2s ease both;
}
@keyframes del-confirm-fade-in { from { opacity: 0 } to { opacity: 1 } }
.del-confirm-dialog {
  background: #fff; border-radius: 18px; padding: 36px 32px 28px;
  width: 400px; max-width: 90vw; text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  animation: del-confirm-pop-in 0.25s ease both;
}
@keyframes del-confirm-pop-in { from { transform: scale(0.92); opacity: 0 } to { transform: scale(1); opacity: 1 } }
.del-confirm-icon { width: 56px; height: 56px; border-radius: 50%; background: rgba(231,76,60,0.1); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px }
.del-confirm-icon i { font-size: 1.4rem; color: #e74c3c }
.del-confirm-icon.kick-icon { background: rgba(106,90,205,0.1) }
.del-confirm-icon.kick-icon i { color: #6a5acd }
.del-confirm-title { font-size: 1.05rem; font-weight: 700; color: var(--text-main); margin: 0 0 10px 0 }
.del-confirm-body { font-size: 0.88rem; color: var(--text-muted); line-height: 1.6; margin: 0 }
.del-confirm-actions { display: flex; gap: 12px; margin-top: 24px; justify-content: center }
.del-confirm-btn { padding: 10px 28px; border-radius: 8px; font-size: 0.88rem; font-weight: 600; cursor: pointer; transition: all 0.2s ease; border: none }
.del-confirm-btn-cancel { background: rgba(0,0,0,0.05); color: var(--text-muted) }
.del-confirm-btn-cancel:hover { background: rgba(0,0,0,0.1); color: var(--text-body) }
.del-confirm-btn-confirm { background: #e74c3c; color: #fff }
.del-confirm-btn-confirm:hover { background: #c0392b; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(231,76,60,0.35) }

/* 申请加入弹窗 */
.join-modal-dialog {
  background: #fff; border-radius: 22px; padding: 40px 36px 32px;
  width: 440px; max-width: 90vw; text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  animation: del-confirm-pop-in 0.25s ease both;
}
.join-modal-icon {
  width: 56px; height: 56px; border-radius: 16px;
  background: linear-gradient(135deg, rgba(92,110,88,0.12), rgba(92,110,88,0.05));
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
}
.join-modal-icon i { font-size: 1.3rem; color: var(--primary-color) }
.join-modal-title { font-size: 1.1rem; font-weight: 800; color: var(--text-main); margin: 0 0 6px 0 }
.join-modal-subtitle { font-size: 0.82rem; color: var(--text-muted); margin: 0 0 20px 0 }
.join-reason-input {
  width: 100%; padding: 14px 16px; border: 1.5px solid rgba(0,0,0,0.08);
  border-radius: 12px; font-size: 0.9rem; color: var(--text-main);
  background: rgba(0,0,0,0.01); transition: all 0.25s ease;
  resize: none; line-height: 1.65; font-family: inherit;
  box-sizing: border-box;
}
.join-reason-input:focus {
  border-color: var(--primary-color); background: #fff;
  outline: none; box-shadow: 0 0 0 4px rgba(92,110,88,0.08);
}
.join-reason-input::placeholder { color: rgba(0,0,0,0.2) }
.join-char-count {
  display: block; text-align: right;
  font-size: 0.7rem; color: var(--text-muted); opacity: 0.5;
  margin-top: 4px;
}
.join-modal-actions { display: flex; gap: 12px; margin-top: 20px; justify-content: center }
.join-submit-btn {
  padding: 10px 28px; border-radius: 8px; border: none;
  background: var(--primary-color); color: #fff;
  font-size: 0.88rem; font-weight: 700; cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
  transition: all 0.25s ease;
}
.join-submit-btn:hover:not(:disabled) { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 6px 16px rgba(92,110,88,0.3) }
.join-submit-btn:disabled { opacity: 0.5; cursor: not-allowed }
.join-submit-btn i { font-size: 0.78rem }

/* ── 解散车队告别页 ── */
.farewell-page {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: linear-gradient(160deg, #f8f6f3 0%, #eae6df 50%, #f0ece5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.farewell-content {
  text-align: center;
  padding: 40px;
  animation: farewell-rise 0.6s ease both;
}
@keyframes farewell-rise {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
.farewell-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(220,53,69,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28px;
}
.farewell-icon i {
  font-size: 1.6rem;
  color: #c85050;
}
.farewell-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #888;
  margin: 0 0 20px;
}
.farewell-quote {
  font-family: 'Georgia', 'STSong', serif;
  font-size: 1.6rem;
  font-weight: 400;
  color: #3a3a3a;
  letter-spacing: 4px;
  margin: 0 0 12px;
}
.farewell-sub {
  font-size: 0.88rem;
  color: #aaa;
  margin: 0 0 40px;
}
.farewell-btn {
  padding: 10px 28px;
  border-radius: 28px;
  border: 1px solid rgba(0,0,0,0.1);
  background: #fff;
  color: #888;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.farewell-btn:hover {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  box-shadow: 0 4px 16px rgba(92,110,88,0.25);
}

/* farewell transition */
.farewell-enter-active { transition: opacity 0.5s ease; }
.farewell-leave-active { transition: opacity 0.4s ease; }
.farewell-enter-from,
.farewell-leave-to { opacity: 0; }

/* ── 退出车队弹窗 ── */
.leave-team-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 28px 32px 24px;
  width: 420px;
  max-width: 92vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
}
.leave-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.leave-dialog-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary, #222);
  margin: 0;
}
.leave-dialog-desc {
  font-size: 0.88rem;
  color: var(--text-muted, #888);
  line-height: 1.6;
  margin: 0 0 16px;
}
.new-captain-list {
  flex: 1;
  overflow-y: auto;
  max-height: 280px;
  border: 1px solid rgba(0,0,0,0.06);
  border-radius: 10px;
  padding: 6px;
  margin-bottom: 18px;
}
.new-captain-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.new-captain-item:hover {
  background: rgba(255,152,0,0.06);
}
.new-captain-item.is-selected {
  background: rgba(255,152,0,0.1);
  box-shadow: inset 0 0 0 1.5px rgba(255,152,0,0.4);
}
.new-captain-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.new-captain-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.new-captain-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary, #222);
}
.new-captain-role {
  font-size: 0.75rem;
  color: var(--text-muted, #888);
}
.new-captain-check {
  color: #ff9800;
  font-size: 1.1rem;
  flex-shrink: 0;
}
.leave-dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.leave-confirm-btn {
  padding: 9px 22px;
  border-radius: 8px;
  border: none;
  background: #ff9800;
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.leave-confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.leave-confirm-btn:not(:disabled):hover {
  background: #e68900;
  box-shadow: 0 4px 16px rgba(255,152,0,0.3);
}
</style>
