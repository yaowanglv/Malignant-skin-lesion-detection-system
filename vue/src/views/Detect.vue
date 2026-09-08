<template>
  <div class="detect-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>皮肤恶性病灶图像辅助检测与分析</h2>
      <p class="subtitle">使用深度学习模型与AI大模型辅助检测与分析皮肤镜图像</p>
    </div>

    <!-- 操作区域 -->
    <div class="operation-area">
      <!-- 第一行：检测操作 -->
      <div class="operation-row">
        <!-- 图像上传 -->
        <el-upload
        class="upload-component"
        :http-request="customUpload"
        :show-file-list="false"
        :before-upload="beforeUpload"
        accept=".jpg,.jpeg,.png"
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          上传待检测图像
        </el-button>
      </el-upload>

      <!-- 模型选择下拉框 -->
      <el-select
        v-model="modelPath"
        placeholder="选择检测模型"
        class="model-select"
        size="default"
        :loading="modelLoading"
        filterable
      >
        <template #prefix>模型</template>
        <el-option
          v-for="item in modelOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <!-- 置信度设置 -->
      <el-tooltip content="置信度阈值" placement="top">
        <el-input-number
          v-model="confThreshold"
          :min="0.05"
          :max="0.95"
          :step="0.05"
          :precision="2"
          class="threshold-input"
          size="default"
        />
      </el-tooltip>

      <!-- 开始检测按钮 -->
      <el-button
        type="success"
        @click="startDetection"
        :loading="detecting"
        :disabled="!currentRecord || currentRecord.detectStatus === 1 || !modelPath"
        class="control-button"
      >
        <el-icon><VideoPlay /></el-icon>
        {{ detecting ? '检测中...' : '开始检测' }}
      </el-button>

      <!-- AI模型选择 -->
      <el-select
        v-model="selectedModel"
        placeholder="选择AI大模型"
        class="ai-select"
      >
        <template #prefix>
          <span
            v-if="selectedAiModelMeta"
            class="ai-model-icon"
            :class="`ai-model-icon--${selectedAiModelMeta.value}`"
          >
            <img
              v-if="selectedAiModelMeta.iconSrc"
              :src="selectedAiModelMeta.iconSrc"
              :alt="selectedAiModelMeta.label"
            />
            <span v-else>{{ selectedAiModelMeta.icon }}</span>
          </span>
        </template>
        <el-option
          v-for="item in aiModelOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
          :disabled="!item.configured"
        >
          <span class="ai-option-label">
            <span class="ai-model-icon" :class="`ai-model-icon--${item.value}`">
              <img
                v-if="getAiModelIconSrc(item.value)"
                :src="getAiModelIconSrc(item.value)"
                :alt="item.label"
              />
              <span v-else>{{ getAiModelIcon(item.value) }}</span>
            </span>
            <span>{{ item.label }}</span>
          </span>
          <el-tag
            v-if="!item.configured"
            size="small"
            type="info"
            class="ai-option-tag"
          >未配置</el-tag>
        </el-option>
      </el-select>

      <!-- AI分析按钮 -->
      <el-button
        type="warning"
        @click="startAiAnalysis"
        :loading="aiAnalyzing"
        :disabled="!canAiAnalyze"
        class="control-button"
      >
        <el-icon><ChatDotRound /></el-icon>
        {{ aiAnalyzing ? '分析中...' : 'AI辅助分析' }}
      </el-button>

      <!-- 测试连接按钮（开发调试用） -->
      <el-button
        v-if="showTestConnectionButton"
        type="info"
        @click="testConnection"
        class="control-button"
        size="small"
      >
        测试连接
      </el-button>

      <!-- 导出检测报告 -->
      <el-button
        type="primary"
        @click="exportReport"
        :disabled="!detectionData"
        class="control-button"
      >
        <el-icon><Document /></el-icon>
        导出检测报告
      </el-button>
      </div>

      <!-- 第二行：飞书配置与上传 -->
      <div class="operation-row">
        <!-- 飞书 Token 配置区域 -->
        <div class="feishu-config-area">
          <el-collapse v-model="feishuConfigExpanded">
            <el-collapse-item title="飞书 Token 配置" name="feishu">
              <div class="feishu-config-row">
                <el-input
                  v-model="feishuToken"
                  placeholder="请输入飞书 User Access Token"
                  class="feishu-token-input"
                  clearable
                >
                  <template #prepend>User Token</template>
                </el-input>
                <el-button
                  type="primary"
                  class="feishu-save-btn"
                  @click="saveFeishuToken"
                >
                  保存
                </el-button>
              </div>
              <div class="feishu-config-hint">
                提示：配置后，上传到飞书功能将使用此 Token 进行身份验证。Token 仅保存在本地浏览器中。
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 上传到飞书 -->
        <el-button
          type="success"
          @click="uploadToFeishu"
          :loading="uploadingToFeishu"
          :disabled="!detectionData"
          class="control-button"
        >
          <el-icon><Upload /></el-icon>
          上传到飞书
        </el-button>
      </div>
    </div>

    <div v-if="aiProgressVisible" class="ai-progress-panel">
      <div class="ai-progress-header">
        <span>正在生成AI大模型辅助分析报告，请稍后</span>
        <strong>{{ aiAnalysisProgress }}%</strong>
      </div>
      <el-progress
        :percentage="aiAnalysisProgress"
        :stroke-width="12"
        :color="aiProgressColors"
      />
    </div>

    <!-- 图像检测区域（左右分栏：左侧原始图像 + 右侧检测结果） -->
    <div class="image-compare-area" v-if="currentRecord" ref="resultAreaRef">
      <div class="split-layout">
        <!-- 左侧：原始影像 -->
        <div class="split-panel">
          <div class="image-card">
            <div class="image-title">原始影像</div>
            <div class="image-wrapper">
              <img
                v-if="originalImageUrl"
                :src="originalImageUrl"
                alt="原始影像"
                class="medical-image"
              />
              <el-empty v-else description="暂无图像" />
            </div>
            <div class="image-info" v-if="currentRecord.originalImageName">
              <span>文件名: {{ currentRecord.originalImageName }}</span>
              <span>大小: {{ formatFileSize(currentRecord.originalImageSize) }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧：检测结果 -->
        <div class="split-panel" ref="resultRowRef">
          <div class="image-card">
            <div class="image-title">
              检测结果
              <el-tag
                :type="detectStatusType"
                size="small"
                class="title-status"
              >
                {{ detectStatusText }}
              </el-tag>
            </div>
            <div class="image-wrapper">
              <img
                v-if="resultImageUrl"
                :src="resultImageUrl"
                alt="检测结果"
                class="medical-image"
                @error="handleImageError"
                crossorigin="anonymous"
              />
              <el-empty v-else description="等待检测">
                <template #description>
                  <div class="empty-detect-state">
                    <p>等待检测</p>
                    <p v-if="currentRecord.detectStatus === 1" class="running-tip">
                      <el-icon class="is-loading"><Loading /></el-icon>
                      正在分析图像...
                    </p>
                  </div>
                </template>
              </el-empty>
            </div>
            <!-- 检测数据概览 -->
            <div class="detection-summary" v-if="detectionData && (detectionData.defect_detected)">
              <el-descriptions :column="2" size="small" border>
                <el-descriptions-item label="检测类型（仅供参考）" :span="2">
                  <span class="defect-type-text">
                    {{ getDefectTypeName(detectionData) }}
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="置信度" :span="2">
                  <el-progress
                    :percentage="Math.round((detectionData.confidence || 0) * 100)"
                    :color="confidenceColor"
                    :stroke-width="10"
                  />
                </el-descriptions-item>
                <el-descriptions-item label="检测框数量">
                  {{ detectionData.boxes?.length || 0 }} 个
                </el-descriptions-item>
                <el-descriptions-item label="处理时间">
                  {{ detectionData.processing_time_ms || '-' }} ms
                </el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="detection-summary" v-else-if="detectionData && !(detectionData.defect_detected)">
              <el-alert
                title="正常"
                type="success"
                description="图像分析完成，未发现明显病变。"
                :closable="false"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI分析结果 -->
    <div class="ai-analysis-area" v-if="currentRecord?.aiAnalysisResult">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><ChatLineRound /></el-icon>
              皮肤癌AI诊断分析
              <el-tag v-if="formatAiModelName(currentRecord.aiModel)" class="title-status" size="small" type="warning">
                {{ formatAiModelName(currentRecord.aiModel) }}
              </el-tag>
            </span>
            <span class="analysis-time">
              {{ formatDateTime(currentRecord.aiAnalysisTime) }}
            </span>
          </div>
        </template>
        <div class="analysis-content">
          <div class="analysis-text">
            <p
              v-for="(line, index) in aiAnalysisDisplayLines"
              :key="index"
              :class="{
                'analysis-disclaimer': isAnalysisDisclaimerLine(line),
                'analysis-label-line': !isAnalysisDisclaimerLine(line) && line.includes('：')
              }"
            >
              <template v-if="!isAnalysisDisclaimerLine(line) && line.includes('：')">
                <span class="analysis-label">{{ line.substring(0, line.indexOf('：') + 1) }}</span>
                {{ line.substring(line.indexOf('：') + 1) }}
              </template>
              <template v-else>
                {{ line }}
              </template>
            </p>
          </div>
        </div>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onActivated, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload, VideoPlay, ChatDotRound, ChatLineRound, Loading, Document } from '@element-plus/icons-vue'
