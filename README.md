# AIGCCanvasFlow

基于 Vue 3 + VueFlow 的 AIGC 无限画布编辑器。

## 技术栈

- **Vue 3.5** + **Vite 5** + **Pinia 3**
- **@vue-flow/core 1.48** — 画布引擎
- **video.js** — 视频播放器
- **@ffmpeg/ffmpeg + @ffmpeg/util** — 前端视频处理（转码/拼接，懒加载 WASM）

## 已实现功能

### 画布

- 无限画布：平移、缩放、网格背景（线条 + 点阵双层）
- 地图式手势操作：单指/鼠标拖拽平移，双指捏合缩放，触控板双指滑动平移
- 框选节点：Shift + 拖拽
- 吸附网格：可配置网格大小
- 快捷键：`F` 居中视图 / `Ctrl+A` 全选 / `Delete` 删除选中
- 右键菜单：节点、边、空白区域各自的上下文操作
- 从工具栏拖拽节点到画布
- MiniMap 小地图

### 节点类型

| 节点 | 功能 |
|------|------|
| **文本节点** | 双击编辑，内容通过边传播到下游节点 |
| **图片节点** | 本地上传（JPG/PNG/GIF/WebP），hover 显示换图按钮 |
| **视频节点** | 本地上传（MP4/WebM/OGG/MOV），video.js 播放器 |
| **备注节点** | 便利贴样式，可自定义颜色 |
| **分组节点** | 可调整大小的容器，支持子节点跟随移动 |

### 视频帧预览

- 上传视频后自动解析，在播放器下方展示帧条带
- 间隔可选：每帧（1s）/ 15s / 30s / 自定义（最小 0.5s）
- 条带支持左右滑动，hover 帧显示时间戳
- 点击帧缩略图直接跳转播放器到对应时间点
- 每帧下方"**+图**"按钮：以视频原始分辨率重新截取该帧（PNG 无损），生成独立图片节点并自动连边

### 数据流

- 节点间连边，边自动显示上游节点的输出值（最多 30 字符）
- 视频帧生成的图片节点自动与源视频节点连边

### 节点通用操作

- 拖拽移动，调整位置
- 右键菜单：删除、复制节点
- NodeHeader 支持节点类型切换
- 选中高亮，Delete 键删除
- 多选后一键分组 / 解组

## 目录结构

```
AIGCCanvasFlow/
├── h5/                         # 主应用
│   ├── src/
│   │   ├── components/
│   │   │   ├── FlowCanvas.vue  # 画布主组件
│   │   │   ├── Toolbar.vue     # 左侧工具栏
│   │   │   ├── ContextMenu.vue # 右键菜单
│   │   │   └── nodes/
│   │   │       ├── TextNode.vue
│   │   │       ├── ImageNode.vue
│   │   │       ├── VideoNode.vue
│   │   │       ├── NoteNode.vue
│   │   │       ├── GroupNode.vue
│   │   │       ├── VideoFrameStrip.vue  # 帧条带组件
│   │   │       ├── NodeHeader.vue
│   │   │       └── NodeAddButton.vue
│   │   ├── composables/
│   │   │   ├── useVideoFrames.js  # Canvas 截帧 + 高清单帧捕获
│   │   │   └── useFFmpeg.js       # ffmpeg.wasm 封装（转码/拼接）
│   │   └── stores/
│   │       └── flowStore.js       # Pinia store，节点/边状态管理
│   └── vite.config.js
└── admin/                      # 管理后台（待开发）
```

## 快速开始

```bash
cd h5
npm install
npm run dev
```
