import request from '@/utils/request.js'

export const MODEL_CONFIG_CHANGED_EVENT = 'model-config-changed'
const MODEL_CONFIG_CHANGED_STORAGE_KEY = 'skin_model_config_changed'
export const PAGE_AUTO_REFRESH_CONFIG_CHANGED_EVENT = 'page-auto-refresh-config-changed'
const PAGE_AUTO_REFRESH_CONFIG_STORAGE_KEY = 'skin_page_auto_refresh_config'
const DEFAULT_PAGE_AUTO_REFRESH_CONFIG = {
  detect: false,
  video: false,
  imageToVideo: false
}

export const fetchModels = (pageType) => {
  return request.get('/config/models', { params: { pageType } })
}

export const addModelFolder = (pageType, folderPath) => {
  return request.post('/config/models/folder', { pageType, folderPath })
}

export const rescanModelFolder = (pageType, folderPath) => {
  return request.post('/config/models/scan', { pageType, folderPath })
}

export const removeModel = (id) => {
  return request.delete(`/config/models/${id}`)
}

export const updateModelName = (id, displayName) => {
  return request.put(`/config/models/${id}/name`, { displayName })
}

export const emitModelConfigChanged = (pageType) => {
  const detail = { pageType, updatedAt: Date.now() }
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(MODEL_CONFIG_CHANGED_EVENT, { detail }))
  localStorage.setItem(MODEL_CONFIG_CHANGED_STORAGE_KEY, JSON.stringify(detail))
}

export const onModelConfigChanged = (callback) => {
  if (typeof window === 'undefined') return () => {}

  const handleCustomEvent = (event) => callback(event.detail || {})
  const handleStorageEvent = (event) => {
    if (event.key !== MODEL_CONFIG_CHANGED_STORAGE_KEY || !event.newValue) return
    try {
      callback(JSON.parse(event.newValue))
    } catch {
      callback({})
    }
  }

  window.addEventListener(MODEL_CONFIG_CHANGED_EVENT, handleCustomEvent)
  window.addEventListener('storage', handleStorageEvent)

  return () => {
    window.removeEventListener(MODEL_CONFIG_CHANGED_EVENT, handleCustomEvent)
    window.removeEventListener('storage', handleStorageEvent)
  }
}

export const fetchThresholds = () => {
  return request.get('/config/thresholds')
}

export const updateThresholds = (thresholds) => {
  return request.put('/config/thresholds', thresholds)
}

export const fetchGlobalConfig = () => {
  return request.get('/config/global')
}

export const updateGlobalConfig = (config) => {
  return request.put('/config/global', config)
}

export const fetchAiConfig = () => {
  return request.get('/config/ai')
}

export const updateAiConfig = (config) => {
  return request.put('/config/ai', config)
}

export const testAiConnection = (model, apiKey) => {
  return request.post('/config/ai/test', { model, apiKey })
}

export const formatModelOptions = (models = []) => {
  return models
    .filter((model) => model && model.modelPath)
    .map((model) => ({
      label: model.displayName || model.originalName || model.modelName || model.modelPath,
      value: model.modelPath,
      exists: model.exists !== false,
      raw: model
    }))
}

export const getThreshold = async (pageType, defaultValue) => {
  const res = await fetchThresholds()
  if (res.code === '200' && res.data && res.data[pageType] !== undefined) {
    const value = Number(res.data[pageType])
    return Number.isFinite(value) ? value : defaultValue
  }
  return defaultValue
}

export const getPageAutoRefreshConfig = () => {
  if (typeof window === 'undefined') {
    return { ...DEFAULT_PAGE_AUTO_REFRESH_CONFIG }
  }

  try {
    const saved = JSON.parse(localStorage.getItem(PAGE_AUTO_REFRESH_CONFIG_STORAGE_KEY) || '{}')
    return {
      ...DEFAULT_PAGE_AUTO_REFRESH_CONFIG,
      ...saved
    }
  } catch {
    return { ...DEFAULT_PAGE_AUTO_REFRESH_CONFIG }
  }
}

export const updatePageAutoRefreshConfig = (config) => {
  if (typeof window === 'undefined') return
  const nextConfig = {
    ...DEFAULT_PAGE_AUTO_REFRESH_CONFIG,
    ...config
  }
  localStorage.setItem(PAGE_AUTO_REFRESH_CONFIG_STORAGE_KEY, JSON.stringify(nextConfig))
  window.dispatchEvent(new CustomEvent(PAGE_AUTO_REFRESH_CONFIG_CHANGED_EVENT, { detail: nextConfig }))
}

export const shouldAutoRefreshPage = (pageType) => {
  return !!getPageAutoRefreshConfig()[pageType]
}
