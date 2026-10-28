<template>
  <div
    class="floating-teacher"
    :class="{ expanded: showPanel, dragging: isDragging }"
    :style="floatStyle"
  >
    <!-- ═══ 悬浮数字人形象 ═══ -->
    <transition name="float-bounce">
      <div
        v-if="!showPanel"
        class="float-bubble"
        @mousedown.prevent="onFloatMouseDown"
        title="达尼娅 · AI 虚拟教师 · 可拖拽"
      >
        <!-- 达尼娅 — Q版设计形象 120×170 -->
        <div class="avatar-wrap" :class="[ expression, idleAction ? 'action-'+idleAction : '' ]">
          <svg viewBox="0 0 120 170" class="avatar-svg" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="ftHairF" x1=".2" y1="0" x2=".8" y2="1">
                <stop offset="0%" stop-color="#FFDCEB"/><stop offset="45%" stop-color="#F9BBD3"/><stop offset="100%" stop-color="#E492B4"/>
              </linearGradient>
              <linearGradient id="ftHairB" x1=".2" y1="0" x2=".8" y2="1">
                <stop offset="0%" stop-color="#F3B0CC"/><stop offset="55%" stop-color="#E39ABA"/><stop offset="100%" stop-color="#C67C9C"/>
              </linearGradient>
              <linearGradient id="ftBraid" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="#FBC9DD"/><stop offset="42%" stop-color="#F2A7C6"/><stop offset="100%" stop-color="#D283A4"/>
              </linearGradient>
              <linearGradient id="ftDress" x1="0" y1="0" x2=".15" y2="1">
                <stop offset="0%" stop-color="#FFFFFF"/><stop offset="62%" stop-color="#FBF7F1"/><stop offset="100%" stop-color="#E6DECF"/>
              </linearGradient>
              <linearGradient id="ftSkin" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#FFF0E6"/><stop offset="100%" stop-color="#FBDAC6"/>
              </linearGradient>
              <radialGradient id="ftGlow" cx="50%" cy="50%">
                <stop offset="0%" stop-color="rgba(255,198,224,.22)"/><stop offset="100%" stop-color="rgba(255,198,224,0)"/>
              </radialGradient>
            </defs>

            <ellipse cx="60" cy="92" rx="54" ry="78" fill="url(#ftGlow)"/>

            <!-- 后发 -->
            <path d="M60 30 C45.5 30 37 39 34.6 54 C32.6 70 30.4 92 31 110
                     C31.4 120 34 127 38 129.5 C38.6 126 39.2 122.6 39.8 119.2
                     C41.4 123.2 44 126.4 47.2 128.2 C47.6 124.4 47.8 120.4 48 116.4
                     C50.2 120.8 53.2 124.6 56.8 126.8 C57.6 122.6 58 118.4 58.2 114.2
                     L61.8 114.2 C62 118.4 62.4 122.6 63.2 126.8
                     C66.8 124.6 69.8 120.8 72 116.4 C72.2 120.4 72.4 124.4 72.8 128.2
                     C76 126.4 78.6 123.2 80.2 119.2 C80.8 122.6 81.4 126 82 129.5
                     C86 127 88.6 120 89 110 C89.6 92 87.4 70 85.4 54 C83 39 74.5 30 60 30 Z"
                  fill="url(#ftHairB)"/>

            <!-- 脖子 -->
            <path d="M54.2 70 C54.2 77.4 52.4 81.4 48.4 86 L71.6 86 C67.6 81.4 65.8 77.4 65.8 70 Z" fill="#FBDDC9"/>
            <ellipse cx="60" cy="78" rx="8" ry="2.6" fill="#E8B49B" opacity=".35"/>

            <!-- 白裙 A 字 + 泡泡袖 -->
            <path d="M46.5 82 C52 85.8 68 85.8 73.5 82 C80.4 85 84 95 83 104.4
                     L85 119 C76 125.6 44 125.6 35 119 L37 104.4 C36 95 39.6 85 46.5 82 Z"
                  fill="url(#ftDress)"/>
            <path d="M45 86.4 C38 88.4 34 95.4 34.6 102.4 C34.9 107.4 38.2 108.4 40.2 104.4 C41.2 98.4 43.2 91.4 47 87.4 Z" fill="#FFFFFF"/>
            <path d="M75 86.4 C82 88.4 86 95.4 85.4 102.4 C85.1 107.4 81.8 108.4 79.8 104.4 C78.8 98.4 76.8 91.4 73 87.4 Z" fill="#F8F3EC"/>
            <g>
              <path d="M59.4 86.4 C56.4 84.6 53.4 85 53.2 86.8 C53 88.8 56 89.6 59.4 87.8 Z" fill="#F7DDE7"/>
              <path d="M60.6 86.4 C63.6 84.6 66.6 85 66.8 86.8 C67 88.8 64 89.6 60.6 87.8 Z" fill="#F1D2DF"/>
              <circle cx="60" cy="86.8" r="1.4" fill="#EFC8D9"/>
            </g>
            <path d="M40 103.4 Q60 108.4 80 103.4" fill="none" stroke="#E7DDD0" stroke-width=".9"/>
            <circle cx="60" cy="96.4" r="1.05" fill="#D9C9B6"/>
            <circle cx="60" cy="102.4" r="1.05" fill="#D9C9B6"/>
            <g stroke="#EDE5D8" stroke-width=".8" fill="none">
              <path d="M46 109 L44.4 121.6"/><path d="M60 111 L60 123.4"/><path d="M74 109 L75.6 121.6"/>
            </g>

            <!-- 腿 + 鞋 -->
            <rect x="51.2" y="118" width="7.2" height="24" rx="3.6" fill="#FBDFCE"/>
            <rect x="61.6" y="118" width="7.2" height="24" rx="3.6" fill="#F7D4C1"/>
            <ellipse cx="54.4" cy="144" rx="6.4" ry="4.1" fill="#FFFFFF"/>
            <ellipse cx="65.6" cy="144" rx="6.4" ry="4.1" fill="#F7F2EA"/>
            <path d="M48.4 143.2 Q54.4 146.4 60.4 143.2" fill="none" stroke="#DCD1C1" stroke-width=".85"/>
            <path d="M59.6 143.2 Q65.6 146.4 71.6 143.2" fill="none" stroke="#DCD1C1" stroke-width=".85"/>
            <path d="M48.6 139.6 Q54.4 141.6 60.2 139.6" fill="none" stroke="#E4B9A4" stroke-width=".9"/>
            <path d="M59.8 139.6 Q65.6 141.6 71.4 139.6" fill="none" stroke="#E4B9A4" stroke-width=".9"/>

            <!-- 脸 -->
            <ellipse cx="60" cy="55" rx="25" ry="24" fill="url(#ftSkin)"/>
            <ellipse cx="45" cy="68" rx="5.2" ry="2.9" fill="#F9AFC3" opacity=".55">
              <animate attributeName="opacity" values=".4;.62;.4" dur="3.6s" repeatCount="indefinite"/>
            </ellipse>
            <ellipse cx="75" cy="68" rx="5.2" ry="2.9" fill="#F9AFC3" opacity=".55">
              <animate attributeName="opacity" values=".4;.62;.4" dur="3.6s" repeatCount="indefinite" begin="1.8s"/>
            </ellipse>
            <g fill="none" stroke="#C4878F" stroke-width="1.4" stroke-linecap="round" opacity=".7">
              <path d="M45.6 56.6 Q50.5 54 55.4 56.2"/>
              <path d="M64.6 56.2 Q69.5 54 74.4 56.6"/>
            </g>
            <g fill="none" stroke="#4A3230" stroke-width="2.6" stroke-linecap="round">
              <path d="M44.5 62.5 Q50.5 70.5 56.5 62.5"/>
              <path d="M63.5 62.5 Q69.5 70.5 75.5 62.5"/>
            </g>
            <g stroke="#4A3230" stroke-width="1.5" stroke-linecap="round">
              <path d="M43.1 63.4 L39.9 65.2"/><path d="M76.9 63.4 L80.1 65.2"/>
            </g>
            <path d="M56.6 72.2 Q60 77.4 63.4 72.2 Q60 73.8 56.6 72.2 Z" fill="#C9606F"/>

            <!-- 刘海 -->
            <path d="M60 30.5 C44.4 30.5 34.8 40 34.2 52.4 C34.2 55.8 34.6 58.4 35.2 60.4
                     C36.8 55 39.6 51.5 43 49.4 Q51.5 46.4 60 46.2 Q68.5 46.4 77 49.4
                     C80.4 51.5 83.2 55 84.8 60.4 C85.4 58.4 85.8 55.8 85.8 52.4 C85.2 40 75.6 30.5 60 30.5 Z"
                  fill="url(#ftHairF)"/>
            <g fill="none" stroke="#FFF3F9" stroke-width="2" stroke-linecap="round" opacity=".22">
              <path d="M46.2 39.6 Q50 35 55.4 32.6"/><path d="M73.8 39.6 Q70 35 64.6 32.6"/>
            </g>

            <!-- 鬓发 -->
            <path d="M34.6 48 C31.4 60 31.8 73 33.4 85 C33.9 89.4 38.2 89.4 38.2 85
                     C36.6 73 37 60 40.2 50 Z" fill="url(#ftHairF)"/>
            <path d="M85.4 48 C88.6 60 88.2 73 86.6 85 C86.1 89.4 81.8 89.4 81.8 85
                     C83.4 73 83 60 79.8 50 Z" fill="url(#ftHairF)"/>

            <!-- 深棕编发发带 -->
            <path d="M37.6 44 C36.4 34.4 46.4 27.4 60 27.4 C73.6 27.4 83.6 34.4 82.4 44"
                  fill="none" stroke="#6B4A3A" stroke-width="4.4" stroke-linecap="round"/>
            <g stroke="#C79A76" stroke-width="1" stroke-linecap="round" opacity=".95">
              <path d="M41.6 39.4 l2.4 -2.4 M41.6 37 l2.4 2.4"/>
              <path d="M47.4 33.6 l2.4 -2.4 M47.4 31.2 l2.4 2.4"/>
              <path d="M53.6 30.2 l2.4 -2.4 M53.6 27.8 l2.4 2.4"/>
              <path d="M59.8 28.9 l2.4 -2.4 M59.8 26.5 l2.4 2.4"/>
              <path d="M66 30.2 l2.4 -2.4 M66 27.8 l2.4 2.4"/>
              <path d="M72.2 33.6 l2.4 -2.4 M72.2 31.2 l2.4 2.4"/>
              <path d="M78.2 39.4 l2.4 -2.4 M78.2 37 l2.4 2.4"/>
            </g>

            <!-- 双麻花辫 -->
            <g>
              <path d="M40.2 62 C37.4 78 36.4 92 36 104 C35.8 110 35 113 32.6 113
                       C30.2 113 29.3 110 29 104 C28.4 92 28.8 78 29.6 70 Q33.5 64 40.2 62 Z" fill="url(#ftBraid)"/>
              <path d="M40.2 62 C37.4 78 36.4 92 36 104 C35.8 110 35 113 32.6 113
                       C30.2 113 29.3 110 29 104 C28.4 92 28.8 78 29.6 70"
                    fill="none" stroke="#BE7091" stroke-width=".9" stroke-linecap="round"/>
              <g stroke="#AF6284" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".75">
                <path d="M37.8 72 Q34 76.6 30.2 72.6"/><path d="M37.4 80.4 Q33.8 85 30 81"/>
                <path d="M37.1 88.8 Q33.6 93.4 29.9 89.4"/><path d="M36.8 97.2 Q33.5 101.6 30 97.8"/>
                <path d="M36.6 105.4 Q33.4 109.4 30.1 106"/>
              </g>
              <path d="M31.2 113 Q27.8 119 30.4 125.4 Q34.4 119.8 36.6 113.8 Z" fill="#E79DBC" stroke="#BE7091" stroke-width=".8" stroke-linejoin="round"/>
              <path d="M33.2 112.6 Q37 112.4 36.6 108.8" fill="none" stroke="#B2668A" stroke-width="1.3" stroke-linecap="round"/>

              <path d="M79.8 62 C82.6 78 83.6 92 84 104 C84.2 110 85 113 87.4 113
                       C89.8 113 90.7 110 91 104 C91.6 92 91.2 78 90.4 70 Q86.5 64 79.8 62 Z" fill="url(#ftBraid)"/>
              <path d="M79.8 62 C82.6 78 83.6 92 84 104 C84.2 110 85 113 87.4 113
                       C89.8 113 90.7 110 91 104 C91.6 92 91.2 78 90.4 70"
                    fill="none" stroke="#BE7091" stroke-width=".9" stroke-linecap="round"/>
              <g stroke="#AF6284" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".75">
                <path d="M82.2 72 Q86 76.6 89.8 72.6"/><path d="M82.6 80.4 Q86.2 85 90 81"/>
                <path d="M82.9 88.8 Q86.4 93.4 90.1 89.4"/><path d="M83.2 97.2 Q86.5 101.6 90 97.8"/>
                <path d="M83.4 105.4 Q86.6 109.4 89.9 106"/>
              </g>
              <path d="M88.8 113 Q92.2 119 89.6 125.4 Q85.6 119.8 83.4 113.8 Z" fill="#DD90B1" stroke="#BE7091" stroke-width=".8" stroke-linejoin="round"/>
              <path d="M86.8 112.6 Q83 112.4 83.4 108.8" fill="none" stroke="#B2668A" stroke-width="1.3" stroke-linecap="round"/>
            </g>

            <!-- 白色羽毛蝴蝶结 -->
            <g>
              <path d="M92 36 C96.5 25 104 17 111.5 14 C108.5 23 101 32 94 38 Z" fill="#FFFFFF" stroke="#E7DEE9" stroke-width=".7"/>
              <path d="M93 39 C99 33 106.5 29.5 113 28.5 C108 34.5 100.5 39.5 94.5 42 Z" fill="#FDFBFF" stroke="#E7DEE9" stroke-width=".7"/>
              <path d="M95 34 C100 27 105.5 21 110 17.6" fill="none" stroke="#E1D7E5" stroke-width=".6"/>
              <path d="M95.6 38 C100.6 34 106 31 111 29.8" fill="none" stroke="#E1D7E5" stroke-width=".6"/>
              <path d="M92 38 C86 32.5 80 34.5 80 39.5 C80 44.5 86 45.5 92 41 Z" fill="#FFFFFF" stroke="#E6DCE8" stroke-width=".8"/>
              <path d="M92 38 C97 33.5 102 35.5 102 40 C102 44.5 97 45 92 41 Z" fill="#FCF8FE" stroke="#E6DCE8" stroke-width=".8"/>
              <circle cx="92" cy="39.6" r="2.7" fill="#FBEEF6" stroke="#E6DCE8" stroke-width=".8"/>
            </g>

            <!-- 左侧小宝石 -->
            <circle cx="37.4" cy="37.4" r="2.9" fill="#A6BEDC"/>
            <circle cx="36.6" cy="36.6" r="1" fill="#EDF4FC" opacity=".9"/>

            <!-- 漂浮光点 -->
            <circle cx="22" cy="98" r="2.2" fill="#FFE3F0" opacity=".5">
              <animate attributeName="cy" values="98;84;72" dur="4s" repeatCount="indefinite"/>
              <animate attributeName="opacity" values=".55;.2;0" dur="4s" repeatCount="indefinite"/>
            </circle>
            <circle cx="102" cy="102" r="1.8" fill="#FFE3F0" opacity=".4">
              <animate attributeName="cy" values="102;88;76" dur="4.6s" repeatCount="indefinite" begin="1.2s"/>
              <animate attributeName="opacity" values=".45;.15;0" dur="4.6s" repeatCount="indefinite" begin="1.2s"/>
            </circle>
          </svg>
        </div>
      </div>
    </transition>

    <!-- ═══ 互动弹幕（挂载在浮窗内部，跟随拖拽，与虚拟教师连一体）═══ -->
    <transition name="idle-bubble">
      <div v-if="!showPanel && idleBubbleVisible" class="idle-bubble">
        <span class="idle-bubble-text">{{ idleBubbleText }}</span>
        <i class="idle-bubble-tail"></i>
      </div>
    </transition>

    <!-- ═══ 展开面板 ═══ -->
    <transition name="panel-slide">
      <div v-if="showPanel" class="float-panel">
        <!-- 面板头部 -->
        <div class="panel-header">
          <div class="panel-header-left">
            <div class="panel-chibi">
              <svg viewBox="0 0 48 48" class="chibi-small" xmlns="http://www.w3.org/2000/svg">
                <circle cx="24" cy="24" r="22" fill="#E9A0BE"/>
                <ellipse cx="24" cy="26" rx="16.5" ry="15.5" fill="#FFF6EF"/>
                <ellipse cx="16.4" cy="31.4" rx="3.2" ry="1.8" fill="#F9AFC3" opacity=".6"/>
                <ellipse cx="31.6" cy="31.4" rx="3.2" ry="1.8" fill="#F9AFC3" opacity=".6"/>
                <g fill="none" stroke="#4A3230" stroke-width="1.8" stroke-linecap="round">
                  <path d="M14.4 25.4 Q17.8 30 21.2 25.4"/>
                  <path d="M26.8 25.4 Q30.2 30 33.6 25.4"/>
                </g>
                <path d="M21.4 34 Q24 36.8 26.6 34 Q24 35.1 21.4 34 Z" fill="#C9606F"/>
                <path d="M24 9 C13.4 9 8.4 16 8.2 24.4 C8.2 27.6 8.6 29.8 9.2 31.6
                         C10.2 23 14 17.8 19.4 16.8 C20.8 13.4 22.4 11.4 24 10.4
                         C25.6 11.4 27.2 13.4 28.6 16.8 C34 17.8 37.8 23 38.8 31.6
                         C39.4 29.8 39.8 27.6 39.8 24.4 C39.6 16 34.6 9 24 9 Z" fill="#F7B6D0"/>
                <path d="M9.2 20.6 C12.2 13.4 17.4 10 24 10 C30.6 10 35.8 13.4 38.8 20.6
                         L36.9 22.3 C34.1 16.4 29.8 13.6 24 13.6 C18.2 13.6 13.9 16.4 11.1 22.3 Z"
                      fill="#6B4A3A"/>
                <path d="M39.2 18.4 C41.2 14.4 44.6 11.8 47.2 11.2 C45.6 14.6 42.6 17.6 40 19.2 Z"
                      fill="#FFFFFF" stroke="#E7DEE9" stroke-width=".5"/>
                <circle cx="38.6" cy="20.8" r="1.9" fill="#FFFFFF" stroke="#E6DCE8" stroke-width=".6"/>
              </svg>
            </div>
            <div class="panel-title-group">
              <span class="panel-title">达尼娅</span>
              <span class="panel-context">AI 虚拟教师 · {{ contextTitle }}</span>
            </div>
          </div>
          <div class="panel-header-right">
            <!-- 语音开关 -->
            <el-tooltip :content="voiceOn ? '关闭语音讲解' : '开启语音讲解'" placement="bottom" effect="light">
              <button :class="['voice-toggle-btn', { on: voiceOn }]" @click="voiceOn = !voiceOn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="voice-icon">
                  <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
                  <path v-if="voiceOn" d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/>
                  <line v-if="!voiceOn" x1="23" y1="9" x2="17" y2="15"/>
                  <line v-if="!voiceOn" x1="17" y1="9" x2="23" y2="15"/>
                </svg>
              </button>
            </el-tooltip>
            <el-button size="mini" icon="el-icon-minus" circle @click="minimizePanel" title="最小化" />
            <el-button size="mini" icon="el-icon-close" circle @click="closePanel" title="关闭" />
          </div>
        </div>

        <!-- 面板内容 -->
        <div class="panel-body">
          <div class="chat-messages" ref="chatMsgs">
            <!-- ═══ 欢迎界面 — 达尼娅立绘 + 介绍 ═══ -->
            <div v-if="messages.length === 0 && !loading" class="welcome-area">
              <div class="welcome-chibi" :class="{ 'welcome-breathing': true }">
                <!-- Q版达尼娅全身SVG - 直接显示 -->
                                <svg viewBox="0 0 120 170" class="daninya-full" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="ftFullHairF" x1=".2" y1="0" x2=".8" y2="1">
                      <stop offset="0%" stop-color="#FFDCEB"/><stop offset="45%" stop-color="#F9BBD3"/><stop offset="100%" stop-color="#E492B4"/>
                    </linearGradient>
                    <linearGradient id="ftFullHairB" x1=".2" y1="0" x2=".8" y2="1">
                      <stop offset="0%" stop-color="#F3B0CC"/><stop offset="55%" stop-color="#E39ABA"/><stop offset="100%" stop-color="#C67C9C"/>
                    </linearGradient>
                    <linearGradient id="ftFullBraid" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stop-color="#FBC9DD"/><stop offset="42%" stop-color="#F2A7C6"/><stop offset="100%" stop-color="#D283A4"/>
                    </linearGradient>
                    <linearGradient id="ftFullDress" x1="0" y1="0" x2=".15" y2="1">
                      <stop offset="0%" stop-color="#FFFFFF"/><stop offset="62%" stop-color="#FBF7F1"/><stop offset="100%" stop-color="#E6DECF"/>
                    </linearGradient>
                    <linearGradient id="ftFullSkin" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#FFF0E6"/><stop offset="100%" stop-color="#FBDAC6"/>
                    </linearGradient>
                    <radialGradient id="ftFullGlow" cx="50%" cy="50%">
                      <stop offset="0%" stop-color="rgba(255,198,224,.24)"/><stop offset="100%" stop-color="rgba(255,198,224,0)"/>
                    </radialGradient>
                  </defs>

                  <ellipse cx="60" cy="92" rx="54" ry="78" fill="url(#ftFullGlow)"/>

                  <!-- 后发 -->
                  <path d="M60 30 C45.5 30 37 39 34.6 54 C32.6 70 30.4 92 31 110
                           C31.4 120 34 127 38 129.5 C38.6 126 39.2 122.6 39.8 119.2
                           C41.4 123.2 44 126.4 47.2 128.2 C47.6 124.4 47.8 120.4 48 116.4
                           C50.2 120.8 53.2 124.6 56.8 126.8 C57.6 122.6 58 118.4 58.2 114.2
                           L61.8 114.2 C62 118.4 62.4 122.6 63.2 126.8
                           C66.8 124.6 69.8 120.8 72 116.4 C72.2 120.4 72.4 124.4 72.8 128.2
                           C76 126.4 78.6 123.2 80.2 119.2 C80.8 122.6 81.4 126 82 129.5
                           C86 127 88.6 120 89 110 C89.6 92 87.4 70 85.4 54 C83 39 74.5 30 60 30 Z"
                        fill="url(#ftFullHairB)"/>

                  <!-- 脖子 -->
                  <path d="M54.2 70 C54.2 77.4 52.4 81.4 48.4 86 L71.6 86 C67.6 81.4 65.8 77.4 65.8 70 Z" fill="#FBDDC9"/>
                  <ellipse cx="60" cy="78" rx="8" ry="2.6" fill="#E8B49B" opacity=".35"/>

                  <!-- 白裙 A 字 + 泡泡袖 -->
                  <path d="M46.5 82 C52 85.8 68 85.8 73.5 82 C80.4 85 84 95 83 104.4
                           L85 119 C76 125.6 44 125.6 35 119 L37 104.4 C36 95 39.6 85 46.5 82 Z"
                        fill="url(#ftFullDress)"/>
                  <path d="M45 86.4 C38 88.4 34 95.4 34.6 102.4 C34.9 107.4 38.2 108.4 40.2 104.4 C41.2 98.4 43.2 91.4 47 87.4 Z" fill="#FFFFFF"/>
                  <path d="M75 86.4 C82 88.4 86 95.4 85.4 102.4 C85.1 107.4 81.8 108.4 79.8 104.4 C78.8 98.4 76.8 91.4 73 87.4 Z" fill="#F8F3EC"/>
                  <g>
                    <path d="M59.4 86.4 C56.4 84.6 53.4 85 53.2 86.8 C53 88.8 56 89.6 59.4 87.8 Z" fill="#F7DDE7"/>
                    <path d="M60.6 86.4 C63.6 84.6 66.6 85 66.8 86.8 C67 88.8 64 89.6 60.6 87.8 Z" fill="#F1D2DF"/>
                    <circle cx="60" cy="86.8" r="1.4" fill="#EFC8D9"/>
                  </g>
                  <path d="M40 103.4 Q60 108.4 80 103.4" fill="none" stroke="#E7DDD0" stroke-width=".9"/>
                  <circle cx="60" cy="96.4" r="1.05" fill="#D9C9B6"/>
                  <circle cx="60" cy="102.4" r="1.05" fill="#D9C9B6"/>
                  <g stroke="#EDE5D8" stroke-width=".8" fill="none">
                    <path d="M46 109 L44.4 121.6"/><path d="M60 111 L60 123.4"/><path d="M74 109 L75.6 121.6"/>
                  </g>

                  <!-- 腿 + 鞋 -->
                  <rect x="51.2" y="118" width="7.2" height="24" rx="3.6" fill="#FBDFCE"/>
                  <rect x="61.6" y="118" width="7.2" height="24" rx="3.6" fill="#F7D4C1"/>
                  <ellipse cx="54.4" cy="144" rx="6.4" ry="4.1" fill="#FFFFFF"/>
                  <ellipse cx="65.6" cy="144" rx="6.4" ry="4.1" fill="#F7F2EA"/>
                  <path d="M48.4 143.2 Q54.4 146.4 60.4 143.2" fill="none" stroke="#DCD1C1" stroke-width=".85"/>
                  <path d="M59.6 143.2 Q65.6 146.4 71.6 143.2" fill="none" stroke="#DCD1C1" stroke-width=".85"/>
                  <path d="M48.6 139.6 Q54.4 141.6 60.2 139.6" fill="none" stroke="#E4B9A4" stroke-width=".9"/>
                  <path d="M59.8 139.6 Q65.6 141.6 71.4 139.6" fill="none" stroke="#E4B9A4" stroke-width=".9"/>

                  <!-- 脸 -->
                  <ellipse cx="60" cy="55" rx="25" ry="24" fill="url(#ftFullSkin)"/>
                  <ellipse cx="45" cy="68" rx="5.2" ry="2.9" fill="#F9AFC3" opacity=".55">
                    <animate attributeName="opacity" values=".4;.62;.4" dur="3.6s" repeatCount="indefinite"/>
                  </ellipse>
                  <ellipse cx="75" cy="68" rx="5.2" ry="2.9" fill="#F9AFC3" opacity=".55">
                    <animate attributeName="opacity" values=".4;.62;.4" dur="3.6s" repeatCount="indefinite" begin="1.8s"/>
                  </ellipse>
                  <g fill="none" stroke="#C4878F" stroke-width="1.4" stroke-linecap="round" opacity=".7">
                    <path d="M45.6 56.6 Q50.5 54 55.4 56.2"/>
                    <path d="M64.6 56.2 Q69.5 54 74.4 56.6"/>
                  </g>
                  <g fill="none" stroke="#4A3230" stroke-width="2.6" stroke-linecap="round">
                    <path d="M44.5 62.5 Q50.5 70.5 56.5 62.5"/>
                    <path d="M63.5 62.5 Q69.5 70.5 75.5 62.5"/>
                  </g>
                  <g stroke="#4A3230" stroke-width="1.5" stroke-linecap="round">
                    <path d="M43.1 63.4 L39.9 65.2"/><path d="M76.9 63.4 L80.1 65.2"/>
                  </g>
                  <path d="M56.6 72.2 Q60 77.4 63.4 72.2 Q60 73.8 56.6 72.2 Z" fill="#C9606F"/>

                  <!-- 刘海 -->
                  <path d="M60 30.5 C44.4 30.5 34.8 40 34.2 52.4 C34.2 55.8 34.6 58.4 35.2 60.4
                           C36.8 55 39.6 51.5 43 49.4 Q51.5 46.4 60 46.2 Q68.5 46.4 77 49.4
                           C80.4 51.5 83.2 55 84.8 60.4 C85.4 58.4 85.8 55.8 85.8 52.4 C85.2 40 75.6 30.5 60 30.5 Z"
                        fill="url(#ftFullHairF)"/>
                  <g fill="none" stroke="#FFF3F9" stroke-width="2" stroke-linecap="round" opacity=".22">
                    <path d="M46.2 39.6 Q50 35 55.4 32.6"/><path d="M73.8 39.6 Q70 35 64.6 32.6"/>
                  </g>

                  <!-- 鬓发 -->
                  <path d="M34.6 48 C31.4 60 31.8 73 33.4 85 C33.9 89.4 38.2 89.4 38.2 85
                           C36.6 73 37 60 40.2 50 Z" fill="url(#ftFullHairF)"/>
                  <path d="M85.4 48 C88.6 60 88.2 73 86.6 85 C86.1 89.4 81.8 89.4 81.8 85
                           C83.4 73 83 60 79.8 50 Z" fill="url(#ftFullHairF)"/>

                  <!-- 深棕编发发带 -->
                  <path d="M37.6 44 C36.4 34.4 46.4 27.4 60 27.4 C73.6 27.4 83.6 34.4 82.4 44"
                        fill="none" stroke="#6B4A3A" stroke-width="4.4" stroke-linecap="round"/>
                  <g stroke="#C79A76" stroke-width="1" stroke-linecap="round" opacity=".95">
                    <path d="M41.6 39.4 l2.4 -2.4 M41.6 37 l2.4 2.4"/>
                    <path d="M47.4 33.6 l2.4 -2.4 M47.4 31.2 l2.4 2.4"/>
                    <path d="M53.6 30.2 l2.4 -2.4 M53.6 27.8 l2.4 2.4"/>
                    <path d="M59.8 28.9 l2.4 -2.4 M59.8 26.5 l2.4 2.4"/>
                    <path d="M66 30.2 l2.4 -2.4 M66 27.8 l2.4 2.4"/>
                    <path d="M72.2 33.6 l2.4 -2.4 M72.2 31.2 l2.4 2.4"/>
                    <path d="M78.2 39.4 l2.4 -2.4 M78.2 37 l2.4 2.4"/>
                  </g>

                  <!-- 双麻花辫 -->
                  <g>
                    <path d="M40.2 62 C37.4 78 36.4 92 36 104 C35.8 110 35 113 32.6 113
                             C30.2 113 29.3 110 29 104 C28.4 92 28.8 78 29.6 70 Q33.5 64 40.2 62 Z" fill="url(#ftFullBraid)"/>
                    <path d="M40.2 62 C37.4 78 36.4 92 36 104 C35.8 110 35 113 32.6 113
                             C30.2 113 29.3 110 29 104 C28.4 92 28.8 78 29.6 70"
                          fill="none" stroke="#BE7091" stroke-width=".9" stroke-linecap="round"/>
                    <g stroke="#AF6284" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".75">
                      <path d="M37.8 72 Q34 76.6 30.2 72.6"/><path d="M37.4 80.4 Q33.8 85 30 81"/>
                      <path d="M37.1 88.8 Q33.6 93.4 29.9 89.4"/><path d="M36.8 97.2 Q33.5 101.6 30 97.8"/>
                      <path d="M36.6 105.4 Q33.4 109.4 30.1 106"/>
                    </g>
                    <path d="M31.2 113 Q27.8 119 30.4 125.4 Q34.4 119.8 36.6 113.8 Z" fill="#E79DBC" stroke="#BE7091" stroke-width=".8" stroke-linejoin="round"/>
                    <path d="M33.2 112.6 Q37 112.4 36.6 108.8" fill="none" stroke="#B2668A" stroke-width="1.3" stroke-linecap="round"/>

                    <path d="M79.8 62 C82.6 78 83.6 92 84 104 C84.2 110 85 113 87.4 113
                             C89.8 113 90.7 110 91 104 C91.6 92 91.2 78 90.4 70 Q86.5 64 79.8 62 Z" fill="url(#ftFullBraid)"/>
                    <path d="M79.8 62 C82.6 78 83.6 92 84 104 C84.2 110 85 113 87.4 113
                             C89.8 113 90.7 110 91 104 C91.6 92 91.2 78 90.4 70"
                          fill="none" stroke="#BE7091" stroke-width=".9" stroke-linecap="round"/>
                    <g stroke="#AF6284" stroke-width="1.3" fill="none" stroke-linecap="round" opacity=".75">
                      <path d="M82.2 72 Q86 76.6 89.8 72.6"/><path d="M82.6 80.4 Q86.2 85 90 81"/>
                      <path d="M82.9 88.8 Q86.4 93.4 90.1 89.4"/><path d="M83.2 97.2 Q86.5 101.6 90 97.8"/>
                      <path d="M83.4 105.4 Q86.6 109.4 89.9 106"/>
                    </g>
                    <path d="M88.8 113 Q92.2 119 89.6 125.4 Q85.6 119.8 83.4 113.8 Z" fill="#DD90B1" stroke="#BE7091" stroke-width=".8" stroke-linejoin="round"/>
                    <path d="M86.8 112.6 Q83 112.4 83.4 108.8" fill="none" stroke="#B2668A" stroke-width="1.3" stroke-linecap="round"/>
                  </g>

                  <!-- 白色羽毛蝴蝶结 -->
                  <g>
                    <path d="M92 36 C96.5 25 104 17 111.5 14 C108.5 23 101 32 94 38 Z" fill="#FFFFFF" stroke="#E7DEE9" stroke-width=".7"/>
                    <path d="M93 39 C99 33 106.5 29.5 113 28.5 C108 34.5 100.5 39.5 94.5 42 Z" fill="#FDFBFF" stroke="#E7DEE9" stroke-width=".7"/>
                    <path d="M95 34 C100 27 105.5 21 110 17.6" fill="none" stroke="#E1D7E5" stroke-width=".6"/>
                    <path d="M95.6 38 C100.6 34 106 31 111 29.8" fill="none" stroke="#E1D7E5" stroke-width=".6"/>
                    <path d="M92 38 C86 32.5 80 34.5 80 39.5 C80 44.5 86 45.5 92 41 Z" fill="#FFFFFF" stroke="#E6DCE8" stroke-width=".8"/>
                    <path d="M92 38 C97 33.5 102 35.5 102 40 C102 44.5 97 45 92 41 Z" fill="#FCF8FE" stroke="#E6DCE8" stroke-width=".8"/>
                    <circle cx="92" cy="39.6" r="2.7" fill="#FBEEF6" stroke="#E6DCE8" stroke-width=".8"/>
                  </g>

                  <!-- 左侧小宝石 -->
                  <circle cx="37.4" cy="37.4" r="2.9" fill="#A6BEDC"/>
                  <circle cx="36.6" cy="36.6" r="1" fill="#EDF4FC" opacity=".9"/>

                  <!-- 漂浮光点 -->
                  <circle cx="20" cy="94" r="2.4" fill="#FFE3F0" opacity=".45">
                    <animate attributeName="cy" values="94;78;64" dur="5s" repeatCount="indefinite"/>
                    <animate attributeName="opacity" values=".5;.18;0" dur="5s" repeatCount="indefinite"/>
                  </circle>
                  <circle cx="103" cy="104" r="2" fill="#FFE3F0" opacity=".4">
                    <animate attributeName="cy" values="104;88;74" dur="5.6s" repeatCount="indefinite" begin="1.4s"/>
                    <animate attributeName="opacity" values=".42;.14;0" dur="5.6s" repeatCount="indefinite" begin="1.4s"/>
                  </circle>
                </svg>
              </div>
              <div class="welcome-text">
                <p class="welcome-name">我是 <strong>达尼娅</strong></p>
                <p class="welcome-role">星炬学院 · AI 虚拟教师</p>
                <p class="welcome-desc">我可以帮你讲解<br><strong>{{ contextTitle }}</strong><br>的相关内容~</p>
              </div>
              <div class="quick-actions">
                <el-button size="small" @click="askPreset('这个页面是做什么的？请简要介绍')">📖 页面介绍</el-button>
                <el-button size="small" @click="askPreset('请详细讲解这个功能的使用方法和步骤')">🎓 使用教程</el-button>
                <el-button size="small" @click="askPreset('有哪些实用的操作技巧和注意事项？')">💡 实用技巧</el-button>
              </div>
            </div>

            <!-- ═══ 消息列表 ═══ -->
            <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-msg', msg.role]">
              <div class="msg-avatar">
                <svg v-if="msg.role === 'assistant'" viewBox="0 0 32 32" class="mini-chibi" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="16" cy="16" r="15" fill="#E9A0BE"/>
                  <ellipse cx="16" cy="17.4" rx="11" ry="10.6" fill="#FFF6EF"/>
                  <ellipse cx="10.6" cy="21" rx="2.2" ry="1.3" fill="#F9AFC3" opacity=".6"/>
                  <ellipse cx="21.4" cy="21" rx="2.2" ry="1.3" fill="#F9AFC3" opacity=".6"/>
                  <g fill="none" stroke="#4A3230" stroke-width="1.3" stroke-linecap="round">
                    <path d="M9.4 17 Q11.8 20.2 14.2 17"/>
                    <path d="M17.8 17 Q20.2 20.2 22.6 17"/>
                  </g>
                  <path d="M14.3 22.6 Q16 24.6 17.7 22.6 Q16 23.4 14.3 22.6 Z" fill="#C9606F"/>
                  <path d="M16 5.4 C8.8 5.4 5.4 10 5.3 15.8 C5.3 17.9 5.6 19.3 6 20.5
                           C6.7 14.8 9.3 11.3 13 10.6 C13.9 8.3 15 6.9 16 6.2
                           C17 6.9 18.1 8.3 19 10.6 C22.7 11.3 25.3 14.8 26 20.5
                           C26.4 19.3 26.7 17.9 26.7 15.8 C26.6 10 23.2 5.4 16 5.4 Z" fill="#F7B6D0"/>
                  <path d="M6 13.9 C8 9.2 11.5 6.9 16 6.9 C20.5 6.9 24 9.2 26 13.9
                           L24.7 15 C22.8 10.9 19.9 9.1 16 9.1 C12.1 9.1 9.2 10.9 7.3 15 Z" fill="#6B4A3A"/>
                  <path d="M26.2 12.4 C27.5 9.7 29.8 8 31.5 7.6 C30.4 9.9 28.4 11.9 26.8 13 Z" fill="#FFFFFF"/>
                  <circle cx="25.8" cy="14" r="1.3" fill="#FFFFFF" stroke="#E6DCE8" stroke-width=".45"/>
                </svg>
                <span v-else>🙋</span>
              </div>
              <div class="msg-bubble" v-html="renderMsg(msg.content)" />
            </div>
            <!-- 加载中 -->
            <div v-if="loading" class="chat-msg assistant">
              <div class="msg-avatar">
                <svg viewBox="0 0 32 32" class="mini-chibi" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="16" cy="16" r="15" fill="#E9A0BE"/>
                  <ellipse cx="16" cy="17.4" rx="11" ry="10.6" fill="#FFF6EF"/>
                  <ellipse cx="10.6" cy="21" rx="2.2" ry="1.3" fill="#F9AFC3" opacity=".6"/>
                  <ellipse cx="21.4" cy="21" rx="2.2" ry="1.3" fill="#F9AFC3" opacity=".6"/>
                  <g fill="none" stroke="#4A3230" stroke-width="1.3" stroke-linecap="round">
                    <path d="M9.4 17 Q11.8 20.2 14.2 17"/>
                    <path d="M17.8 17 Q20.2 20.2 22.6 17"/>
                  </g>
                  <path d="M14.3 22.6 Q16 24.6 17.7 22.6 Q16 23.4 14.3 22.6 Z" fill="#C9606F"/>
                  <path d="M16 5.4 C8.8 5.4 5.4 10 5.3 15.8 C5.3 17.9 5.6 19.3 6 20.5
                           C6.7 14.8 9.3 11.3 13 10.6 C13.9 8.3 15 6.9 16 6.2
                           C17 6.9 18.1 8.3 19 10.6 C22.7 11.3 25.3 14.8 26 20.5
                           C26.4 19.3 26.7 17.9 26.7 15.8 C26.6 10 23.2 5.4 16 5.4 Z" fill="#F7B6D0"/>
                  <path d="M6 13.9 C8 9.2 11.5 6.9 16 6.9 C20.5 6.9 24 9.2 26 13.9
                           L24.7 15 C22.8 10.9 19.9 9.1 16 9.1 C12.1 9.1 9.2 10.9 7.3 15 Z" fill="#6B4A3A"/>
                  <path d="M26.2 12.4 C27.5 9.7 29.8 8 31.5 7.6 C30.4 9.9 28.4 11.9 26.8 13 Z" fill="#FFFFFF"/>
                  <circle cx="25.8" cy="14" r="1.3" fill="#FFFFFF" stroke="#E6DCE8" stroke-width=".45"/>
                </svg>
              </div>
              <div class="msg-bubble typing">
                <span class="typing-dot">●</span><span class="typing-dot">●</span><span class="typing-dot">●</span>
                <span class="typing-text">{{ voiceOn ? '正在讲解中...' : '正在思考...' }}</span>
              </div>
            </div>
          </div>

          <!-- 流式状态条 -->
          <div v-if="streaming" class="streaming-bar">
            <div class="streaming-wave">
              <span class="wave-bar" v-for="i in 5" :key="i" :style="{ animationDelay: (i * 0.12) + 's' }"/>
            </div>
            <span class="streaming-label">{{ voiceOn ? '🔊 AI 语音讲解中...' : '📝 AI 正在生成...' }}</span>
          </div>

          <!-- 输入区 -->
          <div class="chat-input">
            <el-input
              v-model="userInput"
              placeholder="输入你的问题..."
              size="small"
              @keyup.enter.native="sendMessage"
              :disabled="loading"
            >
              <el-button slot="append" icon="el-icon-s-promotion" @click="sendMessage" :loading="loading" />
            </el-input>
          </div>
        </div>

        <!-- 拖拽手柄 -->
        <div class="panel-drag-handle" @mousedown="startDrag">
          <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
            <circle cx="6" cy="6" r="1.5"/><circle cx="12" cy="6" r="1.5"/><circle cx="18" cy="6" r="1.5"/>
            <circle cx="6" cy="12" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="18" cy="12" r="1.5"/>
          </svg>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'FloatingTeacher',
  data () {
    return {
      showPanel: false,
      messages: [],
      userInput: '',
      loading: false,
      streaming: false,
      voiceOn: true,
      dragging: false,
      dragStartX: 0,
      dragStartY: 0,
      speechSynth: null,
      speechQueue: [],
      speaking: false,
      // 达尼娅Q版动态形象
      avatarUrl: 'https://upload-bbs.miyoushe.com/upload/2024/05/23/341395232/4a0f1a6e3a4f4e97b60c1c8f8b1c6e2c_5124139876802479232.png',
      avatarLoaded: false,
      avatarLoadFailed: false,
      welcomeImgLoaded: false,
      isTalking: false,
      expression: 'normal',
      blinkTimer: null,
      sparkleTimer: null,
      sparkles: [],
      idlePhase: 0,
      svgVisible: true, // 默认显示SVG
      imgFailed: false,
      // ═══ 互动系统 ═══
      eyeFollowX: 0, // 眼球追踪X偏移 (-1 ~ 1)
      eyeFollowY: 0, // 眼球追踪Y偏移 (-1 ~ 1)
      mouseNear: false, // 鼠标是否靠近
      idleAction: '', // 当前空闲动作
      idleActionTimer: null,
      eyeTrackTimer: null,
      idlePhaseTimer: null,
      idleActionNames: ['stretch', 'lookLeft', 'lookRight', 'nod', 'wave', 'think', 'happyJump', 'blushSmile'],
      idleActionIdx: 0,
      idleTalking: false, // 空闲自言自语
      // ═══ 浮窗拖拽 ═══
      isDragging: false,
      floatX: null,
      floatY: null,
      dragMoved: false,
      floatDragStartX: 0,
      floatDragStartY: 0,
      floatStartX: 0,
      floatStartY: 0,
      // ═══ 互动弹幕（跟随浮窗）═══
      idleBubbleText: '',
      idleBubbleVisible: false,
      idleBubbleTimer: null
    }
  },
  computed: {
    contextTitle () {
      const route = this.$route
      if (route && route.meta && route.meta.title) {
        return route.meta.title
      }
      return '当前页面'
    },
    floatStyle () {
      if (this.floatX !== null && this.floatY !== null) {
        return { right: 'auto', bottom: 'auto', left: this.floatX + 'px', top: this.floatY + 'px' }
      }
      return {}
    }
  },
  watch: {},
  mounted () {
    this.startBlinkTimer()
    this.startSparkleTimer()
    this.startIdleActions()
    var self = this
    setTimeout(function () { self.showIdleBubble('你好呀！我是达尼娅～👋 拖我可以移动哦') }, 4000)
  },

  beforeDestroy () {
    this.stopAllSpeech()
    if (this.blinkTimer) clearInterval(this.blinkTimer)
    if (this.sparkleTimer) clearInterval(this.sparkleTimer)
    if (this.idleActionTimer) clearInterval(this.idleActionTimer)
    if (this.eyeTrackTimer) clearInterval(this.eyeTrackTimer)
    if (this.idlePhaseTimer) clearInterval(this.idlePhaseTimer)
    if (this.idleBubbleTimer) clearTimeout(this.idleBubbleTimer)
    document.removeEventListener('mousemove', this._onFloatDrag)
    document.removeEventListener('mouseup', this._onFloatDragUp)
  },

  methods: {
    // ═══ 浮窗拖拽 + 点击 ═══
    onFloatMouseDown (e) {
      if (e.button !== 0) return
      this.dragMoved = false
      this.isDragging = false
      var rect = this.$el.getBoundingClientRect()
      this.floatDragStartX = e.clientX
      this.floatDragStartY = e.clientY
      this.floatStartX = this.floatX !== null ? this.floatX : rect.left
      this.floatStartY = this.floatY !== null ? this.floatY : rect.top
      var self = this
      this._onFloatDrag = function (ev) {
        var dx = ev.clientX - self.floatDragStartX
        var dy = ev.clientY - self.floatDragStartY
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
          self.dragMoved = true
          self.isDragging = true
        }
        if (self.dragMoved) {
          self.floatX = self.floatStartX + dx
          self.floatY = self.floatStartY + dy
        }
      }
      this._onFloatDragUp = function () {
        self.isDragging = false
        document.removeEventListener('mousemove', self._onFloatDrag)
        document.removeEventListener('mouseup', self._onFloatDragUp)
        // 没移动 = 点击
        if (!self.dragMoved) {
          self.openPanel()
        }
      }
      document.addEventListener('mousemove', this._onFloatDrag)
      document.addEventListener('mouseup', this._onFloatDragUp)
    },
    openPanel () {
      this.showPanel = true
      this.setExpression('happy')
      setTimeout(() => { if (this.expression === 'happy') this.setExpression('normal') }, 1800)
    },
    minimizePanel () { this.showPanel = false },
    closePanel () {
      this.showPanel = false
      this.stopAllSpeech()
      this.messages = []
      this.streaming = false
    },
    askPreset (question) { this.userInput = question; this.sendMessage() },
    renderMsg (text) {
      if (!text) return ''
      return text.replace(/\n/g, '<br>').replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/### (.+)/g, '<h4>$1</h4>')
    },

    // 头像图片加载处理
    onAvatarLoad () {
      this.avatarLoaded = true
      this.avatarLoadFailed = false
    },

    onAvatarError () {
      this.avatarLoaded = false
      this.avatarLoadFailed = true
      this.imgFailed = true
    },

    onWelcomeImgLoad () {
      this.welcomeImgLoaded = true
    },

    // 设置说话状态（用于动画）
    setTalkingState (isTalking) {
      this.isTalking = isTalking
      if (isTalking) {
        this.setExpression('talking')
      } else if (this.expression === 'talking') {
        this.setExpression('normal')
      }
      if (this.avatarLoadFailed) {
        this.triggerMouthAnimation(isTalking)
      }
    },

    // 设置表情
    setExpression (expr) {
      this.expression = expr
    },

    // 自动眨眼定时器
    startBlinkTimer () {
      this.blinkTimer = setInterval(() => {
        if (this.expression === 'happy') return // 开心时不眨眼
        const lids = document.querySelectorAll('.eyelid-l, .eyelid-r')
        lids.forEach(el => {
          el.style.transition = 'opacity 0.08s, ry 0.08s'
          el.style.opacity = '1'
          el.style.ry = '6'
          setTimeout(() => {
            el.style.opacity = '0'
            el.style.ry = '0.5'
          }, 120)
        })
      }, 3500 + Math.random() * 2000)
    },

    // 闪光粒子定时器
    startSparkleTimer () {
      this.sparkleTimer = setInterval(() => {
        const sparkles = document.querySelectorAll('.sparkle')
        sparkles.forEach((el, i) => {
          const delay = i * 200
          setTimeout(() => {
            el.style.transition = 'opacity 0.3s, transform 0.3s'
            el.style.opacity = '0.9'
            el.style.transform = 'scale(1.5)'
            setTimeout(() => {
              el.style.opacity = String(0.3 + Math.random() * 0.4)
              el.style.transform = 'scale(0.8)'
            }, 300)
          }, delay)
        })
      }, 2500)
    },

    // ═══ 眼球追踪系统 ═══
    startEyeTracking () {
      var self = this
      this._onMouseMove = function (e) {
        // 获取悬浮球位置
        var bubble = document.querySelector('.float-bubble')
        if (!bubble) return
        var rect = bubble.getBoundingClientRect()
        var cx = rect.left + rect.width / 2
        var cy = rect.top + rect.height / 2
        // 计算鼠标相对偏移（归一化到 -1 ~ 1）
        var dx = (e.clientX - cx) / (window.innerWidth / 2)
        var dy = (e.clientY - cy) / (window.innerHeight / 2)
        self.eyeFollowX = Math.max(-1, Math.min(1, dx))
        self.eyeFollowY = Math.max(-1, Math.min(1, dy))
        // 检测鼠标是否靠近悬浮球
        var dist = Math.sqrt(Math.pow(e.clientX - cx, 2) + Math.pow(e.clientY - cy, 2))
        self.mouseNear = dist < 150
      }
      document.addEventListener('mousemove', this._onMouseMove)
    },

    // ═══ 自主动作系统 ═══
    startIdleActions () {
      var self = this
      // 每8～15秒执行一个随机动作
      this.idlePhaseTimer = setInterval(function () {
        if (self.showPanel) return // 面板打开时不执行
        if (self.dragging) return
        self.triggerRandomIdleAction()
      }, 8000 + Math.random() * 10000)
      // 初始触发一次
      setTimeout(function () { self.triggerRandomIdleAction() }, 5000)
    },

    triggerRandomIdleAction () {
      var actions = this.idleActionNames
      var action = actions[Math.floor(Math.random() * actions.length)]
      this.idleAction = action
      // 显示对应的气泡文字
      var bubbles = {
        stretch: '嗯～伸个懒腰～✨',
        lookLeft: '那边有什么好玩的？👀',
        lookRight: '咦？有同学在认真学习呢！📖',
        nod: '嗯嗯，这个知识点很重要～👍',
        wave: '嗨！有问题随时问我哦～👋',
        think: '让我想想这个怎么讲…🤔',
        happyJump: '学习真是件快乐的事！🎉',
        blushSmile: '被夸奖了好开心呢～😊'
      }
      if (bubbles[action]) {
        this.showIdleBubble(bubbles[action])
      }
      // 动作持续2～3秒后恢复
      var self = this
      setTimeout(function () {
        self.idleAction = ''
      }, 2500 + Math.random() * 1500)
    },

    showIdleBubble (text) {
      // 弹幕挂载在浮窗组件内部，跟随浮窗位置移动，与虚拟教师连一体
      this.idleBubbleText = text
      this.idleBubbleVisible = true
      if (this.idleBubbleTimer) clearTimeout(this.idleBubbleTimer)
      this.idleBubbleTimer = setTimeout(() => {
        this.idleBubbleVisible = false
        this.idleBubbleTimer = null
      }, 3000)
    },

    // 触发SVG嘴巴动画
    triggerMouthAnimation (start) {
      try {
        const mouthEl = document.getElementById('mouthAnim')
        if (mouthEl) {
          if (start) {
            mouthEl.beginElement()
          } else {
            mouthEl.endElement()
          }
        }
      } catch (e) {
        // 忽略错误
      }
    },

    // ═══ TTS 语音 ═══
    getChineseVoice () {
      if (!window.speechSynthesis) return null
      const voices = window.speechSynthesis.getVoices()
      return voices.find(v => v.lang.startsWith('zh') && v.name.includes('Tingting')) ||
        voices.find(v => v.lang.startsWith('zh') && v.name.includes('Meijia')) ||
        voices.find(v => v.lang.startsWith('zh') && v.name.includes('Xia')) ||
        voices.find(v => v.lang.startsWith('zh-CN')) ||
        voices.find(v => v.lang.startsWith('zh')) ||
        null
    },

    speakText (text) {
      if (!this.voiceOn || !window.speechSynthesis) return
      const clean = text.replace(/<[^>]+>/g, '').replace(/[#*`\-|>]/g, '').replace(/\s+/g, ' ').trim()
      if (clean.length < 2) return
      this.speechQueue.push(clean)
      if (!this.speaking) this.processSpeechQueue()
    },

    processSpeechQueue () {
      if (!this.voiceOn || this.speechQueue.length === 0) { this.speaking = false; return }
      this.speaking = true
      const text = this.speechQueue.shift()
      if (this.speechQueue.length > 10) this.speechQueue = this.speechQueue.slice(-5)
      try {
        const u = new SpeechSynthesisUtterance(text)
        u.lang = 'zh-CN'; u.rate = 1.1; u.pitch = 1.05; u.volume = 0.9
        const voice = this.getChineseVoice()
        if (voice) u.voice = voice
        u.onend = () => this.processSpeechQueue()
        u.onerror = () => this.processSpeechQueue()
        window.speechSynthesis.speak(u)
      } catch (e) { this.processSpeechQueue() }
    },

    stopAllSpeech () {
      if (window.speechSynthesis) window.speechSynthesis.cancel()
      this.speechQueue = []; this.speaking = false
    },

    // ═══ AI 对话+流式 ═══
    async sendMessage () {
      const q = this.userInput.trim()
      if (!q || this.loading) return
      this.userInput = ''
      this.messages.push({ role: 'user', content: q })
      this.loading = true; this.streaming = true
      this.setExpression('thinking') // 用户提问时显示思考表情
      this.$nextTick(() => this.scrollToBottom())
      this.messages.push({ role: 'assistant', content: '' })
      const aiMsgIdx = this.messages.length - 1

      try {
        const token = localStorage.getItem('token') || ''
        const ctxPrompt = `[当前页面: ${this.contextTitle}] 用户提问: ${q}`
        const res = await fetch('/api/admin/deepSeek/api/general-chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', token },
          body: JSON.stringify({ messages: [{ role: 'user', content: ctxPrompt }] })
        })
        const data = await res.json()
        const fullText = (data.code === 1) ? (data.response.reply || data.response || '') : ('抱歉: ' + (data.message || '错误'))
        await this.streamText(aiMsgIdx, typeof fullText === 'string' ? fullText : JSON.stringify(fullText))
      } catch (e) {
        this.messages[aiMsgIdx].content = '网络错误，请稍后重试。'
        this.setExpression('surprised')
      } finally {
        this.loading = false; this.streaming = false
        this.setExpression('happy') // 回复完成时显示开心
        setTimeout(() => { if (this.expression === 'happy') this.setExpression('normal') }, 2000)
        this.$nextTick(() => this.scrollToBottom())
      }
    },

    async streamText (msgIdx, fullText) {
      const chars = [...fullText]
      // AI开始回复时，设置说话状态
      this.setTalkingState(true)
      for (let i = 0; i < chars.length; i += 3) {
        await this.$nextTick()
        await this.sleep(30)
        this.messages[msgIdx].content += chars.slice(i, Math.min(i + 3, chars.length)).join('')
        this.$nextTick(() => this.scrollToBottom())
      }
      // 内容完整后朗读
      if (this.voiceOn && fullText.length > 10) {
        const sentences = fullText.match(/[^。！？\n]+[。！？\n]?/g) || [fullText]
        for (const s of sentences) { if (s.trim().length > 2) this.speakText(s.trim()) }
      }
      // 朗读结束后，关闭说话状态
      setTimeout(() => {
        this.setTalkingState(false)
      }, 2000)
    },

    sleep (ms) { return new Promise(resolve => setTimeout(resolve, ms)) },
    scrollToBottom () {
      const el = this.$refs.chatMsgs
      if (el) { el.scrollTop = el.scrollHeight }
    },

    // ═══ 拖拽 ═══
    startDrag (e) {
      const panel = e.currentTarget.parentElement
      const rect = panel.getBoundingClientRect()
      this.dragStartX = e.clientX - rect.left
      this.dragStartY = e.clientY - rect.top
      this.dragging = true
      panel.style.transition = 'none'; panel.style.right = 'auto'; panel.style.bottom = 'auto'
      panel.style.left = rect.left + 'px'; panel.style.top = rect.top + 'px'
      const onMove = (ev) => {
        if (!this.dragging) return
        panel.style.left = (ev.clientX - this.dragStartX) + 'px'
        panel.style.top = (ev.clientY - this.dragStartY) + 'px'
      }
      const onUp = () => {
        this.dragging = false; panel.style.transition = ''
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
      }
      document.addEventListener('mousemove', onMove)
      document.addEventListener('mouseup', onUp)
    }
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════
   Floating Teacher — 达尼娅
   数字人全身像 · 可拖拽 · 表情动效
   ═══════════════════════════════════════ */
.floating-teacher {
  position: fixed; bottom: 24px; right: 28px; z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;
}
.floating-teacher.dragging { z-index: 10001; }

/* ── 悬浮容器 — 圆形头像框，透明无背景 ── */
.float-bubble {
  width: 120px; height: 170px;
  display: flex; align-items: center; justify-content: center;
  cursor: grab; position: relative;
  background: none;
  animation: cardFloat 4.5s ease-in-out infinite;
  user-select: none;
}
.floating-teacher.dragging .float-bubble {
  cursor: grabbing; animation: none; transform: scale(1.06);
}
.float-bubble:hover { transform: scale(1.04); }
@keyframes cardFloat {
  0%, 100% { transform: translateY(0); }
  35% { transform: translateY(-4px); }
  65% { transform: translateY(-6px); }
  85% { transform: translateY(-2px); }
}

/* ── 互动弹窗（跟随浮窗，与虚拟教师连一体）── */
.idle-bubble {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(10, 13, 10, 0.9);
  color: #fff;
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  pointer-events: none;
  z-index: 1;
  max-width: 220px;
  box-shadow: 0 4px 14px rgba(0,0,0,0.25);
  animation: idleBob 2.5s ease-in-out infinite;
}
.idle-bubble-text { display: inline-block; }
.idle-bubble-tail {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0; height: 0;
  border: 6px solid transparent;
  border-top-color: rgba(10, 13, 10, 0.9);
  border-bottom: 0;
}
@keyframes idleBob {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-3px); }
}
.idle-bubble-enter-active { transition: opacity 0.3s ease; }
.idle-bubble-leave-active { transition: opacity 0.3s ease; }
.idle-bubble-enter, .idle-bubble-leave-to { opacity: 0; }

/* ── 纯 SVG 人物立绘 ── */
.avatar-wrap {
  width: 120px; height: 170px;
  position: relative; z-index: 1;
  pointer-events: none;
}
.avatar-svg {
  width: 100%; height: 100%;
  filter: drop-shadow(0 8px 24px rgba(0,0,0,0.35));
}
.avatar-img {
  width: 100%; height: 100%;
  object-fit: contain;
  object-position: center bottom;
  filter: drop-shadow(0 8px 24px rgba(0,0,0,0.35));
}
.avatar-fallback {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  font-size: 36px; color: #c4b5fd;
  text-align: center; line-height: 1.4;
  background: rgba(10, 13, 10, 0.6);
  border-radius: 16px;
}
.avatar-fallback small {
  font-size: 11px; color: #64748b;
}
.avatar-fallback {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  font-size: 36px; color: #c4b5fd;
  text-align: center; line-height: 1.4;
  background: rgba(10, 13, 10, 0.6);
  border-radius: 50%;
}
.avatar-fallback small {
  font-size: 11px; color: #64748b;
}
/* 呼吸动画 — 面板内的立绘 */
.panel-body .welcome-chibi {
  animation: teacherBreath 4.5s ease-in-out infinite;
}
@keyframes teacherBreath {
  0%, 100% { transform: scale(1) translateY(0); filter: drop-shadow(0 4px 12px rgba(200,100,140,0.08)); }
  50% { transform: scale(1.02) translateY(-3px); filter: drop-shadow(0 6px 18px rgba(200,100,140,0.14)); }
}
/* 眨眼动画 — mini chibi */
.mini-chibi circle:nth-child(1) { animation: none; }
@keyframes blinkEyes {
  0%, 90%, 100% { transform: scaleY(1); }
  95% { transform: scaleY(0.1); }
}
/* 气泡球脉动增强 */
.float-bubble:hover .bubble-inner {
  animation: bubbleWiggle 0.6s ease-in-out;
}
@keyframes bubbleWiggle {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
}
/* ── 过渡 ── */
.float-bounce-enter-active { animation: bounceIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1); }
.float-bounce-leave-active { animation: bounceIn 0.3s ease reverse; }
@keyframes bounceIn { from { transform: scale(0); opacity: 0; } to { transform: scale(1); opacity: 1; } }

/* ── 面板 ── */
.float-panel {
  width: 380px; height: 520px; background: #fff; border-radius: 18px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.18), 0 0 0 1px rgba(180,140,200,0.08);
  display: flex; flex-direction: column; overflow: hidden;
  position: absolute; bottom: 0; right: 0;
  border: 1.5px solid #e8d8e8;
}
.panel-slide-enter-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.panel-slide-leave-active { transition: all 0.25s ease; }
.panel-slide-enter, .panel-slide-leave-to { opacity: 0; transform: translateY(24px) scale(0.9); }
/* 当面板展开时，浮层隐藏，面板替代其位置 */
.floating-teacher.expanded {
  position: fixed; bottom: 20px; right: 28px;
}

/* ── 头部 ── */
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px;
  background: linear-gradient(135deg, #fdf5fa 0%, #f8e8f4 40%, #f5e0f2 100%);
  border-bottom: 1px solid #f0d8e8;
}
.panel-header-left { display: flex; align-items: center; gap: 8px; }
.panel-chibi { width: 36px; height: 36px; flex-shrink: 0; }
.chibi-small { width: 100%; height: 100%; }
.panel-title-group { display: flex; flex-direction: column; min-width: 0; }
.panel-title { font-size: 15px; font-weight: 650; color: #8860a0; letter-spacing: 0.3px; }
.panel-context { font-size: 10.5px; color: #9880a8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.panel-header-right { display: flex; gap: 4px; align-items: center; }

/* 语音开关 */
.voice-toggle-btn {
  width: 30px; height: 30px; border-radius: 50%; border: 1.5px solid #d8c8e0;
  background: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.25s; color: #a898b8; padding: 0; margin-right: 2px;
}
.voice-toggle-btn:hover { border-color: #a880c8; color: #7058a0; }
.voice-toggle-btn.on {
  background: var(--ink-3); border-color: #8858a8; color: #6040a0;
  box-shadow: 0 0 8px rgba(140,100,200,0.2);
}
.voice-icon { width: 16px; height: 16px; }

.panel-header-right >>> .el-button--mini {
  width: 28px; height: 28px; padding: 0;
  background: rgba(140,80,160,0.04); border: 1px solid rgba(140,80,160,0.1); color: #9880a8;
}
.panel-header-right >>> .el-button--mini:hover { background: rgba(140,80,160,0.1); color: #7058a0; }

/* ── 内容 ── */
.panel-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-messages { flex: 1; overflow-y: auto; padding: 14px; background: linear-gradient(180deg, #fffaf9 0%, #fdf6f4 100%); }
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: var(--ink-3); border-radius: 10px; }

/* ── 欢迎 ── */
.welcome-area { text-align: center; padding: 4px 0 8px; }
.welcome-chibi { width: 120px; margin: 0 auto 6px; }
.daninya-full { width: 100%; filter: drop-shadow(0 4px 12px rgba(200,100,140,0.08)); }
.welcome-text { margin-bottom: 12px; }
.welcome-name { font-size: 16px; color: #8870a0; margin: 0 0 2px; }
.welcome-name strong { color: #8858a8; font-size: 18px; }
.welcome-role { font-size: 12px; color: #9880a8; margin: 0 0 6px; }
.welcome-desc { font-size: 12.5px; color: #685868; line-height: 1.7; margin: 0; }
.quick-actions { display: flex; flex-direction: column; gap: 5px; margin-top: 8px; }
.quick-actions .el-button {
  width: 100%; text-align: left; border-radius: 10px;
  border-color: var(--ink-5); color: #7060a0; font-size: 12px;
  background: var(--ink-3); transition: all 0.2s;
}
.quick-actions .el-button:hover {
  border-color: #b898d0; color: #6040a0; background: var(--ink-3);
  box-shadow: 0 2px 8px rgba(160,120,200,0.08);
}

/* ── 消息 ── */
.chat-msg { display: flex; gap: 8px; margin-bottom: 14px; align-items: flex-start; }
.chat-msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 15px; flex-shrink: 0; background: var(--ink-3);
}
.mini-chibi { width: 24px; height: 24px; }
.msg-bubble {
  max-width: 78%; padding: 10px 14px; border-radius: 14px;
  font-size: 13px; line-height: 1.65; color: #5c4038;
  background: #fff; border: 1px solid #f2e4e0; word-break: break-word;
}
.chat-msg.user .msg-bubble {
  background: var(--ink-3); border-color: var(--ink-5); color: #604080;
}
.chat-msg.assistant .msg-avatar { background: linear-gradient(135deg, #f8f0fa, #f0e4f6); }
.msg-bubble >>> h4 { font-size: 13px; color: #8870a0; margin: 6px 0 4px; }
.msg-bubble.typing { color: #a898b8; display: flex; align-items: center; gap: 2px; }
.typing-dot { animation: dotBlink 1.4s infinite; font-size: 10px; }
.typing-dot:nth-child(2) { animation-delay: .2s; }
.typing-dot:nth-child(3) { animation-delay: .4s; }
.typing-text { margin-left: 4px; font-size: 12px; font-style: normal; }
@keyframes dotBlink { 0%,80%,100% { opacity: 0.2; } 40% { opacity: 1; } }

/* ── 流式条 ── */
.streaming-bar { display: flex; align-items: center; gap: 8px; padding: 6px 14px; background: var(--ink-3); border-top: 1px solid #f0d8e8; }
.streaming-wave { display: flex; align-items: flex-end; gap: 2px; height: 16px; }
.wave-bar { width: 3px; background: linear-gradient(180deg, #8858a8, #a880c8); border-radius: 2px; animation: waveAnim 0.8s ease-in-out infinite; height: 6px; }
.wave-bar:nth-child(odd) { height: 12px; }
@keyframes waveAnim { 0%,100% { transform: scaleY(1); } 50% { transform: scaleY(2.2); } }
.streaming-label { font-size: 11px; color: #7058a0; font-weight: 500; }

/* ── 输入 ── */
.chat-input { padding: 10px 14px; border-top: 1px solid #e8d8e8; background: #fff; }
.chat-input >>> .el-input-group__append {
  background: #8860a8; border-color: #8860a8; color: #fff;
  border-radius: 0 10px 10px 0; padding: 0 14px;
}
.chat-input >>> .el-input__inner { border-radius: 10px 0 0 10px; border-color: var(--ink-5); height: 36px; }

/* ── 拖拽 ── */
.panel-drag-handle {
  position: absolute; top: 14px; right: 52px;
  cursor: grab; color: rgba(140,80,160,0.35); padding: 3px 5px; border-radius: 4px;
  user-select: none; transition: all 0.2s;
}
.panel-drag-handle:hover { background: rgba(140,80,160,0.06); color: #7058a0; }
.panel-drag-handle:active { cursor: grabbing; }

/* ═══════════════════════════════════════
   达尼娅Q版动态形象 — 外部图片 & 动画
   ═══════════════════════════════════════ */

/* ── 头像图片 ── */
.bubble-avatar-img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%; object-fit: cover; border-radius: 50%;
  transition: opacity 0.5s ease, transform 0.3s ease;
  filter: drop-shadow(0 2px 6px rgba(200,100,140,0.25));
}
.bubble-avatar-img.avatar-talking {
  animation: talkPulse 0.6s ease-in-out infinite;
}
@keyframes talkPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.06); }
}

/* ── 欢迎界面图片 ── */
.welcome-avatar-img {
  width: 100%; max-width: 160px; height: auto; border-radius: 12px;
  filter: drop-shadow(0 8px 20px rgba(200,100,140,0.2));
  animation: welcomeFloat 5s ease-in-out infinite;
}
@keyframes welcomeFloat {
  0%, 100% { transform: translateY(0) scale(1); filter: drop-shadow(0 8px 20px rgba(200,100,140,0.2)); }
  50% { transform: translateY(-6px) scale(1.02); filter: drop-shadow(0 14px 28px rgba(200,100,140,0.3)); }
}
.welcome-breathing { animation: none; }

/* ═══════════════════════════════════════
   Q版SVG动画系统
   ═══════════════════════════════════════ */

/* ── 眨眼动画 ── */
.eyelid-l, .eyelid-r {
  opacity: 0;
  ry: 0.5;
}

/* ── 说话嘴巴动画 ── */
.chibi-talking .mouth-open {
  animation: mouthTalk 0.45s ease-in-out infinite;
}
.chibi-talking .mouth-shape {
  opacity: 0;
}
@keyframes mouthTalk {
  0%, 100% { ry: 0; opacity: 0; }
  25% { ry: 2.5; opacity: 1; }
  50% { ry: 1; opacity: 0.7; }
  75% { ry: 2; opacity: 0.9; }
}

/* ── 呆毛弹跳 ── */
.ahoge {
  animation: ahogeBounce 2s ease-in-out infinite;
  transform-origin: 40px 6px;
}
.ahoge2 {
  animation: ahogeBounce 2.3s ease-in-out infinite 0.3s;
  transform-origin: 42px 4px;
}
@keyframes ahogeBounce {
  0%, 100% { transform: rotate(0deg) translateY(0); }
  25% { transform: rotate(-4deg) translateY(-1.5px); }
  75% { transform: rotate(3deg) translateY(0.5px); }
}

/* ── 头发飘动 ── */
.hair-strand-l {
  animation: hairSwayL 4s ease-in-out infinite;
  transform-origin: 18px 30px;
}
.hair-strand-r {
  animation: hairSwayR 4s ease-in-out infinite;
  transform-origin: 62px 30px;
}
@keyframes hairSwayL {
  0%, 100% { transform: rotate(-12deg) translateX(0); }
  50% { transform: rotate(-10deg) translateX(1.5px); }
}
@keyframes hairSwayR {
  0%, 100% { transform: rotate(12deg) translateX(0); }
  50% { transform: rotate(10deg) translateX(-1.5px); }
}

/* ── 眼睛微光 ── */
.eyes {
  animation: eyesGlow 3s ease-in-out infinite;
}
@keyframes eyesGlow {
  0%, 100% { filter: brightness(1); }
  50% { filter: brightness(1.08); }
}

/* ── 闪光粒子 ── */
.sparkle {
  animation: sparkleFloat 2.5s ease-in-out infinite;
}
.sparkle.s1 { animation-delay: 0s; }
.sparkle.s2 { animation-delay: 0.4s; }
.sparkle.s3 { animation-delay: 0.8s; }
.sparkle.s4 { animation-delay: 1.2s; }
.sparkle.s5 { animation-delay: 0.3s; }
.sparkle.s6 { animation-delay: 1.0s; }
@keyframes sparkleFloat {
  0%, 100% { opacity: 0.3; transform: scale(0.8) translateY(0); }
  50% { opacity: 0.85; transform: scale(1.3) translateY(-2px); }
}

/* ── 表情状态过渡 ── */
.bubble-chibi {
  transition: filter 0.3s ease;
}
.chibi-happy {
  filter: drop-shadow(0 2px 8px rgba(255,180,200,0.4)) brightness(1.05);
}
.chibi-thinking {
  filter: drop-shadow(0 2px 8px rgba(136,200,240,0.3));
}
.brow-think .brow-l {
  transform: rotate(-5deg);
  transform-origin: 26px 26px;
}
.brow-think .brow-r {
  transform: rotate(5deg);
  transform-origin: 54px 26px;
}

/* ── 腮红呼吸 ── */
.blush-l, .blush-r {
  animation: blushPulse 3s ease-in-out infinite;
}
.blush-r { animation-delay: 1.5s; }
@keyframes blushPulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.55; }
}

/* ═══════════════════════════════════════
   自主动作动画系统
   ═══════════════════════════════════════ */

/* ── stretch 伸懒腰 ── */
.chibi-action-stretch {
  animation: actionStretch 2.5s ease-in-out !important;
}
@keyframes actionStretch {
  0%, 100% { transform: scale(1); }
  25% { transform: scale(1.08, 0.95); }
  50% { transform: scale(0.95, 1.08); }
  75% { transform: scale(1.05, 1.02); }
}

/* ── lookLeft / lookRight ── */
.chibi-action-lookLeft {
  animation: actionLookLeft 2s ease-in-out !important;
}
@keyframes actionLookLeft {
  0%, 100% { transform: rotate(0deg); }
  30% { transform: rotate(-8deg); }
  70% { transform: rotate(-6deg); }
}
.chibi-action-lookRight {
  animation: actionLookRight 2s ease-in-out !important;
}
@keyframes actionLookRight {
  0%, 100% { transform: rotate(0deg); }
  30% { transform: rotate(8deg); }
  70% { transform: rotate(6deg); }
}

/* ── nod 点头 ── */
.chibi-action-nod {
  animation: actionNod 2s ease-in-out !important;
}
@keyframes actionNod {
  0%, 100% { transform: rotate(0deg); }
  15% { transform: rotate(6deg); }
  30% { transform: rotate(0deg); }
  45% { transform: rotate(6deg); }
  60% { transform: rotate(0deg); }
}

/* ── wave 挥手 ── */
.chibi-action-wave {
  animation: actionWave 2.5s ease-in-out !important;
}
@keyframes actionWave {
  0%, 100% { transform: rotate(0deg) translateY(0); }
  15% { transform: rotate(-5deg) translateY(-3px); }
  30% { transform: rotate(-8deg) translateY(0); }
  45% { transform: rotate(-5deg) translateY(-3px); }
  60% { transform: rotate(-8deg) translateY(0); }
  80% { transform: rotate(0deg) translateY(0); }
}

/* ── think 思考 ── */
.chibi-action-think {
  animation: actionThink 3s ease-in-out !important;
}
@keyframes actionThink {
  0%, 100% { transform: rotate(0deg) translateY(0); }
  20% { transform: rotate(5deg) translateY(-5px); }
  40% { transform: rotate(3deg) translateY(-6px); }
  60% { transform: rotate(5deg) translateY(-4px); }
  80% { transform: rotate(2deg) translateY(-2px); }
}

/* ── happyJump 高兴跳 ── */
.chibi-action-happyJump {
  animation: actionHappyJump 1.5s ease-in-out !important;
}
@keyframes actionHappyJump {
  0%, 100% { transform: translateY(0) scale(1); }
  20% { transform: translateY(-12px) scale(1.06); }
  40% { transform: translateY(0) scale(0.98); }
  60% { transform: translateY(-6px) scale(1.03); }
  80% { transform: translateY(0) scale(1); }
}

/* ── blushSmile 害羞笑 ── */
.chibi-action-blushSmile {
  animation: actionBlushSmile 2.5s ease-in-out !important;
}
@keyframes actionBlushSmile {
  0%, 100% { transform: rotate(0deg) scale(1); }
  25% { transform: rotate(-4deg) scale(1.04); }
  50% { transform: rotate(4deg) scale(1.06); }
  75% { transform: rotate(-2deg) scale(1.02); }
}

/* ── 浮动文字气泡 ── */
@keyframes floatTextUp {
  0% { opacity: 0; transform: translateY(0) scale(0.8); }
  15% { opacity: 1; transform: translateY(-10px) scale(1); }
  70% { opacity: 1; transform: translateY(-50px) scale(1); }
  100% { opacity: 0; transform: translateY(-80px) scale(0.9); }
}

/* ── 鼠标靠近微动 ── */
.float-bubble:hover {
  animation: gentleFloat 1.5s ease-in-out infinite !important;
}
.float-bubble:hover .bubble-chibi {
  filter: drop-shadow(0 4px 12px rgba(255,180,200,0.4)) brightness(1.06);
  transition: filter 0.3s ease;
}

</style>
