import { ElMessage } from 'element-plus'
import router from '../router'
import axios from "axios";

export const DEFAULT_BACKEND_URL = 'http://localhost:1907'
export const BACKEND_URL_STORAGE_KEY = 'skin_backend_url'

export const getBackendUrl = () => {
    const savedUrl = localStorage.getItem(BACKEND_URL_STORAGE_KEY)
    if (savedUrl === 'http://localhost:1234') {
        localStorage.setItem(BACKEND_URL_STORAGE_KEY, DEFAULT_BACKEND_URL)
        return DEFAULT_BACKEND_URL
    }
    return (savedUrl || DEFAULT_BACKEND_URL).replace(/\/$/, '')
}

export const setBackendUrl = (url) => {
    if (!url) return
    localStorage.setItem(BACKEND_URL_STORAGE_KEY, String(url).replace(/\/$/, ''))
}

const request = axios.create({
    // baseURL: import.meta.env.VITE_BASE_URL,
    baseURL: DEFAULT_BACKEND_URL,
    timeout: 30000  // 后台接口超时时间设置,30s
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
request.interceptors.request.use(config => {
    config.baseURL = getBackendUrl()
    // 如果不是文件上传(FormData)，则设置JSON content-type
    if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config
}, error => {
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return response
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        // 当权限验证不通过的时候给出提示
        if (res.code === '401') {
            ElMessage.error(res.msg || '登录已过期，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('code_user');
            router.push("/login")
        }
        if (res.code === '403') {
            ElMessage.error(res.msg || '权限不足，无法访问该资源');
        }
        return res;
    },
        error => {
        console.log('err' + error)
        if (error.response?.status === 401) {
            ElMessage.error('登录已过期，请重新登录');
            localStorage.removeItem('token');
            localStorage.removeItem('code_user');
            router.push("/login")
        } else if (error.response?.status === 403) {
            ElMessage.error('权限不足，无法访问该资源');
        }
        return Promise.reject(error)
    }
)


export default request