import request, { getBackendUrl } from '@/utils/request.js'
import { fetchModels, formatModelOptions, getThreshold, onModelConfigChanged, shouldAutoRefreshPage } from '@/utils/config.js'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import deepseekIcon from '@/assets/imgs/deepseek - logo.png'
import glmIcon from '@/assets/imgs/GLM.webp'
import kimiIcon from '@/assets/imgs/kimi.webp'

// ==================== 响应式数据 ====================

const AI_DISCLAIMER_TEXT = '本分析报告仅供参考，仅为辅助检测分析，不替代现场专业检测和维护决策。'
const LEGACY_AI_DISCLAIMER_TEXT = '本分析报告仅供参考，仅为辅助检测分析，不替代现场专业检测和维护决策。'
const AI_MODEL_LABELS = {
  deepseek: 'Deepseek-V4',
  kimi: 'Kimi-k2.6',
  glm: 'GLM-5.1',
  doubao: '豆包'
}
const AI_MODEL_ICONS = {
  deepseek: 'DS',
  kimi: 'K',
  glm: 'GLM',
  doubao: '豆'
}
const AI_MODEL_ICON_IMAGES = {
  deepseek: deepseekIcon,
  glm: glmIcon,
  kimi: kimiIcon
}
const AI_ANALYSIS_SECTION_TITLES = [
  '检测类型分析',
  '位置与范围评估',
  '严重程度判断',
  '严重程度初步判断',
  '运维处置建议',
  '建议的运维处置方案',
  '需要注意的安全事项',
  '巡检建议',
  '维护周期建议',
  '环境因素评估',
  '后续检测计划建议'
]

defineOptions({ name: 'Detect' })

const route = useRoute()
const currentRecord = ref(null)
const detecting = ref(false)
const aiAnalyzing = ref(false)
const exporting = ref(false)
const uploadingToFeishu = ref(false)
const aiProgressVisible = ref(false)
const aiAnalysisProgress = ref(0)
const showTestConnectionButton = false
const selectedModel = ref('deepseek')
const aiModelOptions = ref([
  { label: AI_MODEL_LABELS.deepseek, value: 'deepseek', configured: false },
  { label: AI_MODEL_LABELS.glm, value: 'glm', configured: false },
  { label: AI_MODEL_LABELS.kimi, value: 'kimi', configured: false }
])
const selectedAiModelMeta = computed(() => {
  const model = aiModelOptions.value.find(item => item.value === selectedModel.value)
  if (!model) return null
  return {
    ...model,
    icon: getAiModelIcon(model.value),
    iconSrc: getAiModelIconSrc(model.value)
  }
})

// 飞书 Token 配置
const feishuToken = ref('')
const feishuConfigExpanded = ref([])

// 模型配置
const modelPath = ref('')
const modelOptions = ref([])
const modelLoading = ref(false)
const confThreshold = ref(0.55)
let aiProgressTimer = null
let stopModelConfigListener = null
let hasMounted = false

const resultAreaRef = ref(null)
const resultRowRef = ref(null)

const aiProgressColors = [
  { color: '#2A7D8C', percentage: 55 },
  { color: '#3A9BAE', percentage: 92 },
  { color: '#2F8F5B', percentage: 100 }
]

// 上传数据
const uploadData = computed(() => {
  const userStr = localStorage.getItem('code_user')
  const user = userStr ? JSON.parse(userStr) : {}
  return {
    userId: user.id || 1,
    userName: user.name || '管理员'
  }
})

// 图像URL
const originalImageUrl = computed(() => {
  if (!currentRecord.value?.originalImageUrl) return ''
  // 数据集URL特殊处理
  if (currentRecord.value.originalImageUrl.startsWith('detect/dataset?')) {
    const params = currentRecord.value.originalImageUrl.replace('detect/dataset?', '')
    return `${getBackendUrl()}/detect/datasetImage?${params}`
  }
  if (currentRecord.value.originalImageUrl.startsWith('http')) {
    return currentRecord.value.originalImageUrl
  }
  return `${getBackendUrl()}/files/${currentRecord.value.originalImageUrl}`
})

const resultImageUrl = computed(() => {
  if (!currentRecord.value?.resultImageUrl) return ''
  // 如果是完整URL（从Python服务直接返回的），直接使用
  if (currentRecord.value.resultImageUrl.startsWith('http')) {
    return currentRecord.value.resultImageUrl
  }
  // 否则使用Spring Boot的文件服务
  return `${getBackendUrl()}/files/${currentRecord.value.resultImageUrl}`
})

const aiAnalysisDisplayLines = computed(() => {
  return String(currentRecord.value?.aiAnalysisResult || '').split(/\r?\n/)
})

const isAnalysisDisclaimerLine = (line = '') => {
  const text = String(line).trim()
  return text.includes('本分析报告仅供参考')
}

