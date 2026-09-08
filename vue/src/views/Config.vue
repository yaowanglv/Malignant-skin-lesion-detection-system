<template>
  <div class="config-container">
    <div class="page-header">
      <div>
        <h2>系统配置</h2>
        <p class="subtitle">统一管理检测模型、默认阈值和服务地址</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadConfig">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <el-tab-pane label="图像检测配置" name="detect">
        <section class="config-section">
          <div class="section-title">
            <h3>页面返回刷新设置</h3>
            <span>关闭后，离开页面再返回时会保留待检测/已检测画面；开启后返回页面会自动刷新为空白初始状态。</span>
          </div>
          <div class="refresh-control">
            <span>返回图像检测页面时自动刷新</span>
            <el-switch v-model="pageAutoRefresh.detect" />
          </div>
        </section>

        <section class="config-section">
          <div class="section-title">
            <h3>模型文件夹配置</h3>
            <span>扫描 .pt / .pth / .onnx / .engine / .trt 模型文件</span>
          </div>
          <div class="folder-row">
            <el-input
              v-model="folderInputs.detect"
              clearable
              placeholder="例如 D:/models/detect 或 /home/jetson/models/detect"
              :disabled="scanning.detect"
            />
            <el-button type="primary" :icon="FolderAdd" :loading="scanning.detect" @click="addFolder('detect')">
              添加并扫描
            </el-button>
          </div>
          <div v-if="scanStatus.detect.message" class="scan-status" :class="scanStatus.detect.type">
            {{ scanStatus.detect.message }}
          </div>
          <model-table
            :models="models.detect"
            :loading="loading"
            @delete="deleteModel"
            @rename="renameModel"
            @rescan="rescanFolder('detect', $event)"
          />
        </section>

        <threshold-section
          v-model="thresholds.detect"
          title="图像检测默认置信度阈值"
          description="用户打开图像检测页面时默认应用该阈值。"
        />
      </el-tab-pane>

      <el-tab-pane label="视频检测配置" name="video">
        <section class="config-section">
          <div class="section-title">
            <h3>页面返回刷新设置</h3>
            <span>关闭后，离开页面再返回时会保留待检测视频/已检测视频；开启后返回页面会自动刷新为空白初始状态。</span>
          </div>
          <div class="refresh-control">
            <span>返回视频检测页面时自动刷新</span>
            <el-switch v-model="pageAutoRefresh.video" />
          </div>
        </section>

        <section class="config-section">
          <div class="section-title">
            <h3>模型文件夹配置</h3>
            <span>视频检测可独立配置模型目录，支持 .pt / .pth / .onnx / .engine / .trt</span>
          </div>
          <div class="folder-row">
            <el-input
              v-model="folderInputs.video"
              clearable
              placeholder="例如 D:/models/video 或 /home/jetson/models/video"
              :disabled="scanning.video"
            />
            <el-button type="primary" :icon="FolderAdd" :loading="scanning.video" @click="addFolder('video')">
              添加并扫描
            </el-button>
          </div>
          <div v-if="scanStatus.video.message" class="scan-status" :class="scanStatus.video.type">
            {{ scanStatus.video.message }}
          </div>
          <model-table
            :models="models.video"
            :loading="loading"
            @delete="deleteModel"
            @rename="renameModel"
            @rescan="rescanFolder('video', $event)"
          />
        </section>

        <threshold-section
          v-model="thresholds.video"
          title="视频检测默认置信度阈值"
          description="用户打开视频检测页面时默认应用该阈值。"
        />
      </el-tab-pane>

      <el-tab-pane label="全局设置" name="global">
        <section class="config-section">
          <div class="section-title">
            <h3>医学图像剪辑页面刷新设置</h3>
            <span>关闭后，离开页面再返回时会保留已选图片数量、参数和生成的视频预览；开启后返回页面会自动刷新为空白初始状态。</span>
          </div>
          <div class="refresh-control">
            <span>返回医学图像剪辑页面时自动刷新</span>
            <el-switch v-model="pageAutoRefresh.imageToVideo" />
          </div>
        </section>

        <section class="config-section">
          <div class="section-title">
            <h3>服务地址配置</h3>
            <span>修改后建议刷新页面并重启相关服务确认生效</span>
          </div>
          <el-form ref="globalFormRef" :model="globalConfig" :rules="globalRules" label-position="top">
            <el-form-item label="后端 API 地址" prop="backendUrl">
              <el-input v-model="globalConfig.backendUrl" placeholder="http://localhost:1907" />
            </el-form-item>
            <el-form-item label="Python 检测服务地址" prop="pythonDetectUrl">
              <el-input v-model="globalConfig.pythonDetectUrl" placeholder="http://localhost:2026" />
            </el-form-item>
          </el-form>
        </section>
      </el-tab-pane>

      <el-tab-pane label="大模型配置" name="ai">
        <section class="config-section">
          <div class="section-title">
            <h3>AI 大模型 API Key 配置</h3>
            <span>配置后将优先使用数据库中保存的密钥，未配置时回退到 application.yml 默认值</span>
          </div>
          <el-form label-position="top">
            <el-form-item>
              <template #label>
                DeepSeek API Key
                <el-tag v-if="aiConfigStatus.deepseek" size="small" type="success" class="ai-status-tag">已配置</el-tag>
                <el-tag v-else size="small" type="info" class="ai-status-tag">未配置</el-tag>
              </template>
              <div class="ai-key-row">
                <el-input
                  v-model="aiConfig.deepseekApiKey"
                  type="password"
                  show-password
                  clearable
                  placeholder="输入新的 API Key 以覆盖，留空则保持不变"
                />
                <el-button
                  type="primary"
                  :loading="testingModel === 'deepseek'"
                  @click="handleTestAiConnection('deepseek')"
                >
                  测试连接
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <template #label>
                GLM（智谱）API Key
                <el-tag v-if="aiConfigStatus.glm" size="small" type="success" class="ai-status-tag">已配置</el-tag>
                <el-tag v-else size="small" type="info" class="ai-status-tag">未配置</el-tag>
              </template>
              <div class="ai-key-row">
                <el-input
                  v-model="aiConfig.glmApiKey"
                  type="password"
                  show-password
                  clearable
                  placeholder="输入新的 API Key 以覆盖，留空则保持不变"
                />
                <el-button
                  type="primary"
                  :loading="testingModel === 'glm'"
                  @click="handleTestAiConnection('glm')"
                >
                  测试连接
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <template #label>
                Kimi API Key
                <el-tag v-if="aiConfigStatus.kimi" size="small" type="success" class="ai-status-tag">已配置</el-tag>
                <el-tag v-else size="small" type="info" class="ai-status-tag">未配置</el-tag>
              </template>
              <div class="ai-key-row">
                <el-input
                  v-model="aiConfig.kimiApiKey"
                  type="password"
                  show-password
                  clearable
                  placeholder="输入新的 API Key 以覆盖，留空则保持不变"
                />
                <el-button
                  type="primary"
                  :loading="testingModel === 'kimi'"
                  @click="handleTestAiConnection('kimi')"
                >
                  测试连接
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </section>
      </el-tab-pane>
    </el-tabs>

    <div class="action-bar">
      <el-button :icon="RefreshLeft" @click="resetDefaults">重置默认值</el-button>
      <el-button type="primary" :icon="Check" :loading="saving" @click="saveConfig">保存配置</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElButton, ElMessage, ElMessageBox, ElSlider, ElTable, ElTableColumn, ElTag, ElTooltip, ElInputNumber, ElInput } from 'element-plus'
