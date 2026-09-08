<template>
  <div class="imagetovideo-container">
    <div class="page-header">
      <h2>医学图像智能剪辑</h2>
      <p class="subtitle">将批量医学图像智能合成为视频，用于后续检测分析</p>
    </div>

    <div class="operation-area">
      <el-form :model="batchConfig" label-width="132px" class="batch-image-form">
        <el-form-item label="选择图片文件夹">
          <div class="batch-folder-row">
            <el-button type="primary" @click="triggerFolderSelect">
              <el-icon><FolderOpened /></el-icon>
              选择文件夹
            </el-button>
            <input
              ref="folderInput"
              class="folder-input"
              type="file"
              webkitdirectory
              multiple
              accept="image/*"
              @change="handleFolderSelect"
            >
            <el-tag v-if="batchConfig.imageCount > 0" type="success">
              已选择 {{ batchConfig.imageCount }} 张图片
            </el-tag>
            <el-tag v-else type="info">未选择文件夹</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="每张图时长(秒)">
          <el-input-number
            v-model="batchConfig.durationPerImage"
            :min="0.1"
            :max="5"
            :step="0.1"
            :precision="1"
            class="batch-control"
          />
        </el-form-item>

        <el-form-item label="输出格式">
          <el-select v-model="batchConfig.outputFormat" class="batch-control">
            <el-option label="MP4" value="mp4" />
            <el-option label="AVI" value="avi" />
            <el-option label="WebM" value="webm" />
          </el-select>
        </el-form-item>

        <el-form-item label="分辨率">
          <el-select v-model="batchConfig.resolution" class="batch-control">
            <el-option label="原始尺寸" value="original" />
            <el-option label="1920 x 1080" value="1920x1080" />
            <el-option label="1280 x 720" value="1280x720" />
            <el-option label="640 x 480" value="640x480" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="batchConfig.imageCount > 0" label="预计信息">
          <el-descriptions :column="3" size="small" border class="batch-estimate">
            <el-descriptions-item label="图片数量">
              {{ batchConfig.imageCount }} 张
            </el-descriptions-item>
            <el-descriptions-item label="预计时长">
              {{ batchEstimatedDuration }} 秒
            </el-descriptions-item>
            <el-descriptions-item label="预计帧数">
              {{ batchConfig.imageCount }} 帧
            </el-descriptions-item>
          </el-descriptions>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="batchConverting"
            :disabled="batchConfig.imageCount === 0"
            @click="convertImagesToVideo"
          >
            <el-icon><VideoPlay /></el-icon>
            {{ batchConverting ? '正在转换...' : '开始转换' }}
          </el-button>
          <el-button v-if="batchPreviewUrl" @click="resetBatchConfig">
            重新配置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="batchPreviewUrl" ref="batchPreviewCardRef" class="batch-preview-card">
      <div class="batch-preview-header">
        <span>视频预览</span>
        <el-tag type="success">转换成功</el-tag>
      </div>
      <video
        ref="batchVideoPlayer"
        :src="batchPreviewUrl"
        class="batch-preview-video"
        controls
        autoplay
        muted
      >
        您的浏览器不支持视频播放
      </video>
      <el-descriptions :column="3" size="small" border class="batch-preview-meta">
        <el-descriptions-item label="视频时长">
          {{ batchPreviewDuration }} 秒
        </el-descriptions-item>
        <el-descriptions-item label="总帧数">
          {{ batchPreviewFrames }} 帧
        </el-descriptions-item>
        <el-descriptions-item label="图片数量">
          {{ batchConfig.imageCount }} 张
        </el-descriptions-item>
      </el-descriptions>
      <div ref="batchPreviewActionsRef" class="batch-preview-actions">
        <el-button type="success" size="large" @click="useBatchVideo">
          <el-icon><Check /></el-icon>
          使用此视频进行检测
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, FolderOpened, VideoPlay } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import { shouldAutoRefreshPage } from '@/utils/config.js'

defineOptions({ name: 'ImageToVideo' })

const router = useRouter()

const folderInput = ref(null)
const batchVideoPlayer = ref(null)
const batchPreviewCardRef = ref(null)
const batchPreviewActionsRef = ref(null)

const batchConfig = reactive({
  imageCount: 0,
  durationPerImage: 0.3,
  outputFormat: 'mp4',
  resolution: 'original',
  files: []
})

const batchConverting = ref(false)
const batchPreviewUrl = ref('')
const batchPreviewDuration = ref(0)
const batchPreviewFrames = ref(0)
const batchGeneratedRecordId = ref(null)
let hasMounted = false

const uploadData = computed(() => {
  const userStr = localStorage.getItem('code_user')
  const user = userStr ? JSON.parse(userStr) : {}
  return {
    userId: user.id || 1,
    userName: user.name || '管理员'
  }
})

const batchEstimatedDuration = computed(() => {
  return (batchConfig.imageCount * batchConfig.durationPerImage).toFixed(1)
})

const triggerFolderSelect = () => {
  folderInput.value?.click()
}

const handleFolderSelect = (event) => {
  const files = Array.from(event.target.files || [])
  const imageExts = ['jpg', 'jpeg', 'png', 'bmp', 'tiff', 'webp']
  const maxSize = 50 * 1024 * 1024

  const imageFiles = files
    .filter((file) => {
      const ext = String(file.name.split('.').pop() || '').toLowerCase()
      return imageExts.includes(ext) && file.size <= maxSize
    })
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }))

  if (imageFiles.length > 500) {
    batchConfig.files = imageFiles.slice(0, 500)
    batchConfig.imageCount = 500
    ElMessage.warning('一次最多处理500张图片，已自动取前500张')
  } else {
    batchConfig.files = imageFiles
    batchConfig.imageCount = imageFiles.length
  }

  batchPreviewUrl.value = ''
  batchPreviewDuration.value = 0
  batchPreviewFrames.value = 0
  batchGeneratedRecordId.value = null

  if (batchConfig.imageCount === 0) {
    ElMessage.warning('所选文件夹中未找到可用图片，或图片超过50MB限制')
  } else {
    ElMessage.success(`已识别 ${batchConfig.imageCount} 张图片`)
  }
}