const isAnalysisSectionTitleLine = (line = '') => {
  const text = String(line)
    .trim()
    .replace(/^(?:[\s\d一二三四五六七八九十]+[.、．)]|[（(][\d一二三四五六七八九十]+[）)])\s*/, '')
    .replace(/[：:].*$/, '')
    .trim()
  return AI_ANALYSIS_SECTION_TITLES.includes(text)
}

const formatAiModelName = (model) => {
  if (!model) return ''
  const key = String(model).toLowerCase()
  return Object.prototype.hasOwnProperty.call(AI_MODEL_LABELS, key) ? AI_MODEL_LABELS[key] : model
}

const getAiModelIcon = (model) => {
  const key = String(model || '').toLowerCase()
  return AI_MODEL_ICONS[key] || 'AI'
}

const getAiModelIconSrc = (model) => {
  const key = String(model || '').toLowerCase()
  return AI_MODEL_ICON_IMAGES[key] || ''
}

// 添加图像加载错误处理
const handleImageError = (e) => {
  console.error('图像加载失败:', e)
  ElMessage.error('结果图像加载失败')
}

// 皮肤癌检测类型映射
// 0: Benign-skin-cancer(良性皮肤肿瘤), 1: Malignant-skin-cancer(恶性皮肤癌)
const defectTypeMapping = {
  'normal': '正常',
  // Benign-skin-cancer 良性皮肤肿瘤 (class 0)
  'Benign-skin-cancer': '良性皮肤肿瘤',
  'benign-skin-cancer': '良性皮肤肿瘤',
  '0': '良性皮肤肿瘤',
  // Malignant-skin-cancer 恶性皮肤癌 (class 1)
  'Malignant-skin-cancer': '恶性皮肤癌',
  'malignant-skin-cancer': '恶性皮肤癌',
  '1': '恶性皮肤癌'
}