import { Check, Delete, Edit, FolderAdd, Refresh, RefreshLeft } from '@element-plus/icons-vue'
import {
  addModelFolder,
  emitModelConfigChanged,
  fetchAiConfig,
  fetchGlobalConfig,
  fetchModels,
  fetchThresholds,
  getPageAutoRefreshConfig,
  removeModel,
  rescanModelFolder,
  testAiConnection,
  updateAiConfig,
  updateGlobalConfig,
  updateModelName,
  updatePageAutoRefreshConfig,
  updateThresholds
} from '@/utils/config'
import { setBackendUrl } from '@/utils/request.js'

const ModelTable = defineComponent({
  name: 'ModelTable',
  props: {
    models: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false }
  },
  emits: ['delete', 'rename', 'rescan'],
  setup(props, { emit }) {
    const folders = computed(() => [...new Set(props.models.map((item) => item.folderPath).filter(Boolean))])
    const editingId = ref(null)
    const editingName = ref('')
    const startEdit = (row) => {
      editingId.value = row.id
      editingName.value = row.displayName || row.originalName || row.modelName || ''
    }
    const cancelEdit = () => {
      editingId.value = null
      editingName.value = ''
    }
    const saveEdit = (row) => {
      emit('rename', { row, displayName: editingName.value, done: cancelEdit })
    }
    return () => h('div', { class: 'model-table-wrap' }, [
      h(ElTable, { data: props.models, loading: props.loading, emptyText: '暂无模型配置' }, () => [
        h(ElTableColumn, { label: '自定义名称', minWidth: 210 }, {
          default: ({ row }) => editingId.value === row.id
            ? h('div', { class: 'rename-editor' }, [
              h(ElInput, {
                modelValue: editingName.value,
                size: 'small',
                maxlength: 100,
                showWordLimit: true,
                onKeydown: (event) => {
                  if (event.key === 'Enter') saveEdit(row)
                  if (event.key === 'Escape') cancelEdit()
                },
                'onUpdate:modelValue': (value) => {
                  editingName.value = value
                }
              }),
              h(ElButton, { size: 'small', type: 'primary', link: true, onClick: () => saveEdit(row) }, () => '保存'),
              h(ElButton, { size: 'small', link: true, onClick: cancelEdit }, () => '取消')
            ])
            : h('div', { class: 'model-name-cell' }, [
              h('span', { class: 'model-name-text' }, row.displayName || row.originalName || row.modelName || '-'),
              h(ElButton, {
                type: 'primary',
                link: true,
                icon: Edit,
                onClick: () => startEdit(row)
              }, () => '改名')
            ])
        }),
        h(ElTableColumn, { prop: 'originalName', label: '原名称', minWidth: 150, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'modelName', label: '文件名', minWidth: 150, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'folderPath', label: '对应文件夹', minWidth: 240, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'modelType', label: '类型', width: 86 }, {
          default: ({ row }) => h(ElTag, { size: 'small' }, () => row.modelType)
        }),
        h(ElTableColumn, { label: '状态', width: 88 }, {
          default: ({ row }) => h(ElTag, {
            size: 'small',
            type: row.exists === false ? 'danger' : 'success'
          }, () => row.exists === false ? '文件缺失' : '可用')
        }),
        h(ElTableColumn, { label: '操作', width: 96, fixed: 'right' }, {
          default: ({ row }) => h(ElButton, {
            type: 'danger',
            link: true,
            icon: Delete,
            onClick: () => emit('delete', row)
          }, () => '删除')
        })
      ]),
      folders.value.length ? h('div', { class: 'rescan-row' }, folders.value.map((folder) =>
        h(ElTooltip, { content: folder, placement: 'top' }, {
          default: () => h(ElButton, {
            link: true,
            type: 'primary',
            icon: Refresh,
            onClick: () => emit('rescan', folder)
          }, () => `重新扫描 ${shortPath(folder)}`)
        })
      )) : null
    ])
  }
})