const scrollBatchPreviewToVisualCenter = async () => {
  await nextTick()

  const previewCard = batchPreviewCardRef.value
  const actions = batchPreviewActionsRef.value
  if (!previewCard || !actions) return

  const visibilityPadding = 32
  const viewportHeight = window.innerHeight
  const maxScrollTop = Math.max(0, document.documentElement.scrollHeight - viewportHeight)
  const cardRect = previewCard.getBoundingClientRect()
  const actionsRect = actions.getBoundingClientRect()
  const previewTop = window.scrollY + cardRect.top
  const actionsBottom = window.scrollY + actionsRect.bottom
  const visualCenter = previewTop + (actionsBottom - previewTop) / 2

  const centeredTop = visualCenter - viewportHeight / 2
  const minTopForButton = actionsBottom - viewportHeight + visibilityPadding
  const targetTop = Math.min(maxScrollTop, Math.max(0, centeredTop, minTopForButton))
  window.scrollTo({ top: targetTop, behavior: 'smooth' })
}

const convertImagesToVideo = async () => {
  if (batchConfig.imageCount === 0) {
    ElMessage.warning('请先选择包含图片的文件夹')
    return
  }

  batchConverting.value = true
  try {
    const formData = new FormData()
    batchConfig.files.forEach((file) => {
      formData.append('images', file)
    })
    formData.append('durationPerImage', String(batchConfig.durationPerImage))
    formData.append('outputFormat', batchConfig.outputFormat)
    formData.append('resolution', batchConfig.resolution)
    formData.append('userId', String(uploadData.value.userId))
    formData.append('userName', uploadData.value.userName)

    const res = await request.post('/video/imagesToVideo', formData, {
      timeout: 300000
    })

    if (res.code === '200') {
      const data = res.data || {}
      batchPreviewUrl.value = data.videoUrl || data.originalVideoUrl || ''
      batchPreviewDuration.value = data.duration || 0
      batchPreviewFrames.value = data.totalFrames || 0
      batchGeneratedRecordId.value = data.recordId || data.id
      ElMessage.success('视频生成成功')

      await scrollBatchPreviewToVisualCenter()
      batchVideoPlayer.value?.play?.().catch(() => {})
    } else {
      ElMessage.error(res.msg || '转换失败')
    }
  } catch (error) {
    console.error('批量图片转视频失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '转换失败，请检查服务状态')
  } finally {
    batchConverting.value = false
  }
}

const useBatchVideo = () => {
  if (!batchGeneratedRecordId.value) {
    ElMessage.warning('请先生成视频')
    return
  }

  router.push({
    path: '/manager/video',
    query: { recordId: batchGeneratedRecordId.value }
  })

  ElMessage.success('已跳转到视频检测页面')
}

const resetBatchConfig = () => {
  batchConfig.imageCount = 0
  batchConfig.files = []
  batchConfig.durationPerImage = 0.3
  batchConfig.outputFormat = 'mp4'
  batchConfig.resolution = 'original'
  batchPreviewUrl.value = ''
  batchPreviewDuration.value = 0
  batchPreviewFrames.value = 0
  batchGeneratedRecordId.value = null
  if (folderInput.value) {
    folderInput.value.value = ''
  }
}

onMounted(() => {
  hasMounted = true
})

onActivated(() => {
  if (hasMounted && shouldAutoRefreshPage('imageToVideo')) {
    resetBatchConfig()
  }
})
</script>

<style scoped>
.imagetovideo-container {
  min-height: calc(100vh - 60px);
  padding: 24px;
  background: var(--app-bg);
}

.page-header {
  position: relative;
  margin-bottom: 24px;
  padding: 24px 28px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  box-shadow: var(--app-shadow);
}

.page-header::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  content: '';
  background: linear-gradient(90deg, #2A7D8C 0%, #D9747E 70%, #D4944F 100%);
}

.page-header h2 {
  margin: 0 0 8px;
  color: var(--app-text);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.subtitle {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
}

.operation-area {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
  padding: 18px 20px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
}

.batch-image-form {
  max-width: 760px;
}

.batch-folder-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.folder-input {
  display: none;
}

.batch-control {
  width: 220px;
}

.batch-estimate {
  width: 100%;
  max-width: 620px;
}

.batch-preview-card {
  margin-top: 18px;
  padding: 18px;
  background: #F7FAFC;
  border: 1px solid #C9E5D3;
  border-radius: var(--app-radius);
}

.batch-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  color: var(--app-text);
  font-weight: 600;
}

.batch-preview-video {
  display: block;
  width: 100%;
  max-width: 760px;
  max-height: 420px;
  margin: 0 auto;
  background: #000;
  border-radius: var(--app-radius-sm);
}

.batch-preview-meta {
  margin-top: 16px;
}

.batch-preview-actions {
  margin-top: 18px;
  text-align: center;
}

:deep(.el-button .el-icon) {
  margin-right: 6px;
}

@media (max-width: 900px) {
  .imagetovideo-container {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .batch-control {
    width: 100%;
    max-width: 220px;
  }
}
</style>
