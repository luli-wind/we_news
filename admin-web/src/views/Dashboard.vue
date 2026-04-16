<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">控制台总览</h1>
        <p class="page-subtitle">查看内容生产、审核和运营状态</p>
      </div>
    </div>

    <div class="stat-grid">
      <div class="inline-stat">
        <div class="inline-stat__label">新闻总数</div>
        <div class="inline-stat__value">128</div>
      </div>
      <div class="inline-stat">
        <div class="inline-stat__label">视频总数</div>
        <div class="inline-stat__value">64</div>
      </div>
      <div class="inline-stat">
        <div class="inline-stat__label">待审核投稿</div>
        <div class="inline-stat__value">18</div>
      </div>
      <div class="inline-stat">
        <div class="inline-stat__label">今日操作量</div>
        <div class="inline-stat__value">302</div>
      </div>
    </div>

    <div class="card-surface" style="margin-top: 14px">
      <el-alert
        title="系统运行正常，可通过左侧菜单进入各模块管理内容。"
        type="success"
        show-icon
        :closable="false"
      />
    </div>
  </div>
</template>
