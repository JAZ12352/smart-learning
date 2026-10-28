<template>
  <aside class="agent-sidebar">
    <!-- 品牌区 -->
    <div class="sb-brand" @click="$emit('home')">
      <div class="sb-brand-icon"><i class="el-icon-cpu"></i></div>
      <div class="sb-brand-text">
        <div class="sb-brand-title">智能体生成</div>
        <div class="sb-brand-sub">8 大专业 Agent 协同</div>
      </div>
    </div>

    <div class="sb-divider"></div>

    <!-- Agent 列表 -->
    <div class="sb-label">选择 Agent</div>
    <div class="sb-agents">
      <div
        v-for="a in agents"
        :key="a.key"
        :class="['sb-agent-item', { active: a.key === active }]"
        @click="$emit('switch', a)"
      >
        <div class="sb-agent-icon" :style="{ background: a.color }">
          <i :class="a.icon"></i>
        </div>
        <div class="sb-agent-text">
          <div class="sb-agent-name">{{ a.name }}</div>
          <div class="sb-agent-desc">{{ a.label }}</div>
        </div>
        <i v-if="a.key === active" class="el-icon-arrow-right sb-agent-arrow"></i>
      </div>
    </div>

    <div class="sb-spacer"></div>

    <!-- 底部入口 -->
    <div class="sb-footer">
      <div class="sb-footer-item" @click="$emit('collab')">
        <i class="el-icon-connection"></i><span>多智能体协作</span>
      </div>
      <div class="sb-footer-item" @click="$emit('report')">
        <i class="el-icon-data-analysis"></i><span>实验报告</span>
      </div>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'AgentSidebar',
  props: {
    active: { type: String, default: 'document' }
  },
  data () {
    return {
      agents: [
        { key: 'document', name: 'Doc Agent', label: '课程文档', icon: 'el-icon-document', color: '#CCFF00' },
        { key: 'mind_map', name: 'MindMap Agent', label: '思维导图', icon: 'el-icon-share', color: '#A9F0FF' },
        { key: 'exercise', name: 'Quiz Agent', label: '练习题目', icon: 'el-icon-edit-outline', color: '#22c55e' },
        { key: 'extended_reading', name: 'Reading Agent', label: '拓展阅读', icon: 'el-icon-reading', color: '#f59e0b' },
        { key: 'video', name: 'Video Agent', label: '教学视频', icon: 'el-icon-video-camera', color: '#ef4444' },
        { key: 'code_example', name: 'Code Agent', label: '代码案例', icon: 'el-icon-cpu', color: '#A9F0FF' },
        { key: 'operation_guide', name: 'Guide Agent', label: '实操指南', icon: 'el-icon-guide', color: '#CCFF00' },
        { key: 'ppt', name: 'PPT Agent', label: 'PPT 课件', icon: 'el-icon-data-board', color: '#A9F0FF' }
      ]
    }
  }
}
</script>

<style scoped>
.agent-sidebar {
  width: 248px;
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(175deg, #1a3558 0%, var(--ink-4) 30%, #203e66 55%, var(--ink-4) 75%, #1a3558 100%);
  color: var(--ice);
  padding: 20px 14px;
  overflow-y: auto;
}
.agent-sidebar::-webkit-scrollbar { width: 4px; }
.agent-sidebar::-webkit-scrollbar-thumb { background: rgba(var(--acid-rgb), 0.2); border-radius: 2px; }

/* 品牌 */
.sb-brand { display: flex; align-items: center; gap: 12px; padding: 6px 8px 16px; cursor: pointer; }
.sb-brand-icon {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--acid) 0%, var(--ice) 50%, var(--acid) 100%);
  color: #fff; font-size: 20px;
  box-shadow: 0 6px 16px rgba(var(--acid-rgb), 0.45);
}
.sb-brand-title { font-size: 15px; font-weight: 700; color: #fff; letter-spacing: 0.5px; }
.sb-brand-sub { font-size: 11px; color: rgba(var(--ice-rgb), 0.6); margin-top: 2px; }

.sb-divider { height: 1px; background: rgba(255,255,255,0.08); margin: 0 4px 16px; }

.sb-label { font-size: 11px; color: rgba(var(--ice-rgb), 0.5); letter-spacing: 1px; padding: 0 10px 10px; font-weight: 600; }

.sb-agents { display: flex; flex-direction: column; gap: 4px; }
.sb-agent-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 12px; cursor: pointer;
  position: relative; transition: background 0.2s ease;
}
.sb-agent-item:hover { background: rgba(var(--acid-rgb), 0.12); }
.sb-agent-item.active { background: linear-gradient(135deg, rgba(var(--acid-rgb), 0.25), rgba(var(--acid-rgb), 0.1)); }
.sb-agent-item.active::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 28px; border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--acid), var(--ice));
  box-shadow: 0 0 10px rgba(var(--acid-rgb), 0.6);
}
.sb-agent-icon {
  width: 36px; height: 36px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 18px;
}
.sb-agent-text { flex: 1; min-width: 0; }
.sb-agent-name { font-size: 13px; font-weight: 600; color: var(--ice); }
.sb-agent-item.active .sb-agent-name { color: #fff; }
.sb-agent-desc { font-size: 11px; color: rgba(var(--ice-rgb), 0.55); margin-top: 1px; }
.sb-agent-item.active .sb-agent-desc { color: rgba(232, 237, 228, 0.8); }
.sb-agent-arrow { color: var(--ice); font-size: 14px; }

.sb-spacer { flex: 1; }

.sb-footer { border-top: 1px solid rgba(255,255,255,0.08); padding-top: 12px; display: flex; flex-direction: column; gap: 2px; }
.sb-footer-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 12px; border-radius: 10px; cursor: pointer;
  font-size: 13px; color: rgba(var(--ice-rgb), 0.7); transition: all 0.2s;
}
.sb-footer-item i { font-size: 15px; color: var(--acid); }
.sb-footer-item:hover { background: rgba(var(--acid-rgb), 0.12); color: var(--ice); }
</style>
