<template>
  <div class="dataview-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h2>数据可视化分析</h2>
          <p class="subtitle">检测数据统计与用户分析</p>
        </div>
        <div class="actions-section">
          <span class="update-time">数据更新于: {{ lastUpdateTime }}</span>
          <el-button 
            type="primary" 
            size="small" 
            :icon="Refresh" 
            :loading="loadingStatus.defectType || loadingStatus.userPrediction"
            @click="handleRefresh"
          >
            刷新数据
          </el-button>
        </div>
      </div>
    </div>

    <!-- 第一行：症状统计 + 用户占比 -->
    <el-row :gutter="20" class="row-section">
      <!-- 左侧：症状检测统计柱状图 -->
      <el-col :span="12">
        <el-card class="chart-card" v-loading="loadingStatus.defectType" element-loading-text="加载真实数据中...">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><Histogram /></el-icon>
                皮肤癌检测结果统计
                <el-tag class="status-tag" size="small" type="success">实时</el-tag>
              </span>
            </div>
          </template>
          <div ref="defectTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 右侧：用户预测占比饼图 -->
      <el-col :span="12">
        <el-card class="chart-card" v-loading="loadingStatus.userPrediction" element-loading-text="加载真实数据中...">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><PieChart /></el-icon>
                不同用户的预测占比
                <el-tag class="status-tag" size="small" type="success">实时</el-tag>
              </span>
            </div>
          </template>
          <div class="pie-chart-wrapper">
            <div ref="userPieChartRef" class="chart-container pie-chart"></div>
            <!-- 详细数值列表 -->
            <div class="pie-data-list">
              <div v-for="(item, index) in userPredictionData" :key="index" class="pie-data-item">
                <span class="user-name">{{ item.userName }}</span>
                <span class="user-count">{{ item.count }}次</span>
                <span class="user-percentage">({{ item.percentage }}%)</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：用户置信度 + 实时日志 -->
    <el-row :gutter="20" class="row-section">
      <!-- 左侧：用户平均置信度雷达图 -->
      <el-col :span="12">
        <el-card class="chart-card" v-loading="loadingStatus.userConfidence" element-loading-text="加载真实数据中...">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><Aim /></el-icon>
                不同用户间的平均置信度
                <el-tag class="status-tag" size="small" type="success">实时</el-tag>
              </span>
            </div>
          </template>
          <div class="radar-wrapper">
            <div ref="userRadarChartRef" class="chart-container radar-chart"></div>
            <!-- 置信度数值显示 -->
            <div class="confidence-list">
              <div v-for="(item, index) in userConfidenceData" :key="index" class="confidence-item">
                <div class="confidence-value">{{ item.value }}%</div>
                <div class="confidence-user">{{ item.name }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：实时预测信息表格 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><List /></el-icon>
                实时预测信息
              </span>
              <el-tag size="small" type="success">实时更新</el-tag>
            </div>
          </template>
          <el-table :data="realtimeLogs" class="realtime-table" height="320" v-loading="logLoading">
            <el-table-column prop="userName" label="用户名" width="100" />
            <el-table-column prop="modelName" label="权重模型" show-overflow-tooltip min-width="120">
              <template #default="{ row }">
                {{ getModelName(row.modelName) }}
              </template>
            </el-table-column>
            <el-table-column prop="confThreshold" label="最小阈值" width="90">
              <template #default="{ row }">
                {{ row.confThreshold?.toFixed(2) || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="aiModel" label="AI助手" width="100">
              <template #default="{ row }">
                <el-tag
                  v-if="formatAiModel(row.aiModel)"
                  size="small"
                  class="ai-model-tag"
                  :class="getAiModelTagClass(row.aiModel)"
                >
                  {{ formatAiModel(row.aiModel) }}
                </el-tag>
                <span v-else class="text-gray">未使用</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间戳" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：近十天预测趋势 -->
    <el-row class="row-section">
      <el-col :span="24">
        <el-card class="chart-card" v-loading="loadingStatus.dailyTrend" element-loading-text="加载真实数据中...">
          <template #header>
            <div class="card-header">
              <span>
                <el-icon><TrendCharts /></el-icon>
                近十天总预测量趋势
                <el-tag class="status-tag" size="small" type="success">实时</el-tag>
              </span>
            </div>
          </template>
          <div ref="dailyTrendChartRef" class="chart-container trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Histogram, PieChart, Aim, List, TrendCharts, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import * as echarts from 'echarts'

// ==================== 响应式数据 ====================

const defectTypeChartRef = ref(null)
const userPieChartRef = ref(null)
const userRadarChartRef = ref(null)
const dailyTrendChartRef = ref(null)

const defectTypeChart = ref(null)
const userPieChart = ref(null)
const userRadarChart = ref(null)
const dailyTrendChart = ref(null)

const userPredictionData = ref([])
const userConfidenceData = ref([])
const realtimeLogs = ref([])
const logLoading = ref(false)

// 加载状态
const loadingStatus = ref({
  defectType: false,
  userPrediction: false,
  userConfidence: false,
  dailyTrend: false,
  realtimeLogs: false
})

// 数据最后更新时间
const lastUpdateTime = ref('-')

// 定时器
let refreshTimer = null
let statsRefreshTimer = null

// ==================== 图表初始化 ====================

// 初始化症状类型柱状图
const initDefectTypeChart = (data) => {
  if (!defectTypeChartRef.value) return
  
  defectTypeChart.value = echarts.init(defectTypeChartRef.value)
  
  // 定义每个皮肤癌检测类型的渐变色
  const colorMap = {
    '正常': ['#2f8f5b', '#8fd0ad'],         // 绿色 - 正常
    '良性皮肤肿瘤': ['#2A7D8C', '#7BB8C7'],  // 医疗青 - 良性
    '恶性皮肤癌': ['#dc2626', '#f87171'],    // 红色 - 恶性
  }

  // 只显示皮肤癌相关类别
  const validCategories = ['正常', '良性皮肤肿瘤', '恶性皮肤癌']
  const filteredCategories = []
  const filteredValues = []
  
  data.categories.forEach((category, index) => {
    if (validCategories.includes(category)) {
      filteredCategories.push(category)
      filteredValues.push(data.values[index])
    }
  })
  
  // 为每个数据点配置颜色
  const seriesData = filteredCategories.map((category, index) => {
    const colors = colorMap[category] || ['#2A7D8C', '#7BB8C7']
    return {
      value: filteredValues[index],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: colors[0] },
          { offset: 1, color: colors[1] }
        ]),
        borderRadius: [8, 8, 0, 0],  // 顶部圆角
        shadowColor: 'rgba(0, 0, 0, 0.1)',
        shadowBlur: 8,
        shadowOffsetY: 3
      }
    }
  })
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { 
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(0, 0, 0, 0.05)'
        }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#dce6e3',
      borderWidth: 1,
      textStyle: {
        color: '#1A2D3D'
      },
      formatter: function(params) {
        const item = params[0]
        return `<div style="font-weight:bold;margin-bottom:5px;">${item.name}</div>
                <div style="display:flex;align-items:center;">
                  <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${item.color.colorStops ? item.color.colorStops[0].color : item.color};margin-right:8px;"></span>
                  检测数量：<b>${item.value}</b>
                </div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '5%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: filteredCategories,
      axisLabel: { 
        rotate: 0,
        fontSize: 13,
        fontWeight: 500,
        color: '#4A6577',
        interval: 0
      },
      axisLine: {
        lineStyle: {
          color: '#C5E4EB'
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: '检测个数',
      nameTextStyle: {
        color: '#7A95A8',
        fontSize: 12
      },
      axisLabel: {
        color: '#7A95A8'
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#E2EAF0',
          type: 'dashed'
        }
      }
    },
    series: [{
      data: seriesData,
      type: 'bar',
      barWidth: '55%',
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#3A9BAE' },
          { offset: 1, color: '#2A7D8C' }
        ])
      },
      label: {
        show: true,
        position: 'top',
        fontSize: 14,
        fontWeight: 'bold',
        color: '#1D6370',
        distance: 10
      },
      emphasis: {
        itemStyle: {
          shadowColor: 'rgba(42, 125, 140, 0.35)',
          shadowBlur: 12,
          shadowOffsetY: 5
        }
      }
    }]
  }
  
  defectTypeChart.value.setOption(option)
}

// 初始化用户占比饼图
const initUserPieChart = (data) => {
  if (!userPieChartRef.value) return
  
  userPieChart.value = echarts.init(userPieChartRef.value)
  
  const pieData = data.map(item => ({
    name: item.userName,
    value: item.count
  }))
  
  const option = {
    // 医疗科技主题配色
    color: ['#2A7D8C', '#D9747E', '#D4944F', '#5E8DA8', '#2F8F5B', '#7B8DB5'],
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'center'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      data: pieData
    }]
  }
  
  userPieChart.value.setOption(option)
}

// 初始化用户置信度雷达图
const initUserRadarChart = (data) => {
  if (!userRadarChartRef.value) return
  
  userRadarChart.value = echarts.init(userRadarChartRef.value)
  const confidenceValues = (data?.values || []).map(value => Number(value) || 0)
  const minConfidence = confidenceValues.length ? Math.min(...confidenceValues) : 0
  const maxConfidence = confidenceValues.length ? Math.max(...confidenceValues) : 100
  const radarMin = minConfidence >= 60 ? Math.max(0, Math.floor(minConfidence / 5) * 5 - 5) : 0
  const radarMax = maxConfidence >= 95 ? 100 : Math.min(100, Math.ceil(maxConfidence / 5) * 5 + 5)
  const radarIndicators = (data?.indicators || []).map(item => ({
    ...item,
    min: radarMin,
    max: radarMax
  }))
  
  const option = {
    tooltip: {
      formatter: (params) => {
        const values = params.value || []
        return values
          .map((value, index) => {
            const name = radarIndicators[index]?.name || `用户${index + 1}`
            return `${name}：${Number(value || 0).toFixed(2)}%`
          })
          .join('<br/>')
      }
    },
    radar: {
      indicator: radarIndicators,
      radius: '65%',
      center: ['50%', '50%'],
      splitNumber: 4,
      axisName: {
        color: '#52606d',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: ['#E8F4F7', '#C5E4EB', '#A3D3DD', '#7BB8C7']
        }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(232, 244, 247, 0.9)', 'rgba(197, 228, 235, 0.4)']
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: confidenceValues,
        name: '平均置信度',
        areaStyle: {
          color: 'rgba(42, 125, 140, 0.2)'
        },
        lineStyle: {
          color: '#2A7D8C',
          width: 2.5
        },
        itemStyle: {
          color: '#D9747E'
        }
      }]
    }]
  }
  
  userRadarChart.value.setOption(option)
  
  // 同步更新置信度列表数据
  userConfidenceData.value = (data?.indicators || []).map((item, index) => ({
    name: item.name,
    value: (confidenceValues[index] ?? 0).toFixed(2)
  }))
}

// 初始化日趋势曲线图
const initDailyTrendChart = (data) => {
  if (!dailyTrendChartRef.value) return
  
  dailyTrendChart.value = echarts.init(dailyTrendChartRef.value)
  const dates = data?.dates || []
  const counts = (data?.counts || []).map(count => Math.trunc(Number(count) || 0))
  const trendLineColor = new echarts.graphic.LinearGradient(0, 0, 1, 0, [
    { offset: 0, color: '#2A7D8C' },
    { offset: 1, color: '#3A9BAE' }
  ])
  
  const option = {
    tooltip: {
      trigger: 'item',
      borderColor: '#C5E4EB',
      axisPointer: {
        type: 'line',
        label: { backgroundColor: '#2A7D8C' }
      },
      formatter: (params) => {
        const value = Math.trunc(Number(params.value) || 0)
        return `${params.name}<br/>检测量：${value} 次`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value',
      name: '检测量',
      minInterval: 1,
      axisLabel: {
        formatter: (value) => Math.trunc(Number(value) || 0)
      }
    },
    series: [{
      name: '检测量',
      type: 'line',
      smooth: true,
      lineStyle: {
        width: 3,
        color: trendLineColor
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(197, 228, 235, 0.82)' },
          { offset: 0.55, color: 'rgba(42, 125, 140, 0.16)' },
          { offset: 1, color: 'rgba(197, 228, 235, 0.04)' }
        ])
      },
      data: counts,
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: {
        color: '#2A7D8C',
        borderColor: '#ffffff',
        borderWidth: 2
      },
      emphasis: {
        label: {
          show: true,
          position: 'top',
          color: '#2A7D8C',
          fontWeight: 700,
          formatter: (params) => Math.trunc(Number(params.value) || 0)
        }
      }
    }]
  }
  
  dailyTrendChart.value.setOption(option)
}

// ==================== 数据加载 ====================

// 更新最后更新时间
const updateLastUpdateTime = () => {
  lastUpdateTime.value = new Date().toLocaleString('zh-CN')
}

// 加载症状类型统计
const loadDefectTypeStats = async () => {
  loadingStatus.value.defectType = true
  try {
    const res = await request.get('/dataview/stats/defectType')
    if (res.code === '200') {
      nextTick(() => {
        initDefectTypeChart(res.data)
      })
    } else {
      ElMessage.warning('缺陷统计数据加载失败: ' + res.msg)
    }
  } catch (error) {
    console.error('加载缺陷统计失败', error)
    ElMessage.error('无法连接到服务器，请检查后端服务')
  } finally {
    loadingStatus.value.defectType = false
  }
}

// 加载用户预测占比
const loadUserPredictionStats = async () => {
  loadingStatus.value.userPrediction = true
  try {
    const res = await request.get('/dataview/stats/userPrediction')
    if (res.code === '200') {
      userPredictionData.value = res.data
      nextTick(() => {
        initUserPieChart(res.data)
      })
    } else {
      ElMessage.warning('用户预测数据加载失败: ' + res.msg)
    }
  } catch (error) {
    console.error('加载用户占比失败', error)
  } finally {
    loadingStatus.value.userPrediction = false
  }
}

// 加载用户置信度统计
const loadUserConfidenceStats = async () => {
  loadingStatus.value.userConfidence = true
  try {
    const res = await request.get('/dataview/stats/userConfidence')
    if (res.code === '200') {
      nextTick(() => {
        initUserRadarChart(res.data)
      })
    } else {
      ElMessage.warning('置信度数据加载失败: ' + res.msg)
    }
  } catch (error) {
    console.error('加载置信度统计失败', error)
  } finally {
    loadingStatus.value.userConfidence = false
  }
}

// 加载日趋势统计
const loadDailyTrend = async () => {
  loadingStatus.value.dailyTrend = true
  try {
    const res = await request.get('/dataview/stats/dailyTrend?days=10')
    if (res.code === '200') {
      nextTick(() => {
        initDailyTrendChart(res.data)
      })
    } else {
      ElMessage.warning('趋势数据加载失败: ' + res.msg)
    }
  } catch (error) {
    console.error('加载趋势数据失败', error)
  } finally {
    loadingStatus.value.dailyTrend = false
  }
}

// 加载实时日志
const loadRealtimeLogs = async () => {
  loadingStatus.value.realtimeLogs = true
  try {
    const res = await request.get('/dataview/logs/realtime?limit=20')
    if (res.code === '200') {
      realtimeLogs.value = res.data
    } else {
      ElMessage.warning('实时日志加载失败: ' + res.msg)
    }
  } catch (error) {
    console.error('加载实时日志失败', error)
  } finally {
    loadingStatus.value.realtimeLogs = false
  }
}

// 加载所有统计数据
const loadStatsData = async () => {
  await Promise.all([
    loadDefectTypeStats(),
    loadUserPredictionStats(),
    loadUserConfidenceStats(),
    loadDailyTrend()
  ])
  updateLastUpdateTime()
}

// 加载所有数据
const loadAllData = () => {
  loadStatsData()
  loadRealtimeLogs()
}

// 手动刷新数据
const handleRefresh = () => {
  ElMessage.info('正在刷新数据...')
  loadAllData().then(() => {
    ElMessage.success('数据刷新成功')
  })
}

// ==================== 工具函数 ====================

// 获取模型文件名
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

// 格式化AI模型名称
const formatAiModel = (model) => {
  if (!model) return ''
  const rawModel = String(model).trim().replace(/[.。]+$/, '')
  const key = rawModel.toLowerCase()
  const mapping = {
    'deepseek': 'Deepseek-V4',
    'deepseek-v4': 'Deepseek-V4',
    'glm': '',
    'kimi': 'Kimi-k2.6',
    'doubao': '豆包'
  }
  return Object.prototype.hasOwnProperty.call(mapping, key) ? mapping[key] : rawModel
}

const getAiModelTagClass = (model) => {
  const rawModel = String(model || '').trim().replace(/[.。]+$/, '')
  const key = rawModel.toLowerCase()
  if (key === 'deepseek' || key === 'deepseek-v4') return 'ai-model-deepseek'
  if (key === 'kimi') return 'ai-model-kimi'
  if (key === 'doubao') return 'ai-model-doubao'
  return 'ai-model-default'
}

// 格式化日期时间
const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return new Date(datetime).toLocaleString('zh-CN')
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadAllData()
  
  // 设置定时刷新统计数据（每60秒）
  statsRefreshTimer = setInterval(() => {
    loadStatsData()
  }, 60000)
  
  // 设置定时刷新实时日志（每30秒）
  refreshTimer = setInterval(() => {
    loadRealtimeLogs()
  }, 30000)
  
  // 窗口大小改变时重新渲染图表
  window.addEventListener('resize', () => {
    defectTypeChart.value?.resize()
    userPieChart.value?.resize()
    userRadarChart.value?.resize()
    dailyTrendChart.value?.resize()
  })
})

onUnmounted(() => {
  // 清除定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (statsRefreshTimer) {
    clearInterval(statsRefreshTimer)
  }
  
  // 销毁图表实例
  defectTypeChart.value?.dispose()
  userPieChart.value?.dispose()
  userRadarChart.value?.dispose()
  dailyTrendChart.value?.dispose()
})
</script>

<style scoped>
/* ===== 数据可视化页面 - 医疗AI主题 ===== */
.dataview-container {
  padding: 24px;
  min-height: calc(100vh - 68px);
  background: var(--app-bg);
}

.page-header {
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 20px 24px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
  position: relative;
  overflow: hidden;
}

.header-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2A7D8C 0%, #D9747E 70%, #D4944F 100%);
}

.title-section {
  text-align: left;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: -0.3px;
}

.subtitle {
  color: var(--app-text-muted);
  margin: 0;
  font-size: 13px;
  letter-spacing: 0.2px;
}

.actions-section {
  display: flex;
  align-items: center;
  gap: 14px;
}

.update-time {
  color: var(--app-text-muted);
  font-size: 12px;
  padding: 5px 12px;
  background: var(--app-surface-soft);
  border-radius: 20px;
  border: 1px solid var(--app-border);
}

.row-section {
  margin-bottom: 24px;
}

.chart-card {
  height: 100%;
  border-color: var(--app-border);
  box-shadow: var(--app-shadow);
  border-radius: var(--app-radius);
  transition: all 0.3s ease;
}

.chart-card:hover {
  box-shadow: var(--app-shadow-strong);
  border-color: var(--app-border-strong);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text);
  font-weight: 600;
}

.card-header .el-icon {
  margin-right: 6px;
  color: var(--app-primary);
}

.status-tag {
  margin-left: 8px;
}

.realtime-table {
  width: 100%;
}

.ai-model-tag {
  border: 0;
  font-weight: 600;
}

/* AI模型标签 */
.ai-model-deepseek {
  background: var(--app-primary-soft);
  color: var(--app-primary);
  border: 1px solid var(--app-primary-light);
}

.ai-model-kimi {
  background: var(--app-warning-soft);
  color: var(--app-warning);
  border: 1px solid #F0D5B0;
}

.ai-model-doubao {
  background: var(--app-success-soft);
  color: var(--app-success);
  border: 1px solid #C8E6C9;
}

.ai-model-default {
  background: #F5F5F5;
  color: #616161;
  border: 1px solid #E0E0E0;
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* 饼图布局 */
.pie-chart-wrapper {
  display: flex;
  align-items: center;
}

.pie-chart {
  flex: 1;
}

.pie-data-list {
  width: 150px;
  padding-left: 20px;
  border-left: 1px solid var(--app-border);
}

.pie-data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
}

.user-name {
  color: var(--app-text-muted);
}

.user-count {
  color: var(--app-primary);
  font-weight: 600;
}

.user-percentage {
  color: var(--app-text-muted);
  font-size: 12px;
}

/* 雷达图布局 */
.radar-wrapper {
  display: flex;
  align-items: center;
}

.radar-chart {
  flex: 1;
}

.confidence-list {
  width: 120px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.confidence-item {
  text-align: center;
}

.confidence-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--app-primary);
}

.confidence-user {
  font-size: 14px;
  color: var(--app-text-muted);
  margin-top: 4px;
}

/* 趋势图 */
.trend-chart {
  height: 280px;
}

/* 表格样式 */
.text-gray {
  color: var(--app-text-muted);
}

/* ===== Element UI 组件样式优化 ===== */
:deep(.el-card__header) {
  padding: 14px 20px;
  background: linear-gradient(180deg, #F7FAFC 0%, #EDF4F7 100%);
  border-bottom-color: var(--app-border);
}

:deep(.el-card__body) {
  padding: 20px;
}

/* 状态标签样式 */
:deep(.status-tag.el-tag--success) {
  background: var(--app-success-soft);
  border-color: #C8E6C9;
  color: var(--app-success);
}

@media (max-width: 900px) {
  .header-content,
  .actions-section {
    align-items: flex-start;
    flex-direction: column;
  }

  :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .pie-chart-wrapper,
  .radar-wrapper {
    align-items: stretch;
    flex-direction: column;
  }

  .pie-data-list,
  .confidence-list {
    width: 100%;
    padding-left: 0;
    border-left: 0;
  }
}
</style>