const normalizeDefectType = (value) => {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

const mapDefectTypeName = (value) => {
  const rawValue = normalizeDefectType(value)
  if (!rawValue) return ''
  const lowerValue = rawValue.toLowerCase()
  if (['未知', 'unknown', 'none', 'null', 'undefined', '-'].includes(lowerValue)) return ''
  const normalizedKey = lowerValue.replace(/[-_]+/g, ' ')
  const underscoreKey = lowerValue.replace(/[\s-]+/g, '_')
  return defectTypeMapping[lowerValue] ||
    defectTypeMapping[normalizedKey] ||
    defectTypeMapping[underscoreKey] ||
    rawValue
}

const normalizeDetectionBoxes = (boxes) => {
  if (Array.isArray(boxes)) return boxes
  if (typeof boxes === 'string') {
    try {
      const parsedBoxes = JSON.parse(boxes)
      return Array.isArray(parsedBoxes) ? parsedBoxes : []
    } catch {
      return []
    }
  }
  return []
}

const getMaxConfidenceBox = (boxes = []) => {
  const normalizedBoxes = normalizeDetectionBoxes(boxes)
  if (normalizedBoxes.length === 0) return null
  return normalizedBoxes.reduce((max, box) => {
    const currentConfidence = Number(box?.confidence ?? 0)
    const maxConfidence = Number(max?.confidence ?? 0)
    return currentConfidence > maxConfidence ? box : max
  }, normalizedBoxes[0])
}

const getBoxDefectTypeValue = (box) => {
  if (!box) return ''
  return box.defect_type ||
    box.defectType ||
    box.defect_type_en ||
    box.label ||
    box.label_cn ||
    box.class_name ||
    box.className ||
    box.name ||
    box.cls_name ||
    box.class_id ||
    box.classId ||
    box.class ||
    box.cls
}

const getDefectTypeName = (data, record = currentRecord.value) => {
  if (!data) return '未知'
  const maxConfBox = getMaxConfidenceBox(data.boxes)
  const candidates = [
    getBoxDefectTypeValue(maxConfBox),
    data.defect_type_cn,
    data.defect_type,
    data.defect_type_en,
    data.defectType,
    data.label_cn,
    data.label,
    data.class_name,
    data.className,
    data.class_id,
    data.classId,
    data.class,
    data.cls
  ]

  for (const item of candidates) {
    const name = mapDefectTypeName(item)
    if (name) return name
  }
  return '未知'
}

const escapeHtml = (value) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const drawText = (ctx, text, x, y, options = {}) => {
  ctx.fillStyle = options.color || '#1A2D3D'
  ctx.font = options.font || '14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  ctx.textAlign = options.align || 'left'
  ctx.textBaseline = 'top'
  ctx.fillText(String(text ?? '-'), x, y)
}

const createReportHeaderCanvas = (record, data) => {
  const canvas = document.createElement('canvas')
  const scale = 2
  const width = 800
  const height = data ? 260 : 150
  canvas.width = width * scale
  canvas.height = height * scale
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`

  const ctx = canvas.getContext('2d')
  ctx.scale(scale, scale)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, width, height)

  drawText(ctx, '皮肤癌检测分析报告', width / 2, 16, {
    align: 'center',
    color: '#1A2D3D',
    font: '700 24px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  drawText(ctx, '大模型驱动视频流皮肤恶性病灶实时辅助研判系统', width / 2, 52, {
    align: 'center',
    color: '#687782',
    font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  ctx.strokeStyle = '#2A7D8C'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(30, 76)
  ctx.lineTo(width - 30, 76)
  ctx.stroke()

  drawText(ctx, `报告编号: ${record?.id || '-'}`, 35, 94, { color: '#687782', font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif' })
  drawText(ctx, `生成时间: ${new Date().toLocaleString('zh-CN')}`, width / 2, 94, { align: 'center', color: '#687782', font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif' })
  drawText(ctx, `检测模型: ${getModelName(record?.modelName || modelPath.value)}`, width - 35, 94, { align: 'right', color: '#687782', font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif' })

  if (!data) return canvas

  const defectTypeName = getDefectTypeName(data, record)
  const confidenceText = `${Math.round((Number(data.confidence) || 0) * 100)}%`
  const boxCountText = `${normalizeDetectionBoxes(data.boxes).length} 个`
  const processingTimeText = `${data.processing_time_ms || '-'} ms`

  ctx.fillStyle = '#F7FAFC'
  ctx.fillRect(30, 125, width - 60, 105)
  ctx.strokeStyle = '#E2EAF0'
  ctx.lineWidth = 1
  ctx.strokeRect(30, 125, width - 60, 105)
  ctx.fillStyle = '#2A7D8C'
  ctx.fillRect(45, 144, 4, 18)
  drawText(ctx, '检测结果详情', 58, 142, {
    color: '#1A2D3D',
    font: '700 16px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })

  drawText(ctx, '检测类型:', 58, 178, { color: '#687782' })
  drawText(ctx, defectTypeName, 128, 178, {
    color: '#c2413b',
    font: '700 14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  drawText(ctx, '置信度:', 415, 178, { color: '#687782' })
  drawText(ctx, confidenceText, 472, 178, {
    color: '#2A7D8C',
    font: '700 14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  drawText(ctx, '检测框数量:', 58, 206, { color: '#687782' })
  drawText(ctx, boxCountText, 142, 206, { color: '#1A2D3D' })
  drawText(ctx, '处理时间:', 415, 206, { color: '#687782' })
  drawText(ctx, processingTimeText, 486, 206, { color: '#1A2D3D' })

  return canvas
}

const wrapCanvasText = (ctx, text, maxWidth) => {
  const lines = []
  const paragraphs = String(text || '-').split('\n')

  paragraphs.forEach((paragraph) => {
    const content = paragraph.trimEnd()
    if (!content) {
      lines.push('')
      return
    }

    let line = ''
    for (const char of content) {
      const testLine = line + char
      if (ctx.measureText(testLine).width > maxWidth && line) {
        lines.push(line)
        line = char
      } else {
        line = testLine
      }
    }
    if (line) lines.push(line)
  })

  return lines
}

const wrapCanvasAnalysisText = (ctx, text, maxWidth) => {
  const lines = []
  const paragraphs = String(text || '-').split('\n')

  paragraphs.forEach((paragraph) => {
    const content = paragraph.trimEnd()
    const isDisclaimer = isAnalysisDisclaimerLine(content)
    const isSectionTitle = isAnalysisSectionTitleLine(content)
    if (!content) {
      lines.push({ text: '', isDisclaimer: false, isSectionTitle: false })
      return
    }

    ctx.font = `${isDisclaimer || isSectionTitle ? '700 ' : ''}14px "Microsoft YaHei", "SimHei", Arial, sans-serif`
    let line = ''
    for (const char of content) {
      const testLine = line + char
      if (ctx.measureText(testLine).width > maxWidth && line) {
        lines.push({ text: line, isDisclaimer, isSectionTitle })
        line = char
      } else {
        line = testLine
      }
    }
    if (line) lines.push({ text: line, isDisclaimer, isSectionTitle })
  })

  return lines
}

const createAiAnalysisCanvas = (record) => {
  const scale = 2
  const width = 800
  const padding = 30
  const contentWidth = width - padding * 2
  const lineHeight = 24
  const measureCanvas = document.createElement('canvas')
  const measureCtx = measureCanvas.getContext('2d')
  measureCtx.font = '14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  const lines = wrapCanvasAnalysisText(measureCtx, record?.aiAnalysisResult || '-', contentWidth)
  const height = Math.max(210, 140 + lines.length * lineHeight)

  const canvas = document.createElement('canvas')
  canvas.width = width * scale
  canvas.height = height * scale
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`

  const ctx = canvas.getContext('2d')
  ctx.scale(scale, scale)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, width, height)

  drawText(ctx, 'AI 辅助分析报告', width / 2, 20, {
    align: 'center',
    color: '#1A2D3D',
    font: '700 22px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  ctx.strokeStyle = '#D9747E'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(padding, 60)
  ctx.lineTo(width - padding, 60)
  ctx.stroke()

  drawText(ctx, `分析模型: ${formatAiModelName(record?.aiModel) || '-'}`, padding, 82, {
    color: '#687782',
    font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })
  drawText(ctx, `分析时间: ${formatDateTime(record?.aiAnalysisTime)}`, width - padding, 82, {
    align: 'right',
    color: '#687782',
    font: '12px "Microsoft YaHei", "SimHei", Arial, sans-serif'
  })

  let y = 120
  const safeBreaksPx = []
  lines.forEach((line) => {
    const lineY = y
    const text = line.text
    const isDisclaimer = line.isDisclaimer

    if (!isDisclaimer && text.includes('：')) {
      // 冒号前的标签加粗，冒号后正常字体
      const colonIdx = text.indexOf('：')
      const label = text.substring(0, colonIdx + 1)
      const content = text.substring(colonIdx + 1)
      const labelWidth = ctx.measureText(label).width
      drawText(ctx, label, padding, y, {
        color: '#1A2D3D',
        font: '700 14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
      })
      if (content) {
        drawText(ctx, content, padding + labelWidth, y, {
          color: '#1A2D3D',
          font: '14px "Microsoft YaHei", "SimHei", Arial, sans-serif'
        })
      }
    } else {
      drawText(ctx, text, padding, y, {
        color: isDisclaimer ? '#c2413b' : '#1A2D3D',
        font: `${isDisclaimer || line.isSectionTitle ? '700 ' : ''}14px "Microsoft YaHei", "SimHei", Arial, sans-serif`
      })
    }
    const rowHeight = text ? lineHeight : lineHeight * 0.65
    y += rowHeight
    safeBreaksPx.push(Math.round((lineY + rowHeight - 3) * scale))
  })
  canvas.pdfSafeBreaksPx = safeBreaksPx

  return canvas
}

const normalizeSafeBreaksPx = (breaks, canvasHeight) => {
  return [...new Set((breaks || [])
    .map((breakY) => Math.round(Number(breakY)))
    .filter((breakY) => Number.isFinite(breakY) && breakY > 0 && breakY < canvasHeight)
  )].sort((a, b) => a - b)
}

const getSafeSliceHeightPx = (sourceY, maxSliceHeightPx, canvasHeight, safeBreaksPx, minSliceHeightPx, tolerancePx) => {
  const remainingPx = canvasHeight - sourceY
  const hardSliceHeightPx = Math.max(1, Math.min(remainingPx, maxSliceHeightPx))

  if (!safeBreaksPx.length || remainingPx <= hardSliceHeightPx + tolerancePx) {
    return hardSliceHeightPx
  }

  const maxBreakY = sourceY + hardSliceHeightPx
  const minBreakY = sourceY + Math.min(hardSliceHeightPx - 1, minSliceHeightPx)
  let safeBreakY = null

  for (const breakY of safeBreaksPx) {
    if (breakY > minBreakY && breakY <= maxBreakY) {
      safeBreakY = breakY
    }
    if (breakY > maxBreakY) break
  }

  return safeBreakY ? Math.max(1, safeBreakY - sourceY) : hardSliceHeightPx
}

const addCanvasToPdfPaged = (pdf, canvas, options = {}) => {
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const x = options.x ?? 0
  const width = options.width ?? pageWidth
  const marginTop = options.marginTop ?? 10
  const marginBottom = options.marginBottom ?? 10
  let currentY = options.y ?? marginTop
  let sourceY = 0
  const pxPerPdfUnit = canvas.width / width
  const tolerancePx = 2
  const safeBreaksPx = normalizeSafeBreaksPx(options.safeBreaksPx || canvas.pdfSafeBreaksPx, canvas.height)
  const minSliceHeightPx = options.minSliceHeightPx ?? Math.max(24, Math.round(canvas.width * 0.03))
  const imgFormat = String(options.imgFormat || 'PNG').toUpperCase()
  const mimeType = imgFormat === 'JPEG' ? 'image/jpeg' : 'image/png'
  const quality = options.quality ?? 1
  const jsCompression = options.jsCompression

  while (sourceY < canvas.height - tolerancePx) {
    let availableHeight = pageHeight - currentY - marginBottom
    if (availableHeight <= 5) {
      pdf.addPage()
      currentY = marginTop
      availableHeight = pageHeight - currentY - marginBottom
    }

    const maxSliceHeightPx = Math.max(1, Math.floor(availableHeight * pxPerPdfUnit))
    const sliceHeightPx = getSafeSliceHeightPx(
      sourceY,
      maxSliceHeightPx,
      canvas.height,
      safeBreaksPx,
      minSliceHeightPx,
      tolerancePx
    )
    const sliceCanvas = document.createElement('canvas')
    sliceCanvas.width = canvas.width
    sliceCanvas.height = sliceHeightPx
    const sliceCtx = sliceCanvas.getContext('2d')
    sliceCtx.drawImage(
      canvas,
      0,
      sourceY,
      canvas.width,
      sliceHeightPx,
      0,
      0,
      canvas.width,
      sliceHeightPx
    )

    const sliceHeight = sliceHeightPx / pxPerPdfUnit
    const sliceImgData = sliceCanvas.toDataURL(mimeType, quality)
    if (jsCompression) {
      pdf.addImage(sliceImgData, imgFormat, x, currentY, width, sliceHeight, undefined, jsCompression)
    } else {
      pdf.addImage(sliceImgData, imgFormat, x, currentY, width, sliceHeight)
    }
    sourceY += sliceHeightPx
    currentY += sliceHeight

    if (sourceY < canvas.height - tolerancePx) {
      pdf.addPage()
      currentY = marginTop
    }
  }

  return currentY
}

// 解析检测数据
const detectionData = computed(() => {
  if (!currentRecord.value?.detectionData) return null
  try {
    const data = JSON.parse(currentRecord.value.detectionData)
    if (data.detection && typeof data.detection === 'object') {
      Object.assign(data, data.detection)
    }
    data.boxes = normalizeDetectionBoxes(data.boxes)
    // 从 boxes 中提取最高置信度（顶层 confidence 可能不存在）
    if (data.boxes && data.boxes.length > 0) {
      const maxConfBox = getMaxConfidenceBox(data.boxes)
      data.confidence = Number(maxConfBox?.confidence ?? data.confidence ?? 0)
    }
    data.defect_type_cn = getDefectTypeName(data, currentRecord.value)
    return data
  } catch {
    return null
  }
})

// 检测状态
const detectStatusType = computed(() => {
  const status = currentRecord.value?.detectStatus
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
})

const detectStatusText = computed(() => {
  const status = currentRecord.value?.detectStatus
  const texts = { 0: '待检测', 1: '检测中', 2: '检测完成', 3: '检测失败' }
  return texts[status] || '未知'
})

// 是否可进行AI分析
const canAiAnalyze = computed(() => {
  return currentRecord.value?.detectStatus === 2 &&
         currentRecord.value?.aiStatus !== 1
})

// 置信度颜色
const confidenceColor = computed(() => {
  const conf = detectionData.value?.confidence || 0
  if (conf >= 0.9) return '#2f8f5b'
  if (conf >= 0.7) return '#D4944F'
  return '#c2413b'
})

// ==================== 方法 ====================

const clearAiProgressTimer = () => {
  if (aiProgressTimer) {
    clearInterval(aiProgressTimer)
    aiProgressTimer = null
  }
}

const startAiProgress = () => {
  clearAiProgressTimer()
  aiProgressVisible.value = true
  aiAnalysisProgress.value = 3
  aiProgressTimer = setInterval(() => {
    if (aiAnalysisProgress.value >= 92) return
    const step = aiAnalysisProgress.value < 35 ? 7 : aiAnalysisProgress.value < 70 ? 4 : 1
    aiAnalysisProgress.value = Math.min(92, aiAnalysisProgress.value + step)
  }, 500)
}

const completeAiProgress = () => {
  clearAiProgressTimer()
  aiAnalysisProgress.value = 100
  setTimeout(() => {
    aiProgressVisible.value = false
  }, 1200)
}

const stopAiProgress = () => {
  clearAiProgressTimer()
  aiProgressVisible.value = false
  aiAnalysisProgress.value = 0
}

// 上传前校验
const beforeUpload = (file) => {
  const validExts = ['jpg', 'jpeg', 'png']
  const ext = file.name.split('.').pop().toLowerCase()

  if (!validExts.includes(ext)) {
    ElMessage.error('请上传 JPG、PNG 格式的皮肤镜图像')
    return false
  }

  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }

  return true
}

// 根据上传图像路径计算另一模态的对应图像路径
// images <-> images_ir 互换，保持其余路径不变
const computeCounterpartPath = (filePath) => {
  const irMarker = '/images_ir/'
  const rgbMarker = '/images/'
  if (filePath.includes(irMarker)) {
    return filePath.replace(irMarker, rgbMarker)
  } else if (filePath.includes(rgbMarker)) {
    return filePath.replace(rgbMarker, irMarker)
  }
  return null
}

// 自定义上传：同步读取另一模态图像并同时上传双模态图像
const customUpload = async (options) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  const userStr = localStorage.getItem('code_user')
  const user = userStr ? JSON.parse(userStr) : {}
  formData.append('userId', user.id || 1)
  formData.append('userName', user.name || '管理员')
  // Chromium 浏览器会在 File 对象上附加 path 属性（含完整本地路径）
  if (file.path) {
    const normalizedPath = file.path.replace(/\\/g, '/')
    formData.append('filePath', normalizedPath)
  }
  try {
    const res = await request.post('/detect/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === '200') {
      ElMessage.success('影像上传成功')
      loadRecordById(res.data.id)
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (error) {
    console.error('上传错误详情:', error)
    let msg = '上传失败'
    if (error?.status === 0) {
      msg = '无法连接到服务器，请确保后端服务已启动 (http://localhost:1907)'
    } else if (error?.message) {
      msg = '上传失败: ' + error.message
    }
    ElMessage.error(msg)
  }
}

// 上传成功
const handleUploadSuccess = (res) => {
  if (res.code === '200') {
    ElMessage.success('影像上传成功')
    loadRecordById(res.data.id)
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// 上传失败
const handleUploadError = (error) => {
  console.error('上传错误详情:', error)
  let msg = '上传失败'
  if (error?.status === 0) {
    msg = '无法连接到服务器，请确保后端服务已启动 (http://localhost:1907)'
  } else if (error?.message) {
    msg = '上传失败: ' + error.message
  }
  ElMessage.error(msg)
}

// 开始检测
const startDetection = async () => {
  if (!modelPath.value) {
    ElMessage.warning('请选择检测模型')
    return
  }

  // 普通上传模式
  if (!currentRecord.value?.id) {
    ElMessage.warning('请先上传待检测图像')
    return
  }

  detecting.value = true
  try {
    const res = await request.post(`/detect/startDetect/${currentRecord.value.id}`, null, {
      params: {
        ptPath: modelPath.value,
        conf: confThreshold.value
      }
    })
    if (res.code === '200') {
      ElMessage.success('检测完成')
      currentRecord.value = res.data
      // 自动下滑到检测结果
      scrollToResult()
    } else {
      ElMessage.error(res.msg || '检测失败')
    }
  } catch (error) {
    console.error('检测失败:', error)
    ElMessage.error(error.response?.data?.msg || '检测请求失败')
  } finally {
    detecting.value = false
  }
}

// 自动滚动到检测结果区域
const scrollToResult = () => {
  setTimeout(() => {
    if (resultRowRef.value) {
      resultRowRef.value.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
        ?? resultRowRef.value.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
    } else if (resultAreaRef.value) {
      resultAreaRef.value.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
        ?? resultAreaRef.value.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
    }
  }, 300)
}

// AI辅助分析
const startAiAnalysis = async () => {
  if (!selectedModel.value) {
    ElMessage.warning('请选择AI模型')
    return
  }
  
  if (!currentRecord.value?.id) {
    ElMessage.warning('请先选择检测记录')
    return
  }

  aiAnalyzing.value = true
  startAiProgress()
  try {
    const res = await request.post(
      `/detect/aiAnalysis/${currentRecord.value.id}?model=${selectedModel.value}`
    )
    if (res.code === '200') {
      completeAiProgress()
      ElMessage.success('AI大模型辅助分析报告已生成')
      currentRecord.value = res.data
    } else {
      stopAiProgress()
      ElMessage.error(res.msg || '分析失败')
    }
  } catch (error) {
    stopAiProgress()
    ElMessage.error('AI分析请求失败')
  } finally {
    aiAnalyzing.value = false
  }
}

// 导出检测报告为PDF
const exportReport = async () => {
  if (!currentRecord.value?.id) {
    ElMessage.warning('请先进行检测')
    return
  }

  exporting.value = true
  ElMessage.info('正在生成PDF报告，请稍后')

  try {
    const headerCanvas = createReportHeaderCanvas(currentRecord.value, detectionData.value)
    const headerImgData = headerCanvas.toDataURL('image/png')
    
    // 获取报告区域的DOM元素（对比图）
    const reportElement = document.querySelector('.image-compare-area')
    const canvas = await html2canvas(reportElement, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    // 创建PDF
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfWidth = pdf.internal.pageSize.getWidth()
    
    let currentY = 10

    // 添加标题区域到PDF
    const headerRatio = pdfWidth / headerCanvas.width
    const headerHeight = headerCanvas.height * headerRatio
    pdf.addImage(headerImgData, 'PNG', 0, currentY, pdfWidth, headerHeight)
    currentY += headerHeight + 5

    // 添加对比图到PDF，内容过长时自动分页，避免截断
    addCanvasToPdfPaged(pdf, canvas, {
      y: currentY,
      width: pdfWidth,
      marginTop: 10,
      marginBottom: 10
    })

    // 如果有AI分析结果，添加新页面
    if (currentRecord.value.aiAnalysisResult) {
      const aiCanvas = createAiAnalysisCanvas(currentRecord.value)
      pdf.addPage()
      addCanvasToPdfPaged(pdf, aiCanvas, {
        y: 10,
        width: pdfWidth,
        marginTop: 10,
        marginBottom: 10
      })
    }

    // 下载PDF
    const fileName = `皮肤癌检测分析报告_${currentRecord.value.id}_${new Date().toISOString().split('T')[0]}.pdf`
    pdf.save(fileName)

    ElMessage.success('PDF报告已生成')
  } catch (error) {
    console.error('导出PDF失败:', error)
    ElMessage.error('导出PDF失败: ' + error.message)
  } finally {
    exporting.value = false
  }
}

// 构建上传用PDF（compress=true时压缩以减小体积，quality 为图片质量 0~1）
const buildUploadPdf = (headerCanvas, mainCanvas, compressed, quality) => {
  if (quality === undefined) {
    quality = compressed ? 0.5 : 1.0
  }
  const headerFormat = compressed ? 'image/jpeg' : 'image/png'
  const imgFormat = compressed ? 'JPEG' : 'PNG'
  const jsCompression = compressed ? 'MEDIUM' : undefined

  const headerImgData = headerCanvas.toDataURL(headerFormat, quality)
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pdfWidth = pdf.internal.pageSize.getWidth()
  let currentY = 10

  const headerRatio = pdfWidth / headerCanvas.width
  const headerHeight = headerCanvas.height * headerRatio
  if (jsCompression) {
    pdf.addImage(headerImgData, imgFormat, 0, currentY, pdfWidth, headerHeight, undefined, jsCompression)
  } else {
    pdf.addImage(headerImgData, imgFormat, 0, currentY, pdfWidth, headerHeight)
  }
  currentY += headerHeight + 5

  addCanvasToPdfPaged(pdf, mainCanvas, {
    y: currentY,
    width: pdfWidth,
    marginTop: 10,
    marginBottom: 10,
    imgFormat,
    quality,
    jsCompression
  })

  if (currentRecord.value.aiAnalysisResult) {
    const aiCanvas = createAiAnalysisCanvas(currentRecord.value)
    pdf.addPage()
    addCanvasToPdfPaged(pdf, aiCanvas, {
      y: 10,
      width: pdfWidth,
      marginTop: 10,
      marginBottom: 10,
      imgFormat,
      quality,
      jsCompression
    })
  }

  return pdf
}

// 上传到飞书云文档
const uploadToFeishu = async () => {
  if (!currentRecord.value?.id) {
    ElMessage.warning('请先进行检测')
    return
  }

  const userToken = feishuToken.value.trim()

  uploadingToFeishu.value = true
  ElMessage.info('正在生成并上传PDF到飞书，请稍后')

  try {
    // 首先生成高质量PDF（与导出报告一致）
    const headerCanvas = createReportHeaderCanvas(currentRecord.value, detectionData.value)
    const reportElement = document.querySelector('.image-compare-area')
    if (!reportElement) {
      throw new Error('未找到报告区域，无法生成PDF')
    }

    // 飞书限制 20MB，优先压到 19MB 以内，留出少量表单/边界余量。
    const FEISHU_MAX_SIZE = 20 * 1024 * 1024
    const TARGET_SIZE = 19 * 1024 * 1024
    const compressionLevels = [
      { compressed: false, quality: 1.0, scale: 2, label: 'high' },
      { compressed: true, quality: 0.5, scale: 2, label: 'medium' },
      { compressed: true, quality: 0.25, scale: 2, label: 'low' },
      { compressed: true, quality: 0.15, scale: 1.5, label: 'very low' },
      { compressed: true, quality: 0.1, scale: 1, label: 'minimum' }
    ]

    let pdfBlob = null
    let canvas = null
    let prevScale = 0

    for (const level of compressionLevels) {
      if (!canvas || level.scale !== prevScale) {
        canvas = await html2canvas(reportElement, {
          scale: level.scale,
          useCORS: true,
          allowTaint: true,
          backgroundColor: '#ffffff',
          logging: false
        })
        prevScale = level.scale
      }

      const pdf = buildUploadPdf(headerCanvas, canvas, level.compressed, level.quality)
      const blob = pdf.output('blob')
      console.log(`PDF生成(${level.label}): ${(blob.size / 1024 / 1024).toFixed(1)}MB`)
      if (blob.size <= TARGET_SIZE) {
        pdfBlob = blob
        break
      }
      pdfBlob = blob
    }

    if (pdfBlob.size > FEISHU_MAX_SIZE) {
      const finalPdf = buildUploadPdf(headerCanvas, canvas, true, 0.08)
      pdfBlob = finalPdf.output('blob')
      console.warn(`PDF最低质量体积: ${(pdfBlob.size / 1024 / 1024).toFixed(1)}MB`)
    }

    if (pdfBlob.size > FEISHU_MAX_SIZE) {
      throw new Error(`PDF压缩后仍为 ${(pdfBlob.size / 1024 / 1024).toFixed(1)}MB，超过飞书20MB限制`)
    }

    const fileName = `皮肤癌检测分析报告_${currentRecord.value.id}_${new Date().toISOString().split('T')[0]}.pdf`

    const formData = new FormData()
    formData.append('file', pdfBlob, fileName)
    formData.append('fileName', fileName)
    formData.append('fileType', 'pdf')

    const res = await request.post('/feishu/upload-with-user-token', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-Token': userToken
      }
    })

    if (res.code === '200') {
      ElMessage.success(`上传成功！文件token: ${res.data.fileToken}`)
    } else {
      ElMessage.error(res.msg || '上传到飞书失败')
    }
  } catch (error) {
    console.error('上传到飞书失败:', error)
    ElMessage.error('上传到飞书失败: ' + error.message)
  } finally {
    uploadingToFeishu.value = false
  }
}

// 加载记录详情
const loadRecordById = async (id) => {
  if (!id) return
  try {
    const res = await request.get(`/detect/selectById/${id}`)
    if (res.code === '200') {
      currentRecord.value = res.data
    } else {
      ElMessage.error(res.msg || '加载记录失败')
    }
  } catch (error) {
    console.error('加载记录失败', error)
    ElMessage.error('加载记录失败')
  }
}

const loadRecordFromRoute = (recordId) => {
  if (recordId) {
    if (currentRecord.value?.id == recordId) return
    loadRecordById(recordId)
  } else if (shouldAutoRefreshPage('detect')) {
    currentRecord.value = null
  }
}

const resetPageState = () => {
  currentRecord.value = null
  detecting.value = false
  aiAnalyzing.value = false
  exporting.value = false
  uploadingToFeishu.value = false
  clearAiProgressTimer()
}

// 测试后端连接
const testConnection = async () => {
  try {
    const res = await request.get('/detect/health')
    if (res.code === '200') {
      ElMessage.success('后端连接正常: ' + res.data)
    } else {
      ElMessage.warning('后端响应异常: ' + res.msg)
    }
  } catch (error) {
    console.error('连接测试失败:', error)
    ElMessage.error('无法连接到后端服务，请确保: 1)后端已启动 2)端口907可用')
  }
}

// ==================== 工具函数 ====================

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return new Date(datetime).toLocaleString('zh-CN')
}

// 获取模型名称：D:\...\YOLOv11\weights\best.pt -> YOLOv11
const getModelName = (fullPath) => {
  if (!fullPath) return '-'
  const parts = String(fullPath).split(/[\\/]/).filter(Boolean)
  if (!parts.length) return '-'

  const fileName = parts[parts.length - 1]
  const parentName = parts[parts.length - 2]
  const grandParentName = parts[parts.length - 3]
  const lowerFileName = fileName.toLowerCase()

  if ((lowerFileName === 'best.pt' || lowerFileName === 'last.pt') && parentName) {
    return parentName.toLowerCase() === 'weights' && grandParentName ? grandParentName : parentName
  }

  return fileName
}

// 加载AI模型配置状态
const loadAiConfigStatus = async () => {
  try {
    const res = await request.get('/detect/aiConfigStatus')
    if (res.code === '200') {
      const status = res.data
      aiModelOptions.value = [
        { label: AI_MODEL_LABELS.deepseek, value: 'deepseek', configured: status.deepseek },
        { label: AI_MODEL_LABELS.glm, value: 'glm', configured: status.glm },
        { label: AI_MODEL_LABELS.kimi, value: 'kimi', configured: status.kimi }
      ]
      // 自动选择第一个已配置的模型
      const firstConfigured = aiModelOptions.value.find(m => m.configured)
      if (firstConfigured) {
        selectedModel.value = firstConfigured.value
      }
    }
  } catch (error) {
    console.error('加载AI配置状态失败', error)
  }
}

// 加载/保存飞书 Token
const loadFeishuToken = async ({ silent = true } = {}) => {
  try {
    const res = await request.get('/feishu/user-token')
    if (res.code === '200') {
      feishuToken.value = res.data?.token || ''
      return
    }
    if (!silent) {
      ElMessage.warning(res.msg || '加载飞书 Token 失败')
    }
  } catch (error) {
    console.error('加载飞书 Token 失败', error)
    if (!silent) {
      ElMessage.error('加载飞书 Token 失败')
    }
  }
}

const saveFeishuToken = async () => {
  try {
    const token = feishuToken.value.trim()
    const res = await request.put('/feishu/user-token', { token })
    if (res.code === '200') {
      feishuToken.value = token
      ElMessage.success('飞书 Token 已保存')
      feishuConfigExpanded.value = []
      return
    }
    ElMessage.error(res.msg || '保存飞书 Token 失败')
  } catch (error) {
    console.error('保存飞书 Token 失败', error)
    ElMessage.error('保存飞书 Token 失败')
  }
}

// 加载可用模型列表
const loadModels = async () => {
  modelLoading.value = true
  try {
    const configRes = await fetchModels('detect')
    if (configRes.code !== '200') {
      ElMessage.warning('加载模型列表失败: ' + configRes.msg)
      return
    }

    modelOptions.value = formatModelOptions(configRes.data || [])

    if (modelOptions.value.length > 0 && (!modelPath.value || !modelOptions.value.some((item) => item.value === modelPath.value))) {
      modelPath.value = modelOptions.value[0].value
    } else if (modelOptions.value.length === 0) {
      modelPath.value = ''
    }
  } catch (error) {
    console.error('加载模型列表失败', error)
    modelOptions.value = []
    modelPath.value = ''
    ElMessage.error('无法加载模型列表，请检查后端服务')
  } finally {
    modelLoading.value = false
  }
}

const loadDefaultThreshold = async () => {
  try {
    confThreshold.value = await getThreshold('detect', 0.55)
  } catch (error) {
    console.warn('加载默认阈值失败，使用本地默认值', error)
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  hasMounted = true
  loadFeishuToken()
  loadRecordFromRoute(route.query.recordId)
  loadModels()
  loadDefaultThreshold()
  loadAiConfigStatus()
  stopModelConfigListener = onModelConfigChanged(({ pageType }) => {
    if (!pageType || pageType === 'detect') {
      loadModels()
    }
  })
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onActivated(() => {
  if (!hasMounted || !shouldAutoRefreshPage('detect')) return
  resetPageState()
  loadFeishuToken()
  loadRecordFromRoute(route.query.recordId)
  loadModels()
  loadDefaultThreshold()
  loadAiConfigStatus()
})

onUnmounted(() => {
  clearAiProgressTimer()
  stopModelConfigListener?.()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

// detectionData 变化时重绘检测框叠加层
watch(
  () => route.query.recordId,
  (recordId) => loadRecordFromRoute(recordId)
)

const handleVisibilityChange = () => {
  if (!document.hidden) {
    loadModels()
  }
}
</script>

<style scoped>
/* ===== 医疗AI主题 - 皮肤癌检测页面样式 ===== */

.detect-container {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: var(--app-bg);
}

/* ===== 页面头部 - 医疗科技风格 ===== */
.page-header {
  margin-bottom: 24px;
  padding: 24px 28px;
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow);
  position: relative;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2A7D8C 0%, #D9747E 70%, #D4944F 100%);
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: -0.3px;
}

.subtitle {
  color: var(--app-text-muted);
  margin: 0;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.2px;
}

/* ===== 操作区域 - 白色卡片 ===== */
.operation-area {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
  padding: 18px 20px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  gap: 14px;
}

.operation-row {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px;
  overflow-x: auto;
}

.report-operation-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.operation-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, var(--app-border) 20%, var(--app-border) 80%, transparent 100%);
  margin: 2px 0;
}

/* ===== 选择器样式 ===== */
.model-select {
  width: 200px;
  flex: 0 0 200px;
}

.threshold-input {
  width: 120px;
  flex: 0 0 120px;
}

.ai-select {
  width: 170px;
  flex: 0 0 170px;
  margin-left: 0;
}

.control-button {
  flex: 0 0 auto;
  margin-left: 0;
}

.upload-component {
  flex: 0 0 auto;
}

.ai-option-label {
  float: left;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ai-option-tag {
  float: right;
  margin-left: 10px;
}

.ai-model-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 20px;
  overflow: hidden;
  color: #ffffff;
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
}

.ai-model-icon img {
  width: 88%;
  height: 88%;
  object-fit: contain;
  display: block;
}

.ai-model-icon--deepseek {
  background: #ffffff;
  border: 1px solid #d8e2e7;
}

.ai-model-icon--kimi {
  background: #5b5fc7;
}

.ai-model-icon--glm {
  background: #2f8f5b;
}

.ai-model-icon--doubao {
  background: #c2413b;
}

/* ===== AI进度面板 - 医疗冷光蓝 ===== */
.ai-progress-panel {
  margin-bottom: 24px;
  padding: 18px 22px;
  background: linear-gradient(135deg, #E8F4F7 0%, #F0F8FA 100%);
  border: 1px solid #C5E4EB;
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  position: relative;
}

.ai-progress-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2A7D8C 0%, #5BA8B8 100%);
  border-radius: var(--app-radius) 0 0 var(--app-radius);
}

.ai-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #1D6370;
  font-weight: 600;
}

.ai-progress-header strong {
  color: #2A7D8C;
  font-size: 18px;
}

/* ===== 图像对比区域 ===== */
.image-compare-area {
  margin-bottom: 24px;
}

/* 左右分栏布局 */
.split-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.split-panel {
  flex: 1;
  min-width: 0;
  width: 50%;
}

.image-card {
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  overflow: hidden;
  height: 100%;
  transition: all 0.3s ease;
}

.image-card:hover {
  box-shadow: var(--app-shadow-strong);
  border-color: var(--app-border-strong);
}

.image-title {
  padding: 14px 20px;
  background: linear-gradient(180deg, #F7FAFC 0%, #EDF4F7 100%);
  color: var(--app-text);
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid var(--app-border);
  display: flex;
  align-items: center;
}

.title-status {
  margin-left: 8px;
}

.image-wrapper {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FAFCFD;
  padding: 24px;
}

.empty-detect-state {
  text-align: center;
  color: var(--app-text-muted);
}

.empty-detect-state p {
  margin: 8px 0;
}

.running-tip {
  color: var(--app-primary);
  margin-top: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.medical-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: var(--app-radius-sm);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.image-info {
  padding: 10px 20px;
  font-size: 12px;
  color: var(--app-text-muted);
  background: var(--app-surface-soft);
  border-top: 1px solid var(--app-border);
}

.image-info span {
  margin-right: 20px;
}

.detection-summary {
  padding: 16px 20px;
  border-top: 1px solid var(--app-border);
}

.defect-type-text {
  display: inline-block;
  color: var(--app-danger);
  font-weight: 600;
  line-height: 1.4;
}

/* ===== AI分析区域 ===== */
.ai-analysis-area {
  margin-bottom: 24px;
}

.ai-analysis-area :deep(.el-card) {
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  border: 1px solid var(--app-border);
}

.ai-analysis-area :deep(.el-card__header) {
  background: linear-gradient(180deg, #FDF0F1 0%, #FEF7F8 100%);
  border-bottom: 1px solid var(--app-border);
  padding: 14px 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.analysis-time {
  color: var(--app-text-muted);
  font-size: 12px;
}

.analysis-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.analysis-text {
  margin: 0;
  font-family: inherit;
  line-height: 1.8;
  color: var(--app-text);
  font-size: 14px;
}

.analysis-text p {
  min-height: 1.8em;
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.analysis-disclaimer {
  color: var(--app-danger);
  font-weight: 700;
  background: var(--app-danger-soft);
  padding: 8px 12px;
  border-radius: var(--app-radius-sm);
  margin-top: 8px !important;
}

.analysis-label {
  font-weight: 600;
  color: var(--app-text);
}

/* ===== Element 组件深度样式优化 ===== */
:deep(.el-button .el-icon) {
  margin-right: 6px;
}

:deep(.el-card__header) {
  background: var(--app-surface-soft);
  border-bottom-color: var(--app-border);
}

:deep(.el-card__body) {
  background: var(--app-surface);
}

/* ===== Element UI Tag 样式 ===== */
:deep(.el-tag--success) {
  background: var(--app-success-soft);
  border-color: #C8E6C9;
  color: var(--app-success);
}

:deep(.el-tag--warning) {
  background: var(--app-warning-soft);
  border-color: #F0D5B0;
  color: var(--app-warning);
}

:deep(.el-tag--danger) {
  background: var(--app-danger-soft);
  border-color: #E8B4B2;
  color: var(--app-danger);
}

/* ===== 描述列表样式 ===== */
:deep(.el-descriptions__label) {
  color: var(--app-text-secondary);
  font-weight: 500;
}

:deep(.el-descriptions__content) {
  color: var(--app-text);
}

/* ===== 空状态样式 ===== */
:deep(.el-empty__description) {
  color: var(--app-text-muted);
}

/* ===== 响应式适配 ===== */
@media (max-width: 900px) {
  .detect-container {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .operation-row {
    flex-wrap: wrap;
  }

  .image-wrapper {
    height: 300px;
    padding: 16px;
  }

  /* 小屏幕下左右分栏改为上下排列 */
  .split-layout {
    flex-direction: column;
  }

  .split-panel {
    width: 100%;
  }
}

/* ===== 数据集根目录配置区域 ===== */
.dataset-config-area {
  margin: 8px 0;
  max-width: 800px;
}

.dataset-config-area :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-muted);
  padding-left: 4px;
}

.dataset-config-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dataset-root-input {
  flex: 1;
}

.dataset-save-btn {
  flex-shrink: 0;
}

.dataset-config-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.4;
}

/* ===== 飞书 Token 配置区域 ===== */
.feishu-config-area {
  flex: 0 0 auto;
}

.feishu-config-area :deep(.el-collapse) {
  border: none;
}

.feishu-config-area :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-muted);
  padding-left: 4px;
  height: 32px;
  line-height: 32px;
  background: transparent;
  border: none;
}

.feishu-config-area :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.feishu-config-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 280px;
}

.feishu-token-input {
  flex: 1;
  max-width: 400px;
}

.feishu-save-btn {
  flex-shrink: 0;
}

.feishu-config-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.4;
}
</style>