const ThresholdSection = defineComponent({
  name: 'ThresholdSection',
  props: {
    modelValue: { type: Number, default: 0.55 },
    title: { type: String, default: '默认置信度阈值' },
    description: { type: String, default: '' }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const update = (value) => emit('update:modelValue', value)
    return () => h('section', { class: 'config-section threshold-section' }, [
      h('div', { class: 'section-title' }, [
        h('h3', props.title),
        h('span', props.description)
      ]),
      h('div', { class: 'threshold-control' }, [
        h(ElSlider, {
          modelValue: props.modelValue,
          min: 0.05,
          max: 0.95,
          step: 0.05,
          showStops: true,
          onInput: update,
          'onUpdate:modelValue': update
        }),
        h(ElInputNumber, {
          modelValue: props.modelValue,
          min: 0.05,
          max: 0.95,
          step: 0.05,
          precision: 2,
          controlsPosition: 'right',
          'onUpdate:modelValue': update
        })
      ]),
      h('div', { class: 'threshold-hint' }, '范围 0.05 到 0.95，步长 0.05。阈值越高，检测结果越严格。')
    ])
  }
})

const activeTab = ref('detect')
const loading = ref(false)
const saving = ref(false)
const scanning = reactive({ detect: false, video: false })
const scanStatus = reactive({
  detect: { type: '', message: '' },
  video: { type: '', message: '' }
})
const globalFormRef = ref()
const folderInputs = reactive({ detect: '', video: '' })
const models = reactive({ detect: [], video: [] })
const thresholds = reactive({ detect: 0.55, video: 0.6 })
const pageAutoRefresh = reactive(getPageAutoRefreshConfig())
const globalConfig = reactive({
  backendUrl: 'http://localhost:1907',
  pythonDetectUrl: 'http://localhost:2026'
})
const aiConfig = reactive({
  deepseekApiKey: '',
  glmApiKey: '',
  kimiApiKey: ''
})
const aiConfigStatus = reactive({
  deepseek: false,
  glm: false,
  kimi: false
})
const testingModel = ref('')

const urlRule = (_rule, value, callback) => {
  try {
    const parsed = new URL(value)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      callback(new Error('请输入 http:// 或 https:// 地址'))
      return
    }
    callback()
  } catch {
    callback(new Error('请输入有效地址'))
  }
}

const globalRules = {
  backendUrl: [{ required: true, message: '请输入后端 API 地址', trigger: 'blur' }, { validator: urlRule, trigger: 'blur' }],
  pythonDetectUrl: [{ required: true, message: '请输入 Python 检测服务地址', trigger: 'blur' }, { validator: urlRule, trigger: 'blur' }]
}

const shortPath = (path) => {
  const parts = String(path).split(/[\\/]/).filter(Boolean)
  if (parts.length <= 2) return path
  return `${parts.at(-2)}/${parts.at(-1)}`
}

const loadPageModels = async (pageType) => {
  const res = await fetchModels(pageType)
  if (res.code === '200') {
    models[pageType] = res.data || []
  }
}

const loadConfig = async () => {
  loading.value = true
  try {
    await Promise.all([loadPageModels('detect'), loadPageModels('video'), loadThresholds(), loadGlobalConfig(), loadAiConfig()])
  } catch (error) {
    console.error('加载配置失败', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

const loadThresholds = async () => {
  const res = await fetchThresholds()
  if (res.code === '200' && res.data) {
    thresholds.detect = Number(res.data.detect ?? 0.55)
    thresholds.video = Number(res.data.video ?? 0.6)
  }
}

const loadGlobalConfig = async () => {
  const res = await fetchGlobalConfig()
  if (res.code === '200' && res.data) {
    globalConfig.backendUrl = res.data.backendUrl || 'http://localhost:1907'
    globalConfig.pythonDetectUrl = res.data.pythonDetectUrl || 'http://localhost:2026'
  }
}

const loadAiConfig = async () => {
  const res = await fetchAiConfig()
  if (res.code === '200' && res.data) {
    aiConfigStatus.deepseek = !!res.data.deepseekApiKey
    aiConfigStatus.glm = !!res.data.glmApiKey
    aiConfigStatus.kimi = !!res.data.kimiApiKey
  }
  aiConfig.deepseekApiKey = ''
  aiConfig.glmApiKey = ''
  aiConfig.kimiApiKey = ''
}

const getAiConfigValueKey = (model) => `${model}ApiKey`

const getAiModelName = (model) => {
  const names = {
    deepseek: 'DeepSeek',
    glm: 'GLM',
    kimi: 'Kimi'
  }
  return names[model] || model
}

const handleTestAiConnection = async (model) => {
  const key = getAiConfigValueKey(model)
  if (!aiConfig[key] && !aiConfigStatus[model]) {
    ElMessage.warning('请先配置 API Key')
    return
  }
  testingModel.value = model
  try {
    const res = await testAiConnection(model, aiConfig[key] || null)
    if (res.code === '200') {
      ElMessage.success(`${getAiModelName(model)} 连接成功`)
    } else {
      ElMessage.error(res.msg || '连接失败')
    }
  } catch (error) {
    console.error('测试连接失败', error)
    ElMessage.error(error.response?.data?.msg || '连接失败')
  } finally {
    testingModel.value = ''
  }
}

const clearScanStatus = (pageType, delay = 3000) => {
  window.setTimeout(() => {
    scanStatus[pageType].type = ''
    scanStatus[pageType].message = ''
  }, delay)
}

const addFolder = async (pageType) => {
  const folderPath = folderInputs[pageType].trim()
  if (!folderPath) {
    ElMessage.warning('请输入模型文件夹路径')
    return
  }
  scanning[pageType] = true
  scanStatus[pageType].type = 'scanning'
  scanStatus[pageType].message = '正在扫描模型文件...'
  try {
    const res = await addModelFolder(pageType, folderPath)
    if (res.code === '200') {
      scanStatus[pageType].type = 'success'
      scanStatus[pageType].message = `扫描完成，发现 ${res.data?.length || 0} 个模型`
      ElMessage.success(`扫描完成，发现 ${res.data?.length || 0} 个模型`)
      folderInputs[pageType] = ''
      await loadPageModels(pageType)
      emitModelConfigChanged(pageType)
    } else {
      scanStatus[pageType].type = 'error'
      scanStatus[pageType].message = res.msg || '扫描失败'
      ElMessage.error(res.msg || '扫描失败')
    }
  } catch (error) {
    console.error('扫描失败', error)
    scanStatus[pageType].type = 'error'
    scanStatus[pageType].message = error.response?.data?.msg || '扫描失败'
    ElMessage.error(error.response?.data?.msg || '扫描失败')
  } finally {
    scanning[pageType] = false
    clearScanStatus(pageType)
  }
}

const rescanFolder = async (pageType, folderPath) => {
  try {
    await ElMessageBox.confirm('重新扫描会刷新该文件夹下的模型记录，是否继续？', '确认重新扫描', { type: 'warning' })
    scanning[pageType] = true
    scanStatus[pageType].type = 'scanning'
    scanStatus[pageType].message = '正在重新扫描模型文件...'
    const res = await rescanModelFolder(pageType, folderPath)
    if (res.code === '200') {
      scanStatus[pageType].type = 'success'
      scanStatus[pageType].message = `重新扫描完成，发现 ${res.data?.length || 0} 个模型`
      ElMessage.success(`重新扫描完成，发现 ${res.data?.length || 0} 个模型`)
      await loadPageModels(pageType)
      emitModelConfigChanged(pageType)
    } else {
      scanStatus[pageType].type = 'error'
      scanStatus[pageType].message = res.msg || '重新扫描失败'
      ElMessage.error(res.msg || '重新扫描失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新扫描失败', error)
      scanStatus[pageType].type = 'error'
      scanStatus[pageType].message = error.response?.data?.msg || '重新扫描失败'
    }
  } finally {
    scanning[pageType] = false
    clearScanStatus(pageType)
  }
}

const deleteModel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除模型配置“${row.displayName || row.modelName}”吗？不会删除物理文件。`, '确认删除', { type: 'warning' })
    const res = await removeModel(row.id)
    if (res.code === '200') {
      const pageType = row.pageType || activeTab.value
      models[pageType] = models[pageType].filter((item) => item.id !== row.id)
      emitModelConfigChanged(pageType)
      ElMessage.success('已删除模型配置')
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }
}

const renameModel = async ({ row, displayName, done }) => {
  const name = String(displayName || '').trim()
  if (!name) {
    ElMessage.warning('请输入自定义名称')
    return
  }
  try {
    const res = await updateModelName(row.id, name)
    if (res.code === '200') {
      const pageType = row.pageType || activeTab.value
      const target = models[pageType].find((item) => item.id === row.id)
      if (target) {
        target.displayName = name
      }
      done?.()
      emitModelConfigChanged(pageType)
      ElMessage.success('模型名称已更新')
    } else {
      ElMessage.error(res.msg || '更新模型名称失败')
    }
  } catch (error) {
    console.error('更新模型名称失败', error)
    ElMessage.error(error.response?.data?.msg || '更新模型名称失败')
  }
}

const resetDefaults = () => {
  thresholds.detect = 0.55
  thresholds.video = 0.6
  pageAutoRefresh.detect = false
  pageAutoRefresh.video = false
  pageAutoRefresh.imageToVideo = false
  globalConfig.backendUrl = 'http://localhost:1907'
  globalConfig.pythonDetectUrl = 'http://localhost:2026'
  aiConfig.deepseekApiKey = ''
  aiConfig.glmApiKey = ''
  aiConfig.kimiApiKey = ''
}

const saveConfig = async () => {
  if (activeTab.value === 'global') {
    const valid = await globalFormRef.value?.validate?.().catch(() => false)
    if (!valid) return
  }
  saving.value = true
  try {
    const tasks = [
      updateThresholds({ detect: thresholds.detect, video: thresholds.video }),
      updateGlobalConfig(globalConfig),
      updateAiConfig({
        deepseekApiKey: aiConfig.deepseekApiKey || null,
        glmApiKey: aiConfig.glmApiKey || null,
        kimiApiKey: aiConfig.kimiApiKey || null
      })
    ]
    const [thresholdRes, globalRes, aiRes] = await Promise.all(tasks)
    if (thresholdRes.code === '200' && globalRes.code === '200' && aiRes.code === '200') {
      setBackendUrl(globalConfig.backendUrl)
      updatePageAutoRefreshConfig(pageAutoRefresh)
      ElMessage.success('配置已保存')
      await loadConfig()
    } else {
      ElMessage.error(thresholdRes.msg || globalRes.msg || aiRes.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存配置失败', error)
    ElMessage.error(error.response?.data?.msg || '保存配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.config-container {
  min-height: calc(100vh - 60px);
  padding: 24px;
  background: var(--app-bg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
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
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(90deg, #2A7D8C 0%, #D9747E 70%, #D4944F 100%);
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: var(--app-text);
}

.subtitle {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.config-tabs {
  padding: 18px 20px 4px;
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
}

.config-section {
  margin-bottom: 18px;
  padding: 18px;
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
}

.section-title {
  margin-bottom: 14px;
}

.section-title h3 {
  margin: 0 0 5px;
  font-size: 16px;
  color: var(--app-text);
}

.section-title span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.folder-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.folder-row .el-input {
  flex: 1;
}

.model-table-wrap {
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  overflow: hidden;
}

.model-name-cell,
.rename-editor {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-name-text {
  min-width: 0;
  overflow: hidden;
  color: var(--app-text);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rename-editor .el-input {
  min-width: 120px;
  max-width: 260px;
}

.rescan-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--app-border);
  background: #ffffff;
}

.scan-status {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: var(--app-radius-sm);
  font-size: 13px;
}

.scan-status.scanning {
  color: #1d4ed8;
  background: #e0f2fe;
}

.scan-status.success {
  color: #15803d;
  background: #dcfce7;
}

.scan-status.error {
  color: #b91c1c;
  background: #fee2e2;
}

.threshold-control {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) 130px;
  gap: 18px;
  align-items: center;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
}

.threshold-hint {
  margin-top: 10px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.refresh-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  color: var(--app-text);
  font-size: 14px;
}

.ai-status-tag {
  margin-left: 8px;
}

.ai-key-row {
  display: flex;
  gap: 10px;
}

.ai-key-row .el-input {
  flex: 1;
}

.action-bar {
  position: sticky;
  bottom: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding: 14px 0 4px;
  background: linear-gradient(180deg, rgba(247, 250, 252, 0), var(--app-bg) 35%);
}

@media (max-width: 900px) {
  .config-container {
    padding: 16px;
  }

  .page-header,
  .folder-row,
  .ai-key-row {
    flex-direction: column;
  }

  .threshold-control {
    grid-template-columns: 1fr;
  }
}
</style>
